package com.example.VoxCode.agent.service;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.VoxCode.agent.dto.InvestigationDecision;
import com.example.VoxCode.agent.dto.RequestClassification;
import com.example.VoxCode.agent.tools.AstTools;
import com.example.VoxCode.agent.tools.GraphTools;
import com.example.VoxCode.agent.tools.RagTools;
import com.example.VoxCode.agent.tools.RepositoryTools;
import com.example.VoxCode.entity.AgentTrace;
import com.example.VoxCode.entity.CodeRepository;
import com.example.VoxCode.entity.Investigation;
import com.example.VoxCode.repository.AgentTraceRepository;
import com.example.VoxCode.repository.CodeRepositoryRepository;
import com.example.VoxCode.repository.InvestigationRepository;
import com.example.VoxCode.evidence.model.EvidenceItem;
import com.example.VoxCode.evidence.model.LineRange;
import com.example.VoxCode.evidence.model.StructuredFinding;
import com.example.VoxCode.evidence.service.EvidenceEngine;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Core investigation agent that implements the Request → Hypothesis → Tool → Observation → Evidence → Decision → Finding loop.
 * Uses ONE primary Spring AI agent (not multi-agent swarm).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvestigationAgentService {

    private final ChatClient chatClient;
    private final RequestClassificationService classificationService;
    private final AstTools astTools;
    private final GraphTools graphTools;
    private final RagTools ragTools;
    private final RepositoryTools repositoryTools;
    private final InvestigationRepository investigationRepository;
    private final AgentTraceRepository agentTraceRepository;
    private final CodeRepositoryRepository codeRepositoryRepository;
    private final EvidenceEngine evidenceEngine;
    private final ObjectMapper objectMapper;
    private final com.example.VoxCode.service.MetricsService metricsService;

    /**
     * Orchestrates the complete investigation workflow.
     */
    @Transactional
    public Investigation orchestrateInvestigation(Long repositoryId, String userRequest) {
        log.info("Starting investigation for repository {} with request: {}", repositoryId, userRequest);
        metricsService.recordInvestigationEvent("started");

        long startTime = System.currentTimeMillis();

        // Step 1: Classify the request
        RequestClassification classification = classificationService.classifyRequest(userRequest);
        
        // Step 2: Reject OUT_OF_SCOPE requests immediately
        if (classification == RequestClassification.OUT_OF_SCOPE) {
            Investigation investigation = createInvestigation(repositoryId, userRequest, "REJECTED_OUT_OF_SCOPE");
            persistTrace(investigation, "CLASSIFICATION", "Request rejected as out of scope", 
                    Map.of("request", userRequest), Map.of("classification", classification.toString()));
            return investigation;
        }
        
        // Step 3: Create investigation record
        Investigation investigation = createInvestigation(repositoryId, userRequest, "IN_PROGRESS");
        persistTrace(investigation, "CLASSIFICATION", "Request classified", 
                    Map.of("request", userRequest), Map.of("classification", classification.toString()));
        
        // Step 4: Run investigation loop
        java.util.List<com.example.VoxCode.evidence.model.EvidenceItem> collectedEvidence = new java.util.ArrayList<>();
        InvestigationDecision finalDecision = runInvestigationLoop(investigation, userRequest, classification, collectedEvidence);
        
        // Step 5: Update investigation status based on outcome
        if (finalDecision.sufficientEvidence()) {
            investigation.setStatus("COMPLETED");
            investigation.setCompletedAt(LocalDateTime.now());
            persistTrace(investigation, "FINDING", "Investigation completed with finding", 
                    Map.of("finding", finalDecision.preliminaryFinding()), null);

            // Link LLM finding to formal Evidence Model
            recordFindingFromDecision(investigation, finalDecision, collectedEvidence, classification);
        } else {
            investigation.setStatus("FAILED_INSUFFICIENT_EVIDENCE");
            investigation.setCompletedAt(LocalDateTime.now());
            persistTrace(investigation, "FAILURE", "Investigation failed due to insufficient evidence", null, null);
        }
        
        investigationRepository.save(investigation);
        log.info("Investigation {} completed with status: {}", investigation.getId(), investigation.getStatus());

        long duration = System.currentTimeMillis() - startTime;
        metricsService.recordInvestigationDuration(investigation.getStatus(), duration);
        metricsService.recordInvestigationEvent("completed");

        return investigation;
    }

    /**
     * Runs the investigation loop: Request → Hypothesis → Tool → Observation → Evidence → Decision → Finding
     */
    private InvestigationDecision runInvestigationLoop(Investigation investigation, String userRequest, 
                                                   RequestClassification classification,
                                                   java.util.List<EvidenceItem> collectedEvidence) {
        int maxIterations = 10; // Prevent infinite loops
        InvestigationDecision currentDecision = null;
        
        for (int iteration = 0; iteration < maxIterations; iteration++) {
            log.info("Investigation iteration {} for investigation {}", iteration + 1, investigation.getId());
            
            // Get next decision from agent
            currentDecision = getNextDecision(investigation, userRequest, classification, currentDecision);
            
            persistTrace(investigation, "DECISION", "Agent decision", 
                    Map.of("iteration", iteration), 
                    Map.of("action", currentDecision.action(), "reason", currentDecision.reason()));
            
            // Handle different decision actions
            switch (currentDecision.action()) {
                case CALL_TOOL:
                    String toolResult = executeTool(currentDecision);
                    persistTrace(investigation, "TOOL_CALL", "Tool execution", 
                            Map.of("tool", currentDecision.toolName(), "params", currentDecision.toolParameters()),
                            Map.of("result", toolResult));

                    EvidenceItem evidenceItem = buildEvidenceFromTool(currentDecision.toolName(), currentDecision.toolParameters(), toolResult);
                    if (evidenceItem != null && collectedEvidence != null) {
                        collectedEvidence.add(evidenceItem);
                    }
                    break;
                    
                case MAKE_FINDING:
                    // Agent has sufficient evidence to make a finding
                    log.info("Agent decided to make finding: {}", currentDecision.preliminaryFinding());
                    return currentDecision;
                    
                case REQUEST_INFO:
                    // Agent needs more information from user
                    log.info("Agent requested additional information: {}", currentDecision.reason());
                    investigation.setStatus("AWAITING_USER_INPUT");
                    investigationRepository.save(investigation);
                    return currentDecision;
                    
                case STOP:
                    // Agent decided to stop without a finding
                    log.info("Agent decided to stop investigation: {}", currentDecision.reason());
                    return currentDecision;
                default:
                    throw new IllegalStateException("Unexpected decision action: " + currentDecision.action());
            }
        }
        
        // Max iterations reached without finding
        log.warn("Investigation reached max iterations without finding");
        return new InvestigationDecision(
                InvestigationDecision.DecisionAction.STOP,
                null,
                null,
                "Maximum iterations reached without sufficient evidence",
                0.0,
                false,
                null
        );
    }

    /**
     * Gets the next decision from the LLM based on current context.
     */
    private InvestigationDecision getNextDecision(Investigation investigation, String userRequest, 
                                                  RequestClassification classification, InvestigationDecision previousDecision) {
        long startTime = System.currentTimeMillis();
        String model = "configured";
        try {
            String prompt = buildInvestigationPrompt(investigation, userRequest, classification, previousDecision);
            
            String response = chatClient.prompt()
                    .user(prompt)
                    .system("""
                        You are an investigation agent for VoxCode, a Java/Spring repository intelligence system.
                        
                        Your goal is to investigate the user's request using available tools and make evidence-based decisions.
                        
                        Available tools:
                        - findClass(repositoryId, className): Find a class by name
                        - findMethod(repositoryId, methodName): Find methods by name
                        - findAnnotation(repositoryId, annotationName): Find classes with specific annotation
                        - findDependencies(repositoryId, classFqn): Find class dependencies
                        - findDependents(repositoryId, classFqn): Find classes that depend on this class
                        - searchSemanticContext(repositoryId, query): Search for semantic context
                        - readFile(repositoryId, filePath): Read a file from the repository
                        - listFiles(repositoryId, directoryPath): List files in a directory
                        
                        Output your decision as JSON with this structure:
                        {
                            "action": "CALL_TOOL" | "MAKE_FINDING" | "REQUEST_INFO" | "STOP",
                            "toolName": "tool name (if CALL_TOOL)",
                            "toolParameters": "parameters as JSON string (if CALL_TOOL)",
                            "reason": "explanation of your decision",
                            "confidence": 0.0 to 1.0,
                            "sufficientEvidence": true/false,
                            "preliminaryFinding": "finding description (if sufficientEvidence is true)"
                        }
                        
                        You MUST NOT make a finding without sufficient evidence from tools.
                        You MUST NOT expose hidden chain-of-thought in your output.
                        """)
                    .call()
                    .content()
                    .trim();

            InvestigationDecision decision = objectMapper.readValue(response, InvestigationDecision.class);

            long duration = System.currentTimeMillis() - startTime;
            metricsService.recordLlmCall(model, "success");
            metricsService.recordLlmLatency(model, duration);
            // Spring AI's ChatClient abstraction does not expose provider usage consistently.
            // Keep this explicitly labelled as an estimate rather than reporting fabricated usage.
            metricsService.recordLlmTokenUsage(model, "input_estimated", estimateTokens(prompt));
            metricsService.recordLlmTokenUsage(model, "output_estimated", estimateTokens(response));
            
            return decision;
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            metricsService.recordLlmCall(model, "error");
            metricsService.recordLlmLatency(model, duration);
            log.error("Failed to get next decision from agent", e);
            return new InvestigationDecision(
                    InvestigationDecision.DecisionAction.STOP,
                    null,
                    null,
                    "Failed to get decision: " + e.getMessage(),
                    0.0,
                    false,
                    null
            );
        }
    }

    private int estimateTokens(String content) {
        if (content == null || content.isBlank()) {
            return 0;
        }
        return (content.length() + 3) / 4;
    }

    /**
     * Executes a tool call based on the agent's decision.
     * Simplified version for initial implementation.
     */
    private String executeTool(InvestigationDecision decision) {
        long startTime = System.currentTimeMillis();
        String toolName = decision.toolName();
        try {
            log.info("Executing tool: {}", toolName);

            // For now, return a placeholder response
            // Full implementation would parse parameters and call actual tools
            String result = "Tool " + toolName + " executed (placeholder implementation)";

            long duration = System.currentTimeMillis() - startTime;
            metricsService.recordToolExecution(toolName, "success");
            metricsService.recordToolLatency(toolName, duration);

            return result;

        } catch (Exception e) {
            log.error("Failed to execute tool: {}", decision.toolName(), e);
            long duration = System.currentTimeMillis() - startTime;
            metricsService.recordToolExecution(toolName, "error");
            metricsService.recordToolLatency(toolName, duration);
            return "Error executing tool: " + e.getMessage();
        }
    }

    /**
     * Builds the investigation prompt for the agent.
     */
    private String buildInvestigationPrompt(Investigation investigation, String userRequest, 
                                          RequestClassification classification, InvestigationDecision previousDecision) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("User Request: ").append(userRequest).append("\n");
        prompt.append("Classification: ").append(classification).append("\n");
        prompt.append("Repository ID: ").append(investigation.getRepository().getId()).append("\n");
        
        if (previousDecision != null) {
            prompt.append("Previous Decision: ").append(previousDecision.action())
                   .append(" - ").append(previousDecision.reason()).append("\n");
        }
        
        prompt.append("\nDetermine your next action. Should you call a tool to gather more evidence, ");
        prompt.append("or do you have sufficient evidence to make a finding?");
        
        return prompt.toString();
    }

    /**
     * Creates a new investigation record.
     */
    private Investigation createInvestigation(Long repositoryId, String userRequest, String status) {
        Investigation investigation = new Investigation();
        // Load the repository from database
        CodeRepository repository = codeRepositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new IllegalArgumentException("Repository not found: " + repositoryId));
        investigation.setRepository(repository);
        investigation.setTitle(userRequest.substring(0, Math.min(255, userRequest.length())));
        investigation.setDescription(userRequest);
        investigation.setStatus(status);
        investigation.setStartedAt(LocalDateTime.now());
        return investigationRepository.save(investigation);
    }

    /**
     * Persists an agent trace for observability.
     */
    private void persistTrace(Investigation investigation, String stepType, String stepDescription, 
                             Map<String, Object> inputData, Map<String, Object> outputData) {
        try {
            AgentTrace trace = new AgentTrace();
            trace.setInvestigation(investigation);
            trace.setStepType(stepType);
            trace.setStepDescription(stepDescription);
            trace.setInputData(inputData != null ? objectMapper.writeValueAsString(inputData) : null);
            trace.setOutputData(outputData != null ? objectMapper.writeValueAsString(outputData) : null);
            agentTraceRepository.save(trace);
        } catch (Exception e) {
            log.error("Failed to persist agent trace", e);
        }
    }

    /**
     * Constructs a deterministic EvidenceItem from an executed agent tool call.
     */
    private EvidenceItem buildEvidenceFromTool(String toolName, String params, String result) {
        if (toolName == null) {
            return null;
        }
        Map<String, Object> meta = new java.util.HashMap<>();
        if (params != null) {
            meta.put("parameters", params);
        }

        switch (toolName) {
            case "findClass":
            case "findMethod":
            case "findAnnotation":
                return new EvidenceItem("AST_NODE", "AstTools." + toolName, result, meta, 1.0);
            case "findDependencies":
            case "findDependents":
                return new EvidenceItem("GRAPH_EDGE", "GraphTools." + toolName, result, meta, 1.0);
            case "searchSemanticContext":
                return new EvidenceItem("RAG_CHUNK", "RagTools." + toolName, result, meta, 0.9);
            case "readFile":
            case "listFiles":
                return new EvidenceItem("FILE_CONTENT", "RepositoryTools." + toolName, result, meta, 1.0);
            default:
                return new EvidenceItem("TOOL_RESULT", toolName, result, meta, 0.8);
        }
    }

    /**
     * Constructs and records a structured Finding linked with formal evidence.
     */
    private void recordFindingFromDecision(Investigation investigation, InvestigationDecision decision, 
                                           java.util.List<EvidenceItem> evidenceItems, RequestClassification classification) {
        try {
            Long repoId = investigation.getRepository() != null ? investigation.getRepository().getId() : 1L;
            String findingText = decision.preliminaryFinding() != null ? decision.preliminaryFinding() : "Issue detected";
            
            String targetClass = "UnknownClass";
            String filePath = "src/main/java/Unknown.java";
            LineRange lineRange = LineRange.of(1, 1);
            String issueType = classification != null ? classification.name() : "CODE_ISSUE";

            // Extract verifiable metadata if present in collected evidence items
            for (EvidenceItem item : evidenceItems) {
                if (item.metadata() != null) {
                    if (item.metadata().get("fullyQualifiedName") != null) {
                        targetClass = item.metadata().get("fullyQualifiedName").toString();
                    } else if (item.metadata().get("simpleName") != null) {
                        targetClass = item.metadata().get("simpleName").toString();
                    }
                    if (item.metadata().get("filePath") != null) {
                        filePath = item.metadata().get("filePath").toString();
                    }
                    if (item.metadata().get("startLine") instanceof Number start && item.metadata().get("endLine") instanceof Number end) {
                        lineRange = LineRange.of(start.intValue(), Math.max(start.intValue(), end.intValue()));
                    }
                }
            }

            java.util.List<EvidenceItem> refs = new java.util.ArrayList<>(evidenceItems);
            if (refs.isEmpty()) {
                refs.add(new EvidenceItem("INFERRED", "AgentInference", findingText, 
                        Map.of("confidence", decision.confidence()), decision.confidence()));
            }

            String source = "HYBRID";
            if (!refs.isEmpty() && refs.get(0).evidenceType() != null) {
                String type = refs.get(0).evidenceType();
                if (type.contains("AST")) source = "AST";
                else if (type.contains("GRAPH")) source = "GRAPH";
                else if (type.contains("RAG")) source = "RAG";
            }

            StructuredFinding structuredFinding = StructuredFinding.builder()
                    .repositoryId(repoId)
                    .filePath(filePath)
                    .lineRange(lineRange)
                    .targetClass(targetClass)
                    .issueType(issueType)
                    .severity("HIGH")
                    .title(findingText.length() > 80 ? findingText.substring(0, 77) + "..." : findingText)
                    .description(findingText)
                    .evidenceReferences(refs)
                    .evidenceSource(source)
                    .validationStatus("PENDING_VALIDATION")
                    .build();

            evidenceEngine.recordFinding(investigation, structuredFinding);
        } catch (Exception e) {
            log.error("Failed to record structured finding for investigation {}", investigation.getId(), e);
        }
    }
}
