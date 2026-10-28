package com.example.Jark.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepositoryStatusResponse {
    
    private Long id;
    private String name;
    private String status; // "PENDING", "CLONING", "VALIDATING", "READY", "FAILED"
    private boolean isJavaSpring;
    private List<String> detectedTechnologies;
    private String errorMessage;
    private LocalDateTime lastUpdatedAt;
}
