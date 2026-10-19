package com.example.Jark.agent.dto;

/**
 * Classification of user requests for the Jark agent.
 * Every request must be classified into exactly one category.
 */
public enum RequestClassification {
    /**
     * The user wants explanation/investigation only.
     * Stops after report, no remediation.
     */
    INVESTIGATE,
    
    /**
     * The user identifies a specific issue and wants a bounded fix.
     * May skip discovery, but MUST still validate and create a CONFIRMED finding before remediation.
     */
    REMEDIATE,
    
    /**
     * The user wants Jark to investigate and then fix it.
     * Follows complete lifecycle from investigation to remediation.
     */
    INVESTIGATE_AND_REMEDIATE,
    
    /**
     * Requires arbitrary feature generation, large-scale dev, or unrestricted coding.
     * Must be explicitly rejected. Stops after classification.
     */
    OUT_OF_SCOPE
}