package com.example.Jark.service;

import com.example.Jark.dto.rollback.RollbackResult;
import com.example.Jark.dto.rollback.WorkspaceSnapshot;
import com.example.Jark.entity.Execution;
import com.example.Jark.entity.Plan;
import com.example.Jark.entity.Remediation;
import com.example.Jark.entity.VerificationResult;
import com.example.Jark.repository.ExecutionRepository;
import com.example.Jark.repository.RemediationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit and filesystem assertion tests for {@link RollbackService} (VXC-200 — State Preservation and Recovery).
 */
class RollbackServiceTest {

    private RemediationRepository remediationRepository;
    private ExecutionRepository executionRepository;
    private ObjectMapper objectMapper;
    private RollbackService rollbackService;

    @TempDir
    Path tempBaseDir;

    private Path workspaceDir;
    private Path snapshotBaseDir;

    @BeforeEach
    void setUp() throws IOException {
        remediationRepository = mock(RemediationRepository.class);
        executionRepository = mock(ExecutionRepository.class);
        objectMapper = new ObjectMapper();

        workspaceDir = tempBaseDir.resolve("workspace");
        snapshotBaseDir = tempBaseDir.resolve("snapshots");

        Files.createDirectories(workspaceDir);
        Files.createDirectories(snapshotBaseDir);

        rollbackService = new RollbackService(
                remediationRepository,
                executionRepository,
                objectMapper,
                snapshotBaseDir.toString());
    }

    // ────────────────────────────────────────────────────────────────────────────
    // 1. Rollback Trigger Detection Tests
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    void testDetectRollbackTrigger_unauthorizedFilesDetected() {
        VerificationResult result = new VerificationResult();
        result.setUnauthorizedFilesDetected(true);

        Optional<String> trigger = rollbackService.detectRollbackTrigger(result, null, null);

        assertTrue(trigger.isPresent());
        assertEquals(RollbackService.TRIGGER_UNAUTHORIZED_MODIFICATIONS, trigger.get());
    }

    @Test
    void testDetectRollbackTrigger_unexpectedModificationsDetected() {
        VerificationResult result = new VerificationResult();
        result.setUnexpectedModifications(true);

        Optional<String> trigger = rollbackService.detectRollbackTrigger(result, null, null);

        assertTrue(trigger.isPresent());
        assertEquals(RollbackService.TRIGGER_UNAUTHORIZED_MODIFICATIONS, trigger.get());
    }

    @Test
    void testDetectRollbackTrigger_remediationViolatesScope() {
        Remediation remediation = new Remediation();
        remediation.setWithinScope(false);

        Optional<String> trigger = rollbackService.detectRollbackTrigger(null, remediation, null);

        assertTrue(trigger.isPresent());
        assertEquals(RollbackService.TRIGGER_SCOPE_VIOLATION, trigger.get());
    }

    @Test
    void testDetectRollbackTrigger_diffDoesNotMatchPlan() {
        VerificationResult result = new VerificationResult();
        result.setDiffMatchesPlan(false);

        Optional<String> trigger = rollbackService.detectRollbackTrigger(result, null, null);

        assertTrue(trigger.isPresent());
        assertEquals(RollbackService.TRIGGER_SCOPE_VIOLATION, trigger.get());
    }

    @Test
    void testDetectRollbackTrigger_scopeExpansionRejected() {
        Optional<String> trigger = rollbackService.detectRollbackTrigger(
                null, null, RepairLoopService.SCOPE_EXPANSION_REJECTED);

        assertTrue(trigger.isPresent());
        assertEquals(RollbackService.TRIGGER_SCOPE_VIOLATION, trigger.get());
    }

    @Test
    void testDetectRollbackTrigger_repairBudgetExceeded() {
        Optional<String> trigger = rollbackService.detectRollbackTrigger(
                null, null, RepairLoopService.BUDGET_EXCEEDED);

        assertTrue(trigger.isPresent());
        assertEquals(RollbackService.TRIGGER_REPAIR_BUDGET_EXCEEDED, trigger.get());
    }

    @Test
    void testDetectRollbackTrigger_verificationPermanentlyFailed_maxRetries() {
        VerificationResult result = new VerificationResult();
        result.setStatus(VerificationService.STATUS_FAILED);
        result.setDiffMatchesPlan(true);
        result.setIntendedFilesModified(true);
        result.setUnauthorizedFilesDetected(false);
        result.setUnexpectedModifications(false);
        result.setRepairAttemptCount(2);

        Optional<String> trigger = rollbackService.detectRollbackTrigger(result, null, null);

        assertTrue(trigger.isPresent());
        assertEquals(RollbackService.TRIGGER_VERIFICATION_PERMANENT_FAILURE, trigger.get());
    }

