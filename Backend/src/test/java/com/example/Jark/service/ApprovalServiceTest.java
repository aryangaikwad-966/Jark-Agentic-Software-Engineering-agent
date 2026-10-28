package com.example.Jark.service;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.Jark.entity.Approval;
import com.example.Jark.entity.Finding;
import com.example.Jark.entity.Investigation;
import com.example.Jark.entity.Plan;
import com.example.Jark.entity.User;
import com.example.Jark.repository.ApprovalRepository;
import com.example.Jark.repository.PlanRepository;
import com.example.Jark.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ApprovalServiceTest {

    @Mock
    private ApprovalRepository approvalRepository;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ApprovalService approvalService;

    private User user;
    private Finding finding;
    private Plan plan;
    private Approval approval;
    private Investigation investigation;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");

        investigation = new Investigation();
        investigation.setId(1L);

        finding = new Finding();
        finding.setId(1L);
        finding.setValidationStatus("CONFIRMED");

        plan = new Plan();
        plan.setId(1L);
        plan.setFinding(finding);
        plan.setInvestigation(investigation);
        plan.setStatus("PENDING");

        approval = new Approval();
        approval.setId(1L);
        approval.setPlan(plan);
        approval.setUser(user);
        approval.setDecision(ApprovalService.APPROVED);
        approval.setRepositoryContext("/path/to/repo");
        approval.setWorkspaceContext("/path/to/workspace");
        approval.setUsedForRemediation(false);
    }

    @Test
    void testApprovePlan_Success() {
        when(planRepository.findById(1L)).thenReturn(Optional.of(plan));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(approvalRepository.save(any(Approval.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(planRepository.save(any(Plan.class))).thenReturn(plan);

        Approval result = approvalService.approvePlan(
                1L, 1L, "Looks good", "/path/to/repo", "/path/to/workspace");

        assertNotNull(result);
        assertEquals(ApprovalService.APPROVED, result.getDecision());
        assertEquals(user, result.getUser());
        assertEquals(plan, result.getPlan());
        assertEquals("Looks good", result.getComments());
        assertEquals("/path/to/repo", result.getRepositoryContext());
        assertEquals("/path/to/workspace", result.getWorkspaceContext());
        assertFalse(result.getUsedForRemediation());

        verify(approvalRepository).save(any(Approval.class));
        verify(planRepository).save(any(Plan.class));
    }

    @Test
    void testApprovePlan_PlanNotFound() {
        when(planRepository.findById(1L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> approvalService.approvePlan(1L, 1L, null, null, null)
        );

        assertEquals("Plan not found: 1", exception.getMessage());
        verify(approvalRepository, never()).save(any());
    }

    @Test
    void testApprovePlan_UserNotFound() {
        when(planRepository.findById(1L)).thenReturn(Optional.of(plan));
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> approvalService.approvePlan(1L, 1L, null, null, null)
        );

        assertEquals("User not found: 1", exception.getMessage());
        verify(approvalRepository, never()).save(any());
    }

    @Test
    void testApprovePlan_FindingNotConfirmed() {
        finding.setValidationStatus("PENDING_VALIDATION");

        when(planRepository.findById(1L)).thenReturn(Optional.of(plan));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> approvalService.approvePlan(1L, 1L, null, null, null)
        );

        assertTrue(exception.getMessage().contains("not CONFIRMED"));
        verify(approvalRepository, never()).save(any());
    }

    @Test
    void testApprovePlan_AlreadyApproved() {
        plan.setStatus("APPROVED");

        when(planRepository.findById(1L)).thenReturn(Optional.of(plan));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> approvalService.approvePlan(1L, 1L, null, null, null)
        );

        assertEquals("Plan is already approved", exception.getMessage());
        verify(approvalRepository, never()).save(any());
    }

    @Test
    void testApprovePlan_AlreadyRejected() {
        plan.setStatus("REJECTED");

        when(planRepository.findById(1L)).thenReturn(Optional.of(plan));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> approvalService.approvePlan(1L, 1L, null, null, null)
        );

        assertEquals("Plan has been rejected. Create a new plan to re-approve.", exception.getMessage());
        verify(approvalRepository, never()).save(any());
    }

    @Test
    void testRejectPlan_Success() {
        when(planRepository.findById(1L)).thenReturn(Optional.of(plan));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(approvalRepository.save(any(Approval.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(planRepository.save(any(Plan.class))).thenReturn(plan);

        Approval result = approvalService.rejectPlan(1L, 1L, "Not ready");

        assertNotNull(result);
        assertEquals(ApprovalService.REJECTED, result.getDecision());
        assertEquals(user, result.getUser());
        assertEquals(plan, result.getPlan());
        assertEquals("Not ready", result.getComments());
        assertNull(result.getRepositoryContext());
        assertNull(result.getWorkspaceContext());
        assertFalse(result.getUsedForRemediation());

        verify(approvalRepository).save(any(Approval.class));
        verify(planRepository).save(any(Plan.class));
    }

    @Test
    void testRejectPlan_PlanNotFound() {
        when(planRepository.findById(1L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> approvalService.rejectPlan(1L, 1L, null)
        );

        assertEquals("Plan not found: 1", exception.getMessage());
        verify(approvalRepository, never()).save(any());
    }

    @Test
    void testGetApprovalsForPlan() {
        when(approvalRepository.findByPlanId(1L)).thenReturn(List.of(approval));

        List<Approval> approvals = approvalService.getApprovalsForPlan(1L);

        assertNotNull(approvals);
        assertEquals(1, approvals.size());
        assertEquals(approval, approvals.get(0));

        verify(approvalRepository).findByPlanId(1L);
    }

    @Test
    void testGetApprovedApprovalsForPlan() {
        when(approvalRepository.findApprovedByPlanId(1L)).thenReturn(List.of(approval));

        List<Approval> approvals = approvalService.getApprovedApprovalsForPlan(1L);

        assertNotNull(approvals);
        assertEquals(1, approvals.size());
        assertEquals(approval, approvals.get(0));

        verify(approvalRepository).findApprovedByPlanId(1L);
    }

    @Test
    void testVerifyApprovalForRemediation_Success() {
        when(approvalRepository.findUnusedApprovalForPlan(1L)).thenReturn(Optional.of(approval));
        when(planRepository.findById(1L)).thenReturn(Optional.of(plan));
        when(approvalRepository.save(any(Approval.class))).thenReturn(approval);

        Approval result = approvalService.verifyApprovalForRemediation(
                1L, "/path/to/repo", "/path/to/workspace");

        assertNotNull(result);
        assertTrue(result.getUsedForRemediation());

        verify(approvalRepository).save(any(Approval.class));
    }

    @Test
    void testVerifyApprovalForRemediation_NoApprovalFound() {
        when(approvalRepository.findUnusedApprovalForPlan(1L)).thenReturn(Optional.empty());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> approvalService.verifyApprovalForRemediation(1L, null, null)
        );

        assertEquals("No approved approval found for plan 1", exception.getMessage());
    }

    @Test
    void testVerifyApprovalForRemediation_PlanNotFound() {
        when(approvalRepository.findUnusedApprovalForPlan(1L)).thenReturn(Optional.of(approval));
        when(planRepository.findById(1L)).thenReturn(Optional.empty());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> approvalService.verifyApprovalForRemediation(1L, null, null)
        );

        assertEquals("Plan not found: 1", exception.getMessage());
    }

    @Test
    void testVerifyApprovalForRemediation_FindingNotConfirmed() {
        finding.setValidationStatus("PENDING_VALIDATION");

        when(approvalRepository.findUnusedApprovalForPlan(1L)).thenReturn(Optional.of(approval));
        when(planRepository.findById(1L)).thenReturn(Optional.of(plan));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> approvalService.verifyApprovalForRemediation(1L, null, null)
        );

        assertTrue(exception.getMessage().contains("not CONFIRMED"));
    }

    @Test
    void testVerifyApprovalForRemediation_RepositoryContextMismatch() {
        when(approvalRepository.findUnusedApprovalForPlan(1L)).thenReturn(Optional.of(approval));
        when(planRepository.findById(1L)).thenReturn(Optional.of(plan));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> approvalService.verifyApprovalForRemediation(1L, "/different/path", null)
        );

        assertTrue(exception.getMessage().contains("Repository context mismatch"));
    }

    @Test
    void testVerifyApprovalForRemediation_WorkspaceContextMismatch() {
        when(approvalRepository.findUnusedApprovalForPlan(1L)).thenReturn(Optional.of(approval));
        when(planRepository.findById(1L)).thenReturn(Optional.of(plan));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> approvalService.verifyApprovalForRemediation(1L, "/path/to/repo", "/different/workspace")
        );

        assertTrue(exception.getMessage().contains("Workspace context mismatch"));
    }

    @Test
    void testGetPlanApprovalStatus_Approved() {
        plan.setStatus("APPROVED");
        when(planRepository.findById(1L)).thenReturn(Optional.of(plan));

        String status = approvalService.getPlanApprovalStatus(1L);

        assertEquals(ApprovalService.APPROVED, status);
    }

    @Test
    void testGetPlanApprovalStatus_Rejected() {
        plan.setStatus("REJECTED");
        when(planRepository.findById(1L)).thenReturn(Optional.of(plan));

        String status = approvalService.getPlanApprovalStatus(1L);

        assertEquals(ApprovalService.REJECTED, status);
    }

    @Test
    void testGetPlanApprovalStatus_Pending() {
        plan.setStatus("PENDING");
        when(planRepository.findById(1L)).thenReturn(Optional.of(plan));

        String status = approvalService.getPlanApprovalStatus(1L);

        assertEquals(ApprovalService.PENDING_REVIEW, status);
    }

    @Test
    void testMarkApprovalAsUsed() {
        when(approvalRepository.findById(1L)).thenReturn(Optional.of(approval));
        when(approvalRepository.save(any(Approval.class))).thenReturn(approval);

        approvalService.markApprovalAsUsed(1L);

        assertTrue(approval.getUsedForRemediation());
        verify(approvalRepository).save(approval);
    }

    @Test
    void testMarkApprovalAsUsed_NotFound() {
        when(approvalRepository.findById(1L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> approvalService.markApprovalAsUsed(1L)
        );

        assertEquals("Approval not found: 1", exception.getMessage());
    }

    @Test
    void testGetApprovalsForUser() {
        when(approvalRepository.findByUserId(1L)).thenReturn(List.of(approval));

        List<Approval> approvals = approvalService.getApprovalsForUser(1L);

        assertNotNull(approvals);
        assertEquals(1, approvals.size());
        assertEquals(approval, approvals.get(0));

        verify(approvalRepository).findByUserId(1L);
    }

    @Test
    void testGetApprovedApprovalsForInvestigation() {
        when(approvalRepository.findApprovedByInvestigationId(1L)).thenReturn(List.of(approval));

        List<Approval> approvals = approvalService.getApprovedApprovalsForInvestigation(1L);

        assertNotNull(approvals);
        assertEquals(1, approvals.size());
        assertEquals(approval, approvals.get(0));

        verify(approvalRepository).findApprovedByInvestigationId(1L);
    }

    @Test
    void testApprovalIsValidForRemediation() {
        assertTrue(approval.isValidForRemediation());
    }

    @Test
    void testApprovalIsNotValidForRemediation_Rejected() {
        approval.setDecision(ApprovalService.REJECTED);
        assertFalse(approval.isValidForRemediation());
    }

    @Test
    void testApprovalIsNotValidForRemediation_NoPlan() {
        approval.setPlan(null);
        assertFalse(approval.isValidForRemediation());
    }

    @Test
    void testApprovalIsNotValidForRemediation_FindingNotConfirmed() {
        finding.setValidationStatus("PENDING_VALIDATION");
        assertFalse(approval.isValidForRemediation());
    }
}
