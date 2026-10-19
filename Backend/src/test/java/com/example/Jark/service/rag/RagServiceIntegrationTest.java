package com.example.Jark.service.rag;

import com.example.Jark.dto.rag.AssembledContext;
import com.example.Jark.dto.rag.CodeChunk;
import com.example.Jark.dto.rag.DocumentType;
import com.example.Jark.dto.rag.RagQuery;
import com.example.Jark.dto.rag.ScoredChunk;
import com.example.Jark.service.AstAnalysisService;
import com.example.Jark.service.DependencyGraphService;
import com.example.Jark.service.RepositoryIndexService;
import com.example.Jark.service.rag.retrieval.DependencyAwareRetriever;
import com.example.Jark.service.rag.retrieval.LexicalRetriever;
import com.example.Jark.service.rag.retrieval.SymbolRetriever;
import com.example.Jark.service.rag.retrieval.VectorRetriever;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagServiceIntegrationTest {

    @TempDir
    private Path workspaceDir;

    private RagService ragService;

    @BeforeEach
    void setUp() throws IOException {
        // Create full fixture repository
        createFixtureRepository(workspaceDir);

        DocumentParserService parser = new DocumentParserService();
        AstAnalysisService astService = new AstAnalysisService();
        DependencyGraphService graphService = new DependencyGraphService();
        RepositoryIndexService indexService = new RepositoryIndexService(astService, graphService);

        DeterministicEmbeddingModel embeddingModel = new DeterministicEmbeddingModel();
        VectorStore vectorStore = new SimpleVectorStore(embeddingModel);
        VectorRetriever vectorRetriever = new VectorRetriever(vectorStore);
        LexicalRetriever lexicalRetriever = new LexicalRetriever();
        SymbolRetriever symbolRetriever = new SymbolRetriever();
        DependencyAwareRetriever dependencyRetriever = new DependencyAwareRetriever(indexService);
        RerankerService reranker = new RerankerService();
        ContextAssemblerService assembler = new ContextAssemblerService();

        ragService = new RagServiceImpl(
                parser,
                vectorRetriever,
                lexicalRetriever,
                symbolRetriever,
                dependencyRetriever,
                reranker,
                assembler,
                indexService
        );
    }

    @Test
    void endToEnd_indexAndHybridRetrievePrimaryAndDependencyContext() {
        Long repoId = 42L;

        // 1. Index repository
        List<CodeChunk> indexedChunks = ragService.indexRepository(repoId, workspaceDir);
        assertNotNull(indexedChunks);
        assertTrue(indexedChunks.size() >= 5);

        // 2. Query for PaymentController
        RagQuery query = RagQuery.builder()
                .repositoryId(repoId)
                .query("PaymentController execute payment order")
                .topK(5)
                .build();

        List<ScoredChunk> results = ragService.retrieve(query);
        assertFalse(results.isEmpty());

        // Top hit should be PaymentController
        ScoredChunk topHit = results.get(0);
        assertTrue(topHit.getChunk().getClassName().contains("PaymentController"));
        assertTrue(topHit.getFinalScore() > 0.7);

        // Connected dependency PaymentService should also be retrieved in results
        boolean hasService = results.stream()
                .anyMatch(r -> r.getChunk().getClassName() != null && r.getChunk().getClassName().contains("PaymentService"));
        assertTrue(hasService, "Dependency-aware or hybrid retrieval should include PaymentService");

        // 3. Assemble context
        AssembledContext context = ragService.assembleContext(query, 2000);
        assertNotNull(context);
        assertFalse(context.getChunks().isEmpty());
        assertTrue(context.getEstimatedTokens() > 0);
        assertTrue(context.getFormattedContext().contains("## Repository Context"));
        assertTrue(context.getFormattedContext().contains("PaymentController"));
    }

    @Test
    void endToEnd_retrieveDocumentationAndConfiguration() {
        Long repoId = 42L;
        ragService.indexRepository(repoId, workspaceDir);

        // Retrieve config
        RagQuery configQuery = RagQuery.builder()
                .repositoryId(repoId)
                .query("server port payment application")
                .topK(3)
                .build();

        List<ScoredChunk> configHits = ragService.retrieve(configQuery);
        assertFalse(configHits.isEmpty());
        assertTrue(configHits.stream().anyMatch(h -> h.getChunk().getDocumentType() == DocumentType.CONFIGURATION));

        // Retrieve docs
        RagQuery docsQuery = RagQuery.builder()
                .repositoryId(repoId)
                .query("architecture payment gateway system")
                .topK(3)
                .build();

        List<ScoredChunk> docHits = ragService.retrieve(docsQuery);
        assertFalse(docHits.isEmpty());
        assertTrue(docHits.stream().anyMatch(h -> h.getChunk().getDocumentType() == DocumentType.DOCUMENTATION));
    }

    @Test
    void retrieveScopesVectorResultsToRequestedRepository() throws IOException {
        Long firstRepoId = 42L;
        Long secondRepoId = 84L;
        ragService.indexRepository(firstRepoId, workspaceDir);

        Path secondWorkspace = Files.createDirectory(workspaceDir.resolve("second-repository"));
        Files.writeString(secondWorkspace.resolve("README.md"), "# Inventory Architecture\n\nInventory reconciliation workflow.\n");
        ragService.indexRepository(secondRepoId, secondWorkspace);

        RagQuery query = RagQuery.builder()
                .repositoryId(firstRepoId)
                .query("inventory reconciliation workflow")
                .topK(5)
                .build();

        List<ScoredChunk> results = ragService.retrieve(query);

        assertTrue(results.stream().allMatch(result -> firstRepoId.equals(result.getChunk().getRepositoryId())));
    }

    private void createFixtureRepository(Path root) throws IOException {
        Path mainJava = root.resolve("src/main/java/com/example");
        Path testJava = root.resolve("src/test/java/com/example");
        Files.createDirectories(mainJava);
        Files.createDirectories(testJava);

        // Controller
        Files.writeString(mainJava.resolve("PaymentController.java"), """
                package com.example;

                import org.springframework.web.bind.annotation.RestController;
                import org.springframework.web.bind.annotation.PostMapping;

                @RestController
                public class PaymentController {
                    private final PaymentService paymentService;

                    public PaymentController(PaymentService paymentService) {
                        this.paymentService = paymentService;
                    }

                    @PostMapping("/pay")
                    public String pay(String orderId) {
                        return paymentService.processPayment(orderId);
                    }
                }
                """);

        // Service
        Files.writeString(mainJava.resolve("PaymentService.java"), """
                package com.example;

                import org.springframework.stereotype.Service;

                @Service
                public class PaymentService {
                    private final PaymentRepository paymentRepository;

                    public PaymentService(PaymentRepository paymentRepository) {
                        this.paymentRepository = paymentRepository;
                    }

                    public String processPayment(String orderId) {
                        return paymentRepository.savePayment(orderId);
                    }
                }
                """);

        // Repository
        Files.writeString(mainJava.resolve("PaymentRepository.java"), """
                package com.example;

                public interface PaymentRepository {
                    String savePayment(String orderId);
                }
                """);

        // Test
        Files.writeString(testJava.resolve("PaymentControllerTest.java"), """
                package com.example;

                import org.junit.jupiter.api.Test;

                public class PaymentControllerTest {
                    @Test
                    void testPay() {
                        // test logic
                    }
                }
                """);

        // Docs
        Files.writeString(root.resolve("README.md"), """
                # Payment Architecture

                This system processes payments asynchronously through Stripe and PayPal gateways.

                ## Security
                All endpoints require token validation.
                """);

        // Config
        Files.writeString(root.resolve("application.yml"), """
                server:
                  port: 9090
                spring:
                  application:
                    name: payment-core
                """);
    }
}
