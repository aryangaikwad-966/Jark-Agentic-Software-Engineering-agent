package com.example.Jark.agent.dto;

/**
 * Structured decision output from the agent during investigation.
 * The agent outputs structured decisions without exposing hidden chain-of-thought.
 */
public record InvestigationDecision(
    
    /**
     * The action the agent should take next.
     */
    DecisionAction action,
    
    /**
     * The tool to invoke (if action is CALL_TOOL).
     */
    String toolName,
    
    /**
     * The parameters for the tool call.
     */
    String toolParameters,
    
    /**
     * The reason for this decision.
     */
    String reason,
    
    /**
     * Confidence in this decision (0.0 to 1.0).
     */
    double confidence,
    
    /**
     * Whether the agent has sufficient evidence to make a finding.
     */
    boolean sufficientEvidence,
    
    /**
     * Preliminary finding if sufficient evidence exists.
     */
    String preliminaryFinding
) {
    
    /**
     * Possible actions the agent can take.
     */
    public enum DecisionAction {
        /**
         * Call a tool to gather more information.
         */
        CALL_TOOL,
        
        /**
         * Make a finding based on current evidence.
         */
        MAKE_FINDING,
        
        /**
         * Request additional information from the user.
         */
        REQUEST_INFO,
        
        /**
         * Stop investigation (either success or failure).
         */
        STOP
    }
}