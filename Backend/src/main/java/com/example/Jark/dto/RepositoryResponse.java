package com.example.Jark.dto;

import com.example.Jark.entity.CodeRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepositoryResponse {
    
    private Long id;
    private Long userId;
    private String name;
    private String url;
    private String branch;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static RepositoryResponse fromEntity(CodeRepository repository) {
        return RepositoryResponse.builder()
                .id(repository.getId())
                .userId(repository.getUser().getId())
                .name(repository.getName())
                .url(repository.getUrl())
                .branch(repository.getBranch())
                .status(repository.getStatus())
                .createdAt(repository.getCreatedAt())
                .updatedAt(repository.getUpdatedAt())
                .build();
    }
}
