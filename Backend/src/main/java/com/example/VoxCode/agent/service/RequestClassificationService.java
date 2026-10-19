package com.example.VoxCode.agent.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import com.example.VoxCode.agent.dto.RequestClassification;

import lombok.extern.slf4j.Slf4j;

/**
 * Classifies user requests into investigation categories.
 * This is the first step in the agent workflow.
 */
@Slf4j
@Service
public class RequestClassificationService {

    private final ChatClient chatClient;

    public RequestClassificationService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * Classifies a user request into one of the predefined categories.
     * 
     * @param userRequest The user's request text
     * @return The classification of the request
     */
    public RequestClassification classifyRequest(String userRequest) {
        log.info("Classifying user request: {}", userRequest);
        
        try {
            String classification = chatClient.prompt()
                    .user(userRequest)
                    .system("""
                        You are a request classifier for VoxCode, a Java/Spring repository intelligence system.
                        
                        Classify the user's request into exactly one of these categories:
                        - INVESTIGATE: The user wants explanation/investigation only. No code changes requested.
                        - REMEDIATE: The user identifies a specific issue and wants a bounded fix (e.g., "fix the missing @PreAuthorize").
                        - INVESTIGATE_AND_REMEDIATE: The user wants investigation followed by fixing.
                        - OUT_OF_SCOPE: The request requires arbitrary feature generation, large-scale development,
                          or unrestricted coding (e.g., "build an authentication system from scratch", "create a payment module").
                        
                        Return ONLY the classification name (e.g., "INVESTIGATE").
                        """)
                    .call()
                    .content()
                    .trim()
                    .toUpperCase();
            
            RequestClassification result = RequestClassification.valueOf(classification);
            log.info("Request classified as: {}", result);
            return result;
            
        } catch (Exception e) {
            log.error("Failed to classify request, defaulting to INVESTIGATE", e);
            // Default to investigation if classification fails
            return RequestClassification.INVESTIGATE;
        }
    }

    /**
     * Validates that a classification result is appropriate for the given request.
     * This can be used as a safety check to prevent misclassification.
     */
    public boolean validateClassification(String userRequest, RequestClassification classification) {
        // Basic validation: if the request explicitly mentions "fix", "change", "modify",
        // it should not be classified as INVESTIGATE alone
        if (classification == RequestClassification.INVESTIGATE) {
            String lowerRequest = userRequest.toLowerCase();
            if (lowerRequest.contains("fix") || lowerRequest.contains("change") || 
                lowerRequest.contains("modify") || lowerRequest.contains("add") ||
                lowerRequest.contains("implement") || lowerRequest.contains("create")) {
                log.warn("Request contains remediation keywords but was classified as INVESTIGATE");
                return false;
            }
        }
        
        // OUT_OF_SCOPE validation: if the request mentions large-scale development
        if (classification != RequestClassification.OUT_OF_SCOPE) {
            String lowerRequest = userRequest.toLowerCase();
            if (lowerRequest.contains("from scratch") || lowerRequest.contains("entire system") ||
                lowerRequest.contains("whole application") || lowerRequest.contains("new feature")) {
                log.warn("Request may be out of scope but was not classified as such");
                // We don't auto-correct, but we log a warning
            }
        }
        
        return true;
    }
}