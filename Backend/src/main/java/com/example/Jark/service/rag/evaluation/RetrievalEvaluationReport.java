package com.example.Jark.service.rag.evaluation;

import java.util.List;

/**
 * Aggregate result for a retrieval benchmark run.
 */
public record RetrievalEvaluationReport(
        int k,
        List<RetrievalEvaluationMetrics> caseMetrics,
        double meanRecallAtK,
        double meanPrecisionAtK,
        double meanReciprocalRank,
        double meanNdcgAtK,
        double meanRetrievalRelevance,
        double meanContextRelevance,
        double meanTokenEfficiency,
        double meanInvestigationSuccessRate,
        double meanLatencyMillis) {

    public RetrievalEvaluationReport {
        caseMetrics = List.copyOf(caseMetrics);
    }

    public String toMarkdown() {
        return "| Metric | Value |\n"
                + "|---|---:|\n"
                + String.format("| Recall@%d | %.4f |%n", k, meanRecallAtK)
                + String.format("| Precision@%d | %.4f |%n", k, meanPrecisionAtK)
                + String.format("| MRR | %.4f |%n", meanReciprocalRank)
                + String.format("| NDCG@%d | %.4f |%n", k, meanNdcgAtK)
                + String.format("| Retrieval relevance | %.4f |%n", meanRetrievalRelevance)
                + String.format("| Context relevance | %.4f |%n", meanContextRelevance)
                + String.format("| Token efficiency | %.4f |%n", meanTokenEfficiency)
                + String.format("| Investigation success | %.4f |%n", meanInvestigationSuccessRate)
                + String.format("| Mean latency (ms) | %.3f |%n", meanLatencyMillis);
    }
}
