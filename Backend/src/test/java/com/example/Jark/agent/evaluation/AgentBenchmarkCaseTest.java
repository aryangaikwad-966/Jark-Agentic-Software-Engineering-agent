package com.example.Jark.agent.evaluation;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Test AgentBenchmarkCase record structure and validation.
 */
class AgentBenchmarkCaseTest {

    @Test
    void agentBenchmarkCase_validInput_createsRecord() {
        // Arrange
        String caseId = "SEC-001";
        String description = "Missing @PreAuthorize on sensitive endpoint";
        Long repositoryId = 1L;
        String userRequest = "Investigate UserController for authorization";
        String expectedFinding = "Missing @PreAuthorize annotation";
        Set<String> expectedEvidenceFields = Set.of("authorization", "security");

        // Act
        AgentBenchmarkCase benchmarkCase = new AgentBenchmarkCase(
                caseId, description, repositoryId, userRequest, expectedFinding, expectedEvidenceFields);

        // Assert
        assertEquals(caseId, benchmarkCase.caseId());
        assertEquals(description, benchmarkCase.description());
        assertEquals(repositoryId, benchmarkCase.repositoryId());
        assertEquals(userRequest, benchmarkCase.userRequest());
        assertEquals(expectedFinding, benchmarkCase.expectedFinding());
        assertEquals(expectedEvidenceFields, benchmarkCase.expectedEvidenceFields());
    }

    @Test
    void agentBenchmarkCase_blankCaseId_throwsException() {
        // Arrange
        String blankCaseId = "";

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            new AgentBenchmarkCase(
                    blankCaseId, "description", 1L, "request", "finding", Set.of());
        });
    }

    @Test
    void agentBenchmarkCase_nullCaseId_throwsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            new AgentBenchmarkCase(
                    null, "description", 1L, "request", "finding", Set.of());
        });
    }

    @Test
    void agentBenchmarkCase_nullRepositoryId_throwsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            new AgentBenchmarkCase(
                    "case-1", "description", null, "request", "finding", Set.of());
        });
    }

    @Test
    void agentBenchmarkCase_blankUserRequest_throwsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            new AgentBenchmarkCase(
                    "case-1", "description", 1L, "", "finding", Set.of());
        });
    }

    @Test
    void agentBenchmarkCase_blankExpectedFinding_throwsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            new AgentBenchmarkCase(
                    "case-1", "description", 1L, "request", "", Set.of());
        });
    }

    @Test
    void agentBenchmarkCase_nullExpectedEvidenceFields_allowed() {
        // Act - null evidence fields should be allowed
        AgentBenchmarkCase benchmarkCase = new AgentBenchmarkCase(
                "case-1", "description", 1L, "request", "finding", null);

        // Assert
        assertNull(benchmarkCase.expectedEvidenceFields());
    }
}
