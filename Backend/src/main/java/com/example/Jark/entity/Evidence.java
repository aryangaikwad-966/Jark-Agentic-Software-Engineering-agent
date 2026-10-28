package com.example.Jark.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
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
 * Represents a piece of evidence supporting a finding.
 * Links LLM conclusions to deterministic repository artifacts (AST nodes, file paths, etc.).
 */
@Entity
@Table(name = "evidence")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Evidence {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "finding_id", nullable = false)
    private Finding finding;
    
    /**
     * Source of the evidence (AST, GRAPH, RAG, REPOSITORY, TOOL_CALL).
     */
    @Column(name = "evidence_source", nullable = false, length = 50)
    private String evidenceSource;
    
    /**
     * Type of evidence (CLASS_NODE, METHOD_NODE, ANNOTATION, DEPENDENCY, SEMANTIC_CHUNK, FILE_CONTENT).
     */
    @Column(name = "evidence_type", nullable = false, length = 50)
    private String evidenceType;
    
    /**
     * File path referenced by this evidence.
     */
    @Column(name = "file_path", length = 500)
    private String filePath;
    
    /**
     * Line range referenced by this evidence.
     */
    @Column(name = "line_range", length = 50)
    private String lineRange;
    
    /**
     * Class name referenced by this evidence.
     */
    @Column(name = "class_name", length = 255)
    private String className;
    
    /**
     * Method name referenced by this evidence.
     */
    @Column(name = "method_name", length = 255)
    private String methodName;
    
    /**
     * Annotation name referenced by this evidence.
     */
    @Column(name = "annotation_name", length = 255)
    private String annotationName;
    
    /**
     * The actual evidence content or observation.
     */
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;
    
    /**
     * Confidence in this evidence (0.0 to 1.0).
     */
    @Column(name = "confidence")
    private Double confidence;
    
    /**
     * Additional metadata as JSON.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "JSON")
    private String metadata;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * Validates that this evidence has required fields based on its type.
     */
    public boolean isValid() {
        if (evidenceSource == null || evidenceSource.isBlank()) {
            return false;
        }
        if (evidenceType == null || evidenceType.isBlank()) {
            return false;
        }
        
        // Type-specific validation
        return switch (evidenceType) {
            case "CLASS_NODE", "METHOD_NODE", "ANNOTATION" -> 
                className != null && !className.isBlank();
            case "FILE_CONTENT" -> 
                filePath != null && !filePath.isBlank();
            case "SEMANTIC_CHUNK" -> 
                content != null && !content.isBlank();
            default -> true;
        };
    }
}
