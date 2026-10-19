package com.example.Jark.dto.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a discrete code or text chunk indexed in the Jark RAG pipeline.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeChunk {

    private String id;
    private Long repositoryId;
    private DocumentType documentType;
    private String filePath;
    private String className;
    private String methodName;
    private String symbolInfo;
    private int startLine;
    private int endLine;
    private String content;

    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * Converts chunk fields into a structured metadata map for Spring AI Document and Qdrant storage.
     *
     * @return map containing all chunk metadata
     */
    public Map<String, Object> toMetadataMap() {
        Map<String, Object> map = new HashMap<>(metadata);
        if (repositoryId != null) {
            map.put("repositoryId", repositoryId);
        }
        if (documentType != null) {
            map.put("documentType", documentType.name());
        }
        if (filePath != null) {
            map.put("filePath", filePath);
        }
        if (className != null) {
            map.put("className", className);
        }
        if (methodName != null) {
            map.put("methodName", methodName);
        }
        if (symbolInfo != null) {
            map.put("symbolInfo", symbolInfo);
        }
        map.put("startLine", startLine);
        map.put("endLine", endLine);
        return map;
    }
}
