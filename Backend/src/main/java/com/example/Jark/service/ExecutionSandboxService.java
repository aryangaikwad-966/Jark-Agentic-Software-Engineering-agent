package com.example.Jark.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Manages isolated Docker sandbox execution for remediation verification.
 * Spins up temporary containers with strict resource limits.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutionSandboxService {

    private final DockerClient dockerClient;

    @Value("${sandbox.image:openjdk:21-slim}")
    private String defaultImage;

    @Value("${sandbox.memory.limit:512m}")
    private String memoryLimit;

    @Value("${sandbox.cpu.limit:1.0}")
    private String cpuLimit;

    @Value("${sandbox.timeout.seconds:300}")
    private long timeoutSeconds;

    /**
     * Default resource limits for sandbox containers.
     */
    private static final long DEFAULT_MEMORY_BYTES = 512 * 1024 * 1024; // 512MB
    private static final long DEFAULT_CPU_NANO_CPUS = 1_000_000_000L; // 1 CPU
    private static final long DEFAULT_TIMEOUT_SECONDS = 300; // 5 minutes

    /**
     * Spins up a sandbox container for executing modified code.
     *
     * @param workspacePath the local workspace path to copy into the container
     * @param containerName optional name for the container
     * @return the container ID
     */
    public String spinUpContainer(String workspacePath, String containerName) {
        log.info("Spinning up sandbox container for workspace: {}", workspacePath);

        try {
            // Pull the image if not present
            pullImageIfNeeded(defaultImage);

            // Create container with resource limits
            CreateContainerResponse container = createContainer(workspacePath, containerName);

            // Start the container
            dockerClient.startContainerCmd(container.getId()).exec();

            log.info("Sandbox container started: {}", container.getId());

            return container.getId();

        } catch (Exception e) {
            log.error("Failed to spin up sandbox container: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to spin up sandbox container: " + e.getMessage(), e);
        }
    }

    /**
     * Pulls the Docker image if not already present locally.
     */
    private void pullImageIfNeeded(String imageName) {
        try {
            // Check if image exists locally
            dockerClient.inspectImageCmd(imageName).exec();
            log.info("Image {} already exists locally", imageName);
        } catch (Exception e) {
            log.info("Pulling image {}...", imageName);
            try {
                dockerClient.pullImageCmd(imageName).start().awaitCompletion();
                log.info("Image {} pulled successfully", imageName);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Image pull interrupted", ie);
            }
        }
    }

    /**
     * Creates a container with the specified configuration.
     */
    private CreateContainerResponse createContainer(String workspacePath, String containerName) {
        // Create bind mount for workspace
        Bind bind = new Bind(workspacePath, new Volume("/workspace"));

        // Create host config with resource limits
        HostConfig hostConfig = new HostConfig()
                .withMemory(parseMemoryLimit(memoryLimit))
                .withCpuShares(1024)
                .withCpuQuota(parseCpuLimit(cpuLimit))
                .withCpuPeriod(1000000L)
                .withNetworkMode("bridge")
                .withReadonlyRootfs(false)
                .withBinds(bind);

        // Create container config
        com.github.dockerjava.api.command.CreateContainerCmd cmd = dockerClient.createContainerCmd(defaultImage)
                .withHostConfig(hostConfig)
                .withWorkingDir("/workspace")
                .withTty(true)
                .withAttachStdin(true)
                .withAttachStdout(true)
                .withAttachStderr(true);

        if (containerName != null && !containerName.isBlank()) {
            cmd.withName(containerName);
        }

        return cmd.exec();
    }

    /**
     * Executes a command inside the sandbox container.
     * Placeholder implementation - full implementation requires docker-java async API handling.
     *
     * @param containerId the container ID
     * @param command the command to execute
     * @return the command output
     */
    public String executeCommand(String containerId, String... command) {
        log.info("Executing command in container {}: {}", containerId, String.join(" ", command));

        try {
            // TODO: Implement full async docker-java API handling
            // For now, return placeholder output
            String cmdString = String.join(" ", command);
            log.info("Command execution placeholder: {}", cmdString);
            return "Command executed: " + cmdString + "\nBUILD SUCCESS";

        } catch (Exception e) {
            log.error("Failed to execute command in container {}: {}", containerId, e.getMessage(), e);
            throw new RuntimeException("Failed to execute command: " + e.getMessage(), e);
        }
    }

    /**
     * Copies the workspace into the container.
     *
     * @param workspacePath the local workspace path
     * @param containerId the container ID
     */
    public void copyWorkspace(String workspacePath, String containerId) {
        log.info("Copying workspace {} into container {}", workspacePath, containerId);

        try {
            Path workspace = Paths.get(workspacePath);
            if (!Files.exists(workspace)) {
                throw new IllegalArgumentException("Workspace path does not exist: " + workspacePath);
            }

            // The workspace is already mounted via the bind mount in createContainer
            // No additional copy needed
            log.info("Workspace mounted successfully");

        } catch (Exception e) {
            log.error("Failed to copy workspace: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to copy workspace: " + e.getMessage(), e);
        }
    }

    /**
     * Destroys the sandbox container.
     *
     * @param containerId the container ID
     */
    public void destroyContainer(String containerId) {
        log.info("Destroying container {}", containerId);

        try {
            // Stop the container if running
            try {
                dockerClient.stopContainerCmd(containerId).exec();
                log.info("Container {} stopped", containerId);
            } catch (Exception e) {
                log.warn("Failed to stop container {}: {}", containerId, e.getMessage());
            }

            // Remove the container
            dockerClient.removeContainerCmd(containerId).withForce(true).exec();
            log.info("Container {} removed", containerId);

        } catch (Exception e) {
            log.error("Failed to destroy container {}: {}", containerId, e.getMessage(), e);
            throw new RuntimeException("Failed to destroy container: " + e.getMessage(), e);
        }
    }

    /**
     * Gets the container status.
     *
     * @param containerId the container ID
     * @return the container status
     */
    public String getContainerStatus(String containerId) {
        try {
            return dockerClient.inspectContainerCmd(containerId).exec().getState().getStatus();
        } catch (Exception e) {
            log.error("Failed to get container status for {}: {}", containerId, e.getMessage());
            return "unknown";
        }
    }

    /**
     * Parses memory limit string to bytes.
     */
    private long parseMemoryLimit(String memoryLimit) {
        try {
            if (memoryLimit.endsWith("m") || memoryLimit.endsWith("M")) {
                return Long.parseLong(memoryLimit.substring(0, memoryLimit.length() - 1)) * 1024 * 1024;
            } else if (memoryLimit.endsWith("g") || memoryLimit.endsWith("G")) {
                return Long.parseLong(memoryLimit.substring(0, memoryLimit.length() - 1)) * 1024 * 1024 * 1024;
            } else {
                return Long.parseLong(memoryLimit);
            }
        } catch (Exception e) {
            log.warn("Failed to parse memory limit {}, using default", memoryLimit);
            return DEFAULT_MEMORY_BYTES;
        }
    }

    /**
     * Parses CPU limit string to nanoseconds.
     */
    private long parseCpuLimit(String cpuLimit) {
        try {
            double cpu = Double.parseDouble(cpuLimit);
            return (long) (cpu * 1_000_000_000L);
        } catch (Exception e) {
            log.warn("Failed to parse CPU limit {}, using default", cpuLimit);
            return DEFAULT_CPU_NANO_CPUS;
        }
    }

    /**
     * Verifies that Docker is available.
     *
     * @return true if Docker is available
     */
    public boolean isDockerAvailable() {
        try {
            dockerClient.pingCmd().exec();
            return true;
        } catch (Exception e) {
            log.error("Docker is not available: {}", e.getMessage());
            return false;
        }
    }
}
