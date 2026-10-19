package com.example.Jark.agent.evaluation;

/**
 * Metrics for evaluating agent investigation performance.
 */
public record AgentEvaluationMetrics(
    
    /**
     * Number of benchmark cases run.
     */
    int totalCases,
    
    /**
     * Number of true positive cases (correct findings).
     */
    int truePositives,
    
    /**
     * Number of false positive cases (incorrect findings).
     */
    int falsePositives,
    
    /**
     * Number of false negative cases (missed findings).
     */
    int falseNegatives,
    
    /**
     * Investigation success rate (true positives / total).
     */
    double investigationSuccessRate,
    
    /**
     * Average number of tool calls per successful investigation.
     */
    double averageToolCalls,
    
    /**
     * Average number of iterations per successful investigation.
     */
    double averageIterations,
    
    /**
     * Average confidence score across all successful decisions.
     */
    double averageConfidence,
    
    /**
     * Number of cases where expected evidence fields were present.
     */
    int evidenceValidCases,
    
    /**
     * Evidence validity rate (cases with expected evidence / total).
     */
    double evidenceValidityRate,
    
    /**
     * Finding precision (TP / (TP + FP)).
     */
    double precision,
    
    /**
     * Finding recall (TP / (TP + FN)).
     */
    double recall,
    
    /**
     * F1 score (harmonic mean of precision and recall).
     */
    double f1Score,
    
    /**
     * Total time spent on all investigations (milliseconds).
     */
    long totalLatencyMs,
    
    /**
     * Average latency per investigation (milliseconds).
     */
    double averageLatencyMs
) {
    public AgentEvaluationMetrics {
        if (totalCases < 0) {
            throw new IllegalArgumentException("totalCases cannot be negative");
        }
        if (truePositives < 0) {
            throw new IllegalArgumentException("truePositives cannot be negative");
        }
        if (falsePositives < 0) {
            throw new IllegalArgumentException("falsePositives cannot be negative");
        }
        if (falseNegatives < 0) {
            throw new IllegalArgumentException("falseNegatives cannot be negative");
        }
        if (investigationSuccessRate < 0.0 || investigationSuccessRate > 1.0) {
            throw new IllegalArgumentException("investigationSuccessRate must be between 0.0 and 1.0");
        }
        if (averageToolCalls < 0.0) {
            throw new IllegalArgumentException("averageToolCalls cannot be negative");
        }
        if (averageIterations < 0.0) {
            throw new IllegalArgumentException("averageIterations cannot be negative");
        }
        if (averageConfidence < 0.0 || averageConfidence > 1.0) {
            throw new IllegalArgumentException("averageConfidence must be between 0.0 and 1.0");
        }
        if (evidenceValidityRate < 0.0 || evidenceValidityRate > 1.0) {
            throw new IllegalArgumentException("evidenceValidityRate must be between 0.0 and 1.0");
        }
        if (precision < 0.0 || precision > 1.0) {
            throw new IllegalArgumentException("precision must be between 0.0 and 1.0");
        }
        if (recall < 0.0 || recall > 1.0) {
            throw new IllegalArgumentException("recall must be between 0.0 and 1.0");
        }
        if (f1Score < 0.0 || f1Score > 1.0) {
            throw new IllegalArgumentException("f1Score must be between 0.0 and 1.0");
        }
        if (totalLatencyMs < 0) {
            throw new IllegalArgumentException("totalLatencyMs cannot be negative");
        }
        if (averageLatencyMs < 0.0) {
            throw new IllegalArgumentException("averageLatencyMs cannot be negative");
        }
    }

    /**
     * Creates an empty metrics object for a benchmark with no cases.
     */
    public static AgentEvaluationMetrics empty() {
        return new AgentEvaluationMetrics(
                0, 0, 0, 0, 0.0, 0.0, 0.0, 0.0, 0, 0.0, 0.0, 0.0, 0.0, 0L, 0.0);
    }

    /**
     * Converts metrics to a human-readable markdown report.
     */
    public String toMarkdown() {
        return String.format("""
                # Agent Evaluation Metrics
                
                ## Overview
                - **Total Cases:** %d
                - **True Positives:** %d
                - **False Positives:** %d
                - **False Negatives:** %d
                - **Success Rate:** %.2f%%
                
                ## Investigation Quality
                - **Precision:** %.2f
                - **Recall:** %.2f
                - **F1 Score:** %.2f
                
                ## Investigation Efficiency
                - **Average Tool Calls:** %.2f
                - **Average Iterations:** %.2f
                - **Average Confidence:** %.2f
                
                ## Evidence Quality
                - **Evidence Valid Cases:** %d
                - **Evidence Validity Rate:** %.2f%%
                
                ## Performance
                - **Total Latency:** %d ms
                - **Average Latency:** %.2f ms
                """,
                totalCases, truePositives, falsePositives, falseNegatives, investigationSuccessRate * 100,
                precision, recall, f1Score,
                averageToolCalls, averageIterations, averageConfidence,
                evidenceValidCases, evidenceValidityRate * 100,
                totalLatencyMs, averageLatencyMs);
    }
}
