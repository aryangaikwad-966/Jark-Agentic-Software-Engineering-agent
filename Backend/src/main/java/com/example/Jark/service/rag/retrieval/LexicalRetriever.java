package com.example.Jark.service.rag.retrieval;

import com.example.Jark.dto.rag.CodeChunk;
import com.example.Jark.dto.rag.DocumentType;
import com.example.Jark.dto.rag.RagQuery;
import com.example.Jark.dto.rag.ScoredChunk;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Performs BM25 lexical retrieval over repository CodeChunks.
 * Features code-aware tokenization (camelCase and snake_case splitting).
 */
@Component
public class LexicalRetriever {

    private static final double K1 = 1.2;
    private static final double B = 0.75;
    private static final Pattern CODE_TOKEN_PATTERN = Pattern.compile("[a-zA-Z0-9_]+");

    /**
     * Retrieves chunks ranked by BM25 relevance to the query.
     *
     * @param chunks complete list of chunks to search within
     * @param query search query specification
     * @return list of ScoredChunks sorted descending by BM25 score
     */
    public List<ScoredChunk> retrieve(List<CodeChunk> chunks, RagQuery query) {
        if (chunks == null || chunks.isEmpty() || query == null || query.getQuery() == null || query.getQuery().isBlank()) {
            return Collections.emptyList();
        }

        List<CodeChunk> candidateChunks = filterChunks(chunks, query);
        if (candidateChunks.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> queryTerms = tokenize(query.getQuery());
        if (queryTerms.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, CodeChunk> chunkMap = new HashMap<>();
        Map<String, Double> chunkScores = computeBm25Scores(candidateChunks, queryTerms, chunkMap);
        if (chunkScores.isEmpty()) {
            return Collections.emptyList();
        }

        double maxScore = chunkScores.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
        List<ScoredChunk> results = new ArrayList<>();

        for (Map.Entry<String, Double> entry : chunkScores.entrySet()) {
            double normalized = maxScore > 0 ? (entry.getValue() / maxScore) : 0.0;
            if (normalized >= query.getMinScore()) {
                CodeChunk chunk = chunkMap.get(entry.getKey());
                Map<String, Double> channelScores = new HashMap<>();
                channelScores.put("lexical", normalized);
                results.add(ScoredChunk.builder()
                        .chunk(chunk)
                        .finalScore(normalized)
                        .channelScores(channelScores)
                        .primarySource("LEXICAL")
                        .build());
            }
        }

        Collections.sort(results);
        int limit = Math.min(query.getTopK(), results.size());
        return results.subList(0, limit);
    }

    private Map<String, Double> computeBm25Scores(
            List<CodeChunk> candidates,
            List<String> queryTerms,
            Map<String, CodeChunk> chunkMap) {

        int totalDocuments = candidates.size();
        Map<String, Map<String, Integer>> termDocFrequencies = new HashMap<>();
        Map<String, Integer> docLengths = new HashMap<>();
        double totalLength = 0;

        for (CodeChunk chunk : candidates) {
            chunkMap.put(chunk.getId(), chunk);
            List<String> terms = tokenize(getSearchableText(chunk));
            docLengths.put(chunk.getId(), terms.size());
            totalLength += terms.size();

            Map<String, Integer> termCounts = new HashMap<>();
            for (String term : terms) {
                termCounts.put(term, termCounts.getOrDefault(term, 0) + 1);
            }
            for (Map.Entry<String, Integer> entry : termCounts.entrySet()) {
                termDocFrequencies
                        .computeIfAbsent(entry.getKey(), k -> new HashMap<>())
                        .put(chunk.getId(), entry.getValue());
            }
        }

        double avgDocLength = totalDocuments > 0 ? (totalLength / totalDocuments) : 1.0;
        Map<String, Double> chunkScores = new HashMap<>();

        for (String qTerm : queryTerms) {
            Map<String, Integer> docFreqs = termDocFrequencies.get(qTerm);
            if (docFreqs == null || docFreqs.isEmpty()) {
                continue;
            }
            int n = docFreqs.size();
            double idf = Math.log(1.0 + (totalDocuments - n + 0.5) / (n + 0.5));

            for (Map.Entry<String, Integer> entry : docFreqs.entrySet()) {
                String docId = entry.getKey();
                int tf = entry.getValue();
                int docLen = docLengths.getOrDefault(docId, 1);
                double num = tf * (K1 + 1.0);
                double denom = tf + K1 * (1.0 - B + B * (docLen / avgDocLength));
                double termScore = idf * (num / denom);
                chunkScores.put(docId, chunkScores.getOrDefault(docId, 0.0) + termScore);
            }
        }
        return chunkScores;
    }

    /**
     * Tokenizes a text string into code-aware keywords.
     */
    public List<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }

        List<String> tokens = new ArrayList<>();
        Matcher matcher = CODE_TOKEN_PATTERN.matcher(text);

        while (matcher.find()) {
            String word = matcher.group();
            tokens.add(word.toLowerCase(Locale.ROOT));

            // Split camelCase: PaymentController -> payment, controller
            String[] camelParts = word.split("(?<=[a-z])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])");
            if (camelParts.length > 1) {
                for (String part : camelParts) {
                    if (part.length() > 1) {
                        tokens.add(part.toLowerCase(Locale.ROOT));
                    }
                }
            }

            // Split snake_case: payment_service -> payment, service
            if (word.contains("_")) {
                for (String part : word.split("_")) {
                    if (part.length() > 1) {
                        tokens.add(part.toLowerCase(Locale.ROOT));
                    }
                }
            }
        }

        return tokens;
    }

    private List<CodeChunk> filterChunks(List<CodeChunk> chunks, RagQuery query) {
        Set<DocumentType> allowedTypes = query.getDocumentTypes();
        if (allowedTypes == null || allowedTypes.isEmpty()) {
            return chunks;
        }
        return chunks.stream()
                .filter(c -> allowedTypes.contains(c.getDocumentType()))
                .toList();
    }

    private String getSearchableText(CodeChunk chunk) {
        StringBuilder sb = new StringBuilder();
        if (chunk.getClassName() != null) {
            sb.append(chunk.getClassName()).append(" ");
        }
        if (chunk.getMethodName() != null) {
            sb.append(chunk.getMethodName()).append(" ");
        }
        if (chunk.getSymbolInfo() != null) {
            sb.append(chunk.getSymbolInfo()).append(" ");
        }
        if (chunk.getFilePath() != null) {
            sb.append(chunk.getFilePath()).append(" ");
        }
        sb.append(chunk.getContent());
        return sb.toString();
    }
}
