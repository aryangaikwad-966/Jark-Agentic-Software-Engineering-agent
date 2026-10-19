package com.example.Jark.repository;

import com.example.Jark.entity.Execution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExecutionRepository extends JpaRepository<Execution, Long> {
    
    List<Execution> findByPlanId(Long planId);
    
    List<Execution> findByPlanIdAndStatus(Long planId, String status);
    
    List<Execution> findByStatus(String status);
}
