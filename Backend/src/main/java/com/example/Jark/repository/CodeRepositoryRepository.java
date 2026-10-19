package com.example.Jark.repository;

import com.example.Jark.entity.CodeRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CodeRepositoryRepository extends JpaRepository<CodeRepository, Long> {
    
    List<CodeRepository> findByUserId(Long userId);
    
    List<CodeRepository> findByUserIdAndStatus(Long userId, String status);
    
    Optional<CodeRepository> findByUserIdAndUrl(Long userId, String url);
    
    boolean existsByUserIdAndUrl(Long userId, String url);
    
    @Query("SELECT r FROM CodeRepository r WHERE r.user.id = :userId AND r.status = :status")
    List<CodeRepository> findActiveRepositoriesByUser(@Param("userId") Long userId, @Param("status") String status);
}
