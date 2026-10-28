package com.example.Jark.service;

import com.example.Jark.dto.RepositoryStatusResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class RepositoryValidator {

    /**
     * Validates that the given workspace contains a recognizable Java/Spring project.
     *
     * @param workspacePath Path to the root of the repository
     * @param response The status response to update with findings
     * @return true if valid, false otherwise
     */
    public boolean validateJavaSpringProject(Path workspacePath, RepositoryStatusResponse response) {
        List<String> technologies = new ArrayList<>();
        boolean hasBuildFile = false;
        boolean hasJavaSource = false;

        // Check for build files
        if (Files.exists(workspacePath.resolve("pom.xml"))) {
            technologies.add("Maven");
            hasBuildFile = true;
        } else if (Files.exists(workspacePath.resolve("build.gradle")) || Files.exists(workspacePath.resolve("build.gradle.kts"))) {
            technologies.add("Gradle");
            hasBuildFile = true;
        }

        // Check for Java source directory
        if (Files.exists(workspacePath.resolve("src/main/java"))) {
            technologies.add("Java");
            hasJavaSource = true;
        }

        response.setDetectedTechnologies(technologies);

        if (!hasBuildFile) {
            response.setErrorMessage("No Maven (pom.xml) or Gradle (build.gradle) file found.");
            response.setJavaSpring(false);
            return false;
        }

        if (!hasJavaSource) {
            response.setErrorMessage("No Java source directory (src/main/java) found.");
            response.setJavaSpring(false);
            return false;
        }

        // Simplistic check for Spring Boot (in a real implementation, this would parse the pom.xml/build.gradle)
        response.setJavaSpring(true);
        if (!technologies.contains("Spring Boot")) {
            technologies.add("Spring Boot (Assumed via Java project)");
        }
        
        return true;
    }
}
