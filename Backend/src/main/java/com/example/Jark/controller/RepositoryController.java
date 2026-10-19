package com.example.Jark.controller;

import com.example.Jark.dto.ApiResponse;
import com.example.Jark.dto.RegisterRepositoryRequest;
import com.example.Jark.dto.RepositoryResponse;
import com.example.Jark.dto.RepositoryStatusResponse;
import com.example.Jark.service.RepositoryService;
import com.example.Jark.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.List;

@RestController
@RequestMapping("/api/v1/repositories")
@RequiredArgsConstructor
public class RepositoryController {

    private final RepositoryService repositoryService;

    @PostMapping
    public ResponseEntity<ApiResponse<RepositoryResponse>> registerRepository(
            @Valid @RequestBody RegisterRepositoryRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        request.setUserId(requireUser(authenticatedUser));
        
        RepositoryResponse response = repositoryService.registerRepository(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Repository registered and ingestion started", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RepositoryResponse>>> listRepositories(
            @RequestParam(required = false) Long userId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        Long authenticatedUserId = requireUser(authenticatedUser);
        if (userId != null && !userId.equals(authenticatedUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Cannot access another user's repositories"));
        }
        
        List<RepositoryResponse> repositories = repositoryService.listUserRepositories(authenticatedUserId);
        return ResponseEntity.ok(ApiResponse.success(repositories));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RepositoryResponse>> getRepository(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        
        RepositoryResponse response = repositoryService.getRepository(id, requireUser(authenticatedUser));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<ApiResponse<RepositoryStatusResponse>> getRepositoryStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        
        RepositoryStatusResponse status = repositoryService.getRepositoryStatus(id, requireUser(authenticatedUser));
        return ResponseEntity.ok(ApiResponse.success(status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRepository(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        
        repositoryService.deleteRepository(id, requireUser(authenticatedUser));
        return ResponseEntity.ok(ApiResponse.success("Repository deleted successfully", null));
    }

    private Long requireUser(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null) {
            throw new org.springframework.security.access.AccessDeniedException("Authentication is required");
        }
        return authenticatedUser.id();
    }
}
