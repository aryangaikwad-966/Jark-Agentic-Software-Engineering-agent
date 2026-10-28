package com.example.Jark.dto.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * Encapsulates search and retrieval criteria for the Jark hybrid RAG pipeline.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagQuery {

    private String query;
    private Long repositoryId;
    private Set<DocumentType> documentTypes;
    private String targetClass;
    private String targetMethod;
    private String targetFilePath;

    @Builder.Default
    private int topK = 10;

    @Builder.Default
    private double minScore = 0.0;
}
