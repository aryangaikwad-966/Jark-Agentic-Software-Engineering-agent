package com.example.Jark.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.Jark.entity.Remediation;

class RemediationServiceTest {

    private Remediation remediation;

    @BeforeEach
    void setUp() {
        remediation = new Remediation();
        remediation.setId(1L);
        remediation.setStatus(RemediationService.STATUS_IN_PROGRESS);
        remediation.setWithinScope(true);
    }

    @Test
    void testRemediationIsValidForVerification() {
        remediation.setStatus(RemediationService.STATUS_COMPLETED);
        remediation.setWithinScope(true);

        assertTrue(remediation.isValidForVerification());
    }

    @Test
    void testRemediationIsNotValidForVerification_NotCompleted() {
        remediation.setStatus(RemediationService.STATUS_IN_PROGRESS);
        remediation.setWithinScope(true);

        assertFalse(remediation.isValidForVerification());
    }

    @Test
    void testRemediationIsNotValidForVerification_NotWithinScope() {
        remediation.setStatus(RemediationService.STATUS_COMPLETED);
        remediation.setWithinScope(false);

        assertFalse(remediation.isValidForVerification());
    }

    @Test
    void testRemediationMarkCompleted() {
        String diff = "Sample diff";
        String modifiedFiles = "[\"file1.java\"]";

        remediation.markCompleted(diff, modifiedFiles);

        assertEquals(RemediationService.STATUS_COMPLETED, remediation.getStatus());
        assertEquals(diff, remediation.getDiff());
        assertEquals(modifiedFiles, remediation.getModifiedFiles());
        assertNotNull(remediation.getCompletedAt());
    }

    @Test
    void testRemediationMarkFailed() {
        String errorMessage = "Transformation failed";

        remediation.markFailed(errorMessage);

        assertEquals(RemediationService.STATUS_FAILED, remediation.getStatus());
        assertEquals(errorMessage, remediation.getErrorMessage());
        assertNotNull(remediation.getCompletedAt());
    }

    @Test
    void testRemediationMarkRolledBack() {
        String rollbackInfo = "Rollback via git";

        remediation.markRolledBack(rollbackInfo);

        assertEquals(RemediationService.STATUS_ROLLED_BACK, remediation.getStatus());
        assertEquals(rollbackInfo, remediation.getRollbackInfo());
        assertNotNull(remediation.getCompletedAt());
    }

    @Test
    void testTransformationStrategyConstants() {
        assertEquals("AST", RemediationService.STRATEGY_AST);
        assertEquals("OPENREWRITE", RemediationService.STRATEGY_OPENREWRITE);
        assertEquals("AI_PATCH", RemediationService.STRATEGY_AI_PATCH);
    }

    @Test
    void testStatusConstants() {
        assertEquals("IN_PROGRESS", RemediationService.STATUS_IN_PROGRESS);
        assertEquals("COMPLETED", RemediationService.STATUS_COMPLETED);
        assertEquals("FAILED", RemediationService.STATUS_FAILED);
        assertEquals("ROLLED_BACK", RemediationService.STATUS_ROLLED_BACK);
    }
}
