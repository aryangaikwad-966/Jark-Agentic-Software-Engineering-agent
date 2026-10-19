package com.example.Jark.evidence;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.example.Jark.evidence.model.EvidenceItem;
import com.example.Jark.evidence.model.LineRange;
import com.example.Jark.evidence.model.StructuredFinding;

/**
 * Test EvidenceEngine schema generation and evidence creation.
 */
class EvidenceEngineTest {

    @Test
    void structuredFinding_record_hasRequiredFields() {
        // Arrange
        StructuredFinding finding = StructuredFinding.builder()
                .repositoryId(1L)
                .filePath("src/main/java/com/example/UserController.java")
                .lineRange(10, 50)
                .targetClass("UserController")
                .targetMethod("deleteUser")
                .issueType("SECURITY")
                .severity("HIGH")
                .title("Missing @PreAuthorize on deleteUser")
                .description("The deleteUser endpoint lacks authorization")
                .evidenceSource("AST")
                .validationStatus("PENDING_VALIDATION")
                .build();

        // Assert
        assertEquals(1L, finding.repositoryId());
        assertEquals("src/main/java/com/example/UserController.java", finding.filePath());
        assertEquals(LineRange.of(10, 50), finding.lineRange());
        assertEquals("UserController", finding.targetClass());
        assertEquals("deleteUser", finding.targetMethod());
        assertEquals("SECURITY", finding.issueType());
        assertEquals("HIGH", finding.severity());
        assertEquals("Missing @PreAuthorize on deleteUser", finding.title());
        assertEquals("The deleteUser endpoint lacks authorization", finding.description());
        assertEquals("AST", finding.evidenceSource());
        assertEquals("PENDING_VALIDATION", finding.validationStatus());
    }

    @Test
    void structuredFinding_defaultValues() {
        // Arrange
        StructuredFinding finding = StructuredFinding.builder()
                .repositoryId(1L)
                .filePath("src/main/java/Test.java")
                .targetClass("TestClass")
                .issueType("VALIDATION")
                .build();

        // Assert - should have defaults
        assertEquals("MEDIUM", finding.severity());
        assertEquals("PENDING_VALIDATION", finding.validationStatus());
        assertNotNull(finding.evidenceReferences());
        assertTrue(finding.evidenceReferences().isEmpty());
    }

    @Test
    void evidenceItem_astClassEvidence_hasRequiredFields() {
        // Act
        EvidenceItem evidence = EvidenceItem.astNode("AST", "Class definition found", "CLASS", 
                LineRange.of(10, 50), Map.of("className", "UserController"));

        // Assert
        assertEquals("AST", evidence.source());
        assertEquals("Class definition found", evidence.content());
        assertEquals("AST_NODE", evidence.evidenceType());
        assertEquals(1.0, evidence.confidenceScore());
        assertNotNull(evidence.metadata());
        assertTrue(evidence.metadata().containsKey("nodeType"));
        assertEquals("CLASS", evidence.metadata().get("nodeType"));
    }

    @Test
    void evidenceItem_ragChunkEvidence_hasRequiredFields() {
        // Act
        EvidenceItem evidence = EvidenceItem.ragChunk("RAG", "This is a code chunk about user authentication", 0.85, 
                Map.of("filePath", "src/main/java/AuthService.java"));

        // Assert
        assertEquals("RAG", evidence.source());
        assertEquals("This is a code chunk about user authentication", evidence.content());
        assertEquals(0.85, evidence.confidenceScore());
        assertEquals("RAG_CHUNK", evidence.evidenceType());
        assertNotNull(evidence.metadata());
        assertTrue(evidence.metadata().containsKey("similarityScore"));
    }

    @Test
    void evidenceItem_graphEdgeEvidence_hasRequiredFields() {
        // Act
        EvidenceItem evidence = EvidenceItem.graphEdge("GRAPH", "User -> Order dependency", 
                "DEPENDS_ON", "UserService", "OrderService");

        // Assert
        assertEquals("GRAPH", evidence.source());
        assertEquals("User -> Order dependency", evidence.content());
        assertEquals("GRAPH_EDGE", evidence.evidenceType());
        assertNotNull(evidence.metadata());
        assertTrue(evidence.metadata().containsKey("edgeType"));
        assertEquals("DEPENDS_ON", evidence.metadata().get("edgeType"));
    }

    @Test
    void structuredFinding_withEvidenceReferences() {
        // Arrange
        EvidenceItem evidence1 = EvidenceItem.astNode("AST", "Class found", "CLASS", 
                LineRange.of(10, 50), Map.of());
        EvidenceItem evidence2 = EvidenceItem.ragChunk("RAG", "Semantic context", 0.9, Map.of());

        StructuredFinding finding = StructuredFinding.builder()
                .repositoryId(1L)
                .filePath("src/main/java/Test.java")
                .targetClass("TestClass")
                .issueType("SECURITY")
                .evidenceReferences(List.of(evidence1, evidence2))
                .build();

        // Assert
        assertNotNull(finding.evidenceReferences());
        assertEquals(2, finding.evidenceReferences().size());
        assertEquals("AST", finding.evidenceReferences().get(0).source());
        assertEquals("RAG", finding.evidenceReferences().get(1).source());
    }

    @Test
    void lineRange_record_validation() {
        // Arrange
        LineRange lineRange = LineRange.of(10, 50);

        // Assert
        assertEquals(10, lineRange.startLine());
        assertEquals(50, lineRange.endLine());
        assertEquals("10-50", lineRange.toString());
    }

    @Test
    void lineRange_invalidRange_throwsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            LineRange.of(50, 10); // start > end
        });
    }

    @Test
    void structuredFinding_jsonSerialization_roundTrip() {
        // This test would require Jackson serialization/deserialization
        // For now, we verify the record structure is correct
        StructuredFinding finding = StructuredFinding.builder()
                .repositoryId(1L)
                .filePath("src/main/java/Test.java")
                .targetClass("TestClass")
                .issueType("SECURITY")
                .build();

        // Assert structure is correct
        assertNotNull(finding);
        assertEquals(1L, finding.repositoryId());
        assertEquals("src/main/java/Test.java", finding.filePath());
        assertEquals("TestClass", finding.targetClass());
        assertEquals("SECURITY", finding.issueType());
    }
}
