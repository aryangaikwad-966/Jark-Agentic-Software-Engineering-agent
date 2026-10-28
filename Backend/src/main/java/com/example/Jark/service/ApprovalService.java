package com.example.Jark.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.Jark.entity.Approval;
import com.example.Jark.entity.Plan;
import com.example.Jark.entity.User;
import com.example.Jark.repository.ApprovalRepository;
import com.example.Jark.repository.PlanRepository;
import com.example.Jark.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Manages the approval workflow for engineering plans.
 * Prevents code modification without explicit user consent.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalService {

    private final ApprovalRepository approvalRepository;
    private final PlanRepository planRepository;
    private final UserRepository userRepository;

    /**
     * Decision constants.
     */
    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";
    public static final String PENDING_REVIEW = "PENDING_REVIEW";

    /**
     * Approves an engineering plan for a user.
     *
     * @param planId the ID of the plan to approve
     * @param userId the ID of the user approving the plan
     * @param comments optional user comments
     * @param repositoryContext repository context for verification
     * @param workspaceContext workspace path for verification
     * @return the created Approval
     * @throws IllegalArgumentException if the plan or user is not found, or if the plan is not valid for approval
     */
    @Transactional
    public Approval approvePlan(Long planId, Long userId, String comments, 
                                String repositoryContext, String workspaceContext) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + planId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // Validate plan is ready for approval
        validatePlanForApproval(plan);

        log.info("User {} approving plan {} for investigation {}", 
                userId, planId, plan.getInvestigation() != null ? plan.getInvestigation().getId() : "null");

        // Create approval
        Approval approval = new Approval();
        approval.setPlan(plan);
        approval.setUser(user);
        approval.setDecision(APPROVED);
        approval.setComments(comments);
        approval.setRepositoryContext(repositoryContext);
        approval.setWorkspaceContext(workspaceContext);
        approval.setUsedForRemediation(false);

        Approval savedApproval = approvalRepository.save(approval);

        // Update plan status
        plan.setStatus("APPROVED");
        planRepository.save(plan);

        log.info("Successfully approved Plan ID {} by User ID {}", savedApproval.getId(), userId);

        return savedApproval;
    }

    /**
     * Rejects an engineering plan.
     *
     * @param planId the ID of the plan to reject
     * @param userId the ID of the user rejecting the plan
     * @param comments user comments explaining the rejection
     * @return the created Approval
     * @throws IllegalArgumentException if the plan or user is not found
     */
    @Transactional
    public Approval rejectPlan(Long planId, Long userId, String comments) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + planId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        log.info("User {} rejecting plan {} for investigation {}", 
                userId, planId, plan.getInvestigation() != null ? plan.getInvestigation().getId() : "null");

        // Create rejection
        Approval approval = new Approval();
        approval.setPlan(plan);
        approval.setUser(user);
        approval.setDecision(REJECTED);
        approval.setComments(comments);
        approval.setRepositoryContext(null);
        approval.setWorkspaceContext(null);
        approval.setUsedForRemediation(false);

        Approval savedApproval = approvalRepository.save(approval);

        // Update plan status
        plan.setStatus("REJECTED");
        planRepository.save(plan);

        log.info("Successfully rejected Plan ID {} by User ID {}", savedApproval.getId(), userId);

        return savedApproval;
    }

    /**
     * Gets all approvals for a plan.
     *
     * @param planId the plan ID
     * @return list of approvals
     */
    @Transactional(readOnly = true)
    public List<Approval> getApprovalsForPlan(Long planId) {
        return approvalRepository.findByPlanId(planId);
    }

    /**
     * Gets approved approvals for a plan.
     *
     * @param planId the plan ID
     * @return list of approved approvals, sorted by approval time
     */
    @Transactional(readOnly = true)
    public List<Approval> getApprovedApprovalsForPlan(Long planId) {
        return approvalRepository.findApprovedByPlanId(planId);
    }

    /**
     * Verifies that a plan is approved and valid for remediation.
     * This is the security check that the remediation pipeline MUST use.
     *
     * @param planId the plan ID to verify
     * @param currentRepositoryContext current repository context for verification
     * @param currentWorkspaceContext current workspace path for verification
     * @return the approval if valid
     * @throws IllegalStateException if the plan is not approved or context doesn't match
     */
    @Transactional(readOnly = true)
    public Approval verifyApprovalForRemediation(Long planId, String currentRepositoryContext, 
                                              String currentWorkspaceContext) {
        log.info("Verifying approval for remediation: planId={}, repository={}, workspace={}",
                planId, currentRepositoryContext, currentWorkspaceContext);

        // Find an unused approved approval
        Optional<Approval> approvalOpt = approvalRepository.findUnusedApprovalForPlan(planId);
        
        if (approvalOpt.isEmpty()) {
            throw new IllegalStateException("No approved approval found for plan " + planId);
        }

        Approval approval = approvalOpt.get();

        // Verify plan exists and is valid
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new IllegalStateException("Plan not found: " + planId));

        // Verify finding is CONFIRMED
        if (plan.getFinding() == null || !"CONFIRMED".equals(plan.getFinding().getValidationStatus())) {
            throw new IllegalStateException("Plan finding is not CONFIRMED. Current status: " + 
                    (plan.getFinding() != null ? plan.getFinding().getValidationStatus() : "null"));
        }

        // Verify repository context matches
        if (approval.getRepositoryContext() != null && currentRepositoryContext != null) {
            if (!approval.getRepositoryContext().equals(currentRepositoryContext)) {
                throw new IllegalStateException(
                        "Repository context mismatch. Approval context: " + approval.getRepositoryContext() + 
                        ", Current context: " + currentRepositoryContext);
            }
        }

        // Verify workspace context matches
        if (approval.getWorkspaceContext() != null && currentWorkspaceContext != null) {
            if (!approval.getWorkspaceContext().equals(currentWorkspaceContext)) {
                throw new IllegalStateException(
                        "Workspace context mismatch. Approval context: " + approval.getWorkspaceContext() + 
                        ", Current context: " + currentWorkspaceContext);
            }
        }

        // Verify the approval belongs to the current plan
        if (!approval.getPlan().getId().equals(planId)) {
            throw new IllegalStateException("Approval does not belong to the specified plan");
        }

        // Mark approval as used for remediation
        approval.setUsedForRemediation(true);
        approvalRepository.save(approval);

        log.info("Approval verification successful for plan ID {}", planId);

        return approval;
    }

    /**
     * Validates that a plan is ready for approval.
     */
    private void validatePlanForApproval(Plan plan) {
        if (plan.getFinding() == null) {
            throw new IllegalArgumentException("Plan has no associated finding");
        }

        if (!"CONFIRMED".equals(plan.getFinding().getValidationStatus())) {
            throw new IllegalArgumentException(
                    "Plan finding is not CONFIRMED. Current status: " + 
                    plan.getFinding().getValidationStatus() + 
                    ". Only CONFIRMED findings can be approved.");
        }

        if ("APPROVED".equals(plan.getStatus())) {
            throw new IllegalArgumentException("Plan is already approved");
        }

        if ("REJECTED".equals(plan.getStatus())) {
            throw new IllegalArgumentException("Plan has been rejected. Create a new plan to re-approve.");
        }
    }

    /**
     * Gets approval status for a plan.
     *
     * @param planId the plan ID
     * @return APPROVED, REJECTED, or PENDING_REVIEW
     */
    @Transactional(readOnly = true)
    public String getPlanApprovalStatus(Long planId) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + planId));

        if ("APPROVED".equals(plan.getStatus())) {
            return APPROVED;
        } else if ("REJECTED".equals(plan.getStatus())) {
            return REJECTED;
        } else {
            return PENDING_REVIEW;
        }
    }

    /**
     * Marks an approval as used for remediation.
     *
     * @param approvalId the approval ID
     */
    @Transactional
    public void markApprovalAsUsed(Long approvalId) {
        Approval approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> new IllegalArgumentException("Approval not found: " + approvalId));

        approval.setUsedForRemediation(true);
        approvalRepository.save(approval);

        log.info("Marked Approval ID {} as used for remediation", approvalId);
    }

    /**
     * Gets all approvals for a user.
     *
     * @param userId the user ID
     * @return list of approvals
     */
    @Transactional(readOnly = true)
    public List<Approval> getApprovalsForUser(Long userId) {
        return approvalRepository.findByUserId(userId);
    }

    /**
     * Gets all approved approvals for an investigation.
     *
     * @param investigationId the investigation ID
     * @return list of approved approvals
     */
    @Transactional(readOnly = true)
    public List<Approval> getApprovedApprovalsForInvestigation(Long investigationId) {
        return approvalRepository.findApprovedByInvestigationId(investigationId);
    }
}
