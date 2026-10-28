package com.example.Jark.service.rag.retrieval;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import com.example.Jark.dto.rag.CodeChunk;
import com.example.Jark.dto.rag.DocumentType;
import com.example.Jark.dto.rag.RagQuery;
import com.example.Jark.dto.rag.ScoredChunk;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Executes semantic vector similarity search via Spring AI VectorStore (Qdrant / SimpleVectorStore).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VectorRetriever {

    private final VectorStore vectorStore;

    /**
     * Stores chunks into the underlying vector store.
     *
     * @param chunks chunks to embed and store
     */
    public void indexChunks(List<CodeChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return;
        }

        List<Document> documents = new ArrayList<>();
        for (CodeChunk chunk : chunks) {
            Map<String, Object> metadata = normalizeMetadata(chunk.toMetadataMap());
            metadata.put("chunkId", chunk.getId());
            documents.add(new Document(toVectorStoreId(chunk.getId()), chunk.getContent(), metadata));
        }

        try {
            vectorStore.add(documents);
            log.info("Successfully added {} documents to VectorStore", documents.size());
        } catch (Exception e) {
            log.error("Failed to add documents to VectorStore: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to index documents into vector store: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves chunks matching the query based on semantic vector similarity.
     *
     * @param query search query
     * @param chunkLookup map of chunk ID to original CodeChunk
     * @return list of ScoredChunks sorted descending by semantic similarity
     */
    public List<ScoredChunk> retrieve(RagQuery query, Map<String, CodeChunk> chunkLookup) {
        if (query == null || query.getQuery() == null || query.getQuery().isBlank()) {
            return Collections.emptyList();
        }

        int searchTopK = Math.max(query.getTopK() * 2, 10);
        SearchRequest request = SearchRequest.query(query.getQuery())
                .withTopK(searchTopK);
        if (query.getRepositoryId() != null) {
            request = request.withFilterExpression(repositoryFilter(query.getRepositoryId()));
        }

        List<Document> matches;
        try {
            matches = vectorStore.similaritySearch(request);
        } catch (Exception e) {
            if (query.getRepositoryId() == null) {
                log.warn("Vector similarity search failed: {}", e.getMessage());
                return Collections.emptyList();
            }
            log.debug("Vector store does not support metadata filtering; retrying with defensive post-filter: {}",
                    e.getMessage());
            try {
                matches = vectorStore.similaritySearch(SearchRequest.query(query.getQuery()).withTopK(searchTopK));
            } catch (Exception retryException) {
                log.warn("Vector similarity search failed: {}", retryException.getMessage());
                return Collections.emptyList();
            }
        }

        if (matches == null || matches.isEmpty()) {
            return Collections.emptyList();
        }

        List<ScoredChunk> results = new ArrayList<>();
        int totalMatches = matches.size();

        for (int i = 0; i < totalMatches; i++) {
            Document doc = matches.get(i);
            String chunkId = originalChunkId(doc);
            CodeChunk chunk = chunkLookup != null ? chunkLookup.get(chunkId) : null;

            if (chunk == null) {
                chunk = reconstructChunkFromDocument(doc);
            }

            if (query.getRepositoryId() != null
                    && !query.getRepositoryId().equals(chunk.getRepositoryId())) {
                continue;
            }

            if (query.getDocumentTypes() != null && !query.getDocumentTypes().isEmpty()
                    && !query.getDocumentTypes().contains(chunk.getDocumentType())) {
                continue;
            }

            double score = extractSimilarityScore(doc, i, totalMatches);
            if (score >= query.getMinScore()) {
                Map<String, Double> channelScores = new HashMap<>();
                channelScores.put("vector", score);

                results.add(ScoredChunk.builder()
                        .chunk(chunk)
                        .finalScore(score)
                        .channelScores(channelScores)
                        .primarySource("VECTOR")
                        .build());
            }
        }

        Collections.sort(results);
        int limit = Math.min(query.getTopK(), results.size());
        return results.subList(0, limit);
    }

    private double extractSimilarityScore(Document doc, int rank, int total) {
        Map<String, Object> metadata = doc.getMetadata();
        if (metadata != null) {
            Object sim = metadata.get("similarity");
            if (sim instanceof Number number) {
                return number.doubleValue();
            }
            Object dist = metadata.get("distance");
            if (dist instanceof Number number) {
                return Math.max(0.0, 1.0 - number.doubleValue());
            }
        }
        // Graceful rank decay fallback if vector store doesn't populate score metadata
        return Math.max(0.1, 1.0 - ((double) rank / (double) (total + 1)));
    }

    private CodeChunk reconstructChunkFromDocument(Document doc) {
        Map<String, Object> meta = doc.getMetadata() != null ? doc.getMetadata() : Collections.emptyMap();
        DocumentType type = DocumentType.SOURCE_CODE;
        if (meta.containsKey("documentType")) {
            try {
                type = DocumentType.valueOf(String.valueOf(meta.get("documentType")));
            } catch (Exception ignored) {
                // Keep default
            }
        }

        Long repoId = null;
        if (meta.containsKey("repositoryId")) {
            try {
                repoId = Long.parseLong(String.valueOf(meta.get("repositoryId")));
            } catch (Exception ignored) {
                // Nullable
            }
        }

        return CodeChunk.builder()
            .id(String.valueOf(meta.getOrDefault("chunkId", doc.getId())))
                .repositoryId(repoId)
                .documentType(type)
                .filePath((String) meta.get("filePath"))
                .className((String) meta.get("className"))
                .methodName((String) meta.get("methodName"))
                .symbolInfo((String) meta.get("symbolInfo"))
                .startLine(meta.containsKey("startLine") ? ((Number) meta.get("startLine")).intValue() : 1)
                .endLine(meta.containsKey("endLine") ? ((Number) meta.get("endLine")).intValue() : 1)
                .content(doc.getContent())
                .metadata(new HashMap<>(meta))
                .build();
    }

    private String originalChunkId(Document document) {
        Object chunkId = document.getMetadata() == null ? null : document.getMetadata().get("chunkId");
        return chunkId == null ? document.getId() : String.valueOf(chunkId);
    }

    private String toVectorStoreId(String chunkId) {
        return UUID.nameUUIDFromBytes(chunkId.getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
    }

    private Map<String, Object> normalizeMetadata(Map<String, Object> metadata) {
        Map<String, Object> normalized = new HashMap<>();
        metadata.forEach((key, value) -> {
            if (value instanceof Long longValue
                    && longValue >= Integer.MIN_VALUE && longValue <= Integer.MAX_VALUE) {
                normalized.put(key, longValue.intValue());
            } else if (value instanceof Long) {
                normalized.put(key, String.valueOf(value));
            } else {
                normalized.put(key, value);
            }
        });
        return normalized;
    }

    private String repositoryFilter(Long repositoryId) {
        if (repositoryId >= Integer.MIN_VALUE && repositoryId <= Integer.MAX_VALUE) {
            return "repositoryId == " + repositoryId.intValue();
        }
        return "repositoryId == '" + repositoryId + "'";
    }
}
