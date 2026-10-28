package com.example.Jark.service.rag;

import com.example.Jark.dto.ast.ClassInfo;
import com.example.Jark.dto.graph.DependencyEdge;
import com.example.Jark.dto.graph.DependencyNode;
import com.example.Jark.dto.index.GraphQueryResult;
import com.example.Jark.dto.index.RepositoryIndex;
import com.example.Jark.dto.rag.CodeChunk;
import com.example.Jark.dto.rag.DocumentType;
import com.example.Jark.dto.rag.RagQuery;
import com.example.Jark.dto.rag.ScoredChunk;
import com.example.Jark.service.RepositoryIndexService;
import com.example.Jark.service.rag.retrieval.DependencyAwareRetriever;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class DependencyAwareRetrieverTest {

    private RepositoryIndexService indexService;
    private DependencyAwareRetriever retriever;

    @BeforeEach
    void setUp() {
        indexService = Mockito.mock(RepositoryIndexService.class);
        retriever = new DependencyAwareRetriever(indexService);
    }

    @Test
    void retrieve_fetchesConnectedComponentsFromGraph() {
        RepositoryIndex mockIndex = new RepositoryIndex(
                Path.of("/workspace"),
                Collections.emptyList(),
                new DefaultDirectedGraph<>(DependencyEdge.class)
        );

        DependencyNode serviceNode = DependencyNode.builder()
                .id("com.example.PaymentService")
                .name("com.example.PaymentService")
                .type(DependencyNode.NodeType.CLASS)
                .build();
        GraphQueryResult depResult = GraphQueryResult.of("DEPS", "com.example.PaymentController", List.of(serviceNode), Collections.emptyList());
        GraphQueryResult emptyResult = GraphQueryResult.of("DEPENDENTS", "com.example.PaymentController", Collections.emptyList(), Collections.emptyList());
        when(indexService.findDependencies(any(), eq("com.example.PaymentController"))).thenReturn(depResult);
        when(indexService.findDependents(any(), any())).thenReturn(emptyResult);

        CodeChunk controllerChunk = CodeChunk.builder()
                .id("ctrl")
                .className("com.example.PaymentController")
                .documentType(DocumentType.SOURCE_CODE)
                .content("public class PaymentController {}")
                .build();

        CodeChunk serviceChunk = CodeChunk.builder()
                .id("svc")
                .className("com.example.PaymentService")
                .documentType(DocumentType.SOURCE_CODE)
                .content("public class PaymentService {}")
                .build();

        RagQuery query = RagQuery.builder()
                .query("PaymentController payment handling")
                .topK(5)
                .build();

        List<ScoredChunk> results = retriever.retrieve(
                List.of(controllerChunk, serviceChunk),
                mockIndex,
                query,
                Set.of("com.example.PaymentController")
        );

        assertFalse(results.isEmpty());
        assertEquals("svc", results.get(0).getChunk().getId());
        assertEquals("DEPENDENCY", results.get(0).getPrimarySource());
        assertEquals(0.85, results.get(0).getFinalScore());
    }
}
