package com.example.Jark.evidence;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.Jark.evidence.model.EvidenceItem;
import com.example.Jark.evidence.model.LineRange;
import com.example.Jark.evidence.model.StructuredFinding;
import com.example.Jark.evidence.service.FindingSchemaValidator;
import com.example.Jark.evidence.service.FindingSchemaValidator.ValidationResult;
import com.fasterxml.jackson.databind.ObjectMapper;

class FindingSchemaValidatorTest {

    private FindingSchemaValidator validator;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        validator = new FindingSchemaValidator(objectMapper);
    }

    private StructuredFinding createSampleValidFinding() {
        EvidenceItem astEvidence = EvidenceItem.astNode(
                "AstTools.findMethod",
                "@GetMapping(\"/users\") public List<User> getUsers()",
                "METHOD",
                LineRange.of(45, 60),
                Map.of("annotations", List.of("@GetMapping"))
        );

        return StructuredFinding.builder()
                .repositoryId(1L)
                .filePath("src/main/java/com/example/controller/UserController.java")
                .lineRange(LineRange.of(45, 60))
                .targetClass("com.example.controller.UserController")
                .targetMethod("getUsers")
                .issueType("SECURITY_MISSING_AUTHORIZATION")
                .severity("HIGH")
                .title("Missing @PreAuthorize on sensitive endpoint")
                .description("The getUsers endpoint exposes user PII without access control")
                .evidenceReferences(List.of(astEvidence))
                .evidenceSource("AST")
                .validationStatus("PENDING_VALIDATION")
                .build();
    }

    @Test
    void validate_validFinding_returnsSuccess() {
        StructuredFinding finding = createSampleValidFinding();
        ValidationResult result = validator.validate(finding);

        assertTrue(result.valid());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void validate_missingRepositoryId_fails() {
        StructuredFinding finding = StructuredFinding.builder()
                .filePath("src/Test.java")
                .lineRange(LineRange.of(1, 10))
                .targetClass("Test")
                .issueType("BUG")
                .severity("MEDIUM")
                .evidenceSource("AST")
                .evidenceReferences(List.of(new EvidenceItem("AST_NODE", "source", "content", Map.of(), 1.0)))
                .build();

        ValidationResult result = validator.validate(finding);
        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("repositoryId")));
    }

    @Test
    void validate_blankFilePath_fails() {
        StructuredFinding finding = StructuredFinding.builder()
                .repositoryId(1L)
                .filePath("   ")
                .lineRange(LineRange.of(1, 10))
                .targetClass("Test")
                .issueType("BUG")
                .severity("MEDIUM")
                .evidenceSource("AST")
                .evidenceReferences(List.of(new EvidenceItem("AST_NODE", "source", "content", Map.of(), 1.0)))
                .build();

        ValidationResult result = validator.validate(finding);
        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("filePath")));
    }

    @Test
    void validate_invalidSeverity_fails() {
        StructuredFinding finding = StructuredFinding.builder()
                .repositoryId(1L)
                .filePath("src/Test.java")
                .lineRange(LineRange.of(1, 10))
                .targetClass("Test")
                .issueType("BUG")
                .severity("SUPER_CRITICAL")
                .evidenceSource("AST")
                .evidenceReferences(List.of(new EvidenceItem("AST_NODE", "source", "content", Map.of(), 1.0)))
                .build();

        ValidationResult result = validator.validate(finding);
        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("severity")));
    }

    @Test
    void validate_emptyEvidenceReferences_failsForNonRejected() {
        StructuredFinding finding = StructuredFinding.builder()
                .repositoryId(1L)
                .filePath("src/Test.java")
                .lineRange(LineRange.of(1, 10))
                .targetClass("Test")
                .issueType("BUG")
                .severity("HIGH")
                .evidenceSource("AST")
                .validationStatus("PENDING_VALIDATION")
                .evidenceReferences(List.of())
                .build();

        ValidationResult result = validator.validate(finding);
        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("evidenceReferences")));
    }

    @Test
    void validate_emptyEvidenceAllowed_forRejectedStatus() {
        StructuredFinding finding = StructuredFinding.builder()
                .repositoryId(1L)
                .filePath("src/Test.java")
                .lineRange(LineRange.of(1, 10))
                .targetClass("Test")
                .issueType("BUG")
                .severity("LOW")
                .evidenceSource("AST")
                .validationStatus("REJECTED")
                .evidenceReferences(List.of())
                .build();

        ValidationResult result = validator.validate(finding);
        assertTrue(result.valid());
    }

    @Test
    void toJson_and_fromJson_roundTrip() {
        StructuredFinding original = createSampleValidFinding();
        String json = validator.toJson(original);

        assertNotNull(json);
        assertTrue(json.contains("\"repositoryId\" : 1"));
        assertTrue(json.contains("\"SECURITY_MISSING_AUTHORIZATION\""));

        StructuredFinding roundTrip = validator.fromJson(json);
        assertEquals(original.repositoryId(), roundTrip.repositoryId());
        assertEquals(original.filePath(), roundTrip.filePath());
        assertEquals(original.lineRange(), roundTrip.lineRange());
        assertEquals(original.targetClass(), roundTrip.targetClass());
        assertEquals(original.targetMethod(), roundTrip.targetMethod());
        assertEquals(original.issueType(), roundTrip.issueType());
        assertEquals(original.severity(), roundTrip.severity());
        assertEquals(original.evidenceReferences().size(), roundTrip.evidenceReferences().size());
    }

    @Test
    void validateJson_missingRequiredFields_fails() {
        String invalidJson = """
                {
                    "repositoryId": 1,
                    "targetClass": "UserController"
                }
                """;

        ValidationResult result = validator.validateJson(invalidJson);
        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("filePath")));
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("lineRange")));
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("issueType")));
    }

    @Test
    void lineRange_validationAndHelpers() {
        LineRange range = LineRange.of(10, 25);
        assertEquals(10, range.startLine());
        assertEquals(25, range.endLine());
        assertEquals(16, range.lineCount());
        assertTrue(range.contains(10));
        assertTrue(range.contains(20));
        assertTrue(range.contains(25));
        assertFalse(range.contains(9));
        assertFalse(range.contains(26));
        assertEquals("10-25", range.toString());

        LineRange single = LineRange.singleLine(42);
        assertEquals(1, single.lineCount());
        assertEquals("42", single.toString());

        assertThrows(IllegalArgumentException.class, () -> LineRange.of(0, 5));
        assertThrows(IllegalArgumentException.class, () -> LineRange.of(20, 10));
    }
}
