package com.example.Jark.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.Jark.entity.Plan;

@Repository
public interface PlanRepository extends JpaRepository<Plan, Long> {
    
    List<Plan> findByInvestigationId(Long investigationId);
    
    List<Plan> findByInvestigationIdAndStatus(Long investigationId, String status);
    
    List<Plan> findByStatus(String status);
    
    Optional<Plan> findByFindingId(Long findingId);
    
    @Query("SELECT p FROM Plan p WHERE p.finding.investigation.id = :investigationId")
    List<Plan> findByInvestigationViaFinding(@Param("investigationId") Long investigationId);
}
