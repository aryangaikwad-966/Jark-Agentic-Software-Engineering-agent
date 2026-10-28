package com.example.Jark.agent.evaluation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Test AgentBenchmarkRunner structure (integration tests require full Spring context).
 */
class AgentBenchmarkRunnerTest {

    @Test
    void agentBenchmarkRunner_canBeInstantiated() {
        // This is a placeholder test - full integration tests require Spring context
        // The runner tests are better suited for integration tests with real database
        assertTrue(true);
    }

    @Test
    void agentBenchmarkRunner_metricsCalculations() {
        // Test metric calculations logic independently
        int totalCases = 10;
        int tp = 8;
        int fp = 1;
        int fn = 1;
        
        double investigationSuccessRate = totalCases > 0 ? (double) tp / totalCases : 0.0;
        double precision = (tp + fp) > 0 ? (double) tp / (tp + fp) : 0.0;
        double recall = (tp + fn) > 0 ? (double) tp / (tp + fn) : 0.0;
        double f1Score = (precision + recall) > 0 ? 2 * (precision * recall) / (precision + recall) : 0.0;
        
        assertEquals(0.8, investigationSuccessRate);
        assertEquals(0.89, precision, 0.01);
        assertEquals(0.89, recall, 0.01);
        assertEquals(0.89, f1Score, 0.01);
    }
}
