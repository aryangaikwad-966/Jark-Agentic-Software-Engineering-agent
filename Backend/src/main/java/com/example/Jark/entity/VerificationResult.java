package com.example.Jark.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents verification results for a remediation execution.
 * Tracks all 7 verification gates from VXC-180.
 */
@Entity
@Table(name = "verification_results")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerificationResult {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "execution_id")
    private Execution execution;
    
    /**
     * Gate 1: Intended files were modified
     */
    @Column(name = "intended_files_modified")
    private Boolean intendedFilesModified = false;
    
    /**
     * Gate 2: No unauthorized files were modified
     */
    @Column(name = "unauthorized_files_detected")
    private Boolean unauthorizedFilesDetected = false;
    
    /**
     * Gate 3: Actual diff matches the approved Engineering Plan
     */
    @Column(name = "diff_matches_plan")
    private Boolean diffMatchesPlan = false;
    
    /**
     * Gate 4: Project compilation/build succeeds
     */
    @Column(name = "build_status", length = 50)
    private String buildStatus;
    
    /**
     * Gate 5: Tests pass
     */
    @Column(name = "test_status", length = 50)
    private String testStatus;
    
    /**
     * Gate 6: Required static analysis passes
     */
    @Column(name = "static_analysis_status", length = 50)
    private String staticAnalysisStatus;
    
    /**
     * Gate 7: No unexpected repository modifications exist
     */
    @Column(name = "unexpected_modifications")
    private Boolean unexpectedModifications = false;
    
    @Column(name = "total_files_modified")
    private Integer totalFilesModified = 0;

    /**
     * Number of repair attempts made against this failed verification.
     * Bounded by RepairLoopService.MAX_RETRY_ATTEMPTS.
     */
    @Column(name = "repair_attempt_count")
    private Integer repairAttemptCount = 0;
    
    @Column(name = "stdout", columnDefinition = "TEXT")
    private String stdout;
    
    @Column(name = "stderr", columnDefinition = "TEXT")
    private String stderr;
    
    @Column(name = "exit_code")
    private Integer exitCode;
    
    @Column(name = "verification_log", columnDefinition = "TEXT")
    private String verificationLog;
    
    /**
     * Overall status: PENDING, PASSED, FAILED
     */
    @Column(length = 50)
    private String status = "PENDING";
    
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    /**
     * Checks if all 7 gates passed.
     */
    public boolean allGatesPassed() {
        return intendedFilesModified &&
               !unauthorizedFilesDetected &&
               diffMatchesPlan &&
               "SUCCESS".equals(buildStatus) &&
               "PASSED".equals(testStatus) &&
               "PASSED".equals(staticAnalysisStatus) &&
               !unexpectedModifications;
    }
    
    /**
     * Increments the repair attempt counter and returns the new count.
     */
    public int incrementRepairAttemptCount() {
        this.repairAttemptCount = (this.repairAttemptCount == null ? 0 : this.repairAttemptCount) + 1;
        return this.repairAttemptCount;
    }

    /**
     * Marks the verification as passed.
     */
    public void markPassed() {
        this.status = "PASSED";
        this.verifiedAt = LocalDateTime.now();
    }
    
    /**
     * Marks the verification as failed with a reason.
     */
    public void markFailed(String reason) {
        this.status = "FAILED";
        this.verificationLog = reason;
        this.verifiedAt = LocalDateTime.now();
    }
}
