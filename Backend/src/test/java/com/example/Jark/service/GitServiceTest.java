package com.example.Jark.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class GitServiceTest {

    private GitService gitService;
    private Path tempWorkspace;

    @BeforeEach
    void setUp() throws IOException {
        gitService = new GitService();
        tempWorkspace = Files.createTempDirectory("git_test");
    }

    @AfterEach
    void tearDown() throws IOException {
        FileSystemUtils.deleteRecursively(tempWorkspace);
    }

    @Test
    void isValidGitRepository_WithGitFolder_ShouldReturnTrue() throws IOException {
        Files.createDirectories(tempWorkspace.resolve(".git"));
        
        assertTrue(gitService.isValidGitRepository(tempWorkspace));
    }

    @Test
    void isValidGitRepository_WithoutGitFolder_ShouldReturnFalse() {
        assertFalse(gitService.isValidGitRepository(tempWorkspace));
    }
}
