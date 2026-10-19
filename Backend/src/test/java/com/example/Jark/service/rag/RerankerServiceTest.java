package com.example.Jark.service.rag;

import com.example.Jark.dto.rag.CodeChunk;
import com.example.Jark.dto.rag.DocumentType;
import com.example.Jark.dto.rag.RagQuery;
import com.example.Jark.dto.rag.ScoredChunk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RerankerServiceTest {

    private RerankerService rerankerService;

    @BeforeEach
    void setUp() {
        rerankerService = new RerankerService();
    }

    @Test
    void rerank_promotesMultiChannelConsensus() {
        CodeChunk chunkA = CodeChunk.builder()
                .id("chunkA")
                .className("PaymentService")
                .content("public void processPayment() { /* payment processing logic */ }")
                .documentType(DocumentType.SOURCE_CODE)
                .build();

        CodeChunk chunkB = CodeChunk.builder()
                .id("chunkB")
                .className("OtherService")
                .content("public void doOtherThings() { /* other unrelated logic */ }")
                .documentType(DocumentType.SOURCE_CODE)
                .build();

        // Chunk A appears in both vector and lexical channels
        ScoredChunk vecA = createScored(chunkA, 0.85, "vector");
        ScoredChunk lexA = createScored(chunkA, 0.90, "lexical");

        // Chunk B appears ONLY in vector channel
        ScoredChunk vecB = createScored(chunkB, 0.88, "vector");

        RagQuery query = RagQuery.builder().query("process payment").topK(5).build();

        List<ScoredChunk> reranked = rerankerService.rerank(
                query,
                List.of(vecA, vecB),
                List.of(lexA),
                Collections.emptyList(),
                Collections.emptyList()
        );

        assertFalse(reranked.isEmpty());
        assertEquals("chunkA", reranked.get(0).getChunk().getId());
        assertTrue(reranked.get(0).getFinalScore() > reranked.get(1).getFinalScore());
        assertTrue(reranked.get(0).getChannelScores().containsKey("vector"));
        assertTrue(reranked.get(0).getChannelScores().containsKey("lexical"));
    }

    @Test
    void rerank_appliesTargetClassBoost() {
        CodeChunk chunkTarget = CodeChunk.builder()
                .id("target")
                .className("com.example.OrderService")
                .content("public void createOrder() { /* creation logic for orders */ }")
                .documentType(DocumentType.SOURCE_CODE)
                .build();

        CodeChunk chunkOther = CodeChunk.builder()
                .id("other")
                .className("com.example.BillingService")
                .content("public void createBill() { /* billing logic */ }")
                .documentType(DocumentType.SOURCE_CODE)
                .build();

        ScoredChunk scoredTarget = createScored(chunkTarget, 0.70, "vector");
        ScoredChunk scoredOther = createScored(chunkOther, 0.75, "vector");

        RagQuery query = RagQuery.builder()
                .query("create order")
                .targetClass("OrderService")
                .topK(5)
                .build();

        List<ScoredChunk> reranked = rerankerService.rerank(
                query,
                List.of(scoredOther, scoredTarget),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        );

        assertEquals("target", reranked.get(0).getChunk().getId());
    }

    private ScoredChunk createScored(CodeChunk chunk, double score, String channel) {
        Map<String, Double> map = new HashMap<>();
        map.put(channel, score);
        return ScoredChunk.builder()
                .chunk(chunk)
                .finalScore(score)
                .channelScores(map)
                .primarySource(channel.toUpperCase())
                .build();
    }
}
