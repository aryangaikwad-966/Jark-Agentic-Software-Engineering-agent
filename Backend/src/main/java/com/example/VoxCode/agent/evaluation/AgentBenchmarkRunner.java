package com.example.VoxCode.agent.evaluation;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.VoxCode.agent.service.InvestigationAgentService;
import com.example.VoxCode.entity.AgentTrace;
import com.example.VoxCode.entity.Investigation;
import com.example.VoxCode.repository.AgentTraceRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Executes agent benchmark evaluation on a dataset of test cases.
 * Measures investigation metrics and generates evaluation reports.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AgentBenchmarkRunner {

    private final InvestigationAgentService investigationAgentService;
    private final AgentTraceRepository agentTraceRepository;

    /**
     * Runs the benchmark on the given dataset.
     */
    @Transactional
    public AgentEvaluationReport run(AgentBenchmarkDataset dataset) {
        log.info("Starting agent benchmark with {} cases", dataset.size());
        
        List<AgentEvaluationReport.AgentCaseResult> caseResults = new ArrayList<>();
        int totalCases = dataset.size();
        int tp = 0;
        int fp = 0;
        int fn = 0;
        int totalToolCalls = 0;
        int totalIterations = 0;
        double totalConfidence = 0.0;
        int evidenceValidCases = 0;
        long totalLatency = 0L;
        
        for (AgentBenchmarkCase benchmarkCase : dataset.cases()) {
            log.info("Running benchmark case: {}", benchmarkCase.caseId());
            
            AgentEvaluationReport.AgentCaseResult result = runCase(benchmarkCase);
            caseResults.add(result);
            
            // Accumulate metrics based on outcome
            switch (result.outcome()) {
                case TP -> {
                    tp++;
                    totalToolCalls += result.toolCallCount();
                    totalIterations += result.iterationCount();
                    totalConfidence += result.averageConfidence();
                    if (result.evidenceValid()) {
                        evidenceValidCases++;
                    }
                }
                case FP -> fp++;
                case FN -> fn++;
                default -> throw new IllegalStateException("Unexpected outcome: " + result.outcome());
            }
            totalLatency += result.latencyMs();
        }
        
        // Calculate aggregate metrics
        double investigationSuccessRate = totalCases > 0 ? (double) tp / totalCases : 0.0;
        double averageToolCalls = tp > 0 ? (double) totalToolCalls / tp : 0.0;
        double averageIterations = tp > 0 ? (double) totalIterations / tp : 0.0;
        double averageConfidence = tp > 0 ? totalConfidence / tp : 0.0;
        double evidenceValidityRate = totalCases > 0 ? (double) evidenceValidCases / totalCases : 0.0;
        double averageLatency = totalCases > 0 ? (double) totalLatency / totalCases : 0.0;
        
        // Precision, Recall, F1
        double precision = (tp + fp) > 0 ? (double) tp / (tp + fp) : 0.0;
        double recall = (tp + fn) > 0 ? (double) tp / (tp + fn) : 0.0;
        double f1Score = (precision + recall) > 0 ? 2 * (precision * recall) / (precision + recall) : 0.0;
        
        AgentEvaluationMetrics metrics = new AgentEvaluationMetrics(
                totalCases,
                tp,
                fp,
                fn,
                investigationSuccessRate,
                averageToolCalls,
                averageIterations,
                averageConfidence,
                evidenceValidCases,
                evidenceValidityRate,
                precision,
                recall,
                f1Score,
                totalLatency,
                averageLatency
        );
        
        AgentEvaluationReport report = new AgentEvaluationReport(
                java.time.LocalDateTime.now(),
                metrics,
                caseResults
        );
        
        log.info("Agent benchmark completed:\n{}", report.toMarkdown());
        return report;
    }

    /**
     * Runs a single benchmark case.
     */
    private AgentEvaluationReport.AgentCaseResult runCase(AgentBenchmarkCase benchmarkCase) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Run the investigation
            Investigation investigation = investigationAgentService.orchestrateInvestigation(
                    benchmarkCase.repositoryId(),
                    benchmarkCase.userRequest()
            );
            
            long latency = System.currentTimeMillis() - startTime;
            
            // Extract traces to calculate metrics
            List<AgentTrace> traces = agentTraceRepository.findByInvestigationIdOrderByTimestampAsc(investigation.getId());
            
            int toolCallCount = (int) traces.stream()
                    .filter(trace -> "TOOL_CALL".equals(trace.getStepType()))
                    .count();
            
            int iterationCount = (int) traces.stream()
                    .filter(trace -> "DECISION".equals(trace.getStepType()))
                    .count();
            
            // For confidence, we'll use a placeholder since AgentTrace doesn't store it directly
            // In a real implementation, confidence would be extracted from outputData JSON
            double averageConfidence = 0.8; // Placeholder
            
            // Determine outcome
            boolean success = "COMPLETED".equals(investigation.getStatus());
            String actualFinding = success ? investigation.getDescription() : null;
            
            AgentEvaluationReport.CaseOutcome outcome = determineOutcome(benchmarkCase, actualFinding);
            
            // Validate evidence
            boolean evidenceValid = validateEvidence(
                    benchmarkCase.expectedEvidenceFields(),
                    traces,
                    actualFinding
            );
            
            return new AgentEvaluationReport.AgentCaseResult(
                    benchmarkCase,
                    outcome,
                    actualFinding,
                    toolCallCount,
                    iterationCount,
                    averageConfidence,
                    evidenceValid,
                    latency,
                    null // No error message
            );
            
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            log.error("Benchmark case {} failed: {}", benchmarkCase.caseId(), e.getMessage(), e);
            
            return new AgentEvaluationReport.AgentCaseResult(
                    benchmarkCase,
                    AgentEvaluationReport.CaseOutcome.FN,
                    null,
                    0,
                    0,
                    0.0,
                    false,
                    latency,
                    e.getMessage()
            );
        }
    }

    /**
     * Determines if the agent's finding is a True Positive, False Positive, or False Negative.
     */
    private AgentEvaluationReport.CaseOutcome determineOutcome(AgentBenchmarkCase benchmarkCase, String actualFinding) {
        if (actualFinding == null || actualFinding.isBlank()) {
            return AgentEvaluationReport.CaseOutcome.FN;
        }
        
        // Basic match: if actual finding contains key parts of expected finding
        // In a real scenario, we might use LLM-based evaluation or semantic similarity
        String expected = benchmarkCase.expectedFinding().toLowerCase();
        String actual = actualFinding.toLowerCase();
        
        // If it matches the expected finding reasonably well, it's a TP
        // For now, we use a simple containment check or keyword match
        if (actual.contains(expected) || expected.contains(actual)) {
            return AgentEvaluationReport.CaseOutcome.TP;
        }
        
        // If it produced a finding but it doesn't match the expected one, it's a FP
        return AgentEvaluationReport.CaseOutcome.FP;
    }

    /**
     * Validates that expected evidence fields are present in the investigation.
     */
    private boolean validateEvidence(
            java.util.Set<String> expectedFields,
            List<AgentTrace> traces,
            String finding) {
        if (expectedFields == null || expectedFields.isEmpty()) {
            return true; // No specific evidence required
        }
        
        if (finding == null || finding.isBlank()) {
            return false;
        }
        
        // Check if all expected fields are mentioned in the finding or traces
        // Use a StringBuilder to accumulate text from traces
        StringBuilder textAccumulator = new StringBuilder(finding.toLowerCase());
        for (AgentTrace trace : traces) {
            if (trace.getOutputData() != null) {
                textAccumulator.append(" ").append(trace.getOutputData().toLowerCase());
            }
            if (trace.getInputData() != null) {
                textAccumulator.append(" ").append(trace.getInputData().toLowerCase());
            }
        }
        final String allText = textAccumulator.toString();
        
        return expectedFields.stream()
                .allMatch(field -> allText.contains(field.toLowerCase()));
    }
}
