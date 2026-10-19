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

/**
 * Retrieves chunks via exact and prefix AST symbol matching (class names, method names, annotations, file paths).
 */
@Component
public class SymbolRetriever {

    /**
     * Retrieves chunks whose symbols match the query criteria.
     *
     * @param chunks complete list of chunks
     * @param query search query
     * @return list of ScoredChunks sorted descending by symbol match score
     */
    public List<ScoredChunk> retrieve(List<CodeChunk> chunks, RagQuery query) {
        if (chunks == null || chunks.isEmpty() || query == null) {
            return Collections.emptyList();
        }

        List<ScoredChunk> scored = new ArrayList<>();
        String rawQuery = query.getQuery() != null ? query.getQuery().toLowerCase(Locale.ROOT) : "";
        Set<DocumentType> allowedTypes = query.getDocumentTypes();

        for (CodeChunk chunk : chunks) {
            if (allowedTypes != null && !allowedTypes.isEmpty() && !allowedTypes.contains(chunk.getDocumentType())) {
                continue;
            }

            double score = computeSymbolScore(chunk, query, rawQuery);
            if (score > 0.0 && score >= query.getMinScore()) {
                Map<String, Double> channelScores = new HashMap<>();
                channelScores.put("symbol", score);
                scored.add(ScoredChunk.builder()
                        .chunk(chunk)
                        .finalScore(score)
                        .channelScores(channelScores)
                        .primarySource("SYMBOL")
                        .build());
            }
        }

        Collections.sort(scored);
        int limit = Math.min(query.getTopK(), scored.size());
        return scored.subList(0, limit);
    }

    private double computeSymbolScore(CodeChunk chunk, RagQuery query, String rawQuery) {
        double score = 0.0;

        // Explicit filter matches
        if (query.getTargetClass() != null && matchesClass(chunk, query.getTargetClass())) {
            score = Math.max(score, 1.0);
        }
        if (query.getTargetMethod() != null && matchesMethod(chunk, query.getTargetMethod())) {
            score = Math.max(score, 0.95);
        }
        if (query.getTargetFilePath() != null && chunk.getFilePath() != null
                && chunk.getFilePath().contains(query.getTargetFilePath())) {
            score = Math.max(score, 0.85);
        }

        // Exact symbol mentions in query text
        if (!rawQuery.isBlank()) {
            if (chunk.getClassName() != null && isClassMentioned(chunk.getClassName(), rawQuery)) {
                score = Math.max(score, 0.95);
            }
            if (chunk.getMethodName() != null && isMethodMentioned(chunk.getMethodName(), rawQuery)) {
                score = Math.max(score, 0.90);
            }
            if (chunk.getSymbolInfo() != null && isAnnotationMentioned(chunk.getSymbolInfo(), rawQuery)) {
                score = Math.max(score, 0.85);
            }
        }

        return score;
    }

    private boolean matchesClass(CodeChunk chunk, String targetClass) {
        if (chunk.getClassName() == null) {
            return false;
        }
        String cn = chunk.getClassName();
        return cn.equalsIgnoreCase(targetClass) || cn.endsWith("." + targetClass);
    }

    private boolean matchesMethod(CodeChunk chunk, String targetMethod) {
        if (chunk.getMethodName() == null) {
            return false;
        }
        return chunk.getMethodName().equalsIgnoreCase(targetMethod);
    }

    private boolean isClassMentioned(String className, String rawQuery) {
        String simpleName = className.contains(".")
                ? className.substring(className.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT)
                : className.toLowerCase(Locale.ROOT);
        return rawQuery.contains(simpleName);
    }

    private boolean isMethodMentioned(String methodName, String rawQuery) {
        String name = methodName.toLowerCase(Locale.ROOT);
        return name.length() > 2 && rawQuery.contains(name);
    }

    private boolean isAnnotationMentioned(String symbolInfo, String rawQuery) {
        String info = symbolInfo.toLowerCase(Locale.ROOT);
        for (String part : info.split("[,\\s]+")) {
            String trimmed = part.startsWith("@") ? part.substring(1) : part;
            if (trimmed.length() > 3 && rawQuery.contains(trimmed)) {
                return true;
            }
        }
        return false;
    }
}
