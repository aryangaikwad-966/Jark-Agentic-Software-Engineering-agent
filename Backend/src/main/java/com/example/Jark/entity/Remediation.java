package com.example.Jark.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Represents a remediation attempt for an approved plan.
 * Tracks the transformation, diff, and rollback state.
 */
@Entity
@Table(name = "remediations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Remediation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_id", nullable = false)
    private Approval approval;
    
    /**
     * Transformation strategy used: AST, OPENREWRITE, AI_PATCH
     */
    @Column(nullable = false, length = 50)
    private String transformationStrategy;
    
    /**
     * Unified diff of changes applied
     */
    @Column(columnDefinition = "TEXT")
    private String diff;
    
    /**
     * Workspace path where remediation was applied
     */
    @Column(length = 500)
    private String workspacePath;
    
    /**
     * Status: IN_PROGRESS, COMPLETED, FAILED, ROLLED_BACK
     */
    @Column(nullable = false, length = 50)
    private String status = "IN_PROGRESS";
    
    /**
     * Whether the remediation was within approved scope
     */
    @Column(nullable = false)
    private Boolean withinScope = true;
    
    /**
     * Error message if remediation failed
     */
    @Column(columnDefinition = "TEXT")
    private String errorMessage;
    
    /**
     * Files modified during remediation (JSON)
     */
    @Column(columnDefinition = "TEXT")
    private String modifiedFiles;
    
    /**
     * Rollback information if remediation was rolled back
     */
    @Column(columnDefinition = "TEXT")
    private String rollbackInfo;
    
    @CreationTimestamp
    @Column(name = "started_at", nullable = false, updatable = false)
    private LocalDateTime startedAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    /**
     * Validates that this remediation is valid for verification.
     */
    public boolean isValidForVerification() {
        return "COMPLETED".equals(status) && withinScope;
    }
    
    /**
     * Marks the remediation as completed with the given diff.
     */
    public void markCompleted(String diff, String modifiedFiles) {
        this.status = "COMPLETED";
        this.diff = diff;
        this.modifiedFiles = modifiedFiles;
        this.completedAt = LocalDateTime.now();
    }
    
    /**
     * Marks the remediation as failed with an error message.
     */
    public void markFailed(String errorMessage) {
        this.status = "FAILED";
        this.errorMessage = errorMessage;
        this.completedAt = LocalDateTime.now();
    }
    
    /**
     * Marks the remediation as rolled back.
     */
    public void markRolledBack(String rollbackInfo) {
        this.status = "ROLLED_BACK";
        this.rollbackInfo = rollbackInfo;
        this.completedAt = LocalDateTime.now();
    }
}
