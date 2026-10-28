package com.example.Jark.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a finding discovered during investigation.
 * Contains structured evidence linking LLM conclusions to deterministic repository artifacts.
 */
@Entity
@Table(name = "findings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Finding {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "investigation_id", nullable = false)
    private Investigation investigation;
    
    /**
     * The repository where this finding was discovered.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id", nullable = false)
    private CodeRepository repository;
    
    /**
     * File path where the issue was found.
     */
    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;
    
    /**
     * Line range where the issue was found (start-end).
     */
    @Column(name = "line_range", length = 50)
    private String lineRange;
    
    /**
     * Class name where the issue was found.
     */
    @Column(name = "class_name", length = 255)
    private String className;
    
    /**
     * Method name where the issue was found.
     */
    @Column(name = "method_name", length = 255)
    private String methodName;
    
    /**
     * Type of issue (e.g., SECURITY, VALIDATION, ARCHITECTURE, ERROR_HANDLING).
     */
    @Column(name = "issue_type", nullable = false, length = 50)
    private String issueType;
    
    /**
     * Severity level (CRITICAL, HIGH, MEDIUM, LOW).
     */
    @Column(name = "severity", nullable = false, length = 20)
    private String severity;
    
    /**
     * Human-readable description of the finding.
     */
    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String description;
    
    /**
     * Current validation status (PENDING, CONFIRMED, REJECTED, INSUFFICIENT_EVIDENCE).
     */
    @Column(name = "validation_status", nullable = false, length = 30)
    private String validationStatus;
    
    /**
     * Detailed explanation of the finding.
     */
    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;
    
    /**
     * Suggested remediation (if applicable).
     */
    @Column(name = "suggested_remediation", columnDefinition = "TEXT")
    private String suggestedRemediation;
    
    /**
     * Additional metadata as JSON.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "JSON")
    private String metadata;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * Evidence items supporting this finding.
     */
    @OneToMany(mappedBy = "finding", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Evidence> evidence = new ArrayList<>();
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