    @Test
    void testDetectRollbackTrigger_verificationPermanentlyFailed_retryFailedOutcome() {
        VerificationResult result = new VerificationResult();
        result.setStatus(VerificationService.STATUS_FAILED);
        result.setDiffMatchesPlan(true);
        result.setIntendedFilesModified(true);
        result.setUnauthorizedFilesDetected(false);
        result.setUnexpectedModifications(false);
        result.setRepairAttemptCount(1);

        Optional<String> trigger = rollbackService.detectRollbackTrigger(
                result, null, RepairLoopService.RETRY_FAILED);

        assertTrue(trigger.isPresent());
        assertEquals(RollbackService.TRIGGER_VERIFICATION_PERMANENT_FAILURE, trigger.get());
    }

    @Test
    void testDetectRollbackTrigger_returnsEmptyWhenPassing() {
        VerificationResult result = new VerificationResult();
        result.setStatus(VerificationService.STATUS_PASSED);
        result.setDiffMatchesPlan(true);
        result.setIntendedFilesModified(true);
        result.setUnauthorizedFilesDetected(false);
        result.setUnexpectedModifications(false);

        Remediation remediation = new Remediation();
        remediation.setWithinScope(true);

        Optional<String> trigger = rollbackService.detectRollbackTrigger(
                result, remediation, RepairLoopService.RETRY_SUCCESS);

        assertTrue(trigger.isEmpty());
    }

    // ────────────────────────────────────────────────────────────────────────────
    // 2. Snapshot Creation & Filesystem State Preservation
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    void testTakeSnapshot_createsCopiesAndCalculatesChecksums() throws IOException {
        Path file1 = workspaceDir.resolve("src/main/App.java");
        Path file2 = workspaceDir.resolve("README.md");
        Files.createDirectories(file1.getParent());
        Files.writeString(file1, "public class App {}");
        Files.writeString(file2, "# Readme content");

        WorkspaceSnapshot snapshot = rollbackService.takeSnapshot(100L, workspaceDir.toString());

        assertNotNull(snapshot);
        assertEquals(100L, snapshot.getRemediationId());
        assertEquals(2, snapshot.getFileCount());
        assertEquals(2, snapshot.getFileChecksums().size());
        assertTrue(snapshot.getFileChecksums().containsKey("src/main/App.java"));
        assertTrue(snapshot.getFileChecksums().containsKey("README.md"));

        Path snapDir = Path.of(snapshot.getSnapshotPath());
        assertTrue(Files.exists(snapDir));
        assertTrue(Files.exists(snapDir.resolve("src/main/App.java")));
        assertTrue(Files.exists(snapDir.resolve("README.md")));
        assertTrue(Files.exists(snapDir.resolve("snapshot_metadata.json")));

        assertEquals("public class App {}", Files.readString(snapDir.resolve("src/main/App.java")));
    }

    @Test
    void testTakeSnapshot_nonExistentWorkspace_throwsIllegalArgumentException() {
        Path invalid = tempBaseDir.resolve("non_existent_folder");

        assertThrows(IllegalArgumentException.class, () ->
                rollbackService.takeSnapshot(200L, invalid.toString()));
    }

    // ────────────────────────────────────────────────────────────────────────────
    // 3. Full Rollback & Restoration Execution with Filesystem Assertions
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    void testRollbackWorkspace_restoresCorruptedFilesAndDeletesRogueFiles() throws IOException {
        // Setup initial workspace state
        Path originalFile = workspaceDir.resolve("src/Main.java");
        Path originalConfig = workspaceDir.resolve("config.properties");
        Files.createDirectories(originalFile.getParent());
        Files.writeString(originalFile, "package com.example;\npublic class Main { int x = 1; }");
        Files.writeString(originalConfig, "app.version=1.0.0");

        // Take pre-remediation snapshot
        WorkspaceSnapshot snapshot = rollbackService.takeSnapshot(1L, workspaceDir.toString());
        assertNotNull(snapshot);

        // Mock Remediation and Execution entities in DB
        Remediation remediation = new Remediation();
        remediation.setId(1L);
        Plan plan = new Plan();
        plan.setId(10L);
        remediation.setPlan(plan);
        when(remediationRepository.findById(1L)).thenReturn(Optional.of(remediation));

        Execution execution = new Execution();
        execution.setId(5L);
        execution.setPlan(plan);
        execution.setExecutionLog("Initial execution log");
        when(executionRepository.findByPlanId(10L)).thenReturn(List.of(execution));

        // Corrupt workspace during simulated failed remediation:
        // 1. Mutate existing file
        Files.writeString(originalFile, "BROKEN AND CORRUPTED CODE!!!");
        // 2. Delete existing file
        Files.delete(originalConfig);
        // 3. Introduce rogue unauthorized files
        Path rogue1 = workspaceDir.resolve("src/Backdoor.java");
        Path rogue2 = workspaceDir.resolve("unauthorized_exploit.sh");
        Files.writeString(rogue1, "public class Backdoor {}");
        Files.writeString(rogue2, "#!/bin/sh\nrm -rf /");

        // Verify corruption is present before rollback
        assertEquals("BROKEN AND CORRUPTED CODE!!!", Files.readString(originalFile));
        assertFalse(Files.exists(originalConfig));
        assertTrue(Files.exists(rogue1));
        assertTrue(Files.exists(rogue2));

        // Execute rollback
        RollbackResult result = rollbackService.rollbackWorkspace(
                1L, RollbackService.TRIGGER_UNAUTHORIZED_MODIFICATIONS);

        // Assertions on RollbackResult
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertTrue(result.isRestorationVerified());
        assertEquals(2, result.getFilesRestored());
        assertEquals(2, result.getFilesDeleted());
        assertEquals(RollbackService.TRIGGER_UNAUTHORIZED_MODIFICATIONS, result.getTriggerReason());

        // File system assertions:
        // 1. Mutated file restored to exact original content
        assertEquals("package com.example;\npublic class Main { int x = 1; }", Files.readString(originalFile));
        // 2. Deleted file restored with original content
        assertTrue(Files.exists(originalConfig));
        assertEquals("app.version=1.0.0", Files.readString(originalConfig));
        // 3. Rogue unauthorized files deleted
        assertFalse(Files.exists(rogue1), "Rogue file 1 must be deleted by rollback");
        assertFalse(Files.exists(rogue2), "Rogue file 2 must be deleted by rollback");

        // Database assertions:
        verify(remediationRepository).save(argThat(r ->
                "ROLLED_BACK".equals(r.getStatus()) && r.getRollbackInfo() != null &&
                        r.getRollbackInfo().contains("UNAUTHORIZED_MODIFICATIONS")));

        verify(executionRepository).save(argThat(e ->
                "ROLLED_BACK".equals(e.getStatus()) && e.getExecutionLog().contains("[ROLLBACK]")));
    }

