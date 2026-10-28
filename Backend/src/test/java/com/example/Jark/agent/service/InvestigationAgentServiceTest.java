package com.example.Jark.agent.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.example.Jark.agent.dto.InvestigationDecision;
import com.example.Jark.agent.dto.InvestigationDecision.DecisionAction;

/**
 * Test the investigation agent service loop and decision handling.
 */
class InvestigationAgentServiceTest {

    @Test
    void investigationDecision_recordStructure() {
        // Arrange
        InvestigationDecision decision = new InvestigationDecision(
                DecisionAction.CALL_TOOL,
                "findClass",
                "{\"className\":\"UserController\"}",
                "Need to examine UserController structure",
                0.9,
                false,
                null
        );

        // Assert
        assertEquals(DecisionAction.CALL_TOOL, decision.action());
        assertEquals("findClass", decision.toolName());
        assertEquals("{\"className\":\"UserController\"}", decision.toolParameters());
        assertEquals("Need to examine UserController structure", decision.reason());
        assertEquals(0.9, decision.confidence());
        assertFalse(decision.sufficientEvidence());
        assertNull(decision.preliminaryFinding());
    }

    @Test
    void investigationDecision_finalFinding() {
        // Arrange
        InvestigationDecision decision = new InvestigationDecision(
                DecisionAction.MAKE_FINDING,
                null,
                null,
                "Sufficient evidence gathered",
                0.95,
                true,
                "UserController handles HTTP requests for user operations"
        );

        // Assert
        assertEquals(DecisionAction.MAKE_FINDING, decision.action());
        assertNull(decision.toolName());
        assertNull(decision.toolParameters());
        assertEquals("Sufficient evidence gathered", decision.reason());
        assertEquals(0.95, decision.confidence());
        assertTrue(decision.sufficientEvidence());
        assertEquals("UserController handles HTTP requests for user operations", decision.preliminaryFinding());
    }

    @Test
    void investigationDecision_requestInfo() {
        // Arrange
        InvestigationDecision decision = new InvestigationDecision(
                DecisionAction.REQUEST_INFO,
                null,
                null,
                "Need clarification on which component to investigate",
                0.7,
                false,
                null
        );

        // Assert
        assertEquals(DecisionAction.REQUEST_INFO, decision.action());
        assertNull(decision.toolName());
        assertNull(decision.toolParameters());
        assertEquals("Need clarification on which component to investigate", decision.reason());
        assertEquals(0.7, decision.confidence());
        assertFalse(decision.sufficientEvidence());
        assertNull(decision.preliminaryFinding());
    }

    @Test
    void investigationDecision_stop() {
        // Arrange
        InvestigationDecision decision = new InvestigationDecision(
                DecisionAction.STOP,
                null,
                null,
                "Investigation complete",
                1.0,
                true,
                "Investigation complete: UserController verified"
        );

        // Assert
        assertEquals(DecisionAction.STOP, decision.action());
        assertNull(decision.toolName());
        assertNull(decision.toolParameters());
        assertEquals("Investigation complete", decision.reason());
        assertEquals(1.0, decision.confidence());
        assertTrue(decision.sufficientEvidence());
        assertEquals("Investigation complete: UserController verified", decision.preliminaryFinding());
    }

    @Test
    void investigationDecision_actionEnumHasAllValues() {
        // Verify all expected actions exist
        DecisionAction[] actions = DecisionAction.values();
        assertEquals(4, actions.length); // CALL_TOOL, MAKE_FINDING, REQUEST_INFO, STOP
    }
}
