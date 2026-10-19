package com.example.Jark.service;

import com.example.Jark.entity.VerificationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VerificationServiceTest {

    private VerificationResult result;

    @BeforeEach
    void setUp() {
        result = new VerificationResult();
        result.setStatus(VerificationService.STATUS_PENDING);
    }

    @Test
    void testAllGatesPassed_AllTrue() {
        result.setIntendedFilesModified(true);
        result.setUnauthorizedFilesDetected(false);
        result.setDiffMatchesPlan(true);
        result.setBuildStatus(VerificationService.STATUS_SUCCESS);
        result.setTestStatus(VerificationService.STATUS_PASSED_ANALYSIS);
        result.setStaticAnalysisStatus(VerificationService.STATUS_PASSED_ANALYSIS);
        result.setUnexpectedModifications(false);

        assertTrue(result.allGatesPassed());
    }

    @Test
    void testAllGatesPassed_Gate1False() {
        result.setIntendedFilesModified(false);
        result.setUnauthorizedFilesDetected(false);
        result.setDiffMatchesPlan(true);
        result.setBuildStatus(VerificationService.STATUS_SUCCESS);
        result.setTestStatus(VerificationService.STATUS_PASSED_ANALYSIS);
        result.setStaticAnalysisStatus(VerificationService.STATUS_PASSED_ANALYSIS);
        result.setUnexpectedModifications(false);

        assertFalse(result.allGatesPassed());
    }

    @Test
    void testAllGatesPassed_Gate2True() {
        result.setIntendedFilesModified(true);
        result.setUnauthorizedFilesDetected(true);
        result.setDiffMatchesPlan(true);
        result.setBuildStatus(VerificationService.STATUS_SUCCESS);
        result.setTestStatus(VerificationService.STATUS_PASSED_ANALYSIS);
        result.setStaticAnalysisStatus(VerificationService.STATUS_PASSED_ANALYSIS);
        result.setUnexpectedModifications(false);

        assertFalse(result.allGatesPassed());
    }

    @Test
    void testAllGatesPassed_Gate3False() {
        result.setIntendedFilesModified(true);
        result.setUnauthorizedFilesDetected(false);
        result.setDiffMatchesPlan(false);
        result.setBuildStatus(VerificationService.STATUS_SUCCESS);
        result.setTestStatus(VerificationService.STATUS_PASSED_ANALYSIS);
        result.setStaticAnalysisStatus(VerificationService.STATUS_PASSED_ANALYSIS);
        result.setUnexpectedModifications(false);

        assertFalse(result.allGatesPassed());
    }

    @Test
    void testAllGatesPassed_Gate4Failed() {
        result.setIntendedFilesModified(true);
        result.setUnauthorizedFilesDetected(false);
        result.setDiffMatchesPlan(true);
        result.setBuildStatus("FAILURE");
        result.setTestStatus(VerificationService.STATUS_PASSED_ANALYSIS);
        result.setStaticAnalysisStatus(VerificationService.STATUS_PASSED_ANALYSIS);
        result.setUnexpectedModifications(false);

        assertFalse(result.allGatesPassed());
    }

    @Test
    void testAllGatesPassed_Gate5Failed() {
        result.setIntendedFilesModified(true);
        result.setUnauthorizedFilesDetected(false);
        result.setDiffMatchesPlan(true);
        result.setBuildStatus(VerificationService.STATUS_SUCCESS);
        result.setTestStatus("FAILED");
        result.setStaticAnalysisStatus(VerificationService.STATUS_PASSED_ANALYSIS);
        result.setUnexpectedModifications(false);

        assertFalse(result.allGatesPassed());
    }

    @Test
    void testAllGatesPassed_Gate6Failed() {
        result.setIntendedFilesModified(true);
        result.setUnauthorizedFilesDetected(false);
        result.setDiffMatchesPlan(true);
        result.setBuildStatus(VerificationService.STATUS_SUCCESS);
        result.setTestStatus(VerificationService.STATUS_PASSED_ANALYSIS);
        result.setStaticAnalysisStatus("FAILED");
        result.setUnexpectedModifications(false);

        assertFalse(result.allGatesPassed());
    }

    @Test
    void testAllGatesPassed_Gate7True() {
        result.setIntendedFilesModified(true);
        result.setUnauthorizedFilesDetected(false);
        result.setDiffMatchesPlan(true);
        result.setBuildStatus(VerificationService.STATUS_SUCCESS);
        result.setTestStatus(VerificationService.STATUS_PASSED_ANALYSIS);
        result.setStaticAnalysisStatus(VerificationService.STATUS_PASSED_ANALYSIS);
        result.setUnexpectedModifications(true);

        assertFalse(result.allGatesPassed());
    }

    @Test
    void testMarkPassed() {
        result.markPassed();

        assertEquals(VerificationService.STATUS_PASSED, result.getStatus());
        assertNotNull(result.getVerifiedAt());
    }

    @Test
    void testMarkFailed() {
        String reason = "Gate 4 failed";
        result.markFailed(reason);

        assertEquals(VerificationService.STATUS_FAILED, result.getStatus());
        assertEquals(reason, result.getVerificationLog());
        assertNotNull(result.getVerifiedAt());
    }

    @Test
    void testStatusConstants() {
        assertEquals("PENDING", VerificationService.STATUS_PENDING);
        assertEquals("PASSED", VerificationService.STATUS_PASSED);
        assertEquals("FAILED", VerificationService.STATUS_FAILED);
        assertEquals("SUCCESS", VerificationService.STATUS_SUCCESS);
        assertEquals("PASSED", VerificationService.STATUS_PASSED_ANALYSIS);
    }

    @Test
    void testDefaultValues() {
        VerificationResult newResult = new VerificationResult();

        assertEquals("PENDING", newResult.getStatus());
        assertFalse(newResult.getIntendedFilesModified());
        assertFalse(newResult.getUnauthorizedFilesDetected());
        assertFalse(newResult.getDiffMatchesPlan());
        assertEquals(0, newResult.getTotalFilesModified());
        assertFalse(newResult.getUnexpectedModifications());
    }
}
