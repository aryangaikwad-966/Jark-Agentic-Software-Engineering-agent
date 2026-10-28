package com.example.Jark.service.rag.evaluation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.example.Jark.dto.rag.CodeChunk;
import com.example.Jark.dto.rag.DocumentType;
import com.example.Jark.dto.rag.RagQuery;

/**
 * Deterministic benchmark dataset derived from indexed repository chunks.
 */
public record RetrievalBenchmarkDataset(List<RetrievalBenchmarkCase> cases) {

    public RetrievalBenchmarkDataset {
        cases = List.copyOf(cases);
    }

    public static RetrievalBenchmarkDataset fromIndexedChunks(Long repositoryId, List<CodeChunk> chunks) {
        List<RetrievalBenchmarkCase> cases = new ArrayList<>();
        if (chunks == null) {
            return new RetrievalBenchmarkDataset(cases);
        }

        chunks.stream()
                .filter(chunk -> chunk.getClassName() != null && !chunk.getClassName().isBlank())
                .limit(5)
                .forEach(chunk -> cases.add(new RetrievalBenchmarkCase(
                        "class:" + chunk.getClassName(),
                        RagQuery.builder()
                                .repositoryId(repositoryId)
                                .query(chunk.getClassName())
                                .targetClass(simpleName(chunk.getClassName()))
                                .topK(5)
                                .build(),
                        java.util.Set.of(chunk.getId()))));

        addDocumentCase(cases, repositoryId, chunks, DocumentType.DOCUMENTATION, "documentation");
        addDocumentCase(cases, repositoryId, chunks, DocumentType.CONFIGURATION, "configuration");
        return new RetrievalBenchmarkDataset(cases);
    }

        private static void addDocumentCase(
            List<RetrievalBenchmarkCase> cases,
            Long repositoryId,
            List<CodeChunk> chunks,
            DocumentType documentType,
            String name) {
        chunks.stream()
                .filter(chunk -> documentType == chunk.getDocumentType())
                .findFirst()
                .ifPresent(chunk -> {
                    // The content provides a deterministic query without requiring a hand-authored corpus.
                    String query = firstWords(chunk.getContent(), 8);
                    throwIfBlank(query, name);
                        cases.add(new RetrievalBenchmarkCase(
                            name,
                            RagQuery.builder()
                                .repositoryId(repositoryId)
                                .query(query)
                                .documentTypes(Set.of(documentType))
                                .topK(5)
                                .build(),
                            Set.of(chunk.getId())));
                });
    }

    private static String simpleName(String className) {
        int separator = className.lastIndexOf('.');
        return separator >= 0 ? className.substring(separator + 1) : className;
    }

    private static String firstWords(String content, int limit) {
        if (content == null) {
            return "";
        }
        String[] words = content.strip().split("\\s+");
        return String.join(" ", java.util.Arrays.copyOf(words, Math.min(words.length, limit)));
    }

    private static void throwIfBlank(String value, String name) {
        if (value.isBlank()) {
            throw new IllegalArgumentException("Cannot create " + name + " benchmark case from blank content");
        }
    }
}
