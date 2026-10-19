package com.example.Jark.service.rag.evaluation;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.example.Jark.dto.rag.AssembledContext;
import com.example.Jark.dto.rag.CodeChunk;
import com.example.Jark.dto.rag.ScoredChunk;
import com.example.Jark.service.rag.RagService;

/**
 * Evaluates retrieval quality against deterministic ground-truth chunk IDs.
 */
@Service
public class RetrievalEvaluationService {

    private final RagService ragService;

    public RetrievalEvaluationService(RagService ragService) {
        this.ragService = ragService;
    }

    public RetrievalEvaluationMetrics evaluate(RetrievalBenchmarkCase benchmarkCase, int k) {
        return evaluate(benchmarkCase, k, benchmarkCase.maxContextTokens());
    }

    public RetrievalEvaluationMetrics evaluate(
            RetrievalBenchmarkCase benchmarkCase,
            int k,
            int maxContextTokens) {
        if (k <= 0) {
            throw new IllegalArgumentException("Evaluation k must be positive");
        }
        if (maxContextTokens <= 0) {
            throw new IllegalArgumentException("Evaluation context token budget must be positive");
        }

        long startedAt = System.nanoTime();
        List<ScoredChunk> retrieved = ragService.retrieve(benchmarkCase.query());
        long elapsedNanos = System.nanoTime() - startedAt;

        List<ScoredChunk> topK = retrieved == null
                ? Collections.emptyList()
                : retrieved.stream().limit(k).toList();
        Set<String> relevantIds = benchmarkCase.relevantChunkIds();
        int relevantRetrieved = (int) topK.stream()
                .map(ScoredChunk::getChunk)
                .map(CodeChunk::getId)
                .filter(relevantIds::contains)
                .distinct()
                .count();

        int firstRelevantRank = firstRelevantRank(topK, relevantIds);
        double reciprocalRank = firstRelevantRank == -1 ? 0.0 : 1.0 / firstRelevantRank;
        double idealRelevantCount = Math.min(k, relevantIds.size());
        double dcg = discountedGain(topK, relevantIds);
        double idealDcg = idealDiscountedGain(idealRelevantCount);
        double retrievalRelevance = topK.stream()
            .filter(result -> relevantIds.contains(result.getChunk().getId()))
            .mapToDouble(ScoredChunk::getFinalScore)
            .average()
            .orElse(0.0);
        AssembledContext context = ragService.assembleContext(benchmarkCase.query(), maxContextTokens);
        List<ScoredChunk> contextChunks = context == null || context.getChunks() == null
            ? Collections.emptyList()
            : context.getChunks();
        int relevantContextChunks = (int) contextChunks.stream()
            .map(ScoredChunk::getChunk)
            .map(CodeChunk::getId)
            .filter(relevantIds::contains)
            .distinct()
            .count();
        int contextTokens = context == null ? 0 : context.getEstimatedTokens();

        return new RetrievalEvaluationMetrics(
                benchmarkCase.name(),
                relevantRetrieved / (double) relevantIds.size(),
                relevantRetrieved / (double) k,
                reciprocalRank,
                idealDcg == 0.0 ? 0.0 : dcg / idealDcg,
                retrievalRelevance,
                relevantContextChunks / (double) relevantIds.size(),
                relevantContextChunks / (double) Math.max(1, contextTokens),
                relevantContextChunks == relevantIds.size() ? 1.0 : 0.0,
                elapsedNanos / 1_000_000.0,
                contextTokens,
                topK.size());
    }

    public List<RetrievalEvaluationMetrics> evaluateAll(
            List<RetrievalBenchmarkCase> benchmarkCases,
            int k) {
        if (benchmarkCases == null || benchmarkCases.isEmpty()) {
            return Collections.emptyList();
        }
        return benchmarkCases.stream()
                .map(benchmarkCase -> evaluate(benchmarkCase, k))
                .toList();
    }

    public RetrievalEvaluationReport evaluateReport(
            List<RetrievalBenchmarkCase> benchmarkCases,
            int k) {
        if (k <= 0) {
            throw new IllegalArgumentException("Evaluation k must be positive");
        }
        if (benchmarkCases == null || benchmarkCases.isEmpty()) {
            return new RetrievalEvaluationReport(
                    k, Collections.emptyList(), 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        }
        List<RetrievalEvaluationMetrics> metrics = evaluateAll(benchmarkCases, k);
        return new RetrievalEvaluationReport(
                k,
                metrics,
                average(metrics, RetrievalEvaluationMetrics::recallAtK),
                average(metrics, RetrievalEvaluationMetrics::precisionAtK),
                average(metrics, RetrievalEvaluationMetrics::meanReciprocalRank),
                average(metrics, RetrievalEvaluationMetrics::ndcgAtK),
                average(metrics, RetrievalEvaluationMetrics::retrievalRelevance),
                average(metrics, RetrievalEvaluationMetrics::contextRelevance),
                average(metrics, RetrievalEvaluationMetrics::tokenEfficiency),
                average(metrics, RetrievalEvaluationMetrics::investigationSuccessRate),
                average(metrics, RetrievalEvaluationMetrics::latencyMillis));
    }

    private double average(
            List<RetrievalEvaluationMetrics> metrics,
            java.util.function.ToDoubleFunction<RetrievalEvaluationMetrics> value) {
        return metrics.stream().mapToDouble(value).average().orElse(0.0);
    }

    private int firstRelevantRank(List<ScoredChunk> retrieved, Set<String> relevantIds) {
        for (int index = 0; index < retrieved.size(); index++) {
            if (relevantIds.contains(retrieved.get(index).getChunk().getId())) {
                return index + 1;
            }
        }
        return -1;
    }

    private double discountedGain(List<ScoredChunk> retrieved, Set<String> relevantIds) {
        double gain = 0.0;
        for (int index = 0; index < retrieved.size(); index++) {
            if (relevantIds.contains(retrieved.get(index).getChunk().getId())) {
                gain += 1.0 / log2(index + 2);
            }
        }
        return gain;
    }

    private double idealDiscountedGain(double relevantCount) {
        double gain = 0.0;
        for (int index = 0; index < relevantCount; index++) {
            gain += 1.0 / log2(index + 2);
        }
        return gain;
    }

    private double log2(int value) {
        return Math.log(value) / Math.log(2.0);
    }
}
