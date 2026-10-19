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
 * Represents user approval for an engineering plan.
 * Prevents code modification without explicit user consent.
 */
@Entity
@Table(name = "approvals")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Approval {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    /**
     * Approval decision: APPROVED, REJECTED, or PENDING_REVIEW
     */
    @Column(nullable = false, length = 50)
    private String decision;
    
    /**
     * User comments explaining the approval/rejection decision.
     */
    @Column(columnDefinition = "TEXT")
    private String comments;
    
    /**
     * Repository context at time of approval for verification.
     */
    @Column(length = 500)
    private String repositoryContext;
    
    /**
     * Workspace path at time of approval for verification.
     */
    @Column(length = 500)
    private String workspaceContext;
    
    /**
     * Whether this approval was used for remediation.
     */
    @Column(nullable = false)
    private Boolean usedForRemediation = false;
    
    @CreationTimestamp
    @Column(name = "approved_at", nullable = false, updatable = false)
    private LocalDateTime approvedAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    /**
     * Validates that this approval is valid for remediation.
     */
    public boolean isValidForRemediation() {
        return "APPROVED".equals(decision) && 
               plan != null && 
               plan.getFinding() != null && 
               "CONFIRMED".equals(plan.getFinding().getValidationStatus());
    }
}
