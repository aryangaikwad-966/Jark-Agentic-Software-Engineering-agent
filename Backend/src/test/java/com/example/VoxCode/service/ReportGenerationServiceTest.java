package com.example.VoxCode.service;

import com.example.VoxCode.dto.report.EngineeringReportDTO;
import com.example.VoxCode.entity.*;
import com.example.VoxCode.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ReportGenerationService} (VXC-210 — Report Generation).
 *
 * <p>Tests cover the complete engineering report generation including:
 * <ul>
 *   <li>Investigation trace aggregation (agent steps, findings, evidence)</li>
 *   <li>Remediation trace aggregation (plans, approvals, remediations, executions, verifications, rollbacks)</li>
 *   <li>Overall lifecycle status determination</li>
 *   <li>Executive summary generation</li>
 *   <li>Markdown report formatting</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class ReportGenerationServiceTest {

    @Mock
    private InvestigationRepository investigationRepository;
    @Mock
    private AgentTraceRepository agentTraceRepository;
    @Mock
    private FindingRepository findingRepository;
    @Mock
    private EvidenceRepository evidenceRepository;
    @Mock
    private PlanRepository planRepository;
    @Mock
    private ApprovalRepository approvalRepository;
    @Mock
    private RemediationRepository remediationRepository;
    @Mock
    private ExecutionRepository executionRepository;
    @Mock
    private VerificationResultRepository verificationResultRepository;

    @InjectMocks
    private ReportGenerationService reportGenerationService;

    private Investigation investigation;
    private CodeRepository repository;
    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");

        repository = new CodeRepository();
        repository.setId(1L);
        repository.setName("test-repo");
        repository.setUrl("https://github.com/test/repo");
        repository.setUser(user);

        investigation = new Investigation();
        investigation.setId(1L);
        investigation.setTitle("Test Investigation");
        investigation.setDescription("Test Description");
        investigation.setStatus("COMPLETED");
        investigation.setRepository(repository);
        investigation.setStartedAt(LocalDateTime.now().minusHours(1));
        investigation.setCompletedAt(LocalDateTime.now());
    }

    @Test
    void testGenerateReport_investigationNotFound_throwsIllegalArgumentException() {
        when(investigationRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> reportGenerationService.generateReport(1L));
    }

    @Test
    void testGenerateReport_basicStructure_returnsCompleteReport() {
        when(investigationRepository.findById(1L)).thenReturn(Optional.of(investigation));
        when(agentTraceRepository.findByInvestigationIdOrderByTimestampAsc(1L)).thenReturn(List.of());
        when(findingRepository.findByInvestigationId(1L)).thenReturn(List.of());
        when(planRepository.findByInvestigationId(1L)).thenReturn(List.of());
        when(planRepository.findByInvestigationViaFinding(1L)).thenReturn(List.of());

        EngineeringReportDTO report = reportGenerationService.generateReport(1L);

        assertNotNull(report);
        assertNotNull(report.getReportId());
        assertEquals(1L, report.getInvestigationId());
        assertEquals("test-repo", report.getRepositoryName());
        assertEquals("https://github.com/test/repo", report.getRepositoryUrl());
        assertNotNull(report.getGeneratedAt());
        assertNotNull(report.getOverallStatus());
        assertNotNull(report.getExecutiveSummary());
        assertNotNull(report.getInvestigationTrace());
        assertNotNull(report.getRemediationTrace());
        assertNotNull(report.getMarkdownReport());
    }

    @Test
    void testGenerateReport_withAgentSteps_includesStepsInTrace() {
        AgentTrace trace = new AgentTrace();
        trace.setId(1L);
        trace.setInvestigation(investigation);
        trace.setStepType("SEARCH");
        trace.setStepDescription("Search for security issues");
        trace.setObservation("Found potential SQL injection");
        trace.setTimestamp(LocalDateTime.now());

        when(investigationRepository.findById(1L)).thenReturn(Optional.of(investigation));
        when(agentTraceRepository.findByInvestigationIdOrderByTimestampAsc(1L)).thenReturn(List.of(trace));
        when(findingRepository.findByInvestigationId(1L)).thenReturn(List.of());
        when(planRepository.findByInvestigationId(1L)).thenReturn(List.of());
        when(planRepository.findByInvestigationViaFinding(1L)).thenReturn(List.of());

        EngineeringReportDTO report = reportGenerationService.generateReport(1L);

        assertNotNull(report);
        assertEquals(1, report.getInvestigationTrace().getSteps().size());
        assertEquals("SEARCH", report.getInvestigationTrace().getSteps().get(0).getStepType());
        assertEquals("Search for security issues", report.getInvestigationTrace().getSteps().get(0).getStepDescription());
        assertEquals("Found potential SQL injection", report.getInvestigationTrace().getSteps().get(0).getObservation());
    }

    @Test
    void testGenerateReport_withFindings_includesFindingsInTrace() {
        Finding finding = new Finding();
        finding.setId(1L);
        finding.setInvestigation(investigation);
        finding.setRepository(repository);
        finding.setIssueType("SECURITY");
        finding.setSeverity("HIGH");
        finding.setFilePath("src/main/java/Controller.java");
        finding.setLineRange("10-20");
        finding.setDescription("SQL injection vulnerability");
        finding.setExplanation("User input not sanitized");
        finding.setValidationStatus("CONFIRMED");

        when(investigationRepository.findById(1L)).thenReturn(Optional.of(investigation));
        when(agentTraceRepository.findByInvestigationIdOrderByTimestampAsc(1L)).thenReturn(List.of());
        when(findingRepository.findByInvestigationId(1L)).thenReturn(List.of(finding));
        when(evidenceRepository.findByFindingId(1L)).thenReturn(List.of());
        when(planRepository.findByInvestigationId(1L)).thenReturn(List.of());
        when(planRepository.findByInvestigationViaFinding(1L)).thenReturn(List.of());

        EngineeringReportDTO report = reportGenerationService.generateReport(1L);

        assertNotNull(report);
        assertEquals(1, report.getInvestigationTrace().getFindings().size());
        assertEquals("SECURITY", report.getInvestigationTrace().getFindings().get(0).getIssueType());
        assertEquals("HIGH", report.getInvestigationTrace().getFindings().get(0).getSeverity());
        assertEquals("src/main/java/Controller.java", report.getInvestigationTrace().getFindings().get(0).getFilePath());
        assertEquals("SQL injection vulnerability", report.getInvestigationTrace().getFindings().get(0).getDescription());
        assertEquals("CONFIRMED", report.getInvestigationTrace().getFindings().get(0).getValidationStatus());
    }

    @Test
    void testGenerateReport_withEvidence_includesEvidenceInFinding() {
        Finding finding = new Finding();
        finding.setId(1L);
        finding.setInvestigation(investigation);
        finding.setRepository(repository);
        finding.setIssueType("SECURITY");
        finding.setSeverity("HIGH");
        finding.setFilePath("src/main/java/Controller.java");
        finding.setLineRange("10-20");
        finding.setDescription("SQL injection vulnerability");
        finding.setValidationStatus("CONFIRMED");

        Evidence evidence = new Evidence();
        evidence.setId(1L);
        evidence.setFinding(finding);
        evidence.setEvidenceSource("AST");
        evidence.setEvidenceType("METHOD_NODE");
        evidence.setFilePath("src/main/java/Controller.java");
        evidence.setLineRange("10-20");
        evidence.setClassName("Controller");
        evidence.setMethodName("handleRequest");
        evidence.setContent("public void handleRequest(String input) { ... }");
        evidence.setConfidence(0.95);

        when(investigationRepository.findById(1L)).thenReturn(Optional.of(investigation));
        when(agentTraceRepository.findByInvestigationIdOrderByTimestampAsc(1L)).thenReturn(List.of());
        when(findingRepository.findByInvestigationId(1L)).thenReturn(List.of(finding));
        when(evidenceRepository.findByFindingId(1L)).thenReturn(List.of(evidence));
        when(planRepository.findByInvestigationId(1L)).thenReturn(List.of());
        when(planRepository.findByInvestigationViaFinding(1L)).thenReturn(List.of());

        EngineeringReportDTO report = reportGenerationService.generateReport(1L);

        assertNotNull(report);
        assertEquals(1, report.getInvestigationTrace().getFindings().size());
        assertEquals(1, report.getInvestigationTrace().getFindings().get(0).getEvidence().size());
        assertEquals("AST", report.getInvestigationTrace().getFindings().get(0).getEvidence().get(0).getSource());
        assertEquals("METHOD_NODE", report.getInvestigationTrace().getFindings().get(0).getEvidence().get(0).getType());
        assertEquals("src/main/java/Controller.java", report.getInvestigationTrace().getFindings().get(0).getEvidence().get(0).getFilePath());
        assertEquals(0.95, report.getInvestigationTrace().getFindings().get(0).getEvidence().get(0).getConfidence());
    }

    @Test
    void testGenerateReport_withPlan_includesPlanInRemediationTrace() {
        Plan plan = new Plan();
        plan.setId(1L);
        plan.setInvestigation(investigation);
        plan.setTitle("Fix SQL Injection");
        plan.setDescription("Add input sanitization");
        plan.setRootCause("User input passed directly to query");
        plan.setAffectedFiles("[\"src/main/java/Controller.java\"]");
        plan.setRisk("MEDIUM");
        plan.setStatus("APPROVED");

        when(investigationRepository.findById(1L)).thenReturn(Optional.of(investigation));
        when(agentTraceRepository.findByInvestigationIdOrderByTimestampAsc(1L)).thenReturn(List.of());
        when(findingRepository.findByInvestigationId(1L)).thenReturn(List.of());
        when(planRepository.findByInvestigationId(1L)).thenReturn(List.of(plan));
        when(approvalRepository.findByPlanId(1L)).thenReturn(List.of());
        when(remediationRepository.findByPlanId(1L)).thenReturn(List.of());
        when(executionRepository.findByPlanId(1L)).thenReturn(List.of());

        EngineeringReportDTO report = reportGenerationService.generateReport(1L);

        assertNotNull(report);
        assertEquals(1, report.getRemediationTrace().getPlans().size());
        assertEquals("Fix SQL Injection", report.getRemediationTrace().getPlans().get(0).getTitle());
        assertEquals("Add input sanitization", report.getRemediationTrace().getPlans().get(0).getDescription());
        assertEquals("User input passed directly to query", report.getRemediationTrace().getPlans().get(0).getRootCause());
        assertEquals("MEDIUM", report.getRemediationTrace().getPlans().get(0).getRisk());
    }

    @Test
    void testGenerateReport_withApproval_includesApprovalInPlan() {
        Plan plan = new Plan();
        plan.setId(1L);
        plan.setInvestigation(investigation);
        plan.setTitle("Fix SQL Injection");
        plan.setStatus("APPROVED");

        Approval approval = new Approval();
        approval.setId(1L);
        approval.setPlan(plan);
        approval.setUser(user);
        approval.setDecision("APPROVED");
        approval.setComments("Looks good, proceed");
        approval.setApprovedAt(LocalDateTime.now());

        when(investigationRepository.findById(1L)).thenReturn(Optional.of(investigation));
        when(agentTraceRepository.findByInvestigationIdOrderByTimestampAsc(1L)).thenReturn(List.of());
        when(findingRepository.findByInvestigationId(1L)).thenReturn(List.of());
        when(planRepository.findByInvestigationId(1L)).thenReturn(List.of(plan));
        when(approvalRepository.findByPlanId(1L)).thenReturn(List.of(approval));
        when(remediationRepository.findByPlanId(1L)).thenReturn(List.of());
        when(executionRepository.findByPlanId(1L)).thenReturn(List.of());

        EngineeringReportDTO report = reportGenerationService.generateReport(1L);

        assertNotNull(report);
        assertEquals(1, report.getRemediationTrace().getPlans().size());
        assertNotNull(report.getRemediationTrace().getPlans().get(0).getApproval());
        assertEquals("APPROVED", report.getRemediationTrace().getPlans().get(0).getApproval().getDecision());
        assertEquals("testuser", report.getRemediationTrace().getPlans().get(0).getApproval().getReviewerUsername());
        assertEquals("Looks good, proceed", report.getRemediationTrace().getPlans().get(0).getApproval().getComments());
    }

    @Test
    void testGenerateReport_withRemediation_includesRemediationInPlan() {
        Plan plan = new Plan();
        plan.setId(1L);
        plan.setInvestigation(investigation);
        plan.setTitle("Fix SQL Injection");

        Remediation remediation = new Remediation();
        remediation.setId(1L);
        remediation.setPlan(plan);
        remediation.setTransformationStrategy("AST");
        remediation.setStatus("COMPLETED");
        remediation.setDiff("diff --git a/Controller.java b/Controller.java");
        remediation.setModifiedFiles("[\"src/main/java/Controller.java\"]");
        remediation.setWithinScope(true);

        when(investigationRepository.findById(1L)).thenReturn(Optional.of(investigation));
        when(agentTraceRepository.findByInvestigationIdOrderByTimestampAsc(1L)).thenReturn(List.of());
        when(findingRepository.findByInvestigationId(1L)).thenReturn(List.of());
        when(planRepository.findByInvestigationId(1L)).thenReturn(List.of(plan));
        when(approvalRepository.findByPlanId(1L)).thenReturn(List.of());
        when(remediationRepository.findByPlanId(1L)).thenReturn(List.of(remediation));
        when(executionRepository.findByPlanId(1L)).thenReturn(List.of());

        EngineeringReportDTO report = reportGenerationService.generateReport(1L);

        assertNotNull(report);
        assertEquals(1, report.getRemediationTrace().getPlans().size());
        assertNotNull(report.getRemediationTrace().getPlans().get(0).getRemediation());
        assertEquals("AST", report.getRemediationTrace().getPlans().get(0).getRemediation().getStrategy());
        assertEquals("COMPLETED", report.getRemediationTrace().getPlans().get(0).getRemediation().getStatus());
        assertTrue(report.getRemediationTrace().getPlans().get(0).getRemediation().getWithinScope());
    }

    @Test
    void testGenerateReport_withVerification_includesVerificationInPlan() {
        Plan plan = new Plan();
        plan.setId(1L);
        plan.setInvestigation(investigation);
        plan.setTitle("Fix SQL Injection");

        Execution execution = new Execution();
        execution.setId(1L);
        execution.setPlan(plan);
        execution.setDockerContainerId("container-123");
        execution.setStatus("COMPLETED");

        VerificationResult verificationResult = new VerificationResult();
        verificationResult.setId(1L);
        verificationResult.setExecution(execution);
        verificationResult.setStatus("PASSED");
        verificationResult.setIntendedFilesModified(true);
        verificationResult.setUnauthorizedFilesDetected(false);
        verificationResult.setDiffMatchesPlan(true);
        verificationResult.setBuildStatus("SUCCESS");
        verificationResult.setTestStatus("PASSED");
        verificationResult.setStaticAnalysisStatus("PASSED");
        verificationResult.setUnexpectedModifications(false);
        verificationResult.setRepairAttemptCount(0);

        when(investigationRepository.findById(1L)).thenReturn(Optional.of(investigation));
        when(agentTraceRepository.findByInvestigationIdOrderByTimestampAsc(1L)).thenReturn(List.of());
        when(findingRepository.findByInvestigationId(1L)).thenReturn(List.of());
        when(planRepository.findByInvestigationId(1L)).thenReturn(List.of(plan));
        when(approvalRepository.findByPlanId(1L)).thenReturn(List.of());
        when(remediationRepository.findByPlanId(1L)).thenReturn(List.of());
        when(executionRepository.findByPlanId(1L)).thenReturn(List.of(execution));
        when(verificationResultRepository.findByExecutionId(1L)).thenReturn(Optional.of(verificationResult));

        EngineeringReportDTO report = reportGenerationService.generateReport(1L);

        assertNotNull(report);
        assertEquals(1, report.getRemediationTrace().getPlans().size());
        assertNotNull(report.getRemediationTrace().getPlans().get(0).getVerification());
        assertEquals("PASSED", report.getRemediationTrace().getPlans().get(0).getVerification().getStatus());
        assertTrue(report.getRemediationTrace().getPlans().get(0).getVerification().isAllGatesPassed());
        assertEquals("SUCCESS", report.getRemediationTrace().getPlans().get(0).getVerification().getBuildStatus());
        assertEquals("PASSED", report.getRemediationTrace().getPlans().get(0).getVerification().getTestStatus());
    }

    @Test
    void testGenerateReport_markdownReportContainsRequiredSections() {
        when(investigationRepository.findById(1L)).thenReturn(Optional.of(investigation));
        when(agentTraceRepository.findByInvestigationIdOrderByTimestampAsc(1L)).thenReturn(List.of());
        when(findingRepository.findByInvestigationId(1L)).thenReturn(List.of());
        when(planRepository.findByInvestigationId(1L)).thenReturn(List.of());
        when(planRepository.findByInvestigationViaFinding(1L)).thenReturn(List.of());

        EngineeringReportDTO report = reportGenerationService.generateReport(1L);

        assertNotNull(report.getMarkdownReport());
        assertTrue(report.getMarkdownReport().contains("# 📋 VoxCode Engineering Lifecycle Report"));
        assertTrue(report.getMarkdownReport().contains("## 📝 Executive Summary"));
        assertTrue(report.getMarkdownReport().contains("## 🔍 Trace 1: Investigation Lifecycle"));
    }

    @Test
    void testGenerateReport_overallStatus_verifiedWhenAllGatesPass() {
        Plan plan = new Plan();
        plan.setId(1L);
        plan.setInvestigation(investigation);

        Execution execution = new Execution();
        execution.setId(1L);
        execution.setPlan(plan);

        VerificationResult verificationResult = new VerificationResult();
        verificationResult.setId(1L);
        verificationResult.setExecution(execution);
        verificationResult.setStatus("PASSED");
        verificationResult.setIntendedFilesModified(true);
        verificationResult.setUnauthorizedFilesDetected(false);
        verificationResult.setDiffMatchesPlan(true);
        verificationResult.setBuildStatus("SUCCESS");
        verificationResult.setTestStatus("PASSED");
        verificationResult.setStaticAnalysisStatus("PASSED");
        verificationResult.setUnexpectedModifications(false);

        when(investigationRepository.findById(1L)).thenReturn(Optional.of(investigation));
        when(agentTraceRepository.findByInvestigationIdOrderByTimestampAsc(1L)).thenReturn(List.of());
        when(findingRepository.findByInvestigationId(1L)).thenReturn(List.of());
        when(planRepository.findByInvestigationId(1L)).thenReturn(List.of(plan));
        when(approvalRepository.findByPlanId(1L)).thenReturn(List.of());
        when(remediationRepository.findByPlanId(1L)).thenReturn(List.of());
        when(executionRepository.findByPlanId(1L)).thenReturn(List.of(execution));
        when(verificationResultRepository.findByExecutionId(1L)).thenReturn(Optional.of(verificationResult));

        EngineeringReportDTO report = reportGenerationService.generateReport(1L);

        assertEquals("VERIFIED", report.getOverallStatus());
    }

    @Test
    void testGenerateReport_overallStatus_rolledBackWhenRolledBack() {
        Plan plan = new Plan();
        plan.setId(1L);
        plan.setInvestigation(investigation);

        Remediation remediation = new Remediation();
        remediation.setId(1L);
        remediation.setPlan(plan);
        remediation.setStatus("ROLLED_BACK");

        when(investigationRepository.findById(1L)).thenReturn(Optional.of(investigation));
        when(agentTraceRepository.findByInvestigationIdOrderByTimestampAsc(1L)).thenReturn(List.of());
        when(findingRepository.findByInvestigationId(1L)).thenReturn(List.of());
        when(planRepository.findByInvestigationId(1L)).thenReturn(List.of(plan));
        when(approvalRepository.findByPlanId(1L)).thenReturn(List.of());
        when(remediationRepository.findByPlanId(1L)).thenReturn(List.of(remediation));
        when(executionRepository.findByPlanId(1L)).thenReturn(List.of());

        EngineeringReportDTO report = reportGenerationService.generateReport(1L);

        assertEquals("ROLLED_BACK", report.getOverallStatus());
    }
}
