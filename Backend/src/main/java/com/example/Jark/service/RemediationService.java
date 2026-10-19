package com.example.Jark.service;

import com.example.Jark.entity.Approval;
import com.example.Jark.entity.Plan;
import com.example.Jark.entity.Remediation;
import com.example.Jark.repository.PlanRepository;
import com.example.Jark.repository.RemediationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages bounded remediation of approved engineering plans.
 * Applies changes safely with scope validation and rollback capability.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RemediationService {

    private final RemediationRepository remediationRepository;
    private final PlanRepository planRepository;
    private final ApprovalService approvalService;
    private final AstAnalysisService astAnalysisService;
    private final ObjectMapper objectMapper;
    private final RollbackService rollbackService;
    private final MetricsService metricsService;

    /**
     * Transformation strategy constants.
     */
    public static final String STRATEGY_AST = "AST";
    public static final String STRATEGY_OPENREWRITE = "OPENREWRITE";
    public static final String STRATEGY_AI_PATCH = "AI_PATCH";

    /**
     * Remediation status constants.
     */
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_ROLLED_BACK = "ROLLED_BACK";

    /**
     * Applies remediation for an approved plan.
     *
     * @param planId the plan ID
     * @param approvalId the approval ID
     * @param workspacePath the workspace path
     * @return the created Remediation
     * @throws IllegalStateException if approval verification fails
     */
    @Transactional
    public Remediation applyRemediation(Long planId, Long approvalId, String workspacePath) {
        log.info("Applying remediation for planId={}, approvalId={}, workspace={}",
                planId, approvalId, workspacePath);
        metricsService.recordRemediation("STARTED", "in_progress");

        long startTime = System.currentTimeMillis();

        // Verify approval first
        Approval approval = approvalService.verifyApprovalForRemediation(
                planId, null, workspacePath);

        // Load plan
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new IllegalStateException("Plan not found: " + planId));

        // Create remediation record
        Remediation remediation = new Remediation();
        remediation.setPlan(plan);
        remediation.setApproval(approval);
        remediation.setTransformationStrategy(determineTransformationStrategy(plan));
        remediation.setWorkspacePath(workspacePath);
        remediation.setStatus(STATUS_IN_PROGRESS);
        remediation.setWithinScope(true);

        Remediation savedRemediation = remediationRepository.save(remediation);

        // Take snapshot before applying transformations
        if (rollbackService != null) {
            rollbackService.takeSnapshot(savedRemediation.getId(), workspacePath);
        }

        try {
            // Apply transformation
            applyTransformation(savedRemediation, plan, workspacePath);

            // Generate diff
            String diff = generateDiff(savedRemediation, workspacePath);

            // Validate scope
            validateScope(savedRemediation, plan, workspacePath);

            // Mark as completed
            savedRemediation.markCompleted(diff, savedRemediation.getModifiedFiles());
            remediationRepository.save(savedRemediation);

            log.info("Remediation completed successfully for plan ID {}", planId);

            long duration = System.currentTimeMillis() - startTime;
            metricsService.recordRemediationDuration(savedRemediation.getTransformationStrategy(), "COMPLETED", duration);
            metricsService.recordRemediation(savedRemediation.getTransformationStrategy(), "COMPLETED");

        } catch (Exception e) {
            log.error("Remediation failed for plan ID {}: {}", planId, e.getMessage(), e);
            savedRemediation.markFailed(e.getMessage());
            remediationRepository.save(savedRemediation);
            if (rollbackService != null) {
                rollbackService.rollbackWorkspace(savedRemediation.getId(), RollbackService.TRIGGER_SCOPE_VIOLATION);
            }

            long duration = System.currentTimeMillis() - startTime;
            metricsService.recordRemediationDuration(savedRemediation.getTransformationStrategy(), "FAILED", duration);
            metricsService.recordRemediation(savedRemediation.getTransformationStrategy(), "FAILED");

            throw new IllegalStateException("Remediation failed: " + e.getMessage(), e);
        }

        return savedRemediation;
    }

    /**
     * Determines the transformation strategy based on the plan.
     */
    private String determineTransformationStrategy(Plan plan) {
        // Priority: AST > OpenRewrite > AI Patch
        // For now, default to AST for Java/Spring transformations
        return STRATEGY_AST;
    }

    /**
     * Applies the transformation based on the strategy.
     */
    private void applyTransformation(Remediation remediation, Plan plan, String workspacePath) {
        String strategy = remediation.getTransformationStrategy();

        switch (strategy) {
            case STRATEGY_AST:
                applyAstTransformation(remediation, plan, workspacePath);
                break;
            case STRATEGY_OPENREWRITE:
                applyOpenRewriteTransformation(remediation, plan, workspacePath);
                break;
            case STRATEGY_AI_PATCH:
                applyAiPatchTransformation(remediation, plan, workspacePath);
                break;
            default:
                throw new IllegalArgumentException("Unknown transformation strategy: " + strategy);
        }
    }

    /**
     * Applies AST-aware transformation.
     */
    private void applyAstTransformation(Remediation remediation, Plan plan, String workspacePath) {
        log.info("Applying AST transformation for plan ID {}", plan.getId());

        // Get affected files from plan
        List<String> affectedFiles = extractAffectedFiles(plan);

        // For each affected file, apply the transformation using AST
        List<String> modifiedFiles = new ArrayList<>();

        for (String filePath : affectedFiles) {
            Path fullPath = Paths.get(workspacePath, filePath);

            if (!Files.exists(fullPath)) {
                log.warn("File not found, skipping: {}", fullPath);
                continue;
            }

            try {
                // Apply AST modification based on plan details
                applyAstModification(fullPath, plan);

                modifiedFiles.add(filePath);
                log.info("Applied AST modification to: {}", filePath);

            } catch (Exception e) {
                log.error("Failed to apply AST modification to {}: {}", filePath, e.getMessage());
                throw new RuntimeException("AST modification failed for " + filePath + ": " + e.getMessage(), e);
            }
        }

        // Store modified files as JSON
        try {
            remediation.setModifiedFiles(objectMapper.writeValueAsString(modifiedFiles));
        } catch (Exception e) {
            log.error("Failed to serialize modified files: {}", e.getMessage());
            remediation.setModifiedFiles("[]");
        }
    }

    /**
     * Applies OpenRewrite transformation.
     */
    private void applyOpenRewriteTransformation(Remediation remediation, Plan plan, String workspacePath) {
        log.info("Applying OpenRewrite transformation for plan ID {}", plan.getId());
        // TODO: Implement OpenRewrite integration
        throw new UnsupportedOperationException("OpenRewrite transformation not yet implemented");
    }

    /**
     * Applies AI-generated patch transformation.
     */
    private void applyAiPatchTransformation(Remediation remediation, Plan plan, String workspacePath) {
        log.info("Applying AI patch transformation for plan ID {}", plan.getId());
        // TODO: Implement AI patch integration
        throw new UnsupportedOperationException("AI patch transformation not yet implemented");
    }

    /**
     * Applies a specific AST modification to a file.
     */
    private void applyAstModification(Path filePath, Plan plan) throws Exception {
        // Read the file content
        String content = Files.readString(filePath);

        // Apply transformation based on plan type
        // For now, this is a placeholder for actual AST modification logic
        // In a real implementation, this would use JavaParser to modify the AST

        log.debug("Applying AST modification to file: {}", filePath);
        log.debug("Plan transformation strategy: {}", plan.getTransformationStrategy());

        // Placeholder: no actual modification for now
        // This will be enhanced with actual AST manipulation logic
    }

    /**
     * Extracts affected files from the plan.
     */
    private List<String> extractAffectedFiles(Plan plan) {
        try {
            if (plan.getAffectedFiles() != null && !plan.getAffectedFiles().isBlank()) {
                return objectMapper.readValue(plan.getAffectedFiles(), List.class);
            }
        } catch (Exception e) {
            log.error("Failed to parse affected files from plan: {}", e.getMessage());
        }
        return Collections.emptyList();
    }

    /**
     * Generates a unified diff for the remediation.
     */
    private String generateDiff(Remediation remediation, String workspacePath) {
        log.info("Generating diff for remediation ID {}", remediation.getId());

        // TODO: Implement actual diff generation using git or similar
        // For now, return a placeholder
        return "Diff generation not yet implemented";
    }

    /**
     * Validates that the remediation is within the approved scope.
     */
    private void validateScope(Remediation remediation, Plan plan, String workspacePath) {
        log.info("Validating scope for remediation ID {}", remediation.getId());

        // Get approved scope boundaries from plan
        Map<String, Object> scopeBoundaries = extractScopeBoundaries(plan);

        // Get actual modified files
        List<String> modifiedFiles = extractModifiedFiles(remediation);

        // Check if any modified file is outside the approved scope
        List<String> allowedFiles = (List<String>) scopeBoundaries.getOrDefault("allowedFiles", Collections.emptyList());
        List<String> disallowedFiles = (List<String>) scopeBoundaries.getOrDefault("disallowedFiles", Collections.emptyList());

        for (String modifiedFile : modifiedFiles) {
            // Check if file is in disallowed list
            if (disallowedFiles.contains(modifiedFile)) {
                log.error("Scope violation: Modified file is in disallowed list: {}", modifiedFile);
                remediation.setWithinScope(false);
                throw new IllegalStateException("Scope violation: " + modifiedFile + " is in disallowed list");
            }

            // Check if file is not in allowed list (if allowed list is not empty)
            if (!allowedFiles.isEmpty() && !allowedFiles.contains(modifiedFile)) {
                log.error("Scope violation: Modified file is not in allowed list: {}", modifiedFile);
                remediation.setWithinScope(false);
                throw new IllegalStateException("Scope violation: " + modifiedFile + " is not in allowed list");
            }
        }

        log.info("Scope validation passed for remediation ID {}", remediation.getId());
    }

    /**
     * Extracts scope boundaries from the plan.
     */
    private Map<String, Object> extractScopeBoundaries(Plan plan) {
        try {
            if (plan.getScopeBoundaries() != null && !plan.getScopeBoundaries().isBlank()) {
                return objectMapper.readValue(plan.getScopeBoundaries(), Map.class);
            }
        } catch (Exception e) {
            log.error("Failed to parse scope boundaries from plan: {}", e.getMessage());
        }
        return Collections.emptyMap();
    }

    /**
     * Extracts modified files from the remediation.
     */
    private List<String> extractModifiedFiles(Remediation remediation) {
        try {
            if (remediation.getModifiedFiles() != null && !remediation.getModifiedFiles().isBlank()) {
                return objectMapper.readValue(remediation.getModifiedFiles(), List.class);
            }
        } catch (Exception e) {
            log.error("Failed to parse modified files from remediation: {}", e.getMessage());
        }
        return Collections.emptyList();
    }

    /**
     * Rolls back a remediation.
     *
     * @param remediationId the remediation ID
     * @return the updated Remediation
     */
    @Transactional
    public Remediation rollbackRemediation(Long remediationId) {
        log.info("Rolling back remediation ID {}", remediationId);

        Remediation remediation = remediationRepository.findById(remediationId)
                .orElseThrow(() -> new IllegalArgumentException("Remediation not found: " + remediationId));

        if (!STATUS_COMPLETED.equals(remediation.getStatus())) {
            throw new IllegalStateException("Only completed remediations can be rolled back");
        }

        try {
            // Perform rollback
            performRollback(remediation);

            // Mark as rolled back
            remediation.markRolledBack("Rollback completed successfully");
            remediationRepository.save(remediation);

            log.info("Remediation rolled back successfully: {}", remediationId);

        } catch (Exception e) {
            log.error("Rollback failed for remediation ID {}: {}", remediationId, e.getMessage(), e);
            throw new IllegalStateException("Rollback failed: " + e.getMessage(), e);
        }

        return remediation;
    }

    /**
     * Performs the actual rollback operation.
     */
    private void performRollback(Remediation remediation) throws Exception {
        log.info("Performing rollback for remediation ID {}", remediation.getId());
        if (rollbackService != null) {
            rollbackService.rollbackWorkspace(remediation.getId(), RollbackService.TRIGGER_MANUAL);
        }
    }

    /**
     * Gets remediations for a plan.
     *
     * @param planId the plan ID
     * @return list of remediations
     */
    @Transactional(readOnly = true)
    public List<Remediation> getRemediationsForPlan(Long planId) {
        return remediationRepository.findByPlanId(planId);
    }

    /**
     * Gets completed remediations for a plan.
     *
     * @param planId the plan ID
     * @return list of completed remediations
     */
    @Transactional(readOnly = true)
    public List<Remediation> getCompletedRemediationsForPlan(Long planId) {
        return remediationRepository.findCompletedByPlanId(planId);
    }

    /**
     * Gets remediation by approval ID.
     *
     * @param approvalId the approval ID
     * @return list of remediations
     */
    @Transactional(readOnly = true)
    public List<Remediation> getRemediationsForApproval(Long approvalId) {
        return remediationRepository.findByApprovalId(approvalId);
    }
}
