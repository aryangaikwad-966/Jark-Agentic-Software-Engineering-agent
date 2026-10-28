package com.example.Jark.service;

import com.example.Jark.dto.RegisterRepositoryRequest;
import com.example.Jark.dto.RepositoryResponse;
import com.example.Jark.entity.CodeRepository;
import com.example.Jark.entity.User;
import com.example.Jark.exception.ResourceNotFoundException;
import com.example.Jark.repository.CodeRepositoryRepository;
import com.example.Jark.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RepositoryServiceTest {

    @Mock
    private CodeRepositoryRepository repositoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WorkspaceManager workspaceManager;

    @Mock
    private GitService gitService;

    @Mock
    private RepositoryValidator repositoryValidator;

    @InjectMocks
    private RepositoryService repositoryService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
    }

    @Test
    void registerRepository_ShouldSaveAndTriggerIngestion() {
        // Arrange
        RegisterRepositoryRequest request = new RegisterRepositoryRequest(1L, "https://github.com/test/repo.git", "main");
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(repositoryRepository.existsByUserIdAndUrl(1L, "https://github.com/test/repo.git")).thenReturn(false);
        
        CodeRepository savedRepo = new CodeRepository();
        savedRepo.setId(100L);
        savedRepo.setUser(user);
        savedRepo.setUrl("https://github.com/test/repo.git");
        savedRepo.setName("repo");
        savedRepo.setBranch("main");
        savedRepo.setStatus("CLONING");
        
        when(repositoryRepository.save(any(CodeRepository.class))).thenReturn(savedRepo);
        when(repositoryRepository.findById(100L)).thenReturn(Optional.of(savedRepo));
        
        // Mocking the ingest repository process
        when(workspaceManager.allocateWorkspace(100L)).thenReturn(Path.of("/tmp/test"));
        when(repositoryValidator.validateJavaSpringProject(any(), any())).thenReturn(true);

        // Act
        RepositoryResponse response = repositoryService.registerRepository(request);

        // Assert
        assertNotNull(response);
        assertEquals("repo", response.getName());
        
        // Verify saving was called
        verify(repositoryRepository, atLeastOnce()).save(any(CodeRepository.class));
        verify(gitService).cloneRepository(eq("https://github.com/test/repo.git"), eq("main"), any(Path.class));
    }

    @Test
    void registerRepository_WhenUserNotFound_ShouldThrowException() {
        RegisterRepositoryRequest request = new RegisterRepositoryRequest(999L, "https://github.com/test/repo.git", "main");
        
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> repositoryService.registerRepository(request));
    }

    @Test
    void registerRepository_WhenAlreadyExists_ShouldThrowException() {
        RegisterRepositoryRequest request = new RegisterRepositoryRequest(1L, "https://github.com/test/repo.git", "main");
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(repositoryRepository.existsByUserIdAndUrl(1L, "https://github.com/test/repo.git")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> repositoryService.registerRepository(request));
    }
}
