package com.example.Jark.agent.evaluation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Test AgentEvaluationMetrics record structure and validation.
 */
class AgentEvaluationMetricsTest {

    @Test
    void agentEvaluationMetrics_validInput_createsRecord() {
        // Arrange
        int totalCases = 10;
        int truePositives = 8;
        int falsePositives = 1;
        int falseNegatives = 1;
        double investigationSuccessRate = 0.8;
        double averageToolCalls = 5.5;
        double averageIterations = 3.2;
        double averageConfidence = 0.85;
        int evidenceValidCases = 7;
        double evidenceValidityRate = 0.7;
        double precision = 0.89;
        double recall = 0.89;
        double f1Score = 0.89;
        long totalLatencyMs = 5000L;
        double averageLatencyMs = 500.0;

        // Act
        AgentEvaluationMetrics metrics = new AgentEvaluationMetrics(
                totalCases, truePositives, falsePositives, falseNegatives, investigationSuccessRate,
                averageToolCalls, averageIterations, averageConfidence,
                evidenceValidCases, evidenceValidityRate, precision, recall, f1Score,
                totalLatencyMs, averageLatencyMs);

        // Assert
        assertEquals(totalCases, metrics.totalCases());
        assertEquals(truePositives, metrics.truePositives());
        assertEquals(falsePositives, metrics.falsePositives());
        assertEquals(falseNegatives, metrics.falseNegatives());
        assertEquals(investigationSuccessRate, metrics.investigationSuccessRate());
        assertEquals(averageToolCalls, metrics.averageToolCalls());
        assertEquals(averageIterations, metrics.averageIterations());
        assertEquals(averageConfidence, metrics.averageConfidence());
        assertEquals(evidenceValidCases, metrics.evidenceValidCases());
        assertEquals(evidenceValidityRate, metrics.evidenceValidityRate());
        assertEquals(precision, metrics.precision());
        assertEquals(recall, metrics.recall());
        assertEquals(f1Score, metrics.f1Score());
        assertEquals(totalLatencyMs, metrics.totalLatencyMs());
        assertEquals(averageLatencyMs, metrics.averageLatencyMs());
    }

    @Test
    void agentEvaluationMetrics_empty_returnsEmptyMetrics() {
        // Act
        AgentEvaluationMetrics metrics = AgentEvaluationMetrics.empty();

        // Assert
        assertEquals(0, metrics.totalCases());
        assertEquals(0, metrics.truePositives());
        assertEquals(0, metrics.falsePositives());
        assertEquals(0, metrics.falseNegatives());
        assertEquals(0.0, metrics.investigationSuccessRate());
        assertEquals(0.0, metrics.averageToolCalls());
        assertEquals(0.0, metrics.averageIterations());
        assertEquals(0.0, metrics.averageConfidence());
        assertEquals(0, metrics.evidenceValidCases());
        assertEquals(0.0, metrics.evidenceValidityRate());
        assertEquals(0.0, metrics.precision());
        assertEquals(0.0, metrics.recall());
        assertEquals(0.0, metrics.f1Score());
        assertEquals(0L, metrics.totalLatencyMs());
        assertEquals(0.0, metrics.averageLatencyMs());
    }

    @Test
    void agentEvaluationMetrics_negativeTotalCases_throwsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            new AgentEvaluationMetrics(
                    -1, 0, 0, 0, 0.0, 0.0, 0.0, 0.0, 0, 0.0, 0.0, 0.0, 0.0, 0L, 0.0);
        });
    }

    @Test
    void agentEvaluationMetrics_investigationSuccessRateGreaterThanOne_throwsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            new AgentEvaluationMetrics(
                    10, 8, 2, 0, 1.5, 0.0, 0.0, 0.0, 0, 0.0, 0.0, 0.0, 0.0, 0L, 0.0);
        });
    }

    @Test
    void agentEvaluationMetrics_negativeAverageToolCalls_throwsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            new AgentEvaluationMetrics(
                    10, 8, 2, 0, 0.8, -1.0, 0.0, 0.0, 0, 0.0, 0.0, 0.0, 0.0, 0L, 0.0);
        });
    }

    @Test
    void agentEvaluationMetrics_averageConfidenceGreaterThanOne_throwsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            new AgentEvaluationMetrics(
                    10, 8, 2, 0, 0.8, 0.0, 0.0, 1.5, 0, 0.0, 0.0, 0.0, 0.0, 0L, 0.0);
        });
    }

    @Test
    void agentEvaluationMetrics_toMarkdown_generatesReport() {
        // Arrange
        AgentEvaluationMetrics metrics = new AgentEvaluationMetrics(
                10, 8, 1, 1, 0.8, 5.5, 3.2, 0.85, 7, 0.7, 0.89, 0.89, 0.89, 5000L, 500.0);

        // Act
        String markdown = metrics.toMarkdown();

        // Assert
        assertNotNull(markdown);
        assertTrue(markdown.contains("Agent Evaluation Metrics"));
    }
}
