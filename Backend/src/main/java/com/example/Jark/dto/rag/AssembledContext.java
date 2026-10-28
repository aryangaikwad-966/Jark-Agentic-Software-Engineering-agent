package com.example.Jark.dto.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Encapsulates the final assembled prompt context generated from hybrid retrieval.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssembledContext {

    private String formattedContext;

    @Builder.Default
    private List<ScoredChunk> chunks = new ArrayList<>();

    private int estimatedTokens;

    @Builder.Default
    private Map<DocumentType, Integer> chunkCountsByType = new HashMap<>();
}
