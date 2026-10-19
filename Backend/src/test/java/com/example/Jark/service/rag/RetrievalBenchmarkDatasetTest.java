package com.example.Jark.service.rag;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import com.example.Jark.dto.rag.CodeChunk;
import com.example.Jark.dto.rag.DocumentType;
import com.example.Jark.service.rag.evaluation.RetrievalBenchmarkDataset;

class RetrievalBenchmarkDatasetTest {

    @Test
    void fromIndexedChunksCreatesStructuralAndDocumentCases() {
        List<CodeChunk> chunks = List.of(
                CodeChunk.builder().id("service").className("com.example.PaymentService")
                        .documentType(DocumentType.SOURCE_CODE).content("process payment").build(),
                CodeChunk.builder().id("docs").documentType(DocumentType.DOCUMENTATION)
                        .content("Payment architecture overview").build(),
                CodeChunk.builder().id("config").documentType(DocumentType.CONFIGURATION)
                        .content("server port 8080").build());

        RetrievalBenchmarkDataset dataset = RetrievalBenchmarkDataset.fromIndexedChunks(42L, chunks);

        assertEquals(3, dataset.cases().size());
        assertEquals("class:com.example.PaymentService", dataset.cases().get(0).name());
    }
}
