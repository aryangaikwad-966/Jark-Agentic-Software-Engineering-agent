package com.example.Jark.service;

import com.example.Jark.exception.RepositoryIngestionException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

@Slf4j
@Service
public class GitService {

    /**
     * Clones a remote git repository to the specified local path.
     *
     * @param url The remote Git repository URL
     * @param branch The branch to checkout
     * @param targetDir The local directory to clone into
     * @throws RepositoryIngestionException if cloning fails
     */
    public void cloneRepository(String url, String branch, Path targetDir) {
        log.info("Cloning repository {} (branch: {}) into {}", url, branch, targetDir);
        
        try (Git git = Git.cloneRepository()
                .setURI(url)
                .setDirectory(targetDir.toFile())
                .setBranch(branch)
                .setCloneAllBranches(false)
                .call()) {
            
            log.info("Successfully cloned repository");
            
        } catch (GitAPIException e) {
            log.error("Failed to clone repository: {}", e.getMessage());
            throw new RepositoryIngestionException("Git clone failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Checks if a repository directory is a valid git repository.
     */
    public boolean isValidGitRepository(Path repoDir) {
        return repoDir != null && repoDir.resolve(".git").toFile().exists();
    }
}
