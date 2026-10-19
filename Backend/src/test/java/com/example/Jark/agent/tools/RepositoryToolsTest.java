package com.example.Jark.agent.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Test Repository tool structure and schemas without mocking services.
 * Tests focus on tool function structure, request/response validation, and error handling.
 */
class RepositoryToolsTest {

    @Test
    void toolRequestRecords_haveCorrectStructure() {
        // Assert request record structure
        var readFileRequest = new RepositoryTools.ReadFileRequest(1L, "src/main/java/UserService.java");
        assertEquals(1L, readFileRequest.repositoryId());
        assertEquals("src/main/java/UserService.java", readFileRequest.filePath());

        var listFilesRequest = new RepositoryTools.ListFilesRequest(1L, "src/main/java");
        assertEquals(1L, listFilesRequest.repositoryId());
        assertEquals("src/main/java", listFilesRequest.directoryPath());
    }

    @Test
    void toolResponseRecords_haveCorrectStructure() {
        // Assert response record structure
        var successResponse = new RepositoryTools.ReadFileResponse(true, "Success", "content", "path");
        assertTrue(successResponse.success());
        assertEquals("Success", successResponse.message());
        assertEquals("content", successResponse.content());
        assertEquals("path", successResponse.filePath());

        var errorResponse = new RepositoryTools.ReadFileResponse(false, "Error", null, null);
        assertFalse(errorResponse.success());
        assertEquals("Error", errorResponse.message());
        assertNull(errorResponse.content());
        assertNull(errorResponse.filePath());

        var listFilesSuccessResponse = new RepositoryTools.ListFilesResponse(true, "Success", null, "path");
        assertTrue(listFilesSuccessResponse.success());
        assertEquals("Success", listFilesSuccessResponse.message());
        assertNull(listFilesSuccessResponse.files());
        assertEquals("path", listFilesSuccessResponse.directoryPath());

        var listFilesErrorResponse = new RepositoryTools.ListFilesResponse(false, "Error", null, null);
        assertFalse(listFilesErrorResponse.success());
        assertEquals("Error", listFilesErrorResponse.message());
        assertNull(listFilesErrorResponse.files());
        assertNull(listFilesErrorResponse.directoryPath());
    }

    @Test
    void fileInfoRecord_hasCorrectStructure() {
        var fileInfo = new RepositoryTools.FileInfo("src/main/java/UserService.java", "UserService.java", 1024L, "2024-01-01T00:00:00Z");
        assertEquals("src/main/java/UserService.java", fileInfo.relativePath());
        assertEquals("UserService.java", fileInfo.fileName());
        assertEquals(1024L, fileInfo.size());
        assertEquals("2024-01-01T00:00:00Z", fileInfo.lastModified());
    }

    @Test
    void toolRequestRecords_allowEmptyStrings() {
        // Test that request records allow empty strings (validation happens in tool functions)
        var emptyStringRequest = new RepositoryTools.ReadFileRequest(1L, "");
        assertEquals(1L, emptyStringRequest.repositoryId());
        assertEquals("", emptyStringRequest.filePath());
    }

    @Test
    void toolRequestRecords_allowNullValues() {
        // Test that request records allow null values (validation happens in tool functions)
        var nullRequest = new RepositoryTools.ReadFileRequest(null, "src/main/java/UserService.java");
        assertNull(nullRequest.repositoryId());
        assertEquals("src/main/java/UserService.java", nullRequest.filePath());
    }
}