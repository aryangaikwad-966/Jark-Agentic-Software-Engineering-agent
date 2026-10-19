package com.example.Jark.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.Jark.entity.Evidence;

@Repository
public interface EvidenceRepository extends JpaRepository<Evidence, Long> {
    
    /**
     * Find all evidence for a specific finding.
     */
    List<Evidence> findByFindingId(Long findingId);
    
    /**
     * Find evidence by source type.
     */
    List<Evidence> findByEvidenceSource(String evidenceSource);
    
    /**
     * Find evidence by type.
     */
    List<Evidence> findByEvidenceType(String evidenceType);
    
    /**
     * Find evidence by class name.
     */
    List<Evidence> findByClassName(String className);
    
    /**
     * Find evidence by file path.
     */
    List<Evidence> findByFilePath(String filePath);
    
    /**
     * Count evidence for a finding by source.
     */
    @Query("SELECT COUNT(e) FROM Evidence e WHERE e.finding.id = :findingId AND e.evidenceSource = :source")
    long countByFindingIdAndEvidenceSource(@Param("findingId") Long findingId, @Param("source") String source);
    
    /**
     * Find evidence with low confidence.
     */
    @Query("SELECT e FROM Evidence e WHERE e.confidence < :threshold")
    List<Evidence> findByConfidenceLessThan(@Param("threshold") Double threshold);
}
