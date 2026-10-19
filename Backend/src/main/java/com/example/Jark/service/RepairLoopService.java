package com.example.Jark.service;

import com.example.Jark.entity.Plan;
import com.example.Jark.entity.Remediation;
import com.example.Jark.entity.VerificationResult;
import com.example.Jark.repository.ExecutionRepository;
import com.example.Jark.repository.RemediationRepository;
import com.example.Jark.repository.VerificationResultRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Manages the bounded repair loop for fixing minor verification errors (VXC-190).
 *
 * <p>The repair loop:
 * <ol>
 *   <li>Parses verification failures (compiler errors, failing tests) and classifies them.</li>
 *   <li>Provides ONLY relevant failure evidence back to the LLM agent for a targeted fix.</li>
 *   <li>Applies the correction ONLY within the approved scope (affectedFiles from the plan).</li>
 *   <li>Re-runs verification.</li>
 *   <li>Enforces a strict retry budget of {@value #MAX_RETRY_ATTEMPTS} attempts.</li>
 * </ol>
 *
 * <p>If the fix requires modifying a file outside the approved plan scope → STOP, ROLLBACK.
 * If the budget is exhausted → STOP, ROLLBACK.
 */
@Slf4j
@Service
public class RepairLoopService {

    private final VerificationResultRepository verificationResultRepository;
    private final RemediationRepository remediationRepository;
    private final ExecutionRepository executionRepository;
    private final VerificationService verificationService;
    private final ObjectMapper objectMapper;
    private final ChatClient chatClient;
    private final RollbackService rollbackService;

    public RepairLoopService(
            VerificationResultRepository verificationResultRepository,
            RemediationRepository remediationRepository,
            ExecutionRepository executionRepository,
            VerificationService verificationService,
            ObjectMapper objectMapper,
            ChatClient chatClient,
            RollbackService rollbackService) {
        this.verificationResultRepository = verificationResultRepository;
        this.remediationRepository = remediationRepository;
        this.executionRepository = executionRepository;
        this.verificationService = verificationService;
        this.objectMapper = objectMapper;
        this.chatClient = chatClient;
        this.rollbackService = rollbackService;
    }

    public RepairLoopService(
            VerificationResultRepository verificationResultRepository,
            RemediationRepository remediationRepository,
            ExecutionRepository executionRepository,
            VerificationService verificationService,
            ObjectMapper objectMapper,
            ChatClient chatClient) {
        this(verificationResultRepository, remediationRepository, executionRepository,
                verificationService, objectMapper, chatClient, null);
    }

    /**
     * Maximum repair attempts before giving up and triggering rollback.
     */
    public static final int MAX_RETRY_ATTEMPTS = 2;

    // ─── Repair outcome constants ───────────────────────────────────────────────

    /** Repair succeeded: verification now passes. */
    public static final String RETRY_SUCCESS = "RETRY_SUCCESS";

    /** Repair applied but verification still fails. */
    public static final String RETRY_FAILED = "RETRY_FAILED";

    /** Repair would expand scope beyond the approved plan — rejected. */
    public static final String SCOPE_EXPANSION_REJECTED = "SCOPE_EXPANSION_REJECTED";

    /** Retry budget exhausted — no more attempts allowed. */
    public static final String BUDGET_EXCEEDED = "BUDGET_EXCEEDED";

    // ─── Failure classification constants ───────────────────────────────────────

    /** Build/compilation error (javac, maven). */
    public static final String FAILURE_COMPILER_ERROR = "COMPILER_ERROR";

    /** Unit or integration test failure. */
    public static final String FAILURE_TEST_FAILURE = "TEST_FAILURE";

    /** Static analysis violation (Checkstyle, SpotBugs). */
    public static final String FAILURE_STATIC_ANALYSIS = "STATIC_ANALYSIS";

    /** Scope violation — unauthorized file modified or diff doesn't match plan. */
    public static final String FAILURE_SCOPE_VIOLATION = "SCOPE_VIOLATION";

    /** Failure type could not be determined. */
    public static final String FAILURE_UNKNOWN = "UNKNOWN";

    /**
     * Attempts to repair a failed verification within the bounded retry budget.
     *
     * <p>The caller is responsible for triggering rollback when this method returns
     * {@link #BUDGET_EXCEEDED} or {@link #SCOPE_EXPANSION_REJECTED}.
     *
     * @param verificationResultId the ID of the {@link VerificationResult} that failed
     * @return one of {@link #RETRY_SUCCESS}, {@link #RETRY_FAILED},
     *         {@link #SCOPE_EXPANSION_REJECTED}, or {@link #BUDGET_EXCEEDED}
     */
    @Transactional
    public String attemptRepair(Long verificationResultId) {
        log.info("Starting repair attempt for verification result ID {}", verificationResultId);

        VerificationResult verificationResult = verificationResultRepository.findById(verificationResultId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Verification result not found: " + verificationResultId));

        // Already passed — nothing to do.
        if (VerificationService.STATUS_PASSED.equals(verificationResult.getStatus())) {
            log.info("Verification result {} already passed — no repair needed", verificationResultId);
            return RETRY_SUCCESS;
        }

        // Enforce budget.
        int currentAttempts = verificationResult.getRepairAttemptCount() == null
                ? 0 : verificationResult.getRepairAttemptCount();
        if (currentAttempts >= MAX_RETRY_ATTEMPTS) {
            log.warn("Retry budget exhausted for verification result {} (attempts={})",
                    verificationResultId, currentAttempts);
            triggerRollbackIfConfigured(verificationResult, RollbackService.TRIGGER_REPAIR_BUDGET_EXCEEDED);
            return BUDGET_EXCEEDED;
        }

        // Classify the failure.
        String failureType = classifyFailure(verificationResult);
        log.info("Failure classified as: {}", failureType);

        // Scope violations are never repairable within the loop.
        if (FAILURE_SCOPE_VIOLATION.equals(failureType)) {
            log.warn("Scope violation detected in verification result {} — triggering rollback path",
                    verificationResultId);
            triggerRollbackIfConfigured(verificationResult, RollbackService.TRIGGER_SCOPE_VIOLATION);
            return SCOPE_EXPANSION_REJECTED;
        }

        // Non-repairable failure types.
        if (!isRepairable(failureType)) {
            log.warn("Failure type '{}' is not repairable for result {}", failureType, verificationResultId);
            verificationResult.incrementRepairAttemptCount();
            verificationResultRepository.save(verificationResult);
            return RETRY_FAILED;
        }

        // Retrieve the plan for scope enforcement.
        Plan plan = getPlanFromVerificationResult(verificationResult);
        if (plan == null) {
            log.error("Cannot retrieve plan from verification result {} — aborting repair", verificationResultId);
            verificationResult.incrementRepairAttemptCount();
            verificationResultRepository.save(verificationResult);
            return RETRY_FAILED;
        }

        // Retrieve workspace path from the associated remediation.
        String workspacePath = getWorkspacePath(verificationResult, plan);
        if (workspacePath == null) {
            log.error("Cannot retrieve workspace path for verification result {} — aborting repair",
                    verificationResultId);
            verificationResult.incrementRepairAttemptCount();
            verificationResultRepository.save(verificationResult);
            return RETRY_FAILED;
        }

        // Increment attempt count before applying (counts even failed attempts).
        int attemptNumber = verificationResult.incrementRepairAttemptCount();
        verificationResultRepository.save(verificationResult);
        log.info("Repair attempt #{} for verification result {}", attemptNumber, verificationResultId);

        try {
            // Generate and apply a targeted fix.
            String applyStatus = generateAndApplyFix(verificationResult, plan, workspacePath, failureType);

            if (SCOPE_EXPANSION_REJECTED.equals(applyStatus)) {
                log.warn("Repair fix for result {} would expand scope — rejecting", verificationResultId);
                triggerRollbackIfConfigured(verificationResult, RollbackService.TRIGGER_SCOPE_VIOLATION);
                return SCOPE_EXPANSION_REJECTED;
            }

            if (RETRY_FAILED.equals(applyStatus)) {
                log.warn("Fix generation or application failed for result {}", verificationResultId);
                return RETRY_FAILED;
            }

            // Re-run the full verification pipeline.
            log.info("Re-running verification after repair attempt #{} for result {}",
                    attemptNumber, verificationResultId);
            Remediation remediation = getRemediationFromVerificationResult(verificationResult, plan);
            if (remediation == null) {
                log.error("Cannot find associated remediation to re-run verification — aborting");
                return RETRY_FAILED;
            }

            VerificationResult newResult = verificationService.verifyRemediation(
                    remediation.getId(),
                    getContainerId(verificationResult),
                    workspacePath);

            if (VerificationService.STATUS_PASSED.equals(newResult.getStatus())) {
                log.info("Repair attempt #{} SUCCEEDED for verification result {}", attemptNumber, verificationResultId);
                return RETRY_SUCCESS;
            } else {
                log.warn("Repair attempt #{} failed — verification still failing for result {}",
                        attemptNumber, verificationResultId);
                if (attemptNumber >= MAX_RETRY_ATTEMPTS) {
                    triggerRollbackIfConfigured(verificationResult, RollbackService.TRIGGER_VERIFICATION_PERMANENT_FAILURE);
                }
                return RETRY_FAILED;
            }

        } catch (Exception e) {
            log.error("Repair attempt #{} threw an exception for result {}: {}",
                    attemptNumber, verificationResultId, e.getMessage(), e);
            return RETRY_FAILED;
        }
    }

    private void triggerRollbackIfConfigured(VerificationResult verificationResult, String triggerReason) {
        if (rollbackService != null) {
            try {
                Plan plan = getPlanFromVerificationResult(verificationResult);
                if (plan != null) {
                    Remediation remediation = getRemediationFromVerificationResult(verificationResult, plan);
                    if (remediation != null) {
                        rollbackService.rollbackWorkspace(remediation.getId(), triggerReason);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to trigger automatic rollback for verification result {}: {}",
                        verificationResult != null ? verificationResult.getId() : null, e.getMessage(), e);
            }
        }
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Failure classification
    // ────────────────────────────────────────────────────────────────────────────

    /**
     * Classifies the primary failure reason from the verification result's gate outcomes.
     * Scope violations are checked first because they must never enter the repair loop.
     */
    String classifyFailure(VerificationResult result) {
        // Scope gates checked first.
        if (Boolean.TRUE.equals(result.getUnauthorizedFilesDetected())) {
            return FAILURE_SCOPE_VIOLATION;
        }
        if (Boolean.TRUE.equals(result.getUnexpectedModifications())) {
            return FAILURE_SCOPE_VIOLATION;
        }
        if (!Boolean.TRUE.equals(result.getDiffMatchesPlan())) {
            return FAILURE_SCOPE_VIOLATION;
        }
        if (!Boolean.TRUE.equals(result.getIntendedFilesModified())) {
            // Intended files were not even touched — scope issue.
            return FAILURE_SCOPE_VIOLATION;
        }

        // Build failure?
        if (result.getStdout() != null && result.getStdout().contains("BUILD FAILURE")) {
            // Distinguish compiler errors from test failures.
            if (result.getStdout().contains("COMPILATION ERROR")
                    || result.getStdout().contains("ERROR] Failed to execute goal")
                        && result.getStdout().contains("compile")) {
                return FAILURE_COMPILER_ERROR;
            }
            if (result.getStdout().contains("Tests run:") && result.getStdout().contains("FAILURES")) {
                return FAILURE_TEST_FAILURE;
            }
            // Generic build failure — treat as compiler error.
            return FAILURE_COMPILER_ERROR;
        }
        if (!"SUCCESS".equals(result.getBuildStatus())) {
            return FAILURE_COMPILER_ERROR;
        }
        if (!"PASSED".equals(result.getTestStatus())) {
            return FAILURE_TEST_FAILURE;
        }
        if (!"PASSED".equals(result.getStaticAnalysisStatus())) {
            return FAILURE_STATIC_ANALYSIS;
        }

        return FAILURE_UNKNOWN;
    }

    /**
     * Returns {@code true} for failure types that the repair loop can attempt to fix.
     */
    boolean isRepairable(String failureType) {
        return FAILURE_COMPILER_ERROR.equals(failureType)
                || FAILURE_TEST_FAILURE.equals(failureType)
                || FAILURE_STATIC_ANALYSIS.equals(failureType);
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Fix generation and application
    // ────────────────────────────────────────────────────────────────────────────

    /**
     * Asks the LLM agent to propose a targeted fix for the classified failure, validates
     * that the fix stays within the approved scope, then writes the fix to disk.
     *
     * @return {@link #RETRY_SUCCESS} if applied, {@link #SCOPE_EXPANSION_REJECTED} if the
     *         proposed fix would touch files outside the plan scope, {@link #RETRY_FAILED}
     *         on any error.
     */
    private String generateAndApplyFix(
            VerificationResult result, Plan plan, String workspacePath, String failureType) {
        log.info("Generating targeted fix for failure type '{}' in workspace '{}'", failureType, workspacePath);

        // Build a concise failure summary to send to the LLM (never the full stdout).
        String failureEvidence = extractFailureEvidence(result, failureType);
        List<String> approvedFiles = extractApprovedFiles(plan);
        String approvedFilesText = String.join(", ", approvedFiles);

        String prompt = buildRepairPrompt(plan, failureType, failureEvidence, approvedFilesText);

        String llmResponse;
        try {
            llmResponse = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("LLM call failed during repair: {}", e.getMessage(), e);
            return RETRY_FAILED;
        }

        if (llmResponse == null || llmResponse.isBlank()) {
            log.warn("LLM returned empty response for repair — aborting");
            return RETRY_FAILED;
        }

        log.debug("LLM repair response received ({} chars)", llmResponse.length());

        // Parse the structured patch from the LLM response.
        List<FilePatch> patches;
        try {
            patches = parsePatches(llmResponse);
        } catch (Exception e) {
            log.error("Failed to parse LLM patch response: {}", e.getMessage(), e);
            return RETRY_FAILED;
        }

        if (patches.isEmpty()) {
            log.warn("LLM produced no actionable patches for failure type '{}' — aborting", failureType);
            return RETRY_FAILED;
        }

        // Scope check — every patched file must be in the approved list.
        for (FilePatch patch : patches) {
            if (!approvedFiles.isEmpty() && !approvedFiles.contains(patch.relativePath())) {
                log.warn("Repair patch targets '{}' which is NOT in the approved file list [{}] — scope expansion rejected",
                        patch.relativePath(), approvedFilesText);
                return SCOPE_EXPANSION_REJECTED;
            }
        }

        // Apply patches.
        for (FilePatch patch : patches) {
            try {
                applyPatch(workspacePath, patch);
                log.info("Applied repair patch to '{}'", patch.relativePath());
            } catch (IOException e) {
                log.error("Failed to apply patch to '{}': {}", patch.relativePath(), e.getMessage(), e);
                return RETRY_FAILED;
            }
        }

        return RETRY_SUCCESS;
    }

    /**
     * Extracts a concise failure summary from the verification output for the LLM prompt.
     * Only relevant error lines are included — not the full stdout — to keep context tight.
     */
    private String extractFailureEvidence(VerificationResult result, String failureType) {
        if (result.getStdout() == null) {
            return result.getVerificationLog() != null ? result.getVerificationLog() : "Unknown failure";
        }

        String[] lines = result.getStdout().split("\n");
        List<String> errorLines = new ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("[ERROR]")
                    || trimmed.startsWith("ERROR:")
                    || trimmed.contains("COMPILATION ERROR")
                    || (FAILURE_TEST_FAILURE.equals(failureType)
                            && (trimmed.contains("FAILED") || trimmed.contains("AssertionError")
                                    || trimmed.contains("at com.example")))) {
                errorLines.add(trimmed);
                if (errorLines.size() >= 40) {
                    break;
                }
            }
        }

        if (errorLines.isEmpty() && result.getStderr() != null) {
            String stderr = result.getStderr();
            return stderr.length() > 1500 ? stderr.substring(0, 1500) + "\n...[truncated]" : stderr;
        }

        return String.join("\n", errorLines);
    }

    /**
     * Builds the repair prompt sent to the LLM.
     * The prompt instructs the agent to return ONLY a structured JSON array of patches.
     */
    private String buildRepairPrompt(
            Plan plan, String failureType, String failureEvidence, String approvedFiles) {
        return """
                You are Jark's targeted repair agent. A bounded remediation was applied but
                verification failed. Your task is to propose a minimal, targeted fix.

                STRICT RULES:
                1. You MUST NOT modify any file outside the approved list.
                2. You MUST NOT add new files.
                3. You MUST NOT change business logic — only fix the specific verification failure.
                4. Return ONLY a JSON array of file patches. Do not include any explanation.

                APPROVED FILES (the ONLY files you may modify):
                %s

                FAILURE TYPE: %s

                FAILURE EVIDENCE:
                %s

                ORIGINAL PLAN DESCRIPTION:
                %s

                ORIGINAL PROPOSED CHANGES:
                %s

                Return your response as a JSON array in EXACTLY this format:
                [
                  {
                    "relativePath": "src/main/java/com/example/Foo.java",
                    "newContent": "<full new file content>"
                  }
                ]

                If you cannot fix the failure without expanding scope, return an empty array: []
                """.formatted(
                approvedFiles,
                failureType,
                failureEvidence,
                plan.getDescription() != null ? plan.getDescription() : "",
                plan.getProposedChanges() != null ? plan.getProposedChanges() : "");
    }

    /**
     * Parses the LLM's JSON patch response into a list of {@link FilePatch} objects.
     * Accepts responses that have the JSON array embedded in prose by scanning for the
     * first {@code [} character.
     */
    List<FilePatch> parsePatches(String llmResponse) throws Exception {
        // Trim prose before/after the JSON array if present.
        int start = llmResponse.indexOf('[');
        int end = llmResponse.lastIndexOf(']');
        if (start == -1 || end == -1 || start > end) {
            return Collections.emptyList();
        }
        String jsonFragment = llmResponse.substring(start, end + 1);

        List<Map<String, String>> raw = objectMapper.readValue(jsonFragment,
                new TypeReference<List<Map<String, String>>>() {});

        List<FilePatch> patches = new ArrayList<>();
        for (Map<String, String> entry : raw) {
            String relativePath = entry.get("relativePath");
            String newContent = entry.get("newContent");
            if (relativePath != null && newContent != null) {
                patches.add(new FilePatch(relativePath.trim(), newContent));
            }
        }
        return patches;
    }

    /**
     * Writes a patch's new content to the correct file in the workspace.
     */
    private void applyPatch(String workspacePath, FilePatch patch) throws IOException {
        Path targetFile = Paths.get(workspacePath, patch.relativePath());
        // Ensure parent directories exist (file must already exist for valid repairs).
        if (!Files.exists(targetFile)) {
            throw new IOException("Target file does not exist (scope expansion guard): " + targetFile);
        }
        Files.writeString(targetFile, patch.newContent());
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Helpers for entity navigation
    // ────────────────────────────────────────────────────────────────────────────

    /**
     * Retrieves the {@link Plan} associated with a verification result via
     * {@code VerificationResult → Execution → Plan}.
     */
    Plan getPlanFromVerificationResult(VerificationResult result) {
        try {
            if (result.getExecution() != null && result.getExecution().getPlan() != null) {
                return result.getExecution().getPlan();
            }
            // Fallback: look up the execution explicitly to handle lazy-load scenarios.
            if (result.getExecution() != null) {
                return executionRepository.findById(result.getExecution().getId())
                        .map(exec -> exec.getPlan())
                        .orElse(null);
            }
        } catch (Exception e) {
            log.error("Failed to retrieve plan from verification result: {}", e.getMessage(), e);
        }
        return null;
    }

    /**
     * Retrieves the workspace path from the most recent remediation for the plan.
     */
    private String getWorkspacePath(VerificationResult result, Plan plan) {
        try {
            List<Remediation> remediations = remediationRepository.findCompletedByPlanId(plan.getId());
            if (!remediations.isEmpty()) {
                return remediations.get(0).getWorkspacePath();
            }
        } catch (Exception e) {
            log.error("Failed to retrieve workspace path: {}", e.getMessage(), e);
        }
        return null;
    }

    /**
     * Retrieves the most recently completed {@link Remediation} for the plan.
     */
    private Remediation getRemediationFromVerificationResult(VerificationResult result, Plan plan) {
        List<Remediation> remediations = remediationRepository.findCompletedByPlanId(plan.getId());
        return remediations.isEmpty() ? null : remediations.get(0);
    }

    /**
     * Extracts the Docker container ID from the verification result's execution entity.
     * Returns a sentinel value if unavailable (sandbox will use a new container).
     */
    private String getContainerId(VerificationResult result) {
        try {
            if (result.getExecution() != null) {
                return result.getExecution().getDockerContainerId();
            }
        } catch (Exception e) {
            log.warn("Could not retrieve container ID from verification result: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Extracts the list of approved (affected) files from the plan.
     */
    List<String> extractApprovedFiles(Plan plan) {
        try {
            if (plan.getAffectedFiles() != null && !plan.getAffectedFiles().isBlank()) {
                return objectMapper.readValue(plan.getAffectedFiles(), new TypeReference<List<String>>() {});
            }
        } catch (Exception e) {
            log.error("Failed to parse affected files from plan: {}", e.getMessage());
        }
        return Collections.emptyList();
    }

    /**
     * Returns the maximum number of repair attempts.
     */
    public int getRetryBudget() {
        return MAX_RETRY_ATTEMPTS;
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Inner types
    // ────────────────────────────────────────────────────────────────────────────

    /**
     * A targeted file patch proposed by the LLM.
     *
     * @param relativePath workspace-relative file path (e.g. {@code src/main/java/...})
     * @param newContent   full new file content to write
     */
    record FilePatch(String relativePath, String newContent) {}
}
