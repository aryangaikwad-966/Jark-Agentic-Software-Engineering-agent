package com.example.Jark.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

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
 * Represents an engineering plan generated from a CONFIRMED finding.
 * Contains detailed remediation information with scope boundaries and rollback strategies.
 */
@Entity
@Table(name = "plans")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Plan {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "investigation_id", nullable = false)
    private Investigation investigation;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "finding_id", nullable = false)
    private Finding finding;
    
    @Column(nullable = false, length = 255)
    private String title;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;
    
    /**
     * Root cause analysis of the issue.
     */
    @Column(columnDefinition = "TEXT")
    private String rootCause;
    
    /**
     * List of affected file paths.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "affected_files", columnDefinition = "JSON")
    private String affectedFiles;
    
    /**
     * List of affected classes and methods.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "affected_artifacts", columnDefinition = "JSON")
    private String affectedArtifacts;
    
    /**
     * Detailed proposed changes.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "proposed_changes", nullable = false, columnDefinition = "JSON")
    private String proposedChanges;
    
    /**
     * Transformation strategy for implementing changes.
     */
    @Column(columnDefinition = "TEXT")
    private String transformationStrategy;
    
    /**
     * Risk assessment of the proposed changes.
     */
    @Column(columnDefinition = "TEXT")
    private String risk;
    
    /**
     * Expected behavior after changes are applied.
     */
    @Column(columnDefinition = "TEXT")
    private String expectedBehavior;
    
    /**
     * Strategy for verifying the changes work correctly.
     */
    @Column(columnDefinition = "TEXT")
    private String verificationStrategy;
    
    /**
     * Strategy for rolling back changes if issues occur.
     */
    @Column(columnDefinition = "TEXT")
    private String rollbackStrategy;
    
    /**
     * Scope boundaries - what is allowed to change.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "scope_boundaries", columnDefinition = "JSON")
    private String scopeBoundaries;
    
    /**
     * Estimated impact of the changes.
     */
    @Column(name = "estimated_impact", columnDefinition = "TEXT")
    private String estimatedImpact;
    
    @Column(length = 50)
    private String status = "PENDING_APPROVAL";
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
