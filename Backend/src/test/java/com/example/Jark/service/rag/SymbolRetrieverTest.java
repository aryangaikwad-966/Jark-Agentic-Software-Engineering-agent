package com.example.Jark.service.rag;

import com.example.Jark.dto.rag.CodeChunk;
import com.example.Jark.dto.rag.DocumentType;
import com.example.Jark.dto.rag.RagQuery;
import com.example.Jark.dto.rag.ScoredChunk;
import com.example.Jark.service.rag.retrieval.SymbolRetriever;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SymbolRetrieverTest {

    private SymbolRetriever symbolRetriever;

    @BeforeEach
    void setUp() {
        symbolRetriever = new SymbolRetriever();
    }

    @Test
    void retrieve_matchesExplicitTargetClassAndMethod() {
        CodeChunk chunk = CodeChunk.builder()
                .id("c1")
                .documentType(DocumentType.SOURCE_CODE)
                .className("com.example.PaymentService")
                .methodName("executePayment")
                .content("public void executePayment() {}")
                .build();

        RagQuery query = RagQuery.builder()
                .targetClass("PaymentService")
                .targetMethod("executePayment")
                .topK(5)
                .build();

        List<ScoredChunk> results = symbolRetriever.retrieve(List.of(chunk), query);

        assertFalse(results.isEmpty());
        assertEquals("c1", results.get(0).getChunk().getId());
        assertEquals("SYMBOL", results.get(0).getPrimarySource());
        assertTrue(results.get(0).getFinalScore() >= 0.95);
    }

    @Test
    void retrieve_detectsSymbolsInQueryText() {
        CodeChunk controllerChunk = CodeChunk.builder()
                .id("ctrl")
                .documentType(DocumentType.SOURCE_CODE)
                .className("com.example.OrderController")
                .symbolInfo("RestController, CrossOrigin")
                .content("@RestController public class OrderController {}")
                .build();

        CodeChunk helperChunk = CodeChunk.builder()
                .id("help")
                .documentType(DocumentType.SOURCE_CODE)
                .className("com.example.MathHelper")
                .content("public class MathHelper {}")
                .build();

        RagQuery query = RagQuery.builder()
                .query("Where is the OrderController defined with RestController annotation?")
                .topK(5)
                .build();

        List<ScoredChunk> results = symbolRetriever.retrieve(List.of(controllerChunk, helperChunk), query);

        assertFalse(results.isEmpty());
        assertEquals("ctrl", results.get(0).getChunk().getId());
        assertTrue(results.get(0).getFinalScore() > 0.8);
    }
}
