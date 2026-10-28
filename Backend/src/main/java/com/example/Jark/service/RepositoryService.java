package com.example.Jark.service;

import com.example.Jark.dto.RegisterRepositoryRequest;
import com.example.Jark.dto.RepositoryResponse;
import com.example.Jark.dto.RepositoryStatusResponse;
import com.example.Jark.entity.CodeRepository;
import com.example.Jark.entity.User;
import com.example.Jark.exception.ResourceNotFoundException;
import com.example.Jark.repository.CodeRepositoryRepository;
import com.example.Jark.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RepositoryService {

    private final CodeRepositoryRepository repositoryRepository;
    private final UserRepository userRepository;
    private final WorkspaceManager workspaceManager;
    private final GitService gitService;
    private final RepositoryValidator repositoryValidator;

    /**
     * Registers a new repository and starts the ingestion process.
     * Note: In a real system, the cloning would be fully async/event-driven.
     * For now, we perform it synchronously or in a simple async block.
     */
    @Transactional
    public RepositoryResponse registerRepository(RegisterRepositoryRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.getUserId()));

        if (repositoryRepository.existsByUserIdAndUrl(user.getId(), request.getUrl())) {
            throw new IllegalArgumentException("Repository already registered for this user");
        }

        CodeRepository repository = new CodeRepository();
        repository.setUser(user);
        repository.setUrl(request.getUrl());
        repository.setBranch(request.getBranch());
        
        // Extract a simple name from URL
        String name = extractNameFromUrl(request.getUrl());
        repository.setName(name);
        repository.setStatus("CLONING");

        CodeRepository saved = repositoryRepository.save(repository);
        
        // Start ingestion process (synchronous for MVP, can be moved to @Async)
        ingestRepository(saved.getId());

        // Re-fetch to get updated status
        return RepositoryResponse.fromEntity(repositoryRepository.findById(saved.getId()).orElse(saved));
    }

    /**
     * Performs the actual clone and validation.
     */
    public void ingestRepository(Long repositoryId) {
        CodeRepository repository = repositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Repository", repositoryId));
        
        try {
            // Allocate isolated workspace
            Path workspacePath = workspaceManager.allocateWorkspace(repositoryId);
            
            // Clone repository
            gitService.cloneRepository(repository.getUrl(), repository.getBranch(), workspacePath);
            
            // Validate repository
            RepositoryStatusResponse validationResponse = new RepositoryStatusResponse();
            boolean isValid = repositoryValidator.validateJavaSpringProject(workspacePath, validationResponse);
            
            if (isValid) {
                repository.setStatus("READY");
                repository.setLocalPath(workspacePath.toString());
            } else {
                repository.setStatus("FAILED");
                workspaceManager.cleanupWorkspace(workspacePath);
            }
            
            repositoryRepository.save(repository);
            
        } catch (Exception e) {
            log.error("Failed to ingest repository {}", repositoryId, e);
            repository.setStatus("FAILED");
            repositoryRepository.save(repository);
        }
    }

    @Transactional(readOnly = true)
    public RepositoryResponse getRepository(Long id) {
        CodeRepository repo = repositoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Repository", id));
        return RepositoryResponse.fromEntity(repo);
    }

    /** Returns a repository only when it belongs to the authenticated user. */
    @Transactional(readOnly = true)
    public RepositoryResponse getRepository(Long id, Long userId) {
        return RepositoryResponse.fromEntity(findOwnedRepository(id, userId));
    }

    @Transactional(readOnly = true)
    public List<RepositoryResponse> listUserRepositories(Long userId) {
        return repositoryRepository.findByUserId(userId).stream()
                .map(RepositoryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RepositoryStatusResponse getRepositoryStatus(Long id) {
        CodeRepository repo = repositoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Repository", id));
        
        RepositoryStatusResponse status = new RepositoryStatusResponse();
        status.setId(repo.getId());
        status.setName(repo.getName());
        status.setStatus(repo.getStatus());
        status.setLastUpdatedAt(repo.getUpdatedAt());
        
        return status;
    }

    @Transactional(readOnly = true)
    public RepositoryStatusResponse getRepositoryStatus(Long id, Long userId) {
        CodeRepository repo = findOwnedRepository(id, userId);
        RepositoryStatusResponse status = new RepositoryStatusResponse();
        status.setId(repo.getId());
        status.setName(repo.getName());
        status.setStatus(repo.getStatus());
        status.setLastUpdatedAt(repo.getUpdatedAt());
        return status;
    }

    @Transactional
    public void deleteRepository(Long id) {
        CodeRepository repo = repositoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Repository", id));
        
        if (repo.getLocalPath() != null) {
            workspaceManager.cleanupWorkspace(Path.of(repo.getLocalPath()));
        }
        
        repositoryRepository.delete(repo);
    }

    @Transactional
    public void deleteRepository(Long id, Long userId) {
        CodeRepository repo = findOwnedRepository(id, userId);
        if (repo.getLocalPath() != null) {
            workspaceManager.cleanupWorkspace(Path.of(repo.getLocalPath()));
        }
        repositoryRepository.delete(repo);
    }

    private CodeRepository findOwnedRepository(Long id, Long userId) {
        CodeRepository repository = repositoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Repository", id));
        if (repository.getUser() == null || !userId.equals(repository.getUser().getId())) {
            // Do not disclose the existence of another user's repository.
            throw new ResourceNotFoundException("Repository", id);
        }
        return repository;
    }

    private String extractNameFromUrl(String url) {
        String name = url;
        if (name.endsWith(".git")) {
            name = name.substring(0, name.length() - 4);
        }
        int lastSlash = name.lastIndexOf('/');
        if (lastSlash >= 0) {
            name = name.substring(lastSlash + 1);
        }
        return name;
    }
}
