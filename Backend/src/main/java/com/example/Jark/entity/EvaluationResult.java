package com.example.Jark.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "evaluation_results")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationResult {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "investigation_id", nullable = false)
    private Investigation investigation;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "execution_id")
    private Execution execution;
    
    @Column(name = "rag_precision_score", precision = 5, scale = 2)
    private BigDecimal ragPrecisionScore;
    
    @Column(name = "rag_recall_score", precision = 5, scale = 2)
    private BigDecimal ragRecallScore;
    
    @Column(name = "rag_f1_score", precision = 5, scale = 2)
    private BigDecimal ragF1Score;
    
    @Column(name = "finding_accuracy_score", precision = 5, scale = 2)
    private BigDecimal findingAccuracyScore;
    
    @Column(name = "remediation_success_rate", precision = 5, scale = 2)
    private BigDecimal remediationSuccessRate;
    
    @Column(name = "overall_score", precision = 5, scale = 2)
    private BigDecimal overallScore;
    
    @Column(name = "evaluation_notes", columnDefinition = "TEXT")
    private String evaluationNotes;
    
    @CreationTimestamp
    @Column(name = "evaluated_at", nullable = false, updatable = false)
    private LocalDateTime evaluatedAt;
}
