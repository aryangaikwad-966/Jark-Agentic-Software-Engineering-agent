package com.example.VoxCode.service.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Deterministic in-memory embedding model for test suites and offline operation.
 * Hashes n-grams and tokens into a normalized fixed-dimension unit vector.
 */
public class DeterministicEmbeddingModel implements EmbeddingModel {

    public static final int DEFAULT_DIMENSIONS = 384;
    private final int dimensions;

    public DeterministicEmbeddingModel() {
        this(DEFAULT_DIMENSIONS);
    }

    public DeterministicEmbeddingModel(int dimensions) {
        this.dimensions = dimensions;
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<Embedding> embeddings = new ArrayList<>();
        List<String> instructions = request.getInstructions();
        for (int i = 0; i < instructions.size(); i++) {
            List<Double> vector = embed(instructions.get(i));
            embeddings.add(new Embedding(vector, i));
        }
        return new EmbeddingResponse(embeddings);
    }

    @Override
    public List<Double> embed(Document document) {
        return embed(document.getContent());
    }

    @Override
    public List<Double> embed(String text) {
        double[] raw = new double[dimensions];
        if (text == null || text.isBlank()) {
            return toDoubleList(raw);
        }

        String normalized = text.toLowerCase(Locale.ROOT);
        String[] tokens = normalized.split("[^a-zA-Z0-9_]+");

        for (String token : tokens) {
            if (token.isEmpty()) {
                continue;
            }
            int bucket = Math.floorMod(token.hashCode(), dimensions);
            raw[bucket] += 1.0;

            // Character trigrams
            for (int i = 0; i <= token.length() - 3; i++) {
                String trigram = token.substring(i, i + 3);
                int triBucket = Math.floorMod(trigram.hashCode(), dimensions);
                raw[triBucket] += 0.5;
            }
        }

        // Normalize to unit length (L2 norm)
        double sumSq = 0.0;
        for (double v : raw) {
            sumSq += v * v;
        }

        if (sumSq > 0.0) {
            double norm = Math.sqrt(sumSq);
            for (int i = 0; i < raw.length; i++) {
                raw[i] /= norm;
            }
        }

        return toDoubleList(raw);
    }

    @Override
    public int dimensions() {
        return dimensions;
    }

    private List<Double> toDoubleList(double[] array) {
        List<Double> list = new ArrayList<>(array.length);
        for (double v : array) {
            list.add(v);
        }
        return list;
    }
}
