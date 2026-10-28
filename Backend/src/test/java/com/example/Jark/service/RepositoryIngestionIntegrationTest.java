package com.example.Jark.service;

import com.example.Jark.dto.RepositoryStatusResponse;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RepositoryIngestionIntegrationTest {

    @TempDir
    private Path temporaryDirectory;

    private Path sourceRepository;
    private String sourceBranch;
    private GitService gitService;
    private WorkspaceManager workspaceManager;
    private RepositoryValidator repositoryValidator;

    @BeforeEach
    void setUp() throws IOException, GitAPIException {
        sourceRepository = temporaryDirectory.resolve("fixture-repository");
        createJavaProjectFixture(sourceRepository);

        try (Git git = Git.init().setDirectory(sourceRepository.toFile()).call()) {
            git.add().addFilepattern(".").call();
            git.commit()
                    .setMessage("Create Java project fixture")
                    .setAuthor("Jark Test", "jark-test@example.com")
                    .setCommitter("Jark Test", "jark-test@example.com")
                    .call();
            sourceBranch = git.getRepository().getBranch();
        }

        gitService = new GitService();
        workspaceManager = new WorkspaceManager(temporaryDirectory.resolve("workspaces").toString());
        repositoryValidator = new RepositoryValidator();
    }

    @Test
    void cloneAndValidateLocalJavaProject() {
        Path workspace = workspaceManager.allocateWorkspace(42L);

        gitService.cloneRepository(sourceRepository.toUri().toString(), sourceBranch, workspace);

        RepositoryStatusResponse response = new RepositoryStatusResponse();
        boolean validProject = repositoryValidator.validateJavaSpringProject(workspace, response);

        assertTrue(gitService.isValidGitRepository(workspace));
        assertTrue(Files.exists(workspace.resolve("pom.xml")));
        assertTrue(Files.exists(workspace.resolve("src/main/java/com/example/FixtureController.java")));
        assertTrue(validProject);
        assertTrue(response.isJavaSpring());
        assertTrue(response.getDetectedTechnologies().contains("Maven"));
        assertTrue(response.getDetectedTechnologies().contains("Java"));
    }

    private void createJavaProjectFixture(Path fixtureRoot) throws IOException {
        Files.createDirectories(fixtureRoot.resolve("src/main/java/com/example"));
        Files.writeString(fixtureRoot.resolve("pom.xml"), """
                <project xmlns=\"http://maven.apache.org/POM/4.0.0\">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>fixture</artifactId>
                    <version>1.0.0</version>
                </project>
                """);
        Files.writeString(fixtureRoot.resolve("src/main/java/com/example/FixtureController.java"), """
                package com.example;

                public class FixtureController {
                    public String status() {
                        return \"ready\";
                    }
                }
                """);
    }
}
