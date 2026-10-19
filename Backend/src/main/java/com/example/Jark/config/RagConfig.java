package com.example.Jark.config;

import com.example.Jark.service.rag.DeterministicEmbeddingModel;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

/**
 * Spring configuration providing VectorStore and EmbeddingModel beans.
 * Configures Qdrant when explicitly enabled and a deterministic in-memory fallback otherwise.
 */
@Configuration
public class RagConfig {

    @Bean
    @ConditionalOnMissingBean(EmbeddingModel.class)
    public EmbeddingModel embeddingModel() {
        return new DeterministicEmbeddingModel();
    }

    @Bean
    @ConditionalOnProperty(name = "jark.rag.qdrant.enabled", havingValue = "true")
    public QdrantClient qdrantClient(
            @Value("${jark.rag.qdrant.host:localhost}") String host,
            @Value("${jark.rag.qdrant.grpc-port:6334}") int port,
            @Value("${JARK_QDRANT_API_KEY:}") String apiKey) {
        QdrantGrpcClient.Builder builder = QdrantGrpcClient.newBuilder(host, port, false);
        if (!apiKey.isBlank()) {
            builder.withApiKey(apiKey);
        }
        return new QdrantClient(builder.build());
    }

    @Bean
    @ConditionalOnProperty(name = "jark.rag.qdrant.enabled", havingValue = "true")
    @ConditionalOnMissingBean(VectorStore.class)
    public VectorStore qdrantVectorStore(
            QdrantClient qdrantClient,
            EmbeddingModel embeddingModel,
            @Value("${jark.rag.qdrant.collection:jark_chunks}") String collection) {
        return new QdrantVectorStore(qdrantClient, collection, embeddingModel, true);
    }

    @Bean
    @ConditionalOnMissingBean(VectorStore.class)
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return new SimpleVectorStore(embeddingModel);
    }
}
