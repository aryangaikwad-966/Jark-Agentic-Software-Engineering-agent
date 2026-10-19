package com.example.Jark.service.rag;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.qdrant.QdrantContainer;

import com.example.Jark.dto.rag.CodeChunk;
import com.example.Jark.dto.rag.DocumentType;
import com.example.Jark.dto.rag.RagQuery;
import com.example.Jark.dto.rag.ScoredChunk;
import com.example.Jark.service.rag.retrieval.VectorRetriever;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class QdrantVectorRetrieverIntegrationTest {

    private QdrantContainer qdrant;
    private QdrantClient qdrantClient;
    private VectorRetriever vectorRetriever;

    @BeforeAll
    void startQdrant() throws Exception {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker is required for Qdrant integration tests");

        qdrant = new QdrantContainer("qdrant/qdrant:v1.10.0");
        qdrant.start();

        qdrantClient = new QdrantClient(QdrantGrpcClient.newBuilder(
                qdrant.getHost(), qdrant.getMappedPort(6334), false).build());
        QdrantVectorStore qdrantVectorStore = new QdrantVectorStore(
                qdrantClient,
                "jark_test_" + UUID.randomUUID().toString().replace("-", ""),
                new DeterministicEmbeddingModel(),
                true);
        qdrantVectorStore.afterPropertiesSet();
        VectorStore vectorStore = qdrantVectorStore;
        vectorRetriever = new VectorRetriever(vectorStore);
    }

    @AfterAll
    void stopQdrant() {
        if (qdrantClient != null) {
            qdrantClient.close();
        }
        if (qdrant != null) {
            qdrant.stop();
        }
    }

    @Test
    void indexesRichMetadataAndRetrievesOnlyRequestedRepository() {
        List<CodeChunk> chunks = List.of(
                chunk("payment-service", 42L, "PaymentService", "processPayment",
                        "PaymentService processes payment orders through the payment gateway.",
                        DocumentType.SOURCE_CODE),
                chunk("inventory-service", 84L, "InventoryService", "reconcileInventory",
                        "InventoryService reconciles warehouse stock and inventory adjustments.",
                        DocumentType.SOURCE_CODE));

        vectorRetriever.indexChunks(chunks);

        RagQuery query = RagQuery.builder()
                .repositoryId(42L)
                .query("payment gateway process payment")
                .topK(5)
                .build();

        List<ScoredChunk> results = vectorRetriever.retrieve(query, lookup(chunks));

        assertFalse(results.isEmpty());
        assertTrue(results.stream().allMatch(result -> result.getChunk().getRepositoryId().equals(42L)));
        assertTrue(results.stream().anyMatch(result -> "PaymentService".equals(result.getChunk().getClassName())));
    }

    private Map<String, CodeChunk> lookup(List<CodeChunk> chunks) {
        Map<String, CodeChunk> lookup = new HashMap<>();
        chunks.forEach(chunk -> lookup.put(chunk.getId(), chunk));
        return lookup;
    }

    private CodeChunk chunk(
            String id,
            Long repositoryId,
            String className,
            String methodName,
            String content,
            DocumentType documentType) {
        return CodeChunk.builder()
                .id(id)
                .repositoryId(repositoryId)
                .documentType(documentType)
                .filePath("src/main/java/com/example/" + className + ".java")
                .className(className)
                .methodName(methodName)
                .symbolInfo(className + "#" + methodName)
                .startLine(1)
                .endLine(20)
                .content(content)
                .build();
    }
}