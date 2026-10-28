package com.example.Jark.service.rag;

import com.example.Jark.dto.rag.CodeChunk;
import com.example.Jark.dto.rag.DocumentType;
import com.example.Jark.dto.rag.RagQuery;
import com.example.Jark.dto.rag.ScoredChunk;
import com.example.Jark.service.rag.retrieval.LexicalRetriever;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LexicalRetrieverTest {

    private LexicalRetriever lexicalRetriever;

    @BeforeEach
    void setUp() {
        lexicalRetriever = new LexicalRetriever();
    }

    @Test
    void tokenize_splitsCamelCaseAndSnakeCase() {
        List<String> tokens = lexicalRetriever.tokenize("processPaymentTransaction user_auth_token");

        assertTrue(tokens.contains("processpaymenttransaction"));
        assertTrue(tokens.contains("process"));
        assertTrue(tokens.contains("payment"));
        assertTrue(tokens.contains("transaction"));

        assertTrue(tokens.contains("user_auth_token"));
        assertTrue(tokens.contains("user"));
        assertTrue(tokens.contains("auth"));
        assertTrue(tokens.contains("token"));
    }

    @Test
    void retrieve_ranksRelevantChunksHigher() {
        CodeChunk chunk1 = CodeChunk.builder()
                .id("c1")
                .repositoryId(1L)
                .documentType(DocumentType.SOURCE_CODE)
                .filePath("src/main/java/PaymentService.java")
                .className("PaymentService")
                .methodName("processRefund")
                .content("public void processRefund(String paymentId) { refundClient.execute(paymentId); }")
                .build();

        CodeChunk chunk2 = CodeChunk.builder()
                .id("c2")
                .repositoryId(1L)
                .documentType(DocumentType.SOURCE_CODE)
                .filePath("src/main/java/UserService.java")
                .className("UserService")
                .methodName("createUser")
                .content("public User createUser(String name) { return userRepository.save(name); }")
                .build();

        RagQuery query = RagQuery.builder()
                .query("refund payment client")
                .topK(5)
                .build();

        List<ScoredChunk> results = lexicalRetriever.retrieve(List.of(chunk1, chunk2), query);

        assertFalse(results.isEmpty());
        assertEquals("c1", results.get(0).getChunk().getId());
        assertEquals("LEXICAL", results.get(0).getPrimarySource());
        assertTrue(results.get(0).getFinalScore() > 0.0);
    }

    @Test
    void retrieve_respectsDocumentTypeFilters() {
        CodeChunk sourceChunk = CodeChunk.builder()
                .id("src1")
                .documentType(DocumentType.SOURCE_CODE)
                .content("authentication token validator")
                .build();

        CodeChunk testChunk = CodeChunk.builder()
                .id("test1")
                .documentType(DocumentType.TEST)
                .content("authentication token test")
                .build();

        RagQuery query = RagQuery.builder()
                .query("authentication token")
                .documentTypes(Set.of(DocumentType.TEST))
                .topK(5)
                .build();

        List<ScoredChunk> results = lexicalRetriever.retrieve(List.of(sourceChunk, testChunk), query);

        assertEquals(1, results.size());
        assertEquals("test1", results.get(0).getChunk().getId());
    }
}
