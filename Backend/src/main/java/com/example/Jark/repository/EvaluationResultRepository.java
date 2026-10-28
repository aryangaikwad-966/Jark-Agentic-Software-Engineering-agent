package com.example.Jark.repository;

import com.example.Jark.entity.EvaluationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvaluationResultRepository extends JpaRepository<EvaluationResult, Long> {
    
    List<EvaluationResult> findByInvestigationId(Long investigationId);
    
    List<EvaluationResult> findByExecutionId(Long executionId);
}
