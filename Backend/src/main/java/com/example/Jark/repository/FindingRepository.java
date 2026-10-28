package com.example.Jark.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.Jark.entity.Finding;

@Repository
public interface FindingRepository extends JpaRepository<Finding, Long> {
    
    /**
     * Find all findings for a specific investigation.
     */
    List<Finding> findByInvestigationId(Long investigationId);
    
    /**
     * Find all findings for a specific repository.
     */
    List<Finding> findByRepositoryId(Long repositoryId);
    
    /**
     * Find findings by validation status.
     */
    List<Finding> findByValidationStatus(String validationStatus);
    
    /**
     * Find findings by issue type.
     */
    List<Finding> findByIssueType(String issueType);
    
    /**
     * Find findings by severity.
     */
    List<Finding> findBySeverity(String severity);
    
    /**
     * Find a finding by file path and line range.
     */
    Optional<Finding> findByFilePathAndLineRange(String filePath, String lineRange);
    
    /**
     * Count findings by validation status for an investigation.
     */
    @Query("SELECT COUNT(f) FROM Finding f WHERE f.investigation.id = :investigationId AND f.validationStatus = :status")
    long countByInvestigationIdAndValidationStatus(@Param("investigationId") Long investigationId, @Param("status") String status);
}
