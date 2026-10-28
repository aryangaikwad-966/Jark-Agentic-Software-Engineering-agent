package com.example.Jark.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.Jark.entity.Approval;

@Repository
public interface ApprovalRepository extends JpaRepository<Approval, Long> {
    
    List<Approval> findByPlanId(Long planId);
    
    List<Approval> findByUserId(Long userId);
    
    Optional<Approval> findByPlanIdAndUserId(Long planId, Long userId);
    
    List<Approval> findByDecision(String decision);
    
    @Query("SELECT a FROM Approval a WHERE a.plan.id = :planId AND a.decision = 'APPROVED' ORDER BY a.approvedAt DESC")
    List<Approval> findApprovedByPlanId(@Param("planId") Long planId);
    
    @Query("SELECT a FROM Approval a WHERE a.plan.id = :planId AND a.decision = 'APPROVED' AND a.usedForRemediation = false")
    Optional<Approval> findUnusedApprovalForPlan(@Param("planId") Long planId);
    
    @Query("SELECT a FROM Approval a WHERE a.plan.finding.investigation.id = :investigationId AND a.decision = 'APPROVED'")
    List<Approval> findApprovedByInvestigationId(@Param("investigationId") Long investigationId);
}
