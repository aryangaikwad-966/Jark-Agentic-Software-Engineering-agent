package com.example.Jark.dto.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Encapsulates an indexed CodeChunk accompanied by hybrid retrieval scores.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoredChunk implements Comparable<ScoredChunk> {

    private CodeChunk chunk;
    private double finalScore;

    @Builder.Default
    private Map<String, Double> channelScores = new HashMap<>();

    private String primarySource;

    @Override
    public int compareTo(ScoredChunk other) {
        return Double.compare(other.finalScore, this.finalScore);
    }
}
