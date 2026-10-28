package com.example.Jark.service.rag;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import com.example.Jark.dto.rag.AssembledContext;
import com.example.Jark.dto.rag.CodeChunk;
import com.example.Jark.dto.rag.DocumentType;
import com.example.Jark.dto.rag.RagQuery;
import com.example.Jark.dto.rag.ScoredChunk;
import com.example.Jark.service.rag.evaluation.RetrievalBenchmarkRunner;
import com.example.Jark.service.rag.evaluation.RetrievalEvaluationReport;
import com.example.Jark.service.rag.evaluation.RetrievalEvaluationService;

class RetrievalBenchmarkRunnerTest {

    @Test
    void runIndexesRepositoryAndProducesReport() {
        CodeChunk chunk = CodeChunk.builder().id("service").className("PaymentService")
                .documentType(DocumentType.SOURCE_CODE).content("payment flow").build();
        RagService ragService = new StubRagService(chunk);
        RetrievalEvaluationReport report = new RetrievalBenchmarkRunner(
                ragService, new RetrievalEvaluationService(ragService)).run(42L, Path.of("/workspace"), 1);

        assertEquals(1, report.caseMetrics().size());
        assertEquals(1.0, report.meanRecallAtK());
    }

    private static final class StubRagService implements RagService {
        private final CodeChunk chunk;

        private StubRagService(CodeChunk chunk) {
            this.chunk = chunk;
        }

        @Override
        public List<CodeChunk> indexRepository(Long repositoryId, Path workspacePath) {
            return List.of(chunk);
        }

        @Override
        public List<ScoredChunk> retrieve(RagQuery query) {
            return List.of(ScoredChunk.builder().chunk(chunk).finalScore(1.0).build());
        }

        @Override
        public AssembledContext assembleContext(RagQuery query, int maxTokens) {
            return AssembledContext.builder()
                    .chunks(List.of(ScoredChunk.builder().chunk(chunk).finalScore(1.0).build()))
                    .estimatedTokens(5)
                    .build();
        }

        @Override
        public List<CodeChunk> getIndexedChunks(Long repositoryId) {
            return List.of(chunk);
        }
    }
}
