package com.example.Jark.service.rag;

import com.example.Jark.dto.rag.CodeChunk;
import com.example.Jark.dto.rag.RagQuery;
import com.example.Jark.dto.rag.ScoredChunk;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Combines and reranks retrieved candidates across multiple retrieval channels
 * using Reciprocal Rank Fusion (RRF) and code-specific relevance boosting.
 */
@Service
public class RerankerService {

    private static final double RRF_K = 60.0;
    private static final double WEIGHT_SYMBOL = 1.25;
    private static final double WEIGHT_VECTOR = 1.0;
    private static final double WEIGHT_LEXICAL = 1.0;
    private static final double WEIGHT_DEPENDENCY = 0.85;

    /**
     * Fuses and reranks candidate lists from each retrieval channel into a single sorted list.
     *
     * @param query search query
     * @param vectorResults results from semantic vector search
     * @param lexicalResults results from BM25 lexical search
     * @param symbolResults results from exact symbol search
     * @param dependencyResults results from dependency graph traversal
     * @return unified list of ScoredChunks sorted by final relevance
     */
    public List<ScoredChunk> rerank(
            RagQuery query,
            List<ScoredChunk> vectorResults,
            List<ScoredChunk> lexicalResults,
            List<ScoredChunk> symbolResults,
            List<ScoredChunk> dependencyResults) {

        Map<String, CodeChunk> chunkMap = new HashMap<>();
        Map<String, Double> rrfScores = new HashMap<>();
        Map<String, Map<String, Double>> detailedScores = new HashMap<>();
        Map<String, String> primarySources = new HashMap<>();
        Map<String, Double> topChannelScore = new HashMap<>();

        // Process each channel
        applyRrfChannel(vectorResults, WEIGHT_VECTOR, "vector", chunkMap, rrfScores, detailedScores, primarySources, topChannelScore);
        applyRrfChannel(lexicalResults, WEIGHT_LEXICAL, "lexical", chunkMap, rrfScores, detailedScores, primarySources, topChannelScore);
        applyRrfChannel(symbolResults, WEIGHT_SYMBOL, "symbol", chunkMap, rrfScores, detailedScores, primarySources, topChannelScore);
        applyRrfChannel(dependencyResults, WEIGHT_DEPENDENCY, "dependency", chunkMap, rrfScores, detailedScores, primarySources, topChannelScore);

        if (rrfScores.isEmpty()) {
            return Collections.emptyList();
        }

        double maxRrf = rrfScores.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
        List<ScoredChunk> merged = new ArrayList<>();

        for (Map.Entry<String, Double> entry : rrfScores.entrySet()) {
            String chunkId = entry.getKey();
            CodeChunk chunk = chunkMap.get(chunkId);
            double baseScore = maxRrf > 0 ? ((entry.getValue() / maxRrf) * 0.8) : 0.0;

            // Apply boosts
            double boostedScore = applyFeatureBoosts(chunk, query, baseScore, detailedScores.get(chunkId));

            if (boostedScore >= query.getMinScore()) {
                merged.add(ScoredChunk.builder()
                        .chunk(chunk)
                        .finalScore(boostedScore)
                        .channelScores(detailedScores.getOrDefault(chunkId, Collections.emptyMap()))
                        .primarySource(primarySources.getOrDefault(chunkId, "HYBRID"))
                        .build());
            }
        }

        Collections.sort(merged);
        List<ScoredChunk> diverseResults = new ArrayList<>();
        Map<String, Integer> fileCounts = new HashMap<>();
        int maxPerFile = Math.max(2, query.getTopK() / 2);

        for (ScoredChunk sc : merged) {
            String file = sc.getChunk().getFilePath() != null ? sc.getChunk().getFilePath() : "unknown";
            int count = fileCounts.getOrDefault(file, 0);
            if (count < maxPerFile) {
                diverseResults.add(sc);
                fileCounts.put(file, count + 1);
                if (diverseResults.size() >= query.getTopK()) {
                    break;
                }
            }
        }

        if (diverseResults.size() < query.getTopK() && diverseResults.size() < merged.size()) {
            for (ScoredChunk sc : merged) {
                if (!diverseResults.contains(sc)) {
                    diverseResults.add(sc);
                    if (diverseResults.size() >= query.getTopK()) {
                        break;
                    }
                }
            }
        }

        return diverseResults;
    }

    private void applyRrfChannel(
            List<ScoredChunk> results,
            double weight,
            String channelName,
            Map<String, CodeChunk> chunkMap,
            Map<String, Double> rrfScores,
            Map<String, Map<String, Double>> detailedScores,
            Map<String, String> primarySources,
            Map<String, Double> topChannelScore) {

        if (results == null || results.isEmpty()) {
            return;
        }

        for (int rank = 0; rank < results.size(); rank++) {
            ScoredChunk sc = results.get(rank);
            CodeChunk chunk = sc.getChunk();
            String id = chunk.getId();

            chunkMap.putIfAbsent(id, chunk);

            double channelRrf = weight / (RRF_K + rank + 1);
            rrfScores.put(id, rrfScores.getOrDefault(id, 0.0) + channelRrf);

            Map<String, Double> channelMap = detailedScores.computeIfAbsent(id, k -> new HashMap<>());
            channelMap.put(channelName, sc.getFinalScore());

            if (channelRrf > topChannelScore.getOrDefault(id, 0.0)) {
                topChannelScore.put(id, channelRrf);
                primarySources.put(id, channelName.toUpperCase());
            }
        }
    }

    private double applyFeatureBoosts(CodeChunk chunk, RagQuery query, double baseScore, Map<String, Double> scores) {
        double boosted = baseScore;

        // Boost for exact class or method matches
        if (query.getTargetClass() != null && chunk.getClassName() != null
                && (chunk.getClassName().equalsIgnoreCase(query.getTargetClass())
                || chunk.getClassName().endsWith("." + query.getTargetClass()))) {
            boosted += 0.15;
        }

        if (query.getTargetMethod() != null && chunk.getMethodName() != null
                && chunk.getMethodName().equalsIgnoreCase(query.getTargetMethod())) {
            boosted += 0.12;
        }

        // Multi-channel consensus boost: appears in both semantic and lexical
        if (scores != null && scores.containsKey("vector") && scores.containsKey("lexical")) {
            boosted += 0.08;
        }

        // Penalty for extremely short noise chunks
        if (chunk.getContent() != null && chunk.getContent().strip().length() < 25) {
            boosted = Math.max(0.0, boosted - 0.10);
        }

        return Math.min(1.0, boosted);
    }
}
