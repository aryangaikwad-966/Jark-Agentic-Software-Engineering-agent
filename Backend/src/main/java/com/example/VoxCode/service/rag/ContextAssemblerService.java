package com.example.VoxCode.service.rag;

import com.example.VoxCode.dto.rag.AssembledContext;
import com.example.VoxCode.dto.rag.CodeChunk;
import com.example.VoxCode.dto.rag.DocumentType;
import com.example.VoxCode.dto.rag.ScoredChunk;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Assembles balanced, prompt-ready markdown context from reranked chunks within token budgets.
 */
@Service
public class ContextAssemblerService {

    private static final int CHARS_PER_TOKEN = 4;
    private static final int MAX_CHUNKS_PER_FILE = 3;

    /**
     * Assembles prompt context from retrieved candidate chunks while respecting token limits and file diversity.
     *
     * @param rankedChunks reranked chunks sorted by relevance
     * @param maxTokens maximum token budget (e.g., 4000)
     * @return AssembledContext containing formatted markdown and metadata
     */
    public AssembledContext assemble(List<ScoredChunk> rankedChunks, int maxTokens) {
        if (rankedChunks == null || rankedChunks.isEmpty() || maxTokens <= 0) {
            return AssembledContext.builder()
                    .formattedContext("")
                    .chunks(List.of())
                    .estimatedTokens(0)
                    .chunkCountsByType(Map.of())
                    .build();
        }

        int maxChars = maxTokens * CHARS_PER_TOKEN;
        int currentChars = 0;

        List<ScoredChunk> selectedChunks = new ArrayList<>();
        Map<String, Integer> chunksPerFile = new HashMap<>();
        Map<DocumentType, Integer> typeCounts = new HashMap<>();
        Set<String> seenContents = new HashSet<>();
        StringBuilder contextBuilder = new StringBuilder();

        contextBuilder.append("## Repository Context\n\n");
        currentChars += contextBuilder.length();

        for (ScoredChunk scored : rankedChunks) {
            CodeChunk chunk = scored.getChunk();
            String file = chunk.getFilePath() != null ? chunk.getFilePath() : "unknown";

            // Enforce file diversity cap
            int countForFile = chunksPerFile.getOrDefault(file, 0);
            if (countForFile >= MAX_CHUNKS_PER_FILE) {
                continue;
            }

            // Deduplicate exact content
            String cleanContent = chunk.getContent() != null ? chunk.getContent().strip() : "";
            if (cleanContent.isEmpty() || seenContents.contains(cleanContent)) {
                continue;
            }

            String formattedChunk = formatChunk(scored);
            int chunkChars = formattedChunk.length();

            if (currentChars + chunkChars > maxChars && !selectedChunks.isEmpty()) {
                // Token budget reached
                break;
            }

            seenContents.add(cleanContent);
            selectedChunks.add(scored);
            chunksPerFile.put(file, countForFile + 1);
            typeCounts.put(chunk.getDocumentType(), typeCounts.getOrDefault(chunk.getDocumentType(), 0) + 1);

            contextBuilder.append(formattedChunk).append("\n\n");
            currentChars += chunkChars + 2;
        }

        int estimatedTokens = Math.max(1, currentChars / CHARS_PER_TOKEN);

        return AssembledContext.builder()
                .formattedContext(contextBuilder.toString().stripTrailing())
                .chunks(selectedChunks)
                .estimatedTokens(estimatedTokens)
                .chunkCountsByType(typeCounts)
                .build();
    }

    private String formatChunk(ScoredChunk scored) {
        CodeChunk chunk = scored.getChunk();
        StringBuilder sb = new StringBuilder();

        String typeTag = chunk.getDocumentType() != null ? chunk.getDocumentType().name() : "CODE";
        String targetName = chunk.getClassName() != null ? chunk.getClassName() : chunk.getFilePath();

        sb.append(String.format("### [%s] %s (Relevance: %.2f | Source: %s)%n",
                typeTag,
                targetName,
                scored.getFinalScore(),
                scored.getPrimarySource() != null ? scored.getPrimarySource() : "HYBRID"));

        sb.append(String.format("**File:** `%s` (Lines %d-%d)",
                chunk.getFilePath(),
                chunk.getStartLine(),
                chunk.getEndLine()));

        if (chunk.getMethodName() != null && !chunk.getMethodName().isBlank()) {
            sb.append(String.format(" | **Method:** `%s`", chunk.getMethodName()));
        }
        if (chunk.getSymbolInfo() != null && !chunk.getSymbolInfo().isBlank()) {
            sb.append(String.format(" | **Symbols:** `%s`", chunk.getSymbolInfo()));
        }
        sb.append("\n\n");

        String language = determineLanguage(chunk.getFilePath());
        sb.append("```").append(language).append("\n");
        sb.append(chunk.getContent().stripTrailing()).append("\n");
        sb.append("```");

        return sb.toString();
    }

    private String determineLanguage(String filePath) {
        if (filePath == null) {
            return "";
        }
        String lower = filePath.toLowerCase();
        if (lower.endsWith(".java")) {
            return "java";
        }
        if (lower.endsWith(".md") || lower.endsWith(".markdown")) {
            return "markdown";
        }
        if (lower.endsWith(".yml") || lower.endsWith(".yaml")) {
            return "yaml";
        }
        if (lower.endsWith(".properties")) {
            return "properties";
        }
        if (lower.endsWith(".xml")) {
            return "xml";
        }
        if (lower.endsWith(".json")) {
            return "json";
        }
        return "";
    }
}
