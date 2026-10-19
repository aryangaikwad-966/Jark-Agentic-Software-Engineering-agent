package com.example.Jark.repository;

import com.example.Jark.entity.Remediation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RemediationRepository extends JpaRepository<Remediation, Long> {
    
    List<Remediation> findByPlanId(Long planId);
    
    List<Remediation> findByApprovalId(Long approvalId);
    
    List<Remediation> findByStatus(String status);
    
    List<Remediation> findByPlanIdAndStatus(Long planId, String status);
    
    @Query("SELECT r FROM Remediation r WHERE r.plan.id = :planId AND r.status = 'COMPLETED' ORDER BY r.startedAt DESC")
    List<Remediation> findCompletedByPlanId(@Param("planId") Long planId);
    
    @Query("SELECT r FROM Remediation r WHERE r.approval.id = :approvalId AND r.status = 'COMPLETED'")
    Optional<Remediation> findCompletedByApprovalId(@Param("approvalId") Long approvalId);
    
    @Query("SELECT r FROM Remediation r WHERE r.plan.finding.investigation.id = :investigationId")
    List<Remediation> findByInvestigationId(@Param("investigationId") Long investigationId);
}
