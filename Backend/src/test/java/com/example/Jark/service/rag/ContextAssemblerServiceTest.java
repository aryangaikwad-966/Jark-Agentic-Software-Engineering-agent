package com.example.Jark.service.rag;

import com.example.Jark.dto.rag.AssembledContext;
import com.example.Jark.dto.rag.CodeChunk;
import com.example.Jark.dto.rag.DocumentType;
import com.example.Jark.dto.rag.ScoredChunk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContextAssemblerServiceTest {

    private ContextAssemblerService assemblerService;

    @BeforeEach
    void setUp() {
        assemblerService = new ContextAssemblerService();
    }

    @Test
    void assemble_enforcesMaxTokensLimit() {
        List<ScoredChunk> chunks = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            CodeChunk chunk = CodeChunk.builder()
                    .id("chunk_" + i)
                    .filePath("src/File" + i + ".java")
                    .className("File" + i)
                    .documentType(DocumentType.SOURCE_CODE)
                    .startLine(1)
                    .endLine(20)
                    .content("public class File" + i + " {\n    // Some substantial code content filling space\n}")
                    .build();
            chunks.add(ScoredChunk.builder().chunk(chunk).finalScore(0.9 - i * 0.02).build());
        }

        // Extremely small token budget (~100 tokens -> ~400 chars)
        AssembledContext context = assemblerService.assemble(chunks, 100);

        assertNotNull(context);
        assertTrue(context.getChunks().size() < 20);
        assertTrue(context.getEstimatedTokens() <= 150);
        assertTrue(context.getFormattedContext().contains("## Repository Context"));
    }

    @Test
    void assemble_enforcesPerFileDiversityLimit() {
        List<ScoredChunk> chunks = new ArrayList<>();
        // 5 chunks from the SAME file
        for (int i = 0; i < 5; i++) {
            CodeChunk chunk = CodeChunk.builder()
                    .id("c" + i)
                    .filePath("src/LargeController.java")
                    .className("LargeController")
                    .methodName("method" + i)
                    .documentType(DocumentType.SOURCE_CODE)
                    .startLine(i * 20 + 1)
                    .endLine((i + 1) * 20)
                    .content("public void method" + i + "() { execute(" + i + "); }")
                    .build();
            chunks.add(ScoredChunk.builder().chunk(chunk).finalScore(0.95 - i * 0.01).build());
        }

        AssembledContext context = assemblerService.assemble(chunks, 4000);

        // Maximum 3 chunks allowed per file
        assertEquals(3, context.getChunks().size());
    }

    @Test
    void assemble_formatsMarkdownWithMetadataAndFences() {
        CodeChunk chunk = CodeChunk.builder()
                .id("test_chunk")
                .filePath("src/main/java/PaymentService.java")
                .className("com.example.PaymentService")
                .methodName("chargeCard")
                .symbolInfo("Transactional")
                .documentType(DocumentType.SOURCE_CODE)
                .startLine(15)
                .endLine(30)
                .content("public boolean chargeCard(String token) { return true; }")
                .build();

        ScoredChunk scored = ScoredChunk.builder()
                .chunk(chunk)
                .finalScore(0.92)
                .primarySource("VECTOR")
                .build();

        AssembledContext context = assemblerService.assemble(List.of(scored), 2000);

        String markdown = context.getFormattedContext();
        assertTrue(markdown.contains("### [SOURCE_CODE] com.example.PaymentService"));
        assertTrue(markdown.contains("`src/main/java/PaymentService.java` (Lines 15-30)"));
        assertTrue(markdown.contains("**Method:** `chargeCard`"));
        assertTrue(markdown.contains("**Symbols:** `Transactional`"));
        assertTrue(markdown.contains("```java"));
        assertTrue(markdown.contains("public boolean chargeCard(String token)"));
    }
}
