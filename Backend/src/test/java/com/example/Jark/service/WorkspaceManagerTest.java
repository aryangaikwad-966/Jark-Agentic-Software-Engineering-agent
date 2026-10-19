package com.example.Jark.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class WorkspaceManagerTest {

    private Path tempBaseDir;
    private WorkspaceManager workspaceManager;

    @BeforeEach
    void setUp() throws IOException {
        tempBaseDir = Files.createTempDirectory("jark_test_base");
        workspaceManager = new WorkspaceManager(tempBaseDir.toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        FileSystemUtils.deleteRecursively(tempBaseDir);
    }

    @Test
    void allocateWorkspace_ShouldCreateUniqueDirectory() {
        Path workspacePath = workspaceManager.allocateWorkspace(123L);

        assertTrue(Files.exists(workspacePath));
        assertTrue(workspacePath.startsWith(tempBaseDir));
        assertTrue(workspacePath.getFileName().toString().startsWith("repo_123_"));
    }

    @Test
    void cleanupWorkspace_ShouldDeleteDirectory() throws IOException {
        Path workspacePath = workspaceManager.allocateWorkspace(123L);
        // Create a dummy file inside to ensure recursive deletion works
        Files.writeString(workspacePath.resolve("dummy.txt"), "test");

        assertTrue(Files.exists(workspacePath));

        workspaceManager.cleanupWorkspace(workspacePath);

        assertFalse(Files.exists(workspacePath));
    }

    @Test
    void cleanupWorkspace_ShouldRefuseToDeleteOutsideBaseDir() throws IOException {
        Path outsidePath = Files.createTempDirectory("outside_workspace");
        
        SecurityException exception = assertThrows(SecurityException.class, () -> 
            workspaceManager.cleanupWorkspace(outsidePath)
        );
        
        assertEquals("Attempted to delete directory outside workspace base path", exception.getMessage());
        
        // Clean up
        Files.deleteIfExists(outsidePath);
    }
}
