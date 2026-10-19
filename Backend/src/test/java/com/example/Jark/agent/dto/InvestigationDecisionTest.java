package com.example.Jark.agent.dto;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Test investigation decision structure and schemas.
 */
class InvestigationDecisionTest {

    @Test
    void investigationDecisionRecord_canBeCreated() {
        // Test that decision records can be created with all fields
        InvestigationDecision decision = new InvestigationDecision(
                InvestigationDecision.DecisionAction.CALL_TOOL,
                "findClass",
                "{\"repositoryId\": 1, \"className\": \"UserService\"}",
                "Need to find the UserService class",
                0.9,
                false,
                null
        );
        
        assertEquals(InvestigationDecision.DecisionAction.CALL_TOOL, decision.action());
        assertEquals("findClass", decision.toolName());
        assertEquals("Need to find the UserService class", decision.reason());
        assertEquals(0.9, decision.confidence());
        assertFalse(decision.sufficientEvidence());
        assertNull(decision.preliminaryFinding());
    }

    @Test
    void investigationDecisionActionEnum_hasAllRequiredValues() {
        // Assert that all required action values exist
        assertEquals(4, InvestigationDecision.DecisionAction.values().length);
        
        assertTrue(Arrays.stream(InvestigationDecision.DecisionAction.values())
                .anyMatch(a -> a == InvestigationDecision.DecisionAction.CALL_TOOL));
        assertTrue(Arrays.stream(InvestigationDecision.DecisionAction.values())
                .anyMatch(a -> a == InvestigationDecision.DecisionAction.MAKE_FINDING));
        assertTrue(Arrays.stream(InvestigationDecision.DecisionAction.values())
                .anyMatch(a -> a == InvestigationDecision.DecisionAction.REQUEST_INFO));
        assertTrue(Arrays.stream(InvestigationDecision.DecisionAction.values())
                .anyMatch(a -> a == InvestigationDecision.DecisionAction.STOP));
    }

    @Test
    void investigationDecisionActionEnum_valuesAreCorrect() {
        // Assert enum values match expected strings
        assertEquals("CALL_TOOL", InvestigationDecision.DecisionAction.CALL_TOOL.name());
        assertEquals("MAKE_FINDING", InvestigationDecision.DecisionAction.MAKE_FINDING.name());
        assertEquals("REQUEST_INFO", InvestigationDecision.DecisionAction.REQUEST_INFO.name());
        assertEquals("STOP", InvestigationDecision.DecisionAction.STOP.name());
    }

    @Test
    void investigationDecisionRecord_handlesNulls() {
        // Test that decision records can handle null values appropriately
        InvestigationDecision decision = new InvestigationDecision(
                InvestigationDecision.DecisionAction.STOP,
                null,
                null,
                "No more evidence needed",
                0.5,
                false,
                null
        );
        
        assertEquals(InvestigationDecision.DecisionAction.STOP, decision.action());
        assertNull(decision.toolName());
        assertNull(decision.toolParameters());
        assertEquals("No more evidence needed", decision.reason());
    }

    @Test
    void investigationDecisionRecord_handlesFinding() {
        // Test that decision records can handle preliminary findings
        InvestigationDecision decision = new InvestigationDecision(
                InvestigationDecision.DecisionAction.MAKE_FINDING,
                null,
                null,
                "Sufficient evidence gathered",
                0.95,
                true,
                "UserService is missing @PreAuthorize annotation on the getUser method"
        );
        
        assertEquals(InvestigationDecision.DecisionAction.MAKE_FINDING, decision.action());
        assertTrue(decision.sufficientEvidence());
        assertEquals("UserService is missing @PreAuthorize annotation on the getUser method", 
                decision.preliminaryFinding());
    }
}