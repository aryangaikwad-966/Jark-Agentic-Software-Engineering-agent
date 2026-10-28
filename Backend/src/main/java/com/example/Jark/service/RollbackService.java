package com.example.Jark.service;

import com.example.Jark.dto.rollback.RollbackResult;
import com.example.Jark.dto.rollback.WorkspaceSnapshot;
import com.example.Jark.entity.Execution;
import com.example.Jark.entity.Remediation;
import com.example.Jark.entity.VerificationResult;
import com.example.Jark.repository.ExecutionRepository;
import com.example.Jark.repository.RemediationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service managing state preservation and recovery (VXC-200 — Rollback).
 *
 * <p>Protects the workspace from corruption during remediation attempts by:
 * <ol>
 *   <li>Taking a snapshot of the workspace before remediation begins.</li>
 *   <li>Detecting rollback trigger conditions (permanent verification failure,
 *       exhausted retry budget, unauthorized file modifications, scope violations).</li>
 *   <li>Restoring the workspace to its exact pre-remediation state.</li>
 *   <li>Verifying restoration integrity (checksum matching, no leftover rogue files).</li>
 *   <li>Recording the rollback action in execution history and updating remediation status to ROLLED_BACK.</li>
 * </ol>
 */
@Slf4j
@Service
public class RollbackService {

    private final RemediationRepository remediationRepository;
    private final ExecutionRepository executionRepository;
    private final ObjectMapper objectMapper;
    private final Path baseSnapshotDir;

    // In-memory cache of active snapshots by remediation ID
    private final Map<Long, WorkspaceSnapshot> activeSnapshots = new ConcurrentHashMap<>();

    // ─── Rollback trigger constants ─────────────────────────────────────────────
    public static final String TRIGGER_VERIFICATION_PERMANENT_FAILURE = "VERIFICATION_PERMANENT_FAILURE";
    public static final String TRIGGER_REPAIR_BUDGET_EXCEEDED = "REPAIR_BUDGET_EXCEEDED";
    public static final String TRIGGER_UNAUTHORIZED_MODIFICATIONS = "UNAUTHORIZED_MODIFICATIONS";
    public static final String TRIGGER_SCOPE_VIOLATION = "SCOPE_VIOLATION";
    public static final String TRIGGER_MANUAL = "MANUAL";

    public RollbackService(
            RemediationRepository remediationRepository,
            ExecutionRepository executionRepository,
            ObjectMapper objectMapper,
            @Value("${jark.snapshot.base-dir:/tmp/jark/snapshots}") String baseSnapshotDirStr) {
        this.remediationRepository = remediationRepository;
        this.executionRepository = executionRepository;
        this.objectMapper = objectMapper;
        this.baseSnapshotDir = Paths.get(baseSnapshotDirStr).toAbsolutePath().normalize();
        initializeSnapshotDirectory();
    }

    private void initializeSnapshotDirectory() {
        try {
            if (!Files.exists(baseSnapshotDir)) {
                Files.createDirectories(baseSnapshotDir);
                log.info("Initialized base snapshot directory at: {}", baseSnapshotDir);
            }
        } catch (IOException e) {
            log.error("Failed to initialize base snapshot directory {}: {}", baseSnapshotDir, e.getMessage());
        }
    }

    // ────────────────────────────────────────────────────────────────────────────
    // 1. State Preservation (Snapshotting)
    // ────────────────────────────────────────────────────────────────────────────

    /**
     * Takes a snapshot of the workspace prior to beginning remediation.
     *
     * @param remediationId the ID of the remediation
     * @param workspacePath the absolute or relative path to the workspace
     * @return the created {@link WorkspaceSnapshot}
     */
    public WorkspaceSnapshot takeSnapshot(Long remediationId, String workspacePath) {
        if (workspacePath == null || workspacePath.isBlank()) {
            throw new IllegalArgumentException("Workspace path cannot be null or blank");
        }

        Path wsPath = Paths.get(workspacePath).toAbsolutePath().normalize();
        if (!Files.exists(wsPath) || !Files.isDirectory(wsPath)) {
            throw new IllegalArgumentException("Workspace directory does not exist: " + wsPath);
        }

        String snapshotDirName = "snapshot_rem_" + remediationId + "_" + System.currentTimeMillis();
        Path snapshotPath = baseSnapshotDir.resolve(snapshotDirName).normalize();

        try {
            Files.createDirectories(snapshotPath);
            Map<String, String> checksums = new HashMap<>();

            // Copy all files (excluding .git internal directory) and compute checksums
            try (var stream = Files.walk(wsPath)) {
                stream.filter(Files::isRegularFile).forEach(file -> {
                    Path relPath = wsPath.relativize(file);
                    // Skip .git internal files to avoid copying large history or locking objects
                    if (relPath.startsWith(".git")) {
                        return;
                    }

                    try {
                        Path dest = snapshotPath.resolve(relPath);
                        Files.createDirectories(dest.getParent());
                        Files.copy(file, dest, StandardCopyOption.REPLACE_EXISTING);

                        String hash = computeSha256(file);
                        checksums.put(relPath.toString(), hash);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to copy file to snapshot: " + file, e);
                    }
                });
            }

            // Inspect Git commit SHA if git repository
            String gitSha = null;
            String gitBranch = null;
            Path gitDir = wsPath.resolve(".git");
            if (Files.exists(gitDir)) {
                try (Git git = Git.open(wsPath.toFile())) {
                    var head = git.getRepository().findRef("HEAD");
                    if (head != null && head.getObjectId() != null) {
                        gitSha = head.getObjectId().name();
                    }
                    gitBranch = git.getRepository().getBranch();
                } catch (Exception e) {
                    log.debug("Git repository details could not be read for {}: {}", wsPath, e.getMessage());
                }
            }

            WorkspaceSnapshot snapshot = WorkspaceSnapshot.builder()
                    .remediationId(remediationId)
                    .workspacePath(wsPath.toString())
                    .snapshotPath(snapshotPath.toString())
                    .createdAt(LocalDateTime.now().toString())
                    .fileCount(checksums.size())
                    .fileChecksums(checksums)
                    .gitCommitSha(gitSha)
                    .gitBranch(gitBranch)
                    .build();

            // Cache snapshot in memory and write metadata file to snapshot folder
            activeSnapshots.put(remediationId, snapshot);
            writeSnapshotMetadata(snapshot, snapshotPath);

            log.info("Successfully created workspace snapshot for remediationId={} with {} files at {}",
                    remediationId, checksums.size(), snapshotPath);

            return snapshot;

        } catch (Exception e) {
            log.error("Failed to take workspace snapshot for remediationId={}: {}", remediationId, e.getMessage(), e);
            throw new RuntimeException("Snapshot creation failed: " + e.getMessage(), e);
        }
    }

    // ────────────────────────────────────────────────────────────────────────────
    // 2. Trigger Evaluation
    // ────────────────────────────────────────────────────────────────────────────

    /**
     * Determines whether rollback should be triggered based on verification result,
     * remediation status, and repair loop outcome.
     *
     * @param result verification result
     * @param remediation remediation entity
     * @param repairStatus repair loop outcome (e.g. BUDGET_EXCEEDED, SCOPE_EXPANSION_REJECTED)
     * @return an {@link Optional} containing the trigger reason if rollback should happen,
     *         or empty otherwise
     */
    public Optional<String> detectRollbackTrigger(
            VerificationResult result, Remediation remediation, String repairStatus) {

        // 1. Unauthorized modifications detected
        if (result != null) {
            if (Boolean.TRUE.equals(result.getUnauthorizedFilesDetected())
                    || Boolean.TRUE.equals(result.getUnexpectedModifications())) {
                return Optional.of(TRIGGER_UNAUTHORIZED_MODIFICATIONS);
            }
        }

        // 2. Scope violations (diff mismatch or remediation marked outside scope)
        if (remediation != null && Boolean.FALSE.equals(remediation.getWithinScope())) {
            return Optional.of(TRIGGER_SCOPE_VIOLATION);
        }
        if (result != null && Boolean.FALSE.equals(result.getDiffMatchesPlan())) {
            return Optional.of(TRIGGER_SCOPE_VIOLATION);
        }
        if (RepairLoopService.SCOPE_EXPANSION_REJECTED.equals(repairStatus)) {
            return Optional.of(TRIGGER_SCOPE_VIOLATION);
        }

        // 3. Repair budget exhausted
        if (RepairLoopService.BUDGET_EXCEEDED.equals(repairStatus)) {
            return Optional.of(TRIGGER_REPAIR_BUDGET_EXCEEDED);
        }

        // 4. Verification permanently failed (failed status and max retry attempts reached)
        if (result != null && VerificationService.STATUS_FAILED.equals(result.getStatus())) {
            int attempts = result.getRepairAttemptCount() != null ? result.getRepairAttemptCount() : 0;
            if (attempts >= RepairLoopService.MAX_RETRY_ATTEMPTS
                    || RepairLoopService.RETRY_FAILED.equals(repairStatus)) {
                return Optional.of(TRIGGER_VERIFICATION_PERMANENT_FAILURE);
            }
        }

        return Optional.empty();
    }

    // ────────────────────────────────────────────────────────────────────────────
    // 3. Restoration Execution
    // ────────────────────────────────────────────────────────────────────────────

    /**
     * Rolls back a workspace to the snapshot taken for the specified remediation.
     *
     * @param remediationId the ID of the remediation
     * @param triggerReason the reason triggering the rollback
     * @return the {@link RollbackResult}
     */
    @Transactional
    public RollbackResult rollbackWorkspace(Long remediationId, String triggerReason) {
        log.warn("Initiating workspace rollback for remediationId={} with triggerReason='{}'",
                remediationId, triggerReason);

        WorkspaceSnapshot loadedSnapshot = activeSnapshots.get(remediationId);
        if (loadedSnapshot == null) {
            loadedSnapshot = findSnapshotOnDisk(remediationId);
        }

        if (loadedSnapshot == null) {
            String err = "No snapshot found for remediation ID: " + remediationId;
            log.error(err);
            return RollbackResult.builder()
                    .remediationId(remediationId)
                    .triggerReason(triggerReason)
                    .success(false)
                    .errorMessage(err)
                    .timestamp(LocalDateTime.now().toString())
                    .build();
        }

        final WorkspaceSnapshot snapshot = loadedSnapshot;

        Path wsPath = Paths.get(snapshot.getWorkspacePath()).toAbsolutePath().normalize();
        Path snapPath = Paths.get(snapshot.getSnapshotPath()).toAbsolutePath().normalize();

        int restoredCount = 0;
        int deletedCount = 0;

        try {
            // Step A: Delete rogue files (files created during failed remediation not in snapshot)
            try (var stream = Files.walk(wsPath)) {
                List<Path> filesToDelete = stream
                        .filter(Files::isRegularFile)
                        .filter(f -> {
                            Path rel = wsPath.relativize(f);
                            if (rel.startsWith(".git")) {
                                return false; // Never delete git internal files
                            }
                            return !snapshot.getFileChecksums().containsKey(rel.toString());
                        })
                        .toList();

                for (Path p : filesToDelete) {
                    Files.deleteIfExists(p);
                    deletedCount++;
                    log.info("Deleted unauthorized rogue file during rollback: {}", wsPath.relativize(p));
                }
            }

            // Step B: Restore / overwrite all original files from snapshot
            for (Map.Entry<String, String> entry : snapshot.getFileChecksums().entrySet()) {
                String relPath = entry.getKey();
                Path source = snapPath.resolve(relPath);
                Path target = wsPath.resolve(relPath);

                if (Files.exists(source)) {
                    Files.createDirectories(target.getParent());
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                    restoredCount++;
                }
            }

            // Step C: Verify restoration
            boolean verified = verifyRestoration(snapshot);
            log.info("Rollback restoration verification outcome for remediationId={}: {}",
                    remediationId, verified);

            // Step D: Record rollback in execution history and remediation entity
            recordRollbackInDatabase(remediationId, triggerReason, verified, restoredCount, deletedCount);

            return RollbackResult.builder()
                    .remediationId(remediationId)
                    .triggerReason(triggerReason)
                    .workspacePath(wsPath.toString())
                    .snapshotPath(snapPath.toString())
                    .filesRestored(restoredCount)
                    .filesDeleted(deletedCount)
                    .restorationVerified(verified)
                    .success(verified)
                    .timestamp(LocalDateTime.now().toString())
                    .build();

        } catch (Exception e) {
            log.error("Failed during rollback execution for remediationId={}: {}", remediationId, e.getMessage(), e);
            return RollbackResult.builder()
                    .remediationId(remediationId)
                    .triggerReason(triggerReason)
                    .workspacePath(wsPath.toString())
                    .snapshotPath(snapPath.toString())
                    .success(false)
                    .errorMessage("Rollback failed: " + e.getMessage())
                    .timestamp(LocalDateTime.now().toString())
                    .build();
        }
    }

    // ────────────────────────────────────────────────────────────────────────────
    // 4. Verification of Restoration
    // ────────────────────────────────────────────────────────────────────────────

    /**
     * Verifies that the workspace matches the snapshot exactly (content checksums match,
     * no unauthorized files present).
     *
     * @param snapshot the workspace snapshot
     * @return true if restoration is 100% verified, false otherwise
     */
    public boolean verifyRestoration(WorkspaceSnapshot snapshot) {
        if (snapshot == null) {
            return false;
        }

        Path wsPath = Paths.get(snapshot.getWorkspacePath()).toAbsolutePath().normalize();
        if (!Files.exists(wsPath)) {
            return false;
        }

        try {
            // Verify all snapshotted files exist with identical checksums
            for (Map.Entry<String, String> entry : snapshot.getFileChecksums().entrySet()) {
                Path file = wsPath.resolve(entry.getKey());
                if (!Files.exists(file)) {
                    log.warn("Verification failed: expected file missing after rollback: {}", entry.getKey());
                    return false;
                }
                String currentHash = computeSha256(file);
                if (!currentHash.equals(entry.getValue())) {
                    log.warn("Verification failed: checksum mismatch for file {}", entry.getKey());
                    return false;
                }
            }

            // Verify no extra files remain (excluding .git)
            try (var stream = Files.walk(wsPath)) {
                boolean hasUnexpected = stream
                        .filter(Files::isRegularFile)
                        .anyMatch(f -> {
                            Path rel = wsPath.relativize(f);
                            if (rel.startsWith(".git")) {
                                return false;
                            }
                            return !snapshot.getFileChecksums().containsKey(rel.toString());
                        });

                if (hasUnexpected) {
                    log.warn("Verification failed: unexpected files remain in workspace after rollback");
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            log.error("Verification error during restoration check: {}", e.getMessage(), e);
            return false;
        }
    }

    // ────────────────────────────────────────────────────────────────────────────
    // 5. Database Recording
    // ────────────────────────────────────────────────────────────────────────────

    private void recordRollbackInDatabase(
            Long remediationId, String triggerReason, boolean verified, int restoredCount, int deletedCount) {

        // 1. Mark Remediation as ROLLED_BACK
        Optional<Remediation> remediationOpt = remediationRepository.findById(remediationId);
        if (remediationOpt.isPresent()) {
            Remediation remediation = remediationOpt.get();

            Map<String, Object> rollbackInfoMap = new LinkedHashMap<>();
            rollbackInfoMap.put("triggerReason", triggerReason);
            rollbackInfoMap.put("restorationVerified", verified);
            rollbackInfoMap.put("filesRestored", restoredCount);
            rollbackInfoMap.put("rogueFilesDeleted", deletedCount);
            rollbackInfoMap.put("timestamp", LocalDateTime.now().toString());

            String rollbackInfoJson;
            try {
                rollbackInfoJson = objectMapper.writeValueAsString(rollbackInfoMap);
            } catch (Exception e) {
                rollbackInfoJson = "triggerReason=" + triggerReason + ", verified=" + verified;
            }

            remediation.markRolledBack(rollbackInfoJson);
            remediationRepository.save(remediation);
            log.info("Updated Remediation {} status to ROLLED_BACK", remediationId);

            // 2. Record rollback in associated Execution history
            if (remediation.getPlan() != null) {
                Long planId = remediation.getPlan().getId();
                List<Execution> executions = executionRepository.findByPlanId(planId);
                for (Execution exec : executions) {
                    String existingLog = exec.getExecutionLog() != null ? exec.getExecutionLog() : "";
                    String rollbackEntry = "\n[ROLLBACK] Workspace restored to pre-remediation state. Trigger: "
                            + triggerReason + ", Restoration verified: " + verified
                            + " at " + LocalDateTime.now();
                    exec.setExecutionLog(existingLog + rollbackEntry);
                    exec.setStatus("ROLLED_BACK");
                    exec.setCompletedAt(LocalDateTime.now());
                    executionRepository.save(exec);
                    log.info("Updated Execution {} status to ROLLED_BACK with audit entry", exec.getId());
                }
            }
        } else {
            log.warn("Could not find Remediation {} to record rollback", remediationId);
        }
    }

    // ────────────────────────────────────────────────────────────────────────────
    // 6. Snapshot Lifecycle Management
    // ────────────────────────────────────────────────────────────────────────────

    /**
     * Cleans up snapshot files from disk when remediation is successfully completed.
     */
    public void cleanupSnapshot(Long remediationId) {
        WorkspaceSnapshot snapshot = activeSnapshots.remove(remediationId);
        if (snapshot != null && snapshot.getSnapshotPath() != null) {
            Path snapPath = Paths.get(snapshot.getSnapshotPath());
            try {
                FileSystemUtils.deleteRecursively(snapPath);
                log.info("Cleaned up snapshot directory at: {}", snapPath);
            } catch (IOException e) {
                log.warn("Failed to delete snapshot directory {}: {}", snapPath, e.getMessage());
            }
        }
    }

    /**
     * Retrieves active snapshot for remediation.
     */
    public Optional<WorkspaceSnapshot> getSnapshot(Long remediationId) {
        WorkspaceSnapshot snap = activeSnapshots.get(remediationId);
        if (snap == null) {
            snap = findSnapshotOnDisk(remediationId);
        }
        return Optional.ofNullable(snap);
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Helpers
    // ────────────────────────────────────────────────────────────────────────────

    private String computeSha256(Path file) throws IOException {
        try (InputStream is = Files.newInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception e) {
            throw new IOException("Failed to compute SHA-256 for " + file, e);
        }
    }

    private void writeSnapshotMetadata(WorkspaceSnapshot snapshot, Path snapshotDir) {
        try {
            Path metaFile = snapshotDir.resolve("snapshot_metadata.json");
            objectMapper.writeValue(metaFile.toFile(), snapshot);
        } catch (Exception e) {
            log.warn("Failed to write snapshot metadata file: {}", e.getMessage());
        }
    }

    private WorkspaceSnapshot findSnapshotOnDisk(Long remediationId) {
        if (!Files.exists(baseSnapshotDir)) {
            return null;
        }
        try (var stream = Files.list(baseSnapshotDir)) {
            String prefix = "snapshot_rem_" + remediationId + "_";
            Optional<Path> found = stream
                    .filter(p -> p.getFileName().toString().startsWith(prefix))
                    .findFirst();

            if (found.isPresent()) {
                Path meta = found.get().resolve("snapshot_metadata.json");
                if (Files.exists(meta)) {
                    return objectMapper.readValue(meta.toFile(), WorkspaceSnapshot.class);
                }
            }
        } catch (Exception e) {
            log.warn("Error scanning disk for snapshot: {}", e.getMessage());
        }
        return null;
    }
}
