package com.example.Jark.agent.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Test Graph tool structure and schemas without mocking services.
 * Tests focus on tool function structure, request/response validation, and error handling.
 */
class GraphToolsTest {

    @Test
    void toolRequestRecords_haveCorrectStructure() {
        // Assert request record structure
        var findDependenciesRequest = new GraphTools.FindDependenciesRequest(1L, "com.example.UserService");
        assertEquals(1L, findDependenciesRequest.repositoryId());
        assertEquals("com.example.UserService", findDependenciesRequest.classFqn());

        var findDependentsRequest = new GraphTools.FindDependentsRequest(1L, "com.example.UserService");
        assertEquals(1L, findDependentsRequest.repositoryId());
        assertEquals("com.example.UserService", findDependentsRequest.classFqn());

        var findCallersRequest = new GraphTools.FindCallersRequest(1L, "com.example.UserService.findById");
        assertEquals(1L, findCallersRequest.repositoryId());
        assertEquals("com.example.UserService.findById", findCallersRequest.methodId());

        var findCalleesRequest = new GraphTools.FindCalleesRequest(1L, "com.example.UserService.findById");
        assertEquals(1L, findCalleesRequest.repositoryId());
        assertEquals("com.example.UserService.findById", findCalleesRequest.methodId());
    }

    @Test
    void toolResponseRecords_haveCorrectStructure() {
        // Assert response record structure
        var successResponse = new GraphTools.FindDependenciesResponse(true, "Success", null);
        assertTrue(successResponse.success());
        assertEquals("Success", successResponse.message());
        assertNull(successResponse.result());

        var errorResponse = new GraphTools.FindDependenciesResponse(false, "Error", null);
        assertFalse(errorResponse.success());
        assertEquals("Error", errorResponse.message());
        assertNull(errorResponse.result());
    }

    @Test
    void toolRequestRecords_allowEmptyStrings() {
        // Test that request records allow empty strings (validation happens in tool functions)
        var emptyStringRequest = new GraphTools.FindDependenciesRequest(1L, "");
        assertEquals(1L, emptyStringRequest.repositoryId());
        assertEquals("", emptyStringRequest.classFqn());
    }

    @Test
    void toolRequestRecords_allowNullValues() {
        // Test that request records allow null values (validation happens in tool functions)
        var nullRequest = new GraphTools.FindDependenciesRequest(null, "com.example.UserService");
        assertNull(nullRequest.repositoryId());
        assertEquals("com.example.UserService", nullRequest.classFqn());
    }
}