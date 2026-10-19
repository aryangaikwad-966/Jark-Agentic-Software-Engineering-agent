package com.example.Jark.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Structured DTO representing the comprehensive Engineering Report (VXC-210).
 * Covers both the Investigation trace and the Remediation & Verification lifecycle trace.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EngineeringReportDTO {

    private String reportId;
    private Long investigationId;
    private String repositoryName;
    private String repositoryUrl;
    private String generatedAt;
    private String overallStatus;
    private String executiveSummary;
    private InvestigationTraceDTO investigationTrace;
    private RemediationTraceDTO remediationTrace;
    private String markdownReport;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvestigationTraceDTO {
        private Long investigationId;
        private String title;
        private String description;
        private String status;
        private String startedAt;
        private String completedAt;
        private int stepCount;
        @Builder.Default
        private List<AgentStepDTO> steps = new ArrayList<>();
        @Builder.Default
        private List<FindingDTO> findings = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgentStepDTO {
        private int stepNumber;
        private String stepType;
        private String stepDescription;
        private String observation;
        private String timestamp;
        private Map<String, Object> metadata;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FindingDTO {
        private Long findingId;
        private String issueType;
        private String severity;
        private String filePath;
        private String lineRange;
        private String description;
        private String explanation;
        private Double confidence;
        private String validationStatus;
        @Builder.Default
        private List<EvidenceDTO> evidence = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EvidenceDTO {
        private Long evidenceId;
        private String source;
        private String type;
        private String filePath;
        private String lineRange;
        private String codeSnippet;
        private String description;
        private Double confidence;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RemediationTraceDTO {
        @Builder.Default
        private List<PlanLifecycleDTO> plans = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlanLifecycleDTO {
        private Long planId;
        private String title;
        private String description;
        private String rootCause;
        private String affectedFiles;
        private String risk;
        private String status;
        private ApprovalDTO approval;
        private RemediationDTO remediation;
        private ExecutionDTO execution;
        private VerificationDTO verification;
        private RollbackSummaryDTO rollback;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApprovalDTO {
        private Long approvalId;
        private String decision;
        private String reviewerUsername;
        private String comments;
        private String decidedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RemediationDTO {
        private Long remediationId;
        private String strategy;
        private String status;
        private String diff;
        private String modifiedFiles;
        private Boolean withinScope;
        private String errorMessage;
        private String rollbackInfo;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExecutionDTO {
        private Long executionId;
        private String containerId;
        private String status;
        private String executionLog;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VerificationDTO {
        private Long verificationId;
        private String status;
        private boolean allGatesPassed;
        private Boolean intendedFilesModified;
        private Boolean unauthorizedFilesDetected;
        private Boolean diffMatchesPlan;
        private String buildStatus;
        private String testStatus;
        private String staticAnalysisStatus;
        private Boolean unexpectedModifications;
        private Integer repairAttemptCount;
        private String verificationLog;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RollbackSummaryDTO {
        private boolean isRolledBack;
        private String triggerReason;
        private boolean restorationVerified;
        private String details;
    }
}
