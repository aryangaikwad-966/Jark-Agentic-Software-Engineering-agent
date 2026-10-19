package com.example.Jark.evidence.model;

import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a single piece of verifiable deterministic evidence linking
 * a finding to concrete repository artifacts (AST nodes, Graph relationships, RAG chunks, or files).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EvidenceItem(
        @JsonProperty("evidenceType") String evidenceType,
        @JsonProperty("source") String source,
        @JsonProperty("content") String content,
        @JsonProperty("metadata") Map<String, Object> metadata,
        @JsonProperty("confidenceScore") Double confidenceScore
) {

    @JsonCreator
    public EvidenceItem {
        if (evidenceType == null || evidenceType.isBlank()) {
            throw new IllegalArgumentException("evidenceType must not be null or blank");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content must not be null or blank");
        }
        if (metadata == null) {
            metadata = Collections.emptyMap();
        } else {
            metadata = Collections.unmodifiableMap(metadata);
        }
        if (confidenceScore != null && (confidenceScore < 0.0 || confidenceScore > 1.0)) {
            throw new IllegalArgumentException("confidenceScore must be between 0.0 and 1.0, got: " + confidenceScore);
        }
    }

    public static EvidenceItem astNode(String source, String content, String nodeType, LineRange lineRange, Map<String, Object> extraMeta) {
        java.util.Map<String, Object> meta = new java.util.HashMap<>();
        if (extraMeta != null) {
            meta.putAll(extraMeta);
        }
        meta.put("nodeType", nodeType);
        if (lineRange != null) {
            meta.put("startLine", lineRange.startLine());
            meta.put("endLine", lineRange.endLine());
        }
        return new EvidenceItem("AST_NODE", source, content, meta, 1.0);
    }

    public static EvidenceItem graphEdge(String source, String content, String edgeType, String fromNode, String toNode) {
        java.util.Map<String, Object> meta = new java.util.HashMap<>();
        meta.put("edgeType", edgeType);
        meta.put("from", fromNode);
        meta.put("to", toNode);
        return new EvidenceItem("GRAPH_EDGE", source, content, meta, 1.0);
    }

    public static EvidenceItem ragChunk(String source, String content, double similarityScore, Map<String, Object> chunkMeta) {
        java.util.Map<String, Object> meta = new java.util.HashMap<>();
        if (chunkMeta != null) {
            meta.putAll(chunkMeta);
        }
        meta.put("similarityScore", similarityScore);
        return new EvidenceItem("RAG_CHUNK", source, content, meta, Math.min(1.0, Math.max(0.0, similarityScore)));
    }

    public static EvidenceItem fileContent(String source, String content, String filePath, LineRange lineRange) {
        java.util.Map<String, Object> meta = new java.util.HashMap<>();
        meta.put("filePath", filePath);
        if (lineRange != null) {
            meta.put("startLine", lineRange.startLine());
            meta.put("endLine", lineRange.endLine());
        }
        return new EvidenceItem("FILE_CONTENT", source, content, meta, 1.0);
    }
}
