package com.example.Jark.service;

import com.example.Jark.dto.RepositoryStatusResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class RepositoryValidatorTest {

    private Path tempWorkspace;
    private RepositoryValidator validator;
    private RepositoryStatusResponse response;

    @BeforeEach
    void setUp() throws IOException {
        tempWorkspace = Files.createTempDirectory("repo_test");
        validator = new RepositoryValidator();
        response = new RepositoryStatusResponse();
    }

    @AfterEach
    void tearDown() throws IOException {
        FileSystemUtils.deleteRecursively(tempWorkspace);
    }

    @Test
    void validateJavaSpringProject_WithMavenAndJava_ShouldReturnTrue() throws IOException {
        // Setup valid structure
        Files.createFile(tempWorkspace.resolve("pom.xml"));
        Files.createDirectories(tempWorkspace.resolve("src/main/java"));

        boolean isValid = validator.validateJavaSpringProject(tempWorkspace, response);

        assertTrue(isValid);
        assertTrue(response.isJavaSpring());
        assertTrue(response.getDetectedTechnologies().contains("Maven"));
        assertTrue(response.getDetectedTechnologies().contains("Java"));
        assertNull(response.getErrorMessage());
    }

    @Test
    void validateJavaSpringProject_WithGradleAndJava_ShouldReturnTrue() throws IOException {
        // Setup valid structure
        Files.createFile(tempWorkspace.resolve("build.gradle"));
        Files.createDirectories(tempWorkspace.resolve("src/main/java"));

        boolean isValid = validator.validateJavaSpringProject(tempWorkspace, response);

        assertTrue(isValid);
        assertTrue(response.isJavaSpring());
        assertTrue(response.getDetectedTechnologies().contains("Gradle"));
        assertTrue(response.getDetectedTechnologies().contains("Java"));
    }

    @Test
    void validateJavaSpringProject_WithoutBuildFile_ShouldReturnFalse() throws IOException {
        // Setup missing build file
        Files.createDirectories(tempWorkspace.resolve("src/main/java"));

        boolean isValid = validator.validateJavaSpringProject(tempWorkspace, response);

        assertFalse(isValid);
        assertFalse(response.isJavaSpring());
        assertNotNull(response.getErrorMessage());
        assertTrue(response.getErrorMessage().contains("No Maven"));
    }

    @Test
    void validateJavaSpringProject_WithoutJavaSource_ShouldReturnFalse() throws IOException {
        // Setup missing src/main/java
        Files.createFile(tempWorkspace.resolve("pom.xml"));

        boolean isValid = validator.validateJavaSpringProject(tempWorkspace, response);

        assertFalse(isValid);
        assertFalse(response.isJavaSpring());
        assertNotNull(response.getErrorMessage());
        assertTrue(response.getErrorMessage().contains("No Java source directory"));
    }
}
