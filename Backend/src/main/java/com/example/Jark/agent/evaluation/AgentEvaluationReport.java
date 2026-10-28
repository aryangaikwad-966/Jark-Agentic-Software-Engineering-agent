package com.example.Jark.agent.evaluation;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Report containing detailed results of agent benchmark evaluation.
 */
public record AgentEvaluationReport(
    
    /**
     * When the evaluation was run.
     */
    LocalDateTime timestamp,
    
    /**
     * Summary metrics across all benchmark cases.
     */
    AgentEvaluationMetrics metrics,
    
    /**
     * Detailed results for each benchmark case.
     */
    List<AgentCaseResult> caseResults
) {
    public AgentEvaluationReport {
        if (timestamp == null) {
            throw new IllegalArgumentException("timestamp cannot be null");
        }
        if (metrics == null) {
            throw new IllegalArgumentException("metrics cannot be null");
        }
        if (caseResults == null) {
            throw new IllegalArgumentException("caseResults cannot be null");
        }
        caseResults = List.copyOf(caseResults);
    }

    /**
     * Creates an empty report for a benchmark with no cases.
     */
    public static AgentEvaluationReport empty() {
        return new AgentEvaluationReport(
                LocalDateTime.now(),
                AgentEvaluationMetrics.empty(),
                List.of());
    }

    /**
     * Converts the report to a human-readable markdown format.
     */
    public String toMarkdown() {
        StringBuilder sb = new StringBuilder();
        sb.append("# Agent Evaluation Report\n\n");
        sb.append("**Generated:** ").append(timestamp).append("\n\n");
        sb.append(metrics.toMarkdown());
        sb.append("\n## Detailed Case Results\n\n");
        
        for (AgentCaseResult result : caseResults) {
            sb.append(result.toMarkdown()).append("\n\n");
        }
        
        return sb.toString();
    }

    /**
     * Result for a single benchmark case.
     */
    public record AgentCaseResult(
        
        /**
         * The benchmark case that was evaluated.
         */
        AgentBenchmarkCase benchmarkCase,
        
        /**
         * The outcome of the investigation (TP, FP, FN).
         */
        CaseOutcome outcome,
        
        /**
         * The actual finding produced by the agent.
         */
        String actualFinding,
        
        /**
         * Number of tool calls made during investigation.
         */
        int toolCallCount,
        
        /**
         * Number of iterations in the investigation loop.
         */
        int iterationCount,
        
        /**
         * Average confidence across all decisions.
         */
        double averageConfidence,
        
        /**
         * Whether expected evidence fields were present.
         */
        boolean evidenceValid,
        
        /**
         * Time taken for the investigation (milliseconds).
         */
        long latencyMs,
        
        /**
         * Error message if the investigation failed.
         */
        String errorMessage
    ) {
        public AgentCaseResult {
            if (benchmarkCase == null) {
                throw new IllegalArgumentException("benchmarkCase cannot be null");
            }
            if (outcome == null) {
                throw new IllegalArgumentException("outcome cannot be null");
            }
            if (averageConfidence < 0.0 || averageConfidence > 1.0) {
                throw new IllegalArgumentException("averageConfidence must be between 0.0 and 1.0");
            }
            if (latencyMs < 0) {
                throw new IllegalArgumentException("latencyMs cannot be negative");
            }
        }

        /**
         * Converts the case result to markdown format.
         */
        public String toMarkdown() {
            StringBuilder sb = new StringBuilder();
            sb.append("### Case: ").append(benchmarkCase.caseId()).append("\n\n");
            sb.append("**Description:** ").append(benchmarkCase.description()).append("\n\n");
            sb.append("**User Request:** ").append(benchmarkCase.userRequest()).append("\n\n");
            sb.append("**Outcome:** ").append(formatOutcome(outcome)).append("\n\n");
            
            if (outcome != CaseOutcome.FN || errorMessage != null) {
                sb.append("**Expected Finding:** ").append(benchmarkCase.expectedFinding()).append("\n\n");
                sb.append("**Actual Finding:** ").append(actualFinding != null ? actualFinding : "None").append("\n\n");
                sb.append("**Tool Calls:** ").append(toolCallCount).append("\n");
                sb.append("**Iterations:** ").append(iterationCount).append("\n");
                sb.append("**Average Confidence:** ").append(String.format("%.2f", averageConfidence)).append("\n");
                sb.append("**Evidence Valid:** ").append(evidenceValid ? "✅" : "❌").append("\n");
                sb.append("**Latency:** ").append(latencyMs).append(" ms\n");
            }
            
            if (errorMessage != null) {
                sb.append("**Error:** ").append(errorMessage).append("\n");
            }
            
            return sb.toString();
        }

        private String formatOutcome(CaseOutcome outcome) {
            return switch (outcome) {
                case TP -> "✅ TRUE POSITIVE";
                case FP -> "⚠️ FALSE POSITIVE";
                case FN -> "❌ FALSE NEGATIVE";
            };
        }
    }

    /**
     * Possible outcomes for a single benchmark case.
     */
    public enum CaseOutcome {
        TP, // True Positive: Found the correct issue
        FP, // False Positive: Claimed to find an issue but it was incorrect/irrelevant
        FN  // False Negative: Failed to find the existing issue
    }
}
