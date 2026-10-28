package com.example.Jark.service.rag.evaluation;

import java.nio.file.Path;

import org.springframework.stereotype.Service;

import com.example.Jark.service.rag.RagService;

import lombok.extern.slf4j.Slf4j;

/**
 * Indexes a repository and executes its deterministic retrieval benchmark.
 */
@Service
@Slf4j
public class RetrievalBenchmarkRunner {

    private final RagService ragService;
    private final RetrievalEvaluationService evaluationService;

    public RetrievalBenchmarkRunner(RagService ragService, RetrievalEvaluationService evaluationService) {
        this.ragService = ragService;
        this.evaluationService = evaluationService;
    }

    public RetrievalEvaluationReport run(Long repositoryId, Path workspacePath, int k) {
        ragService.indexRepository(repositoryId, workspacePath);
        RetrievalBenchmarkDataset dataset = RetrievalBenchmarkDataset.fromIndexedChunks(
                repositoryId, ragService.getIndexedChunks(repositoryId));
        RetrievalEvaluationReport report = evaluationService.evaluateReport(dataset.cases(), k);
        log.info("Retrieval benchmark completed for repository {}:\n{}", repositoryId, report.toMarkdown());
        return report;
    }
}
