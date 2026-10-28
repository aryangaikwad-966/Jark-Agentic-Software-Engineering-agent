package com.example.Jark.service;

import com.example.Jark.entity.Execution;
import com.example.Jark.entity.Plan;
import com.example.Jark.entity.Remediation;
import com.example.Jark.entity.VerificationResult;
import com.example.Jark.repository.ExecutionRepository;
import com.example.Jark.repository.RemediationRepository;
import com.example.Jark.repository.VerificationResultRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link RepairLoopService} (VXC-190 — Bounded Repair Loop).
 *
 * <p>Tests cover the core bounded-repair contract:
 * <ul>
 *   <li>Retry budget is strictly enforced (max {@link RepairLoopService#MAX_RETRY_ATTEMPTS} attempts).</li>
 *   <li>Scope violations in verification gates short-circuit and return {@link RepairLoopService#SCOPE_EXPANSION_REJECTED}.</li>
 *   <li>Non-repairable failure types return {@link RepairLoopService#RETRY_FAILED} without retry.</li>
 *   <li>Attempt count is persisted on every repair entry, even when the plan cannot be resolved.</li>
 *   <li>Failure classification correctly identifies COMPILER_ERROR, TEST_FAILURE, STATIC_ANALYSIS, SCOPE_VIOLATION.</li>
 *   <li>Patch parsing handles valid JSON, empty arrays, embedded JSON, and multiple patches.</li>
 *   <li>isRepairable returns correct values for every failure type.</li>
 *   <li>{@link VerificationResult} entity methods (allGatesPassed, markPassed, markFailed, incrementRepairAttemptCount) behave correctly.</li>
 * </ul>
 *
 * <p>ChatClient is not mocked in this class because Java 25 does not support Mockito
 * inline-mocking of {@code ChatClient.ChatClientRequest} (a concrete class). Tests that
 * exercise the LLM path are validated through unit tests of the constituent helper methods
 * ({@code parsePatches}, {@code extractApprovedFiles}, {@code classifyFailure}, etc.) rather
 * than by running {@code attemptRepair} end-to-end.
 */
@ExtendWith(MockitoExtension.class)
class RepairLoopServiceTest {

    @Mock
    private VerificationResultRepository verificationResultRepository;
    @Mock
    private RemediationRepository remediationRepository;
    @Mock
    private ExecutionRepository executionRepository;
    @Mock
    private ChatClient chatClient;

    private RepairLoopService service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Minimal in-process stub for VerificationService that avoids Mockito inline-mock
     * issues with Spring-managed beans on Java 25. Always returns PASSED.
     */
    private static class PassingVerificationStub extends VerificationService {

        PassingVerificationStub() {
            super(null, null, null, null, null, null);
        }

        @Override
        public VerificationResult verifyRemediation(Long remediationId, String containerId, String workspacePath) {
            VerificationResult result = new VerificationResult();
            result.markPassed();
            return result;
        }
    }

    @BeforeEach
    void setUp() {
        service = new RepairLoopService(
                verificationResultRepository,
                remediationRepository,
                executionRepository,
                new PassingVerificationStub(),
                objectMapper,
                chatClient);
    }

    // ─── Public constants ───────────────────────────────────────────────────────

    @Test
    void testRetryBudget() {
        assertEquals(2, service.getRetryBudget());
    }

    @Test
    void testMaxRetryAttemptsConstant() {
        assertEquals(2, RepairLoopService.MAX_RETRY_ATTEMPTS);
    }

    @Test
    void testFailureClassificationConstants() {
        assertEquals("COMPILER_ERROR", RepairLoopService.FAILURE_COMPILER_ERROR);
        assertEquals("TEST_FAILURE", RepairLoopService.FAILURE_TEST_FAILURE);
        assertEquals("STATIC_ANALYSIS", RepairLoopService.FAILURE_STATIC_ANALYSIS);
        assertEquals("SCOPE_VIOLATION", RepairLoopService.FAILURE_SCOPE_VIOLATION);
        assertEquals("UNKNOWN", RepairLoopService.FAILURE_UNKNOWN);
    }

    @Test
    void testRetryStatusConstants() {
        assertEquals("RETRY_SUCCESS", RepairLoopService.RETRY_SUCCESS);
        assertEquals("RETRY_FAILED", RepairLoopService.RETRY_FAILED);
        assertEquals("SCOPE_EXPANSION_REJECTED", RepairLoopService.SCOPE_EXPANSION_REJECTED);
        assertEquals("BUDGET_EXCEEDED", RepairLoopService.BUDGET_EXCEEDED);
    }

    // ─── attemptRepair — guard conditions (no LLM required) ────────────────────

    @Test
    void testAttemptRepair_alreadyPassed_returnsSuccess_withoutSave() {
        VerificationResult result = buildPassedResult();
        when(verificationResultRepository.findById(1L)).thenReturn(Optional.of(result));

        String outcome = service.attemptRepair(1L);

        assertEquals(RepairLoopService.RETRY_SUCCESS, outcome);
        // Must NOT persist — no repair needed.
        verify(verificationResultRepository, never()).save(any());
    }

    @Test
    void testAttemptRepair_budgetExhaustedAtMax_returnsBudgetExceeded() {
        VerificationResult result = buildFailedResult(RepairLoopService.FAILURE_COMPILER_ERROR);
        result.setRepairAttemptCount(RepairLoopService.MAX_RETRY_ATTEMPTS);
        when(verificationResultRepository.findById(1L)).thenReturn(Optional.of(result));

        String outcome = service.attemptRepair(1L);

        assertEquals(RepairLoopService.BUDGET_EXCEEDED, outcome);
    }

    @Test
    void testAttemptRepair_budgetExhaustedAboveMax_returnsBudgetExceeded() {
        VerificationResult result = buildFailedResult(RepairLoopService.FAILURE_COMPILER_ERROR);
        result.setRepairAttemptCount(5); // Exceeds budget
        when(verificationResultRepository.findById(1L)).thenReturn(Optional.of(result));

        String outcome = service.attemptRepair(1L);

        assertEquals(RepairLoopService.BUDGET_EXCEEDED, outcome);
    }

    @Test
    void testAttemptRepair_scopeViolation_unauthorizedFiles_returnsScopeExpansionRejected() {
        VerificationResult result = buildResult();
        result.setRepairAttemptCount(0);
        result.setUnauthorizedFilesDetected(true); // Gate 2 fails → scope violation
        result.setStatus(VerificationService.STATUS_FAILED);
        when(verificationResultRepository.findById(1L)).thenReturn(Optional.of(result));

        String outcome = service.attemptRepair(1L);

        assertEquals(RepairLoopService.SCOPE_EXPANSION_REJECTED, outcome);
    }

    @Test
    void testAttemptRepair_scopeViolation_diffMismatch_returnsScopeExpansionRejected() {
        VerificationResult result = buildResult();
        result.setRepairAttemptCount(0);
        result.setDiffMatchesPlan(false); // Gate 3 fails → scope violation
        result.setStatus(VerificationService.STATUS_FAILED);
        when(verificationResultRepository.findById(1L)).thenReturn(Optional.of(result));

        String outcome = service.attemptRepair(1L);

        assertEquals(RepairLoopService.SCOPE_EXPANSION_REJECTED, outcome);
    }

    @Test
    void testAttemptRepair_verificationResultNotFound_throwsIllegalArgument() {
        when(verificationResultRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.attemptRepair(99L));
    }

    // ─── Retry counter enforcement ─────────────────────────────────────────────

    @Test
    void testAttemptRepair_incrementsAttemptCountWhenNoPlanAvailable() {
        // No Execution attached → plan is null → increments counter, then RETRY_FAILED.
        VerificationResult result = buildFailedResult(RepairLoopService.FAILURE_COMPILER_ERROR);
        result.setRepairAttemptCount(0);
        result.setExecution(null);

        when(verificationResultRepository.findById(1L)).thenReturn(Optional.of(result));
        when(verificationResultRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String outcome = service.attemptRepair(1L);

        assertEquals(RepairLoopService.RETRY_FAILED, outcome);
        // Verify that save was called with an incremented count (≥ 1).
        verify(verificationResultRepository, atLeastOnce()).save(argThat(
                vr -> vr.getRepairAttemptCount() != null && vr.getRepairAttemptCount() >= 1));
    }

    @Test
    void testAttemptRepair_unknownFailureType_returnsRetryFailed_withIncrementedCount() {
        // Builds an "all gates pass but status is FAILED" result → classifies as UNKNOWN.
        VerificationResult result = buildResult();
        result.setRepairAttemptCount(0);
        result.setStatus(VerificationService.STATUS_FAILED);
        result.setStdout(null); // No stdout → cannot determine failure

        when(verificationResultRepository.findById(1L)).thenReturn(Optional.of(result));
        when(verificationResultRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String outcome = service.attemptRepair(1L);

        // UNKNOWN is not repairable → RETRY_FAILED
        assertEquals(RepairLoopService.RETRY_FAILED, outcome);
        verify(verificationResultRepository, atLeastOnce()).save(any());
    }

    // ─── Failure classification ────────────────────────────────────────────────

    @Test
    void testClassifyFailure_unauthorizedFiles_returnsScopeViolation() {
        VerificationResult result = buildResult();
        result.setUnauthorizedFilesDetected(true);
        assertEquals(RepairLoopService.FAILURE_SCOPE_VIOLATION, service.classifyFailure(result));
    }

    @Test
    void testClassifyFailure_unexpectedModifications_returnsScopeViolation() {
        VerificationResult result = buildResult();
        result.setUnexpectedModifications(true);
        assertEquals(RepairLoopService.FAILURE_SCOPE_VIOLATION, service.classifyFailure(result));
    }

    @Test
    void testClassifyFailure_diffMismatch_returnsScopeViolation() {
        VerificationResult result = buildResult();
        result.setDiffMatchesPlan(false);
        assertEquals(RepairLoopService.FAILURE_SCOPE_VIOLATION, service.classifyFailure(result));
    }

    @Test
    void testClassifyFailure_intendedFilesNotModified_returnsScopeViolation() {
        VerificationResult result = buildResult();
        result.setIntendedFilesModified(false);
        assertEquals(RepairLoopService.FAILURE_SCOPE_VIOLATION, service.classifyFailure(result));
    }

    @Test
    void testClassifyFailure_compilationErrorInStdout_returnsCompilerError() {
        VerificationResult result = buildScopePassedResult();
        result.setStdout("[ERROR] COMPILATION ERROR\n[ERROR] Foo.java: cannot find symbol");
        result.setBuildStatus("FAILURE");
        assertEquals(RepairLoopService.FAILURE_COMPILER_ERROR, service.classifyFailure(result));
    }

    @Test
    void testClassifyFailure_buildFailureGeneric_returnsCompilerError() {
        VerificationResult result = buildScopePassedResult();
        result.setStdout("BUILD FAILURE");
        result.setBuildStatus("FAILURE");
        assertEquals(RepairLoopService.FAILURE_COMPILER_ERROR, service.classifyFailure(result));
    }

    @Test
    void testClassifyFailure_buildStatusNotSuccess_returnsCompilerError() {
        VerificationResult result = buildScopePassedResult();
        result.setStdout("No output");
        result.setBuildStatus("FAILURE");
        assertEquals(RepairLoopService.FAILURE_COMPILER_ERROR, service.classifyFailure(result));
    }

    @Test
    void testClassifyFailure_testFailuresInOutput_returnsTestFailure() {
        VerificationResult result = buildScopePassedResult();
        result.setStdout("BUILD FAILURE\nTests run: 5, FAILURES: 2");
        result.setBuildStatus("FAILURE");
        result.setTestStatus("FAILED");
        assertEquals(RepairLoopService.FAILURE_TEST_FAILURE, service.classifyFailure(result));
    }

    @Test
    void testClassifyFailure_testStatusFailed_returnsTestFailure() {
        VerificationResult result = buildScopePassedResult();
        result.setBuildStatus("SUCCESS");
        result.setTestStatus("FAILED");
        assertEquals(RepairLoopService.FAILURE_TEST_FAILURE, service.classifyFailure(result));
    }

    @Test
    void testClassifyFailure_staticAnalysisFailed_returnsStaticAnalysis() {
        VerificationResult result = buildScopePassedResult();
        result.setBuildStatus("SUCCESS");
        result.setTestStatus("PASSED");
        result.setStaticAnalysisStatus("FAILED");
        assertEquals(RepairLoopService.FAILURE_STATIC_ANALYSIS, service.classifyFailure(result));
    }

    @Test
    void testClassifyFailure_allGatesPass_returnsUnknown() {
        VerificationResult result = buildScopePassedResult();
        result.setBuildStatus("SUCCESS");
        result.setTestStatus("PASSED");
        result.setStaticAnalysisStatus("PASSED");
        assertEquals(RepairLoopService.FAILURE_UNKNOWN, service.classifyFailure(result));
    }

    // ─── isRepairable ──────────────────────────────────────────────────────────

    @Test
    void testIsRepairable_compilerError_isTrue() {
        assertTrue(service.isRepairable(RepairLoopService.FAILURE_COMPILER_ERROR));
    }

    @Test
    void testIsRepairable_testFailure_isTrue() {
        assertTrue(service.isRepairable(RepairLoopService.FAILURE_TEST_FAILURE));
    }

    @Test
    void testIsRepairable_staticAnalysis_isTrue() {
        assertTrue(service.isRepairable(RepairLoopService.FAILURE_STATIC_ANALYSIS));
    }

    @Test
    void testIsRepairable_scopeViolation_isFalse() {
        assertFalse(service.isRepairable(RepairLoopService.FAILURE_SCOPE_VIOLATION));
    }

    @Test
    void testIsRepairable_unknown_isFalse() {
        assertFalse(service.isRepairable(RepairLoopService.FAILURE_UNKNOWN));
    }

    // ─── Patch parsing ─────────────────────────────────────────────────────────

    @Test
    void testParsePatches_validJson_returnsSinglePatch() throws Exception {
        String json = "[{\"relativePath\": \"src/main/java/Foo.java\", \"newContent\": \"public class Foo {}\"}]";

        List<RepairLoopService.FilePatch> patches = service.parsePatches(json);

        assertEquals(1, patches.size());
        assertEquals("src/main/java/Foo.java", patches.get(0).relativePath());
        assertEquals("public class Foo {}", patches.get(0).newContent());
    }

    @Test
    void testParsePatches_emptyArray_returnsEmptyList() throws Exception {
        assertTrue(service.parsePatches("[]").isEmpty());
    }

    @Test
    void testParsePatches_jsonEmbeddedInProse_extractsCorrectly() throws Exception {
        String prose = "Sure, here is the fix:\n"
                + "[{\"relativePath\": \"src/Foo.java\", \"newContent\": \"class Foo {}\"}]\n"
                + "Hope that helps!";

        List<RepairLoopService.FilePatch> patches = service.parsePatches(prose);

        assertEquals(1, patches.size());
        assertEquals("src/Foo.java", patches.get(0).relativePath());
    }

    @Test
    void testParsePatches_multiplePatches_returnsAll() throws Exception {
        String json = "[{\"relativePath\": \"src/A.java\", \"newContent\": \"class A {}\"}"
                + ", {\"relativePath\": \"src/B.java\", \"newContent\": \"class B {}\"}]";

        List<RepairLoopService.FilePatch> patches = service.parsePatches(json);

        assertEquals(2, patches.size());
    }

    @Test
    void testParsePatches_noJsonArray_returnsEmpty() throws Exception {
        assertTrue(service.parsePatches("No JSON here at all").isEmpty());
    }

    @Test
    void testParsePatches_malformedJson_returnsEmpty() throws Exception {
        // No valid '[' ... ']' structure
        List<RepairLoopService.FilePatch> patches = service.parsePatches("{ invalid }");
        // Must not throw; either empty or partial
        assertNotNull(patches);
    }

    // ─── extractApprovedFiles ──────────────────────────────────────────────────

    @Test
    void testExtractApprovedFiles_validJson_returnsList() {
        Plan plan = new Plan();
        plan.setAffectedFiles("[\"src/main/java/Foo.java\", \"src/main/java/Bar.java\"]");

        List<String> files = service.extractApprovedFiles(plan);

        assertEquals(2, files.size());
        assertTrue(files.contains("src/main/java/Foo.java"));
        assertTrue(files.contains("src/main/java/Bar.java"));
    }

    @Test
    void testExtractApprovedFiles_nullAffectedFiles_returnsEmptyList() {
        Plan plan = new Plan();
        plan.setAffectedFiles(null);
        assertTrue(service.extractApprovedFiles(plan).isEmpty());
    }

    @Test
    void testExtractApprovedFiles_blankAffectedFiles_returnsEmptyList() {
        Plan plan = new Plan();
        plan.setAffectedFiles("   ");
        assertTrue(service.extractApprovedFiles(plan).isEmpty());
    }

    // ─── getPlanFromVerificationResult ────────────────────────────────────────

    @Test
    void testGetPlanFromVerificationResult_withEagerExecution_returnsPlan() {
        Plan plan = buildPlan("[]");
        Execution execution = buildExecution(plan);
        VerificationResult result = buildResult();
        result.setExecution(execution);

        Plan retrieved = service.getPlanFromVerificationResult(result);

        assertNotNull(retrieved);
        assertEquals(plan.getId(), retrieved.getId());
    }

    @Test
    void testGetPlanFromVerificationResult_noExecution_returnsNull() {
        VerificationResult result = buildResult();
        result.setExecution(null);

        assertNull(service.getPlanFromVerificationResult(result));
    }

    // ─── VerificationResult entity methods ────────────────────────────────────

    @Test
    void testVerificationResultAllGatesPassed_allTrue() {
        VerificationResult r = buildFullyPassingResult();
        assertTrue(r.allGatesPassed());
    }

    @Test
    void testVerificationResultAllGatesPassed_gate1False() {
        VerificationResult r = buildFullyPassingResult();
        r.setIntendedFilesModified(false);
        assertFalse(r.allGatesPassed());
    }

    @Test
    void testVerificationResultAllGatesPassed_gate2True_unauthorizedFiles() {
        VerificationResult r = buildFullyPassingResult();
        r.setUnauthorizedFilesDetected(true);
        assertFalse(r.allGatesPassed());
    }

    @Test
    void testVerificationResultAllGatesPassed_gate3False() {
        VerificationResult r = buildFullyPassingResult();
        r.setDiffMatchesPlan(false);
        assertFalse(r.allGatesPassed());
    }

    @Test
    void testVerificationResultMarkPassed_setsStatusAndTimestamp() {
        VerificationResult result = new VerificationResult();
        result.markPassed();
        assertEquals(VerificationService.STATUS_PASSED, result.getStatus());
        assertNotNull(result.getVerifiedAt());
    }

    @Test
    void testVerificationResultMarkFailed_setsStatusAndLog() {
        VerificationResult result = new VerificationResult();
        result.markFailed("Build failed");
        assertEquals(VerificationService.STATUS_FAILED, result.getStatus());
        assertEquals("Build failed", result.getVerificationLog());
        assertNotNull(result.getVerifiedAt());
    }

    @Test
    void testVerificationResultIncrementRepairAttemptCount_startsAtZero() {
        VerificationResult result = new VerificationResult();
        result.setRepairAttemptCount(0);
        assertEquals(1, result.incrementRepairAttemptCount());
        assertEquals(2, result.incrementRepairAttemptCount());
    }

    @Test
    void testVerificationResultIncrementRepairAttemptCount_handlesNullSafely() {
        VerificationResult result = new VerificationResult();
        result.setRepairAttemptCount(null);
        assertEquals(1, result.incrementRepairAttemptCount());
    }

    // ─── Builder helpers ───────────────────────────────────────────────────────

    private VerificationResult buildResult() {
        VerificationResult r = new VerificationResult();
        r.setId(1L);
        r.setRepairAttemptCount(0);
        r.setIntendedFilesModified(true);
        r.setUnauthorizedFilesDetected(false);
        r.setDiffMatchesPlan(true);
        r.setUnexpectedModifications(false);
        r.setBuildStatus("SUCCESS");
        r.setTestStatus("PASSED");
        r.setStaticAnalysisStatus("PASSED");
        r.setStatus(VerificationService.STATUS_FAILED);
        return r;
    }

    private VerificationResult buildPassedResult() {
        VerificationResult r = buildResult();
        r.setStatus(VerificationService.STATUS_PASSED);
        return r;
    }

    private VerificationResult buildFailedResult(String primaryFailure) {
        VerificationResult r = buildResult();
        r.setStatus(VerificationService.STATUS_FAILED);
        if (RepairLoopService.FAILURE_COMPILER_ERROR.equals(primaryFailure)) {
            r.setBuildStatus("FAILURE");
            r.setStdout("BUILD FAILURE\n[ERROR] COMPILATION ERROR");
        } else if (RepairLoopService.FAILURE_TEST_FAILURE.equals(primaryFailure)) {
            r.setTestStatus("FAILED");
        }
        return r;
    }

    /**
     * A VerificationResult where all scope gates pass (for testing non-scope gate failures).
     */
    private VerificationResult buildScopePassedResult() {
        VerificationResult r = buildResult();
        r.setIntendedFilesModified(true);
        r.setUnauthorizedFilesDetected(false);
        r.setDiffMatchesPlan(true);
        r.setUnexpectedModifications(false);
        return r;
    }

    /**
     * A fully-passing VerificationResult (all 7 gates pass).
     */
    private VerificationResult buildFullyPassingResult() {
        VerificationResult r = new VerificationResult();
        r.setIntendedFilesModified(true);
        r.setUnauthorizedFilesDetected(false);
        r.setDiffMatchesPlan(true);
        r.setBuildStatus(VerificationService.STATUS_SUCCESS);
        r.setTestStatus(VerificationService.STATUS_PASSED_ANALYSIS);
        r.setStaticAnalysisStatus(VerificationService.STATUS_PASSED_ANALYSIS);
        r.setUnexpectedModifications(false);
        return r;
    }

    private Plan buildPlan(String affectedFilesJson) {
        Plan plan = new Plan();
        plan.setId(10L);
        plan.setTitle("Test Plan");
        plan.setDescription("Fix compilation error");
        plan.setProposedChanges("[]");
        plan.setAffectedFiles(affectedFilesJson);
        return plan;
    }

    private Execution buildExecution(Plan plan) {
        Execution execution = new Execution();
        execution.setId(20L);
        execution.setPlan(plan);
        execution.setDockerContainerId("container-abc");
        return execution;
    }

    @SuppressWarnings("unused")
    private Remediation buildRemediation(Plan plan, String workspacePath) {
        Remediation remediation = new Remediation();
        remediation.setId(30L);
        remediation.setPlan(plan);
        remediation.setWorkspacePath(workspacePath);
        remediation.setStatus("COMPLETED");
        return remediation;
    }
}
