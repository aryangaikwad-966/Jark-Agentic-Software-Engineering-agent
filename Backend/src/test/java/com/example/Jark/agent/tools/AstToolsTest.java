package com.example.Jark.agent.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Test AST tool structure and schemas without mocking services.
 * Tests focus on tool function structure, request/response validation, and error handling.
 */
class AstToolsTest {

    @Test
    void toolRequestRecords_haveCorrectStructure() {
        // Assert request record structure
        var findClassRequest = new AstTools.FindClassRequest(1L, "UserService");
        assertEquals(1L, findClassRequest.repositoryId());
        assertEquals("UserService", findClassRequest.className());

        var findMethodRequest = new AstTools.FindMethodRequest(1L, "findById");
        assertEquals(1L, findMethodRequest.repositoryId());
        assertEquals("findById", findMethodRequest.methodName());

        var findAnnotationRequest = new AstTools.FindAnnotationRequest(1L, "RestController");
        assertEquals(1L, findAnnotationRequest.repositoryId());
        assertEquals("RestController", findAnnotationRequest.annotationName());

        var findMethodAnnotationRequest = new AstTools.FindMethodAnnotationRequest(1L, "GetMapping");
        assertEquals(1L, findMethodAnnotationRequest.repositoryId());
        assertEquals("GetMapping", findMethodAnnotationRequest.annotationName());
    }

    @Test
    void toolResponseRecords_haveCorrectStructure() {
        // Assert response record structure
        var successResponse = new AstTools.FindClassResponse(true, "Success", null);
        assertTrue(successResponse.success());
        assertEquals("Success", successResponse.message());
        assertNull(successResponse.result());

        var errorResponse = new AstTools.FindClassResponse(false, "Error", null);
        assertFalse(errorResponse.success());
        assertEquals("Error", errorResponse.message());
        assertNull(errorResponse.result());
    }

    @Test
    void toolRequestRecords_allowEmptyStrings() {
        // Test that request records allow empty strings (validation happens in tool functions)
        var emptyStringRequest = new AstTools.FindClassRequest(1L, "");
        assertEquals(1L, emptyStringRequest.repositoryId());
        assertEquals("", emptyStringRequest.className());
    }

    @Test
    void toolRequestRecords_allowNullValues() {
        // Test that request records allow null values (validation happens in tool functions)
        var nullRequest = new AstTools.FindClassRequest(null, "UserService");
        assertNull(nullRequest.repositoryId());
        assertEquals("UserService", nullRequest.className());
    }
}