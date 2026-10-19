package com.example.Jark.service.rag;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import com.example.Jark.dto.rag.AssembledContext;
import com.example.Jark.dto.rag.CodeChunk;
import com.example.Jark.dto.rag.RagQuery;
import com.example.Jark.dto.rag.ScoredChunk;
import com.example.Jark.service.rag.evaluation.RetrievalBenchmarkCase;
import com.example.Jark.service.rag.evaluation.RetrievalEvaluationMetrics;
import com.example.Jark.service.rag.evaluation.RetrievalEvaluationReport;
import com.example.Jark.service.rag.evaluation.RetrievalEvaluationService;

class RetrievalEvaluationServiceTest {

    @Test
    void evaluate_calculatesRankingMetricsFromGroundTruth() {
        CodeChunk irrelevant = chunk("unrelated");
        CodeChunk relevant = chunk("payment-service");
        RagService ragService = stub(List.of(scored(irrelevant, 0.9), scored(relevant, 0.8)));
        RetrievalEvaluationService evaluator = new RetrievalEvaluationService(ragService);

        RetrievalEvaluationMetrics metrics = evaluator.evaluate(
                new RetrievalBenchmarkCase(
                        "payment-flow",
                        RagQuery.builder().query("payment flow").topK(2).build(),
                        Set.of("payment-service")),
                2);

        assertEquals(1.0, metrics.recallAtK());
        assertEquals(0.5, metrics.precisionAtK());
        assertEquals(0.5, metrics.meanReciprocalRank());
        assertEquals(1.0 / (Math.log(3.0) / Math.log(2.0)), metrics.ndcgAtK(), 0.000001);
        assertEquals(0.8, metrics.retrievalRelevance(), 0.000001);
        assertEquals(2, metrics.retrievedCount());
    }

    @Test
    void evaluate_rejectsNonPositiveK() {
        RetrievalEvaluationService evaluator = new RetrievalEvaluationService(stub(List.of()));
        RetrievalBenchmarkCase benchmarkCase = new RetrievalBenchmarkCase(
                "empty",
                RagQuery.builder().query("query").build(),
                Set.of("chunk"));

        assertThrows(IllegalArgumentException.class, () -> evaluator.evaluate(benchmarkCase, 0));
    }

    @Test
    void evaluateReport_aggregatesCaseMetrics() {
        RetrievalEvaluationService evaluator = new RetrievalEvaluationService(
                stub(List.of(scored(chunk("payment-service"), 0.9))));
        List<RetrievalBenchmarkCase> cases = List.of(
                new RetrievalBenchmarkCase("payment", query("payment"), Set.of("payment-service")),
                new RetrievalBenchmarkCase("billing", query("billing"), Set.of("billing-service")));

        RetrievalEvaluationReport report = evaluator.evaluateReport(cases, 1);

        assertEquals(1, report.k());
        assertEquals(2, report.caseMetrics().size());
        assertEquals(0.5, report.meanRecallAtK());
        assertEquals(0.5, report.meanPrecisionAtK());
        assertEquals(0.5, report.meanReciprocalRank());
        assertEquals(true, report.toMarkdown().contains("Investigation success"));
    }

    private RagService stub(List<ScoredChunk> results) {
        return new RagService() {
            @Override
            public List<CodeChunk> indexRepository(Long repositoryId, Path workspacePath) {
                return List.of();
            }

            @Override
            public List<ScoredChunk> retrieve(RagQuery query) {
                return results;
            }

            @Override
            public AssembledContext assembleContext(RagQuery query, int maxTokens) {
                return null;
            }

            @Override
            public List<CodeChunk> getIndexedChunks(Long repositoryId) {
                return List.of();
            }
        };
    }

    private ScoredChunk scored(CodeChunk chunk, double score) {
        return ScoredChunk.builder().chunk(chunk).finalScore(score).build();
    }

    private RagQuery query(String text) {
        return RagQuery.builder().query(text).topK(1).build();
    }

    private CodeChunk chunk(String id) {
        return CodeChunk.builder().id(id).content("content for " + id).build();
    }
}
