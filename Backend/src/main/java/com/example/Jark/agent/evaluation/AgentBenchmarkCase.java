package com.example.Jark.agent.evaluation;

/**
 * A single benchmark case for agent evaluation.
 * Contains a repository, user request, and expected finding.
 */
public record AgentBenchmarkCase(
    
    /**
     * Unique identifier for this benchmark case.
     */
    String caseId,
    
    /**
     * Human-readable description of the case.
     */
    String description,
    
    /**
     * The repository ID to investigate.
     */
    Long repositoryId,
    
    /**
     * The user request/issue to investigate.
     */
    String userRequest,
    
    /**
     * The expected finding that the agent should produce.
     * This serves as ground truth for evaluation.
     */
    String expectedFinding,
    
    /**
     * Expected evidence fields that should be present in the finding.
     * Used to validate evidence quality.
     */
    java.util.Set<String> expectedEvidenceFields
) {
    public AgentBenchmarkCase {
        if (caseId == null || caseId.isBlank()) {
            throw new IllegalArgumentException("caseId cannot be blank");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("description cannot be blank");
        }
        if (repositoryId == null) {
            throw new IllegalArgumentException("repositoryId cannot be null");
        }
        if (userRequest == null || userRequest.isBlank()) {
            throw new IllegalArgumentException("userRequest cannot be blank");
        }
        if (expectedFinding == null || expectedFinding.isBlank()) {
            throw new IllegalArgumentException("expectedFinding cannot be blank");
        }
    }
}
