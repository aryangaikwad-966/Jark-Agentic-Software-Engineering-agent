package com.example.Jark.agent.tools;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Test RAG tool structure and schemas without mocking services.
 * Tests focus on tool function structure, request/response validation, and error handling.
 */
class RagToolsTest {

    @Test
    void toolRequestRecords_haveCorrectStructure() {
        // Assert request record structure
        var searchRequest = new RagTools.SearchSemanticContextRequest(
                1L, "user authentication", "UserService", "findById", 
                Set.of("SOURCE_CODE", "DOCUMENTATION"), 5);
        assertEquals(1L, searchRequest.repositoryId());
        assertEquals("user authentication", searchRequest.query());
        assertEquals("UserService", searchRequest.targetClass());
        assertEquals("findById", searchRequest.targetMethod());
        assertEquals(Set.of("SOURCE_CODE", "DOCUMENTATION"), searchRequest.documentTypes());
        assertEquals(5, searchRequest.topK());

        var assembleRequest = new RagTools.AssembleContextRequest(
                1L, "user authentication", "UserService", "findById",
                Set.of("SOURCE_CODE"), 10, 2000);
        assertEquals(1L, assembleRequest.repositoryId());
        assertEquals("user authentication", assembleRequest.query());
        assertEquals("UserService", assembleRequest.targetClass());
        assertEquals("findById", assembleRequest.targetMethod());
        assertEquals(Set.of("SOURCE_CODE"), assembleRequest.documentTypes());
        assertEquals(10, assembleRequest.topK());
        assertEquals(2000, assembleRequest.maxTokens());
    }

    @Test
    void toolResponseRecords_haveCorrectStructure() {
        // Assert response record structure
        var successResponse = new RagTools.SearchSemanticContextResponse(true, "Success", List.of());
        assertTrue(successResponse.success());
        assertEquals("Success", successResponse.message());
        assertTrue(successResponse.chunks().isEmpty());

        var errorResponse = new RagTools.SearchSemanticContextResponse(false, "Error", null);
        assertFalse(errorResponse.success());
        assertEquals("Error", errorResponse.message());
        assertNull(errorResponse.chunks());

        var assembleSuccessResponse = new RagTools.AssembleContextResponse(true, "Success", null);
        assertTrue(assembleSuccessResponse.success());
        assertEquals("Success", assembleSuccessResponse.message());
        assertNull(assembleSuccessResponse.context());

        var assembleErrorResponse = new RagTools.AssembleContextResponse(false, "Error", null);
        assertFalse(assembleErrorResponse.success());
        assertEquals("Error", assembleErrorResponse.message());
        assertNull(assembleErrorResponse.context());
    }

    @Test
    void toolRequestRecords_allowEmptyStrings() {
        // Test that request records allow empty strings (validation happens in tool functions)
        var emptyStringRequest = new RagTools.SearchSemanticContextRequest(1L, "", null, null, null, 5);
        assertEquals(1L, emptyStringRequest.repositoryId());
        assertEquals("", emptyStringRequest.query());
    }

    @Test
    void toolRequestRecords_allowNullValues() {
        // Test that request records allow null values (validation happens in tool functions)
        var nullRequest = new RagTools.SearchSemanticContextRequest(null, "user authentication", null, null, null, 5);
        assertNull(nullRequest.repositoryId());
        assertEquals("user authentication", nullRequest.query());
    }

    @Test
    void toolRequestRecords_allowNegativeTopK() {
        // Test that request records allow negative values (validation happens in tool functions)
        var negativeTopKRequest = new RagTools.SearchSemanticContextRequest(1L, "user authentication", null, null, null, -1);
        assertEquals(-1, negativeTopKRequest.topK());
    }

    @Test
    void toolRequestRecords_allowNegativeMaxTokens() {
        // Test that request records allow negative values (validation happens in tool functions)
        var negativeMaxTokensRequest = new RagTools.AssembleContextRequest(1L, "user authentication", null, null, null, 10, -1);
        assertEquals(-1, negativeMaxTokensRequest.maxTokens());
    }
}