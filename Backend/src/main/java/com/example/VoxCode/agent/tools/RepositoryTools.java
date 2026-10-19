package com.example.VoxCode.agent.tools;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import com.example.VoxCode.entity.CodeRepository;
import com.example.VoxCode.exception.ResourceNotFoundException;
import com.example.VoxCode.repository.CodeRepositoryRepository;
import com.example.VoxCode.security.RepositoryContentSanitizer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Spring AI tool functions for repository file operations.
 * All tools are READ-ONLY and enforce strict workspace boundaries to prevent path traversal attacks.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RepositoryTools {

    private final CodeRepositoryRepository repositoryRepository;
    private final RepositoryContentSanitizer contentSanitizer;

    /**
     * Tool function to read a file from the repository workspace.
     * Enforces strict path validation to prevent directory traversal attacks.
     */
    @Description("Read a file from the repository workspace. Returns the file content as text. Only files within the repository workspace are accessible.")
    public Function<ReadFileRequest, ReadFileResponse> readFile() {
        return request -> {
            log.info("Tool call: readFile with filePath='{}' in repository {}", 
                    request.filePath(), request.repositoryId());
            
            try {
                CodeRepository repository = repositoryRepository.findById(request.repositoryId())
                        .orElseThrow(() -> new ResourceNotFoundException("Repository", request.repositoryId()));
                
                if (repository.getLocalPath() == null) {
                    return errorReadFile("Repository workspace not available");
                }
                
                Path workspacePath = Paths.get(repository.getLocalPath()).toRealPath();
                Path requestedPath = workspacePath.resolve(request.filePath()).normalize();
                
                // Security check: ensure the requested path is within the workspace
                if (!requestedPath.startsWith(workspacePath)) {
                    log.warn("Path traversal attempt detected: {} outside workspace {}", requestedPath, workspacePath);
                    return errorReadFile("Access denied: path outside repository workspace");
                }
                
                // Security check: ensure the file exists and is readable
                if (!Files.exists(requestedPath) || !Files.isRegularFile(requestedPath)) {
                    return errorReadFile("File not found or not accessible");
                }
                requestedPath = requestedPath.toRealPath();
                if (!requestedPath.startsWith(workspacePath)) {
                    log.warn("Symlink traversal attempt detected: {} outside workspace {}", requestedPath, workspacePath);
                    return errorReadFile("Access denied: path outside repository workspace");
                }
                
                // Security check: prevent reading sensitive files
                String fileName = requestedPath.getFileName().toString().toLowerCase();
                if (contentSanitizer.isSensitiveFileName(fileName)) {
                    log.warn("Attempt to read sensitive file blocked: {}", requestedPath);
                    return errorReadFile("Access denied: sensitive file");
                }
                
                String content = contentSanitizer.sanitizeForLlm(Files.readString(requestedPath));
                return successReadFile(content, requestedPath.toString());
                
            } catch (ResourceNotFoundException e) {
                log.error("Repository not found", e);
                return errorReadFile("Repository not found");
            } catch (Exception e) {
                log.error("Error in readFile tool", e);
                return errorReadFile(e.getMessage());
            }
        };
    }

    /**
     * Tool function to list files in a directory from the repository workspace.
     * Enforces strict path validation to prevent directory traversal attacks.
     */
    @Description("List files in a directory from the repository workspace. Returns a list of file paths and basic information. Only directories within the repository workspace are accessible.")
    public Function<ListFilesRequest, ListFilesResponse> listFiles() {
        return request -> {
            log.info("Tool call: listFiles with directoryPath='{}' in repository {}", 
                    request.directoryPath(), request.repositoryId());
            
            try {
                CodeRepository repository = repositoryRepository.findById(request.repositoryId())
                        .orElseThrow(() -> new ResourceNotFoundException("Repository", request.repositoryId()));
                
                if (repository.getLocalPath() == null) {
                    return errorListFiles("Repository workspace not available");
                }
                
                Path workspacePath = Paths.get(repository.getLocalPath()).toRealPath();
                Path requestedPath = workspacePath.resolve(request.directoryPath() != null ? request.directoryPath() : "").normalize();
                
                // Security check: ensure the requested path is within the workspace
                if (!requestedPath.startsWith(workspacePath)) {
                    log.warn("Path traversal attempt detected: {} outside workspace {}", requestedPath, workspacePath);
                    return errorListFiles("Access denied: path outside repository workspace");
                }
                
                // Security check: ensure the directory exists and is readable
                if (!Files.exists(requestedPath) || !Files.isDirectory(requestedPath)) {
                    return errorListFiles("Directory not found or not accessible");
                }
                requestedPath = requestedPath.toRealPath();
                if (!requestedPath.startsWith(workspacePath)) {
                    log.warn("Symlink traversal attempt detected: {} outside workspace {}", requestedPath, workspacePath);
                    return errorListFiles("Access denied: path outside repository workspace");
                }
                
                // List files with basic information
                List<FileInfo> files;
                try {
                    files = Files.list(requestedPath)
                            .filter(Files::isRegularFile)
                            .map(path -> {
                                try {
                                    return new FileInfo(
                                            workspacePath.relativize(path).toString(),
                                            path.getFileName().toString(),
                                            Files.size(path),
                                            Files.getLastModifiedTime(path).toString()
                                    );
                                } catch (Exception e) {
                                    log.warn("Failed to get file info for {}", path, e);
                                    return null;
                                }
                            })
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                } catch (Exception e) {
                    log.error("Failed to list files in directory", e);
                    return errorListFiles("Failed to list files: " + e.getMessage());
                }
                
                return successListFiles(files, requestedPath.toString());
                
            } catch (ResourceNotFoundException e) {
                log.error("Repository not found", e);
                return errorListFiles("Repository not found");
            } catch (Exception e) {
                log.error("Error in listFiles tool", e);
                return errorListFiles(e.getMessage());
            }
        };
    }

    // Request/Response DTOs for tool functions

    public record ReadFileRequest(
            Long repositoryId,
            String filePath
    ) {}

    public record ReadFileResponse(
            boolean success,
            String message,
            String content,
            String filePath
    ) {}

    public record ListFilesRequest(
            Long repositoryId,
            String directoryPath
    ) {}

    public record ListFilesResponse(
            boolean success,
            String message,
            List<FileInfo> files,
            String directoryPath
    ) {}

    public record FileInfo(
            String relativePath,
            String fileName,
            long size,
            String lastModified
    ) {}

    // Helper methods for creating responses
    private static ReadFileResponse successReadFile(String content, String filePath) {
        return new ReadFileResponse(true, "File read successfully", content, filePath);
    }

    private static ReadFileResponse errorReadFile(String message) {
        return new ReadFileResponse(false, message, null, null);
    }

    private static ListFilesResponse successListFiles(List<FileInfo> files, String directoryPath) {
        return new ListFilesResponse(true, "Files listed successfully", files, directoryPath);
    }

    private static ListFilesResponse errorListFiles(String message) {
        return new ListFilesResponse(false, message, null, null);
    }
}
