package com.example.Jark.controller;

import com.example.Jark.dto.ApiResponse;
import com.example.Jark.entity.Approval;
import com.example.Jark.service.ApprovalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for managing engineering plan approvals.
 * Prevents code modification without explicit user consent.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/approvals")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalService approvalService;

    /**
     * Approves an engineering plan.
     *
     * @param planId the plan ID
     * @param userId the user ID approving the plan
     * @param comments optional user comments
     * @param repositoryContext repository context for verification
     * @param workspaceContext workspace path for verification
     * @return the created Approval
     */
    @PostMapping("/approve")
    public ResponseEntity<ApiResponse<Approval>> approvePlan(
            @RequestParam Long planId,
            @RequestParam Long userId,
            @RequestParam(required = false) String comments,
            @RequestParam(required = false) String repositoryContext,
            @RequestParam(required = false) String workspaceContext) {
        
        log.info("POST /api/v1/approvals/approve - planId={}, userId={}", planId, userId);
        
        Approval approval = approvalService.approvePlan(
                planId, userId, comments, repositoryContext, workspaceContext);
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Plan approved successfully", approval));
    }

    /**
     * Rejects an engineering plan.
     *
     * @param planId the plan ID
     * @param userId the user ID rejecting the plan
     * @param comments user comments explaining the rejection
     * @return the created Approval
     */
    @PostMapping("/reject")
    public ResponseEntity<ApiResponse<Approval>> rejectPlan(
            @RequestParam Long planId,
            @RequestParam Long userId,
            @RequestParam(required = false) String comments) {
        
        log.info("POST /api/v1/approvals/reject - planId={}, userId={}", planId, userId);
        
        Approval approval = approvalService.rejectPlan(planId, userId, comments);
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Plan rejected successfully", approval));
    }

    /**
     * Gets all approvals for a plan.
     *
     * @param planId the plan ID
     * @return list of approvals
     */
    @GetMapping("/plan/{planId}")
    public ResponseEntity<ApiResponse<List<Approval>>> getApprovalsForPlan(
            @PathVariable Long planId) {
        
        log.info("GET /api/v1/approvals/plan/{}", planId);
        
        List<Approval> approvals = approvalService.getApprovalsForPlan(planId);
        return ResponseEntity.ok(ApiResponse.success(approvals));
    }

    /**
     * Gets approved approvals for a plan.
     *
     * @param planId the plan ID
     * @return list of approved approvals
     */
    @GetMapping("/plan/{planId}/approved")
    public ResponseEntity<ApiResponse<List<Approval>>> getApprovedApprovalsForPlan(
            @PathVariable Long planId) {
        
        log.info("GET /api/v1/approvals/plan/{}/approved", planId);
        
        List<Approval> approvals = approvalService.getApprovedApprovalsForPlan(planId);
        return ResponseEntity.ok(ApiResponse.success(approvals));
    }

    /**
     * Gets approval status for a plan.
     *
     * @param planId the plan ID
     * @return APPROVED, REJECTED, or PENDING_REVIEW
     */
    @GetMapping("/plan/{planId}/status")
    public ResponseEntity<ApiResponse<String>> getPlanApprovalStatus(
            @PathVariable Long planId) {
        
        log.info("GET /api/v1/approvals/plan/{}/status", planId);
        
        String status = approvalService.getPlanApprovalStatus(planId);
        return ResponseEntity.ok(ApiResponse.success(status));
    }

    /**
     * Gets all approvals for a user.
     *
     * @param userId the user ID
     * @return list of approvals
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<Approval>>> getApprovalsForUser(
            @PathVariable Long userId) {
        
        log.info("GET /api/v1/approvals/user/{}", userId);
        
        List<Approval> approvals = approvalService.getApprovalsForUser(userId);
        return ResponseEntity.ok(ApiResponse.success(approvals));
    }

    /**
     * Gets all approved approvals for an investigation.
     *
     * @param investigationId the investigation ID
     * @return list of approved approvals
     */
    @GetMapping("/investigation/{investigationId}/approved")
    public ResponseEntity<ApiResponse<List<Approval>>> getApprovedApprovalsForInvestigation(
            @PathVariable Long investigationId) {
        
        log.info("GET /api/v1/approvals/investigation/{}/approved", investigationId);
        
        List<Approval> approvals = approvalService.getApprovedApprovalsForInvestigation(investigationId);
        return ResponseEntity.ok(ApiResponse.success(approvals));
    }

    /**
     * Marks an approval as used for remediation.
     *
     * @param approvalId the approval ID
     * @return success response
     */
    @PostMapping("/{approvalId}/mark-used")
    public ResponseEntity<ApiResponse<Void>> markApprovalAsUsed(
            @PathVariable Long approvalId) {
        
        log.info("POST /api/v1/approvals/{}/mark-used", approvalId);
        
        approvalService.markApprovalAsUsed(approvalId);
        return ResponseEntity.ok(ApiResponse.success("Approval marked as used for remediation", null));
    }
}
