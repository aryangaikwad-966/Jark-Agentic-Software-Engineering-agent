package com.example.Jark.evidence.model;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Strict structured schema representation of an agent Finding.
 * Guarantees that every finding links to deterministic repository artifacts:
 * repositoryId, filePath, lineRange, targetClass/method, issueType, severity,
 * evidenceReferences, evidenceSource, and validationStatus.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record StructuredFinding(
        @JsonProperty("repositoryId") Long repositoryId,
        @JsonProperty("filePath") String filePath,
        @JsonProperty("lineRange") LineRange lineRange,
        @JsonProperty("targetClass") String targetClass,
        @JsonProperty("targetMethod") String targetMethod,
        @JsonProperty("issueType") String issueType,
        @JsonProperty("severity") String severity,
        @JsonProperty("title") String title,
        @JsonProperty("description") String description,
        @JsonProperty("evidenceReferences") List<EvidenceItem> evidenceReferences,
        @JsonProperty("evidenceSource") String evidenceSource,
        @JsonProperty("validationStatus") String validationStatus
) {

    @JsonCreator
    public StructuredFinding {
        if (evidenceReferences == null) {
            evidenceReferences = Collections.emptyList();
        } else {
            evidenceReferences = Collections.unmodifiableList(evidenceReferences);
        }
        if (severity == null || severity.isBlank()) {
            severity = "MEDIUM";
        }
        if (validationStatus == null || validationStatus.isBlank()) {
            validationStatus = "PENDING_VALIDATION";
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long repositoryId;
        private String filePath;
        private LineRange lineRange;
        private String targetClass;
        private String targetMethod;
        private String issueType;
        private String severity = "MEDIUM";
        private String title;
        private String description;
        private List<EvidenceItem> evidenceReferences = Collections.emptyList();
        private String evidenceSource;
        private String validationStatus = "PENDING_VALIDATION";

        public Builder repositoryId(Long repositoryId) {
            this.repositoryId = repositoryId;
            return this;
        }

        public Builder filePath(String filePath) {
            this.filePath = filePath;
            return this;
        }

        public Builder lineRange(LineRange lineRange) {
            this.lineRange = lineRange;
            return this;
        }

        public Builder lineRange(int startLine, int endLine) {
            this.lineRange = LineRange.of(startLine, endLine);
            return this;
        }

        public Builder targetClass(String targetClass) {
            this.targetClass = targetClass;
            return this;
        }

        public Builder targetMethod(String targetMethod) {
            this.targetMethod = targetMethod;
            return this;
        }

        public Builder issueType(String issueType) {
            this.issueType = issueType;
            return this;
        }

        public Builder severity(String severity) {
            this.severity = severity;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder evidenceReferences(List<EvidenceItem> evidenceReferences) {
            this.evidenceReferences = evidenceReferences;
            return this;
        }

        public Builder evidenceSource(String evidenceSource) {
            this.evidenceSource = evidenceSource;
            return this;
        }

        public Builder validationStatus(String validationStatus) {
            this.validationStatus = validationStatus;
            return this;
        }

        public StructuredFinding build() {
            return new StructuredFinding(
                    repositoryId,
                    filePath,
                    lineRange,
                    targetClass,
                    targetMethod,
                    issueType,
                    severity,
                    title,
                    description,
                    evidenceReferences,
                    evidenceSource,
                    validationStatus
            );
        }
    }
}
