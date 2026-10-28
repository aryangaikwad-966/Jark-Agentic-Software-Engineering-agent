package com.example.Jark.agent.evaluation;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Test AgentBenchmarkDataset creation and structure.
 */
class AgentBenchmarkDatasetTest {

    @Test
    void agentBenchmarkDataset_createDefault_returnsCases() {
        // Act
        AgentBenchmarkDataset dataset = AgentBenchmarkDataset.createDefault();

        // Assert
        assertNotNull(dataset);
        assertFalse(dataset.isEmpty());
        assertEquals(5, dataset.size()); // Should have 5 default cases
    }

    @Test
    void agentBenchmarkDataset_createDefault_hasSecurityCases() {
        // Act
        AgentBenchmarkDataset dataset = AgentBenchmarkDataset.createDefault();

        // Assert
        List<AgentBenchmarkCase> cases = dataset.cases();
        assertTrue(cases.stream().anyMatch(c -> c.caseId().startsWith("SEC-")));
    }

    @Test
    void agentBenchmarkDataset_createDefault_hasValidationCases() {
        // Act
        AgentBenchmarkDataset dataset = AgentBenchmarkDataset.createDefault();

        // Assert
        List<AgentBenchmarkCase> cases = dataset.cases();
        assertTrue(cases.stream().anyMatch(c -> c.caseId().startsWith("VAL-")));
    }

    @Test
    void agentBenchmarkDataset_createDefault_hasArchitectureCases() {
        // Act
        AgentBenchmarkDataset dataset = AgentBenchmarkDataset.createDefault();

        // Assert
        List<AgentBenchmarkCase> cases = dataset.cases();
        assertTrue(cases.stream().anyMatch(c -> c.caseId().startsWith("ARCH-")));
    }

    @Test
    void agentBenchmarkDataset_fromCases_returnsCustomDataset() {
        // Arrange
        List<AgentBenchmarkCase> customCases = List.of(
                new AgentBenchmarkCase(
                        "CUSTOM-001",
                        "Custom test case",
                        1L,
                        "Custom request",
                        "Custom finding",
                        Set.of("custom"))
        );

        // Act
        AgentBenchmarkDataset dataset = AgentBenchmarkDataset.fromCases(customCases);

        // Assert
        assertNotNull(dataset);
        assertEquals(1, dataset.size());
        assertEquals("CUSTOM-001", dataset.cases().get(0).caseId());
    }

    @Test
    void agentBenchmarkDataset_emptyList_returnsEmptyDataset() {
        // Act
        AgentBenchmarkDataset dataset = AgentBenchmarkDataset.fromCases(List.of());

        // Assert
        assertNotNull(dataset);
        assertTrue(dataset.isEmpty());
        assertEquals(0, dataset.size());
    }

    @Test
    void agentBenchmarkDataset_defaultCasesHaveValidStructure() {
        // Act
        AgentBenchmarkDataset dataset = AgentBenchmarkDataset.createDefault();

        // Assert
        for (AgentBenchmarkCase benchmarkCase : dataset.cases()) {
            assertNotNull(benchmarkCase.caseId());
            assertFalse(benchmarkCase.caseId().isBlank());
            assertNotNull(benchmarkCase.description());
            assertFalse(benchmarkCase.description().isBlank());
            assertNotNull(benchmarkCase.repositoryId());
            assertNotNull(benchmarkCase.userRequest());
            assertFalse(benchmarkCase.userRequest().isBlank());
            assertNotNull(benchmarkCase.expectedFinding());
            assertFalse(benchmarkCase.expectedFinding().isBlank());
        }
    }
}
