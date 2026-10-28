package com.example.Jark.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRepositoryRequest {
    
    @NotNull(message = "User ID is required")
    private Long userId;
    
    @NotBlank(message = "Repository URL is required")
    @Pattern(regexp = "^(https?://|git@).*\\.git$", message = "Must be a valid Git URL ending in .git")
    private String url;
    
    @Builder.Default
    private String branch = "main";
}
