package com.example.VoxCode.evidence.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.example.VoxCode.evidence.model.EvidenceItem;
import com.example.VoxCode.evidence.model.StructuredFinding;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Validates that an agent Finding conforms strictly to the VXC-120 Evidence Model Schema.
 * Guarantees that at minimum: repository, file path, line range, class/method, issue type,
 * severity, evidence references, evidence source, and validation status are properly defined.
 */
@Component
public class FindingSchemaValidator {

    private static final Set<String> VALID_SEVERITIES = Set.of(
            "CRITICAL", "HIGH", "MEDIUM", "LOW", "INFO"
    );

    private static final Set<String> VALID_STATUSES = Set.of(
            "PENDING_VALIDATION", "CONFIRMED", "REJECTED", "INSUFFICIENT_EVIDENCE"
    );

    private static final Set<String> VALID_EVIDENCE_SOURCES = Set.of(
            "AST", "GRAPH", "RAG", "HYBRID", "STATIC_ANALYSIS", "SOURCE_CODE"
    );

    private final ObjectMapper objectMapper;

    public FindingSchemaValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public record ValidationResult(boolean valid, List<String> errors) {
        public static ValidationResult success() {
            return new ValidationResult(true, List.of());
        }

        public static ValidationResult failure(List<String> errors) {
            return new ValidationResult(false, List.copyOf(errors));
        }

        public void throwIfInvalid() {
            if (!valid) {
                throw new SchemaValidationException("Finding schema validation failed: " + String.join("; ", errors));
            }
        }
    }

    public static class SchemaValidationException extends IllegalArgumentException {
        public SchemaValidationException(String message) {
            super(message);
        }
    }

    /**
     * Validates a StructuredFinding object.
     */
    public ValidationResult validate(StructuredFinding finding) {
        List<String> errors = new ArrayList<>();

        if (finding == null) {
            return ValidationResult.failure(List.of("Finding must not be null"));
        }

        // 1. Repository ID
        if (finding.repositoryId() == null || finding.repositoryId() <= 0) {
            errors.add("repositoryId must be a positive non-null Long");
        }

        // 2. File Path
        if (finding.filePath() == null || finding.filePath().trim().isEmpty()) {
            errors.add("filePath must be specified and non-blank");
        }

        // 3. Line Range
        if (finding.lineRange() == null) {
            errors.add("lineRange must be specified");
        } else {
            if (finding.lineRange().startLine() <= 0) {
                errors.add("lineRange startLine must be > 0, got: " + finding.lineRange().startLine());
            }
            if (finding.lineRange().endLine() < finding.lineRange().startLine()) {
                errors.add(
                        "lineRange endLine (" + finding.lineRange().endLine()
                                + ") cannot be less than startLine (" + finding.lineRange().startLine() + ")"
                );
            }
        }

        // 4. Class / Method
        if (finding.targetClass() == null || finding.targetClass().trim().isEmpty()) {
            errors.add("targetClass must be specified and non-blank");
        }

        // 5. Issue Type
        if (finding.issueType() == null || finding.issueType().trim().isEmpty()) {
            errors.add("issueType must be specified and non-blank");
        }

        // 6. Severity
        if (finding.severity() == null || !VALID_SEVERITIES.contains(finding.severity().toUpperCase())) {
            errors.add("severity must be one of " + VALID_SEVERITIES + ", got: " + finding.severity());
        }

        // 7. Evidence Source
        if (finding.evidenceSource() == null || !VALID_EVIDENCE_SOURCES.contains(finding.evidenceSource().toUpperCase())) {
            errors.add("evidenceSource must be one of " + VALID_EVIDENCE_SOURCES + ", got: " + finding.evidenceSource());
        }

        // 8. Validation Status
        if (finding.validationStatus() == null || !VALID_STATUSES.contains(finding.validationStatus().toUpperCase())) {
            errors.add("validationStatus must be one of " + VALID_STATUSES + ", got: " + finding.validationStatus());
        }

        // 9. Evidence References
        List<EvidenceItem> evidenceRefs = finding.evidenceReferences();
        if (evidenceRefs == null || evidenceRefs.isEmpty()) {
            // If the status is not REJECTED or INSUFFICIENT_EVIDENCE, evidence references are required
            if (!"REJECTED".equalsIgnoreCase(finding.validationStatus())
                    && !"INSUFFICIENT_EVIDENCE".equalsIgnoreCase(finding.validationStatus())) {
                errors.add("evidenceReferences must contain at least one verifiable evidence item for non-rejected findings");
            }
        } else {
            for (int i = 0; i < evidenceRefs.size(); i++) {
                EvidenceItem item = evidenceRefs.get(i);
                if (item.evidenceType() == null || item.evidenceType().isBlank()) {
                    errors.add("evidenceReferences[" + i + "].evidenceType must not be blank");
                }
                if (item.content() == null || item.content().isBlank()) {
                    errors.add("evidenceReferences[" + i + "].content must not be blank");
                }
            }
        }

        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }

    /**
     * Validates a raw JSON string against the finding schema.
     */
    public ValidationResult validateJson(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            List<String> errors = new ArrayList<>();

            // Verify minimum required fields in JSON representation
            String[] requiredFields = {
                    "repositoryId", "filePath", "lineRange", "targetClass",
                    "issueType", "severity", "evidenceReferences", "evidenceSource", "validationStatus"
            };

            for (String field : requiredFields) {
                if (!root.has(field) || root.get(field).isNull()) {
                    errors.add("Missing required field: " + field);
                }
            }

            if (!errors.isEmpty()) {
                return ValidationResult.failure(errors);
            }

            StructuredFinding finding = objectMapper.treeToValue(root, StructuredFinding.class);
            return validate(finding);

        } catch (JsonProcessingException e) {
            return ValidationResult.failure(List.of("Malformed JSON: " + e.getMessage()));
        }
    }

    /**
     * Serializes a valid StructuredFinding into canonical formatted JSON.
     */
    public String toJson(StructuredFinding finding) {
        validate(finding).throwIfInvalid();
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(finding);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize StructuredFinding", e);
        }
    }

    /**
     * Deserializes and validates a StructuredFinding from JSON.
     */
    public StructuredFinding fromJson(String json) {
        validateJson(json).throwIfInvalid();
        try {
            return objectMapper.readValue(json, StructuredFinding.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize StructuredFinding", e);
        }
    }
}
