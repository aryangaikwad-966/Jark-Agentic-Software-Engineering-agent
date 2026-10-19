package com.example.Jark.service;

import com.example.Jark.exception.RepositoryIngestionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Service
public class WorkspaceManager {

    private final Path baseWorkspaceDir;

    public WorkspaceManager(@Value("${jark.workspace.base-dir:/tmp/jark/workspaces}") String baseDirStr) {
        this.baseWorkspaceDir = Paths.get(baseDirStr).toAbsolutePath().normalize();
        initializeBaseDirectory();
    }

    private void initializeBaseDirectory() {
        try {
            if (!Files.exists(baseWorkspaceDir)) {
                Files.createDirectories(baseWorkspaceDir);
                log.info("Created base workspace directory at: {}", baseWorkspaceDir);
            }
        } catch (IOException e) {
            throw new RepositoryIngestionException("Failed to initialize base workspace directory", e);
        }
    }

    /**
     * Allocates a new isolated workspace directory for a repository.
     *
     * @param repositoryId The ID of the repository
     * @return Path to the newly created workspace
     */
    public Path allocateWorkspace(Long repositoryId) {
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        String workspaceName = "repo_" + repositoryId + "_" + uniqueId;
        
        Path workspacePath = baseWorkspaceDir.resolve(workspaceName).normalize();
        
        // Prevent path traversal
        if (!workspacePath.startsWith(baseWorkspaceDir)) {
            throw new SecurityException("Path traversal attempt detected in workspace allocation");
        }

        try {
            Files.createDirectories(workspacePath);
            log.debug("Allocated new workspace at: {}", workspacePath);
            return workspacePath;
        } catch (IOException e) {
            throw new RepositoryIngestionException("Failed to create workspace directory", e);
        }
    }

    /**
     * Cleans up (deletes) a workspace directory.
     *
     * @param workspacePath Path to the workspace to clean up
     */
    public void cleanupWorkspace(Path workspacePath) {
        if (workspacePath == null || !Files.exists(workspacePath)) {
            return;
        }

        Path normalizedPath = workspacePath.normalize().toAbsolutePath();
        if (!normalizedPath.startsWith(baseWorkspaceDir)) {
            log.error("Refusing to clean up directory outside base workspace path: {}", normalizedPath);
            throw new SecurityException("Attempted to delete directory outside workspace base path");
        }

        try {
            FileSystemUtils.deleteRecursively(normalizedPath);
            log.debug("Cleaned up workspace at: {}", normalizedPath);
        } catch (IOException e) {
            log.warn("Failed to clean up workspace {}: {}", normalizedPath, e.getMessage());
        }
    }
}
