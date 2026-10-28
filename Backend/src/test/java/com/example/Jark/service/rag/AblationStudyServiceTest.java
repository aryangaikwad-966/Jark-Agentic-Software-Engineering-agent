package com.example.Jark.service.rag;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import com.example.Jark.dto.rag.AssembledContext;
import com.example.Jark.dto.rag.CodeChunk;
import com.example.Jark.dto.rag.RagQuery;
import com.example.Jark.dto.rag.ScoredChunk;
import com.example.Jark.service.rag.evaluation.AblationStudyReport;
import com.example.Jark.service.rag.evaluation.AblationStudyService;
import com.example.Jark.service.rag.evaluation.RetrievalBenchmarkCase;

class AblationStudyServiceTest {

    @Test
    void compare_reportsFullRagImprovementOverBaseline() {
        CodeChunk relevant = CodeChunk.builder().id("relevant").content("payment context").build();
        RetrievalBenchmarkCase benchmarkCase = new RetrievalBenchmarkCase(
                "payment", RagQuery.builder().query("payment").topK(1).build(), Set.of("relevant"));

        RagService baseline = stub(List.of());
        RagService fullRag = stub(List.of(ScoredChunk.builder().chunk(relevant).finalScore(0.9).build()));

        AblationStudyReport report = new AblationStudyService().compare(
                baseline, fullRag, List.of(benchmarkCase), 1);

        assertEquals(1.0, report.recallImprovement());
        assertEquals(1.0, report.precisionImprovement());
        assertEquals(1.0, report.ndcgImprovement());
        assertEquals(1.0, report.investigationSuccessImprovement());
    }

    private RagService stub(List<ScoredChunk> results) {
        return new RagService() {
            @Override
            public List<CodeChunk> indexRepository(Long repositoryId, Path workspacePath) {
                return List.of();
            }

            @Override
            public List<ScoredChunk> retrieve(RagQuery query) {
                return results;
            }

            @Override
            public AssembledContext assembleContext(RagQuery query, int maxTokens) {
                return AssembledContext.builder().chunks(results).estimatedTokens(10).build();
            }

            @Override
            public List<CodeChunk> getIndexedChunks(Long repositoryId) {
                return List.of();
            }
        };
    }
}
