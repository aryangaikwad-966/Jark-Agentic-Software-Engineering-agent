package com.example.Jark.service.rag.evaluation;

import java.util.Set;

import com.example.Jark.dto.rag.RagQuery;

/**
 * A semantic retrieval query and the chunk IDs considered relevant for evaluation.
 */
public record RetrievalBenchmarkCase(
        String name,
        RagQuery query,
        Set<String> relevantChunkIds,
        int maxContextTokens) {

    public RetrievalBenchmarkCase(String name, RagQuery query, Set<String> relevantChunkIds) {
        this(name, query, relevantChunkIds, 1000);
    }

    public RetrievalBenchmarkCase {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Benchmark case name must not be blank");
        }
        if (query == null) {
            throw new IllegalArgumentException("Benchmark case query must not be null");
        }
        if (relevantChunkIds == null || relevantChunkIds.isEmpty()) {
            throw new IllegalArgumentException("Benchmark case needs at least one relevant chunk ID");
        }
        if (maxContextTokens <= 0) {
            throw new IllegalArgumentException("Benchmark case context token budget must be positive");
        }
        relevantChunkIds = Set.copyOf(relevantChunkIds);
    }
}
