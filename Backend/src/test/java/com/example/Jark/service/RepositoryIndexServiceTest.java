package com.example.Jark.service;

import com.example.Jark.dto.ast.ClassInfo;
import com.example.Jark.dto.graph.DependencyEdge;
import com.example.Jark.dto.index.GraphQueryResult;
import com.example.Jark.dto.index.RepositoryIndex;
import com.example.Jark.dto.index.StructuralQueryResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests exercising AST and Graph layers through RepositoryIndexService.
 */
class RepositoryIndexServiceTest {

    private RepositoryIndexService indexService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        indexService = new RepositoryIndexService(new AstAnalysisService(), new DependencyGraphService());
    }

    @Test
    void buildIndex_shouldAggregateAstAndGraph() throws IOException {
        createFixtureRepository();

        RepositoryIndex index = indexService.buildIndex(tempDir);

        assertTrue(index.getClassCount() >= 3);
        assertTrue(index.getNodeCount() > index.getClassCount());
        assertTrue(index.getEdgeCount() > 0);
        assertEquals(tempDir, index.getWorkspacePath());
    }

    @Test
    void structuralQueries_shouldFindClassesAndAnnotations() throws IOException {
        createFixtureRepository();
        RepositoryIndex index = indexService.buildIndex(tempDir);

        StructuralQueryResult controllerResult = indexService.findClass(index, "PaymentController");
        assertEquals(1, controllerResult.getTotalCount());
        assertEquals("FIND_CLASS", controllerResult.getQueryType());
        assertEquals("com.example.controller.PaymentController",
                controllerResult.getClasses().get(0).getFullyQualifiedName());

        StructuralQueryResult restControllers = indexService.findClassesWithAnnotation(index, "RestController");
        assertEquals(1, restControllers.getTotalCount());
        assertTrue(restControllers.getClasses().get(0).getAnnotations().contains("RestController"));

        StructuralQueryResult getMappings = indexService.findMethodsWithAnnotation(index, "GetMapping");
        assertEquals(1, getMappings.getTotalCount());
        assertEquals("handlePaymentRequest", getMappings.getClasses().get(0).getMethods().get(0).getName());
    }

    @Test
    void structuralQueries_shouldFindMethodsByName() throws IOException {
        createFixtureRepository();
        RepositoryIndex index = indexService.buildIndex(tempDir);

        StructuralQueryResult saveResults = indexService.findMethodByName(index, "save");
        assertEquals(1, saveResults.getTotalCount());
        assertEquals("PaymentRepository", saveResults.getClasses().get(0).getClassName());
    }

    @Test
    void graphQueries_shouldReturnDependenciesForPaymentController() throws IOException {
        createFixtureRepository();
        RepositoryIndex index = indexService.buildIndex(tempDir);

        GraphQueryResult deps = indexService.findDependencies(index, "com.example.controller.PaymentController");

        assertEquals("FIND_DEPENDENCIES", deps.getQueryType());
        assertEquals("com.example.controller.PaymentController", deps.getSourceId());
        assertEquals(1, deps.getTotalCount());
        assertEquals("com.example.service.PaymentService", deps.getNodes().get(0).getId());
        assertFalse(deps.getRelationships().isEmpty());
        assertEquals(DependencyEdge.EdgeType.DEPENDS_ON, deps.getRelationships().get(0).getEdgeType());
    }

    @Test
    void graphQueries_shouldReturnDependentsForPaymentService() throws IOException {
        createFixtureRepository();
        RepositoryIndex index = indexService.buildIndex(tempDir);

        GraphQueryResult dependents = indexService.findDependents(index, "com.example.service.PaymentService");

        assertEquals("FIND_DEPENDENTS", dependents.getQueryType());
        assertEquals(1, dependents.getTotalCount());
        assertEquals("com.example.controller.PaymentController", dependents.getNodes().get(0).getId());
    }

    @Test
    void graphQueries_shouldReturnCallersAndCallees() throws IOException {
        createFixtureRepository();
        RepositoryIndex index = indexService.buildIndex(tempDir);

        String processPaymentId = "com.example.service.PaymentService.processPayment";

        GraphQueryResult callees = indexService.findCallees(index, processPaymentId);
        assertEquals("FIND_CALLEES", callees.getQueryType());
        assertTrue(callees.getNodes().stream()
                .anyMatch(n -> n.getId().equals("com.example.repo.PaymentRepository.save")));

        GraphQueryResult callers = indexService.findCallers(index, "com.example.repo.PaymentRepository.save");
        assertEquals("FIND_CALLERS", callers.getQueryType());
        assertTrue(callers.getNodes().stream()
                .anyMatch(n -> n.getId().equals(processPaymentId)));
    }

    @Test
    void structuralAndGraphQueries_shouldReturnSeparateResultTypes() throws IOException {
        createFixtureRepository();
        RepositoryIndex index = indexService.buildIndex(tempDir);

        StructuralQueryResult structural = indexService.findClass(index, "PaymentService");
        GraphQueryResult graph = indexService.findDependencies(index, "com.example.service.PaymentService");

        assertInstanceOf(StructuralQueryResult.class, structural);
        assertInstanceOf(GraphQueryResult.class, graph);
        assertNotEquals(structural.getQueryType(), graph.getQueryType());

        ClassInfo serviceClass = structural.getClasses().get(0);
        assertNotNull(serviceClass.getFilePath());
        assertNotNull(serviceClass.getMethods());

        assertFalse(graph.getNodes().isEmpty());
        assertEquals("com.example.repo.PaymentRepository", graph.getNodes().get(0).getId());
    }

    private void createFixtureRepository() throws IOException {
        Path controllerDir = tempDir.resolve("src/main/java/com/example/controller");
        Path serviceDir = tempDir.resolve("src/main/java/com/example/service");
        Path repoDir = tempDir.resolve("src/main/java/com/example/repo");
        Files.createDirectories(controllerDir);
        Files.createDirectories(serviceDir);
        Files.createDirectories(repoDir);

        Files.writeString(repoDir.resolve("PaymentRepository.java"), """
                package com.example.repo;

                public interface PaymentRepository {
                    void save(String paymentId);
                }
                """);

        Files.writeString(serviceDir.resolve("PaymentService.java"), """
                package com.example.service;

                import com.example.repo.PaymentRepository;
                import org.springframework.stereotype.Service;

                @Service
                public class PaymentService {

                    private final PaymentRepository paymentRepository;

                    public PaymentService(PaymentRepository paymentRepository) {
                        this.paymentRepository = paymentRepository;
                    }

                    public void processPayment(String paymentId) {
                        paymentRepository.save(paymentId);
                    }
                }
                """);

        Files.writeString(controllerDir.resolve("PaymentController.java"), """
                package com.example.controller;

                import com.example.service.PaymentService;
                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                public class PaymentController {

                    private final PaymentService paymentService;

                    public PaymentController(PaymentService paymentService) {
                        this.paymentService = paymentService;
                    }

                    @GetMapping("/payments")
                    public String handlePaymentRequest() {
                        paymentService.processPayment("123");
                        return "ok";
                    }
                }
                """);
    }
}