    @Test
    void testRollbackWorkspace_noSnapshotFound_returnsFailedResult() {
        RollbackResult result = rollbackService.rollbackWorkspace(
                999L, RollbackService.TRIGGER_VERIFICATION_PERMANENT_FAILURE);

        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertFalse(result.isRestorationVerified());
        assertTrue(result.getErrorMessage().contains("No snapshot found"));
    }

    // ────────────────────────────────────────────────────────────────────────────
    // 4. Verification of Restoration Integrity
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    void testVerifyRestoration_returnsTrueWhenFilesMatchExactly() throws IOException {
        Path file = workspaceDir.resolve("Data.txt");
        Files.writeString(file, "clean data");

        WorkspaceSnapshot snapshot = rollbackService.takeSnapshot(2L, workspaceDir.toString());

        assertTrue(rollbackService.verifyRestoration(snapshot));
    }

    @Test
    void testVerifyRestoration_returnsFalseWhenFileContentModified() throws IOException {
        Path file = workspaceDir.resolve("Data.txt");
        Files.writeString(file, "clean data");

        WorkspaceSnapshot snapshot = rollbackService.takeSnapshot(3L, workspaceDir.toString());

        // Tamper with file
        Files.writeString(file, "tampered data");

        assertFalse(rollbackService.verifyRestoration(snapshot));
    }

    @Test
    void testVerifyRestoration_returnsFalseWhenFileDeleted() throws IOException {
        Path file = workspaceDir.resolve("Data.txt");
        Files.writeString(file, "clean data");

        WorkspaceSnapshot snapshot = rollbackService.takeSnapshot(4L, workspaceDir.toString());

        Files.delete(file);

        assertFalse(rollbackService.verifyRestoration(snapshot));
    }

    @Test
    void testVerifyRestoration_returnsFalseWhenExtraFileExists() throws IOException {
        Path file = workspaceDir.resolve("Data.txt");
        Files.writeString(file, "clean data");

        WorkspaceSnapshot snapshot = rollbackService.takeSnapshot(5L, workspaceDir.toString());

        // Add extra un-snapshotted file
        Files.writeString(workspaceDir.resolve("Extra.txt"), "extra");

        assertFalse(rollbackService.verifyRestoration(snapshot));
    }

    // ────────────────────────────────────────────────────────────────────────────
    // 5. Snapshot Cleanup & Retrieval
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    void testCleanupSnapshot_deletesSnapshotFromDiskAndMemory() throws IOException {
        Path file = workspaceDir.resolve("App.java");
        Files.writeString(file, "class App {}");

        WorkspaceSnapshot snapshot = rollbackService.takeSnapshot(6L, workspaceDir.toString());
        Path snapDir = Path.of(snapshot.getSnapshotPath());

        assertTrue(Files.exists(snapDir));
        assertTrue(rollbackService.getSnapshot(6L).isPresent());

        rollbackService.cleanupSnapshot(6L);

        assertFalse(Files.exists(snapDir));
        assertTrue(rollbackService.getSnapshot(6L).isEmpty());
    }
}
