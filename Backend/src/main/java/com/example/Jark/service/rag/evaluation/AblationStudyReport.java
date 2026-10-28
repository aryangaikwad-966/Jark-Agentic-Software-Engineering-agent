package com.example.Jark.service.rag.evaluation;

/**
 * Comparison between a deterministic baseline and full repository-aware RAG.
 */
public record AblationStudyReport(
        RetrievalEvaluationReport baseline,
        RetrievalEvaluationReport fullRag,
        double recallImprovement,
        double precisionImprovement,
        double ndcgImprovement,
        double investigationSuccessImprovement) {
}
