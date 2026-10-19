package com.example.Jark.service.rag.evaluation;

/**
 * Quantitative metrics for one retrieval benchmark case.
 */
public record RetrievalEvaluationMetrics(
        String caseName,
        double recallAtK,
        double precisionAtK,
        double meanReciprocalRank,
        double ndcgAtK,
        double retrievalRelevance,
        double contextRelevance,
        double tokenEfficiency,
        double investigationSuccessRate,
        double latencyMillis,
        int contextTokens,
        int retrievedCount) {
}
