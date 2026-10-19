package com.example.Jark.service;

import com.example.Jark.dto.report.EngineeringReportDTO;
import com.example.Jark.dto.report.EngineeringReportDTO.*;
import com.example.Jark.entity.*;
import com.example.Jark.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Service for generating comprehensive Engineering Reports (VXC-210).
 *
 * <p>Aggregates the complete lifecycle covering BOTH traces:
 * <ol>
 *   <li><b>Investigation Trace:</b> User request → agent hypotheses & tool executions → findings & deterministic evidence.</li>
 *   <li><b>Remediation Trace:</b> Approved plan → transformation applied → sandbox execution → 7-gate verification → repair/rollback.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportGenerationService {

    private final InvestigationRepository investigationRepository;
    private final AgentTraceRepository agentTraceRepository;
    private final FindingRepository findingRepository;
    private final EvidenceRepository evidenceRepository;
    private final PlanRepository planRepository;
    private final ApprovalRepository approvalRepository;
    private final RemediationRepository remediationRepository;
    private final ExecutionRepository executionRepository;
    private final VerificationResultRepository verificationResultRepository;
    private final ObjectMapper objectMapper;

    /**
     * Generates a complete Engineering Report for an investigation in structured DTO and Markdown formats.
     *
     * @param investigationId the ID of the investigation
     * @return the complete {@link EngineeringReportDTO}
     */
    @Transactional(readOnly = true)
    public EngineeringReportDTO generateReport(Long investigationId) {
        log.info("Generating Engineering Report for investigation ID {}", investigationId);

        Investigation investigation = investigationRepository.findById(investigationId)
                .orElseThrow(() -> new IllegalArgumentException("Investigation not found: " + investigationId));

        // 1. Build Trace 1: Investigation Trace
        InvestigationTraceDTO investigationTrace = buildInvestigationTrace(investigation);

        // 2. Build Trace 2: Remediation & Verification Trace
        RemediationTraceDTO remediationTrace = buildRemediationTrace(investigationId);

        // 3. Determine Overall Lifecycle Status
        String overallStatus = determineOverallStatus(investigation, investigationTrace, remediationTrace);

        // 4. Generate Executive Summary
        String executiveSummary = generateExecutiveSummary(investigation, investigationTrace, remediationTrace, overallStatus);

        // 5. Generate Markdown Report
        String repoName = investigation.getRepository() != null ? investigation.getRepository().getName() : "Unknown Repo";
        String repoUrl = investigation.getRepository() != null ? investigation.getRepository().getUrl() : "";
        String reportId = "RPT-" + investigationId + "-" + System.currentTimeMillis();
        String generatedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        String markdown = formatMarkdown(reportId, investigation, repoName, repoUrl, generatedAt,
                overallStatus, executiveSummary, investigationTrace, remediationTrace);

        return EngineeringReportDTO.builder()
                .reportId(reportId)
                .investigationId(investigationId)
                .repositoryName(repoName)
                .repositoryUrl(repoUrl)
                .generatedAt(generatedAt)
                .overallStatus(overallStatus)
                .executiveSummary(executiveSummary)
                .investigationTrace(investigationTrace)
                .remediationTrace(remediationTrace)
                .markdownReport(markdown)
                .build();
    }

    // ────────────────────────────────────────────────────────────────────────────
    // 1. Investigation Trace Builder
    // ────────────────────────────────────────────────────────────────────────────

    private InvestigationTraceDTO buildInvestigationTrace(Investigation investigation) {
        Long invId = investigation.getId();

        // Fetch agent steps
        List<AgentTrace> traces = agentTraceRepository.findByInvestigationIdOrderByTimestampAsc(invId);
        List<AgentStepDTO> stepDTOs = new ArrayList<>();
        int stepNum = 1;
        for (AgentTrace trace : traces) {
            stepDTOs.add(AgentStepDTO.builder()
                    .stepNumber(stepNum++)
                    .stepType(trace.getStepType())
                    .stepDescription(trace.getStepDescription())
                    .observation(trace.getObservation())
                    .timestamp(trace.getTimestamp() != null ? trace.getTimestamp().toString() : null)
                    .metadata(trace.getMetadata())
                    .build());
        }

        // Fetch findings and their supporting evidence
        List<Finding> findings = findingRepository.findByInvestigationId(invId);
        List<FindingDTO> findingDTOs = new ArrayList<>();
        for (Finding finding : findings) {
            List<Evidence> evidenceList = evidenceRepository.findByFindingId(finding.getId());
            List<EvidenceDTO> evidenceDTOs = new ArrayList<>();
            for (Evidence ev : evidenceList) {
                evidenceDTOs.add(EvidenceDTO.builder()
                        .evidenceId(ev.getId())
                        .source(ev.getEvidenceSource())
                        .type(ev.getEvidenceType())
                        .filePath(ev.getFilePath())
                        .lineRange(ev.getLineRange())
                        .codeSnippet(ev.getContent())
                        .description(ev.getContent())
                        .confidence(ev.getConfidence())
                        .build());
            }

            findingDTOs.add(FindingDTO.builder()
                    .findingId(finding.getId())
                    .issueType(finding.getIssueType())
                    .severity(finding.getSeverity())
                    .filePath(finding.getFilePath())
                    .lineRange(finding.getLineRange())
                    .description(finding.getDescription())
                    .explanation(finding.getExplanation())
                    .validationStatus(finding.getValidationStatus())
                    .evidence(evidenceDTOs)
                    .build());
        }

        return InvestigationTraceDTO.builder()
                .investigationId(invId)
                .title(investigation.getTitle())
                .description(investigation.getDescription())
                .status(investigation.getStatus())
                .startedAt(investigation.getStartedAt() != null ? investigation.getStartedAt().toString() : null)
                .completedAt(investigation.getCompletedAt() != null ? investigation.getCompletedAt().toString() : null)
                .stepCount(stepDTOs.size())
                .steps(stepDTOs)
                .findings(findingDTOs)
                .build();
    }

    // ────────────────────────────────────────────────────────────────────────────
    // 2. Remediation Trace Builder
    // ────────────────────────────────────────────────────────────────────────────

    private RemediationTraceDTO buildRemediationTrace(Long investigationId) {
        List<Plan> plans = planRepository.findByInvestigationId(investigationId);
        if (plans.isEmpty()) {
            plans = planRepository.findByInvestigationViaFinding(investigationId);
        }

        List<PlanLifecycleDTO> planLifecycles = new ArrayList<>();

        for (Plan plan : plans) {
            Long planId = plan.getId();

            // 1. Approval
            List<Approval> approvals = approvalRepository.findByPlanId(planId);
            ApprovalDTO approvalDTO = null;
            if (!approvals.isEmpty()) {
                Approval app = approvals.get(0);
                approvalDTO = ApprovalDTO.builder()
                        .approvalId(app.getId())
                        .decision(app.getDecision())
                        .reviewerUsername(app.getUser() != null ? app.getUser().getUsername() : "User")
                        .comments(app.getComments())
                        .decidedAt(app.getApprovedAt() != null ? app.getApprovedAt().toString() : null)
                        .build();
            }

            // 2. Remediation
            List<Remediation> remediations = remediationRepository.findByPlanId(planId);
            RemediationDTO remediationDTO = null;
            RollbackSummaryDTO rollbackDTO = null;

            if (!remediations.isEmpty()) {
                Remediation rem = remediations.get(0);
                remediationDTO = RemediationDTO.builder()
                        .remediationId(rem.getId())
                        .strategy(rem.getTransformationStrategy())
                        .status(rem.getStatus())
                        .diff(rem.getDiff())
                        .modifiedFiles(rem.getModifiedFiles())
                        .withinScope(rem.getWithinScope())
                        .errorMessage(rem.getErrorMessage())
                        .rollbackInfo(rem.getRollbackInfo())
                        .build();

                // Rollback info if rolled back
                if ("ROLLED_BACK".equals(rem.getStatus())) {
                    String reason = "Triggered by safety gate";
                    boolean verified = true;
                    if (rem.getRollbackInfo() != null) {
                        try {
                            Map<String, Object> meta = objectMapper.readValue(
                                    rem.getRollbackInfo(), new TypeReference<Map<String, Object>>() {});
                            if (meta.containsKey("triggerReason")) {
                                reason = String.valueOf(meta.get("triggerReason"));
                            }
                            if (meta.containsKey("restorationVerified")) {
                                verified = Boolean.parseBoolean(String.valueOf(meta.get("restorationVerified")));
                            }
                        } catch (Exception ignored) {
                            reason = rem.getRollbackInfo();
                        }
                    }
                    rollbackDTO = RollbackSummaryDTO.builder()
                            .isRolledBack(true)
                            .triggerReason(reason)
                            .restorationVerified(verified)
                            .details(rem.getRollbackInfo())
                            .build();
                }
            }

            // 3. Execution & Verification
            List<Execution> executions = executionRepository.findByPlanId(planId);
            ExecutionDTO executionDTO = null;
            VerificationDTO verificationDTO = null;

            if (!executions.isEmpty()) {
                Execution exec = executions.get(0);
                executionDTO = ExecutionDTO.builder()
                        .executionId(exec.getId())
                        .containerId(exec.getDockerContainerId())
                        .status(exec.getStatus())
                        .executionLog(exec.getExecutionLog())
                        .build();

                Optional<VerificationResult> vrOpt = verificationResultRepository.findByExecutionId(exec.getId());
                if (vrOpt.isPresent()) {
                    VerificationResult vr = vrOpt.get();
                    verificationDTO = VerificationDTO.builder()
                            .verificationId(vr.getId())
                            .status(vr.getStatus())
                            .allGatesPassed(vr.allGatesPassed())
                            .intendedFilesModified(vr.getIntendedFilesModified())
                            .unauthorizedFilesDetected(vr.getUnauthorizedFilesDetected())
                            .diffMatchesPlan(vr.getDiffMatchesPlan())
                            .buildStatus(vr.getBuildStatus())
                            .testStatus(vr.getTestStatus())
                            .staticAnalysisStatus(vr.getStaticAnalysisStatus())
                            .unexpectedModifications(vr.getUnexpectedModifications())
                            .repairAttemptCount(vr.getRepairAttemptCount() != null ? vr.getRepairAttemptCount() : 0)
                            .verificationLog(vr.getVerificationLog())
                            .build();
                }
            }

            planLifecycles.add(PlanLifecycleDTO.builder()
                    .planId(planId)
                    .title(plan.getTitle())
                    .description(plan.getDescription())
                    .rootCause(plan.getRootCause())
                    .affectedFiles(plan.getAffectedFiles())
                    .risk(plan.getRisk())
                    .status(plan.getStatus())
                    .approval(approvalDTO)
                    .remediation(remediationDTO)
                    .execution(executionDTO)
                    .verification(verificationDTO)
                    .rollback(rollbackDTO)
                    .build());
        }

        return RemediationTraceDTO.builder()
                .plans(planLifecycles)
                .build();
    }

    // ────────────────────────────────────────────────────────────────────────────
    // 3. Overall Lifecycle Status & Summary
    // ────────────────────────────────────────────────────────────────────────────

    private String determineOverallStatus(
            Investigation investigation,
            InvestigationTraceDTO investigationTrace,
            RemediationTraceDTO remediationTrace) {

        for (PlanLifecycleDTO plan : remediationTrace.getPlans()) {
            if (plan.getRollback() != null && plan.getRollback().isRolledBack()) {
                return "ROLLED_BACK";
            }
            if (plan.getVerification() != null) {
                if (plan.getVerification().isAllGatesPassed() || "PASSED".equals(plan.getVerification().getStatus())) {
                    return "VERIFIED";
                }
                if ("FAILED".equals(plan.getVerification().getStatus())) {
                    return "VERIFICATION_FAILED";
                }
            }
            if (plan.getRemediation() != null) {
                if ("COMPLETED".equals(plan.getRemediation().getStatus())) {
                    return "REMEDIATED";
                }
                if ("FAILED".equals(plan.getRemediation().getStatus())) {
                    return "REMEDIATION_FAILED";
                }
            }
            if (plan.getApproval() != null) {
                if ("REJECTED".equals(plan.getApproval().getDecision())) {
                    return "APPROVAL_REJECTED";
                }
                if ("APPROVED".equals(plan.getApproval().getDecision())) {
                    return "PLAN_APPROVED";
                }
            }
        }

        if (!investigationTrace.getFindings().isEmpty()) {
            return "FINDINGS_CONFIRMED";
        }

        return investigation.getStatus() != null ? investigation.getStatus() : "IN_PROGRESS";
    }

    private String generateExecutiveSummary(
            Investigation investigation,
            InvestigationTraceDTO investigationTrace,
            RemediationTraceDTO remediationTrace,
            String overallStatus) {

        int findingsCount = investigationTrace.getFindings().size();
        int stepsCount = investigationTrace.getSteps().size();
        int plansCount = remediationTrace.getPlans().size();

        return switch (overallStatus) {
            case "VERIFIED" -> String.format(
                    "Investigation '%s' concluded successfully with %d finding(s) discovered across %d agent steps. " +
                    "Engineering plan remediation was applied, sandbox-tested, and verified through all 7 validation gates.",
                    investigation.getTitle(), findingsCount, stepsCount);
            case "ROLLED_BACK" -> String.format(
                    "Investigation '%s' identified %d finding(s). Remediation was attempted but triggered automated safety rollback. " +
                    "The workspace has been completely restored to its pre-remediation state.",
                    investigation.getTitle(), findingsCount);
            case "VERIFICATION_FAILED" -> String.format(
                    "Investigation '%s' identified %d finding(s). Remediation applied but failed automated sandbox verification gates.",
                    investigation.getTitle(), findingsCount);
            case "FINDINGS_CONFIRMED" -> String.format(
                    "Investigation '%s' completed with %d confirmed finding(s). %d remediation plan(s) prepared pending user approval.",
                    investigation.getTitle(), findingsCount, plansCount);
            default -> String.format(
                    "Investigation '%s' in status '%s'. %d finding(s) discovered across %d agent execution step(s).",
                    investigation.getTitle(), overallStatus, findingsCount, stepsCount);
        };
    }

    // ────────────────────────────────────────────────────────────────────────────
    // 4. Markdown Formatting
    // ────────────────────────────────────────────────────────────────────────────

    private String formatMarkdown(
            String reportId,
            Investigation inv,
            String repoName,
            String repoUrl,
            String generatedAt,
            String overallStatus,
            String executiveSummary,
            InvestigationTraceDTO invTrace,
            RemediationTraceDTO remTrace) {

        StringBuilder sb = new StringBuilder();

        sb.append("# 📋 Jark Engineering Lifecycle Report\n\n");
        sb.append("**Report ID:** `").append(reportId).append("`  \n");
        sb.append("**Repository:** ").append(repoName).append(repoUrl != null && !repoUrl.isBlank() ? " (" + repoUrl + ")" : "").append("  \n");
        sb.append("**Generated At:** ").append(generatedAt).append("  \n");
        sb.append("**Lifecycle Status:** `").append(overallStatus).append("`\n\n");

        sb.append("## 📝 Executive Summary\n\n");
        sb.append(executiveSummary).append("\n\n");

        // ─── Trace 1: Investigation ──────────────────────────────────────────────
        sb.append("## 🔍 Trace 1: Investigation Lifecycle\n\n");
        sb.append("**Investigation:** ").append(inv.getTitle()).append("  \n");
        sb.append("**Description:** ").append(inv.getDescription() != null ? inv.getDescription() : "N/A").append("  \n");
        sb.append("**Agent Steps Executed:** ").append(invTrace.getStepCount()).append("\n\n");

        if (!invTrace.getSteps().isEmpty()) {
            sb.append("### Agent Execution Steps\n\n");
            sb.append("| Step | Type | Description | Observation |\n");
            sb.append("| :--- | :--- | :--- | :--- |\n");
            for (AgentStepDTO s : invTrace.getSteps()) {
                String desc = s.getStepDescription() != null ? s.getStepDescription().replace("|", "\\|") : "";
                String obs = s.getObservation() != null ? s.getObservation().replace("|", "\\|") : "";
                if (obs.length() > 80) obs = obs.substring(0, 77) + "...";
                sb.append("| ").append(s.getStepNumber())
                        .append(" | `").append(s.getStepType()).append("`")
                        .append(" | ").append(desc)
                        .append(" | ").append(obs).append(" |\n");
            }
            sb.append("\n");
        }

        if (!invTrace.getFindings().isEmpty()) {
            sb.append("### Confirmed Findings\n\n");
            for (FindingDTO f : invTrace.getFindings()) {
                sb.append("#### Finding #").append(f.getFindingId()).append(": ")
                        .append(f.getIssueType() != null ? f.getIssueType() : "Issue")
                        .append(" (`").append(f.getSeverity() != null ? f.getSeverity() : "NORMAL").append("`)\n\n");
                sb.append("- **File:** `").append(f.getFilePath()).append("` (Lines: ").append(f.getLineRange()).append(")\n");
                sb.append("- **Description:** ").append(f.getDescription() != null ? f.getDescription() : "N/A").append("\n");
                if (f.getExplanation() != null) {
                    sb.append("- **Explanation:** ").append(f.getExplanation()).append("\n");
                }

                if (!f.getEvidence().isEmpty()) {
                    sb.append("\n**Supporting Evidence:**\n\n");
                    sb.append("| Source | Type | Target | Description |\n");
                    sb.append("| :--- | :--- | :--- | :--- |\n");
                    for (EvidenceDTO ev : f.getEvidence()) {
                        String loc = (ev.getFilePath() != null ? ev.getFilePath() : "") +
                                (ev.getLineRange() != null ? ":" + ev.getLineRange() : "");
                        sb.append("| `").append(ev.getSource()).append("`")
                                .append(" | `").append(ev.getType()).append("`")
                                .append(" | `").append(loc).append("`")
                                .append(" | ").append(ev.getDescription() != null ? ev.getDescription() : "").append(" |\n");
                    }
                    sb.append("\n");
                }
            }
        }

        // ─── Trace 2: Remediation & Verification ────────────────────────────────
        sb.append("## 🛠️ Trace 2: Remediation & Verification Lifecycle\n\n");

        if (remTrace.getPlans().isEmpty()) {
            sb.append("*No engineering remediation plans created for this investigation yet.*\n\n");
        } else {
            for (PlanLifecycleDTO p : remTrace.getPlans()) {
                sb.append("### Engineering Plan #").append(p.getPlanId()).append(": ").append(p.getTitle()).append("\n\n");
                sb.append("- **Description:** ").append(p.getDescription()).append("\n");
                if (p.getRootCause() != null) {
                    sb.append("- **Root Cause:** ").append(p.getRootCause()).append("\n");
                }
                sb.append("- **Affected Files:** `").append(p.getAffectedFiles() != null ? p.getAffectedFiles() : "[]").append("`\n");
                sb.append("- **Risk:** ").append(p.getRisk() != null ? p.getRisk() : "N/A").append("\n\n");

                // Approval
                sb.append("#### 🛡️ Human Approval Gate\n\n");
                if (p.getApproval() != null) {
                    sb.append("- **Decision:** `").append(p.getApproval().getDecision()).append("`\n");
                    sb.append("- **Reviewer:** ").append(p.getApproval().getReviewerUsername()).append("\n");
                    if (p.getApproval().getComments() != null) {
                        sb.append("- **Comments:** ").append(p.getApproval().getComments()).append("\n");
                    }
                } else {
                    sb.append("*Pending human review and approval.*\n");
                }
                sb.append("\n");

                // Remediation
                if (p.getRemediation() != null) {
                    sb.append("#### 🔧 Remediation Execution\n\n");
                    sb.append("- **Status:** `").append(p.getRemediation().getStatus()).append("`\n");
                    sb.append("- **Strategy:** `").append(p.getRemediation().getStrategy()).append("`\n");
                    if (p.getRemediation().getDiff() != null && !p.getRemediation().getDiff().isBlank()) {
                        sb.append("\n```diff\n").append(p.getRemediation().getDiff()).append("\n```\n\n");
                    }
                }

                // Verification
                if (p.getVerification() != null) {
                    VerificationDTO v = p.getVerification();
                    sb.append("#### ✅ 7-Gate Verification Results\n\n");
                    sb.append("**Final Verification Status:** `").append(v.getStatus()).append("`\n\n");

                    sb.append("| Gate # | Gate Name | Condition | Status |\n");
                    sb.append("| :--- | :--- | :--- | :--- |\n");
                    sb.append("| 1 | Intended Files Modified | Files exist | ").append(Boolean.TRUE.equals(v.getIntendedFilesModified()) ? "PASSED" : "FAILED").append(" |\n");
                    sb.append("| 2 | Unauthorized Files Guard | No unauthorized files | ").append(Boolean.FALSE.equals(v.getUnauthorizedFilesDetected()) ? "PASSED" : "FAILED").append(" |\n");
                    sb.append("| 3 | Diff Plan Alignment | Diff matches approved plan | ").append(Boolean.TRUE.equals(v.getDiffMatchesPlan()) ? "PASSED" : "FAILED").append(" |\n");
                    sb.append("| 4 | Sandbox Build | `mvn compile` | `").append(v.getBuildStatus() != null ? v.getBuildStatus() : "N/A").append("` |\n");
                    sb.append("| 5 | Test Suite Execution | `mvn test` | `").append(v.getTestStatus() != null ? v.getTestStatus() : "N/A").append("` |\n");
                    sb.append("| 6 | Static Analysis | Linters & security checks | `").append(v.getStaticAnalysisStatus() != null ? v.getStaticAnalysisStatus() : "N/A").append("` |\n");
                    sb.append("| 7 | Unexpected Modifications Guard | No side-effect edits | ").append(Boolean.FALSE.equals(v.getUnexpectedModifications()) ? "PASSED" : "FAILED").append(" |\n\n");

                    if (v.getRepairAttemptCount() != null && v.getRepairAttemptCount() > 0) {
                        sb.append("- **Bounded Repair Attempts Made:** ").append(v.getRepairAttemptCount()).append(" / 2\n\n");
                    }
                }

                // Rollback
                if (p.getRollback() != null && p.getRollback().isRolledBack()) {
                    sb.append("#### 🔄 State Preservation & Rollback\n\n");
                    sb.append("- **Trigger Reason:** `").append(p.getRollback().getTriggerReason()).append("`\n");
                    sb.append("- **Restoration Integrity Verified:** `").append(p.getRollback().isRestorationVerified()).append("`\n");
                    sb.append("- **Workspace State:** Fully recovered from pre-remediation snapshot.\n\n");
                }
            }
        }

        sb.append("---\n*Generated by Jark Autonomous Code Intelligence Engine*\n");

        return sb.toString();
    }
}
