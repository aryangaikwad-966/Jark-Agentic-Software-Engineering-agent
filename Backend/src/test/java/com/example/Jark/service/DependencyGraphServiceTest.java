package com.example.Jark.service;

import com.example.Jark.dto.ast.ClassInfo;
import com.example.Jark.dto.ast.FieldInfo;
import com.example.Jark.dto.ast.MethodInfo;
import com.example.Jark.dto.graph.DependencyEdge;
import com.example.Jark.dto.graph.DependencyNode;
import org.jgrapht.Graph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DependencyGraphServiceTest {

    private DependencyGraphService graphService;

    @BeforeEach
    void setUp() {
        graphService = new DependencyGraphService();
    }

    @Test
    void buildGraph_shouldCreateNodesAndEdges() {
        // Arrange
        ClassInfo repositoryClass = ClassInfo.builder()
                .fullyQualifiedName("com.example.repo.UserRepository")
                .className("UserRepository")
                .classType("INTERFACE")
                .packageName("com.example.repo")
                .imports(List.of())
                .methods(List.of(
                        MethodInfo.builder()
                                .name("save")
                                .returnType("void")
                                .parameterTypes(List.of("User"))
                                .build()
                ))
                .build();

        ClassInfo serviceClass = ClassInfo.builder()
                .fullyQualifiedName("com.example.service.UserService")
                .className("UserService")
                .classType("CLASS")
                .packageName("com.example.service")
                .imports(List.of("com.example.repo.UserRepository"))
                .fields(List.of(
                        FieldInfo.builder()
                                .name("userRepository")
                                .type("UserRepository")
                                .build()
                ))
                .methods(List.of(
                        MethodInfo.builder()
                                .name("createUser")
                                .returnType("void")
                                .parameterTypes(List.of())
                                .methodCalls(List.of("save"))
                                .build()
                ))
                .build();

        ClassInfo controllerClass = ClassInfo.builder()
                .fullyQualifiedName("com.example.controller.UserController")
                .className("UserController")
                .classType("CLASS")
                .packageName("com.example.controller")
                .imports(List.of("com.example.service.UserService"))
                .fields(List.of(
                        FieldInfo.builder()
                                .name("userService")
                                .type("UserService")
                                .build()
                ))
                .methods(List.of())
                .build();

        List<ClassInfo> classes = List.of(repositoryClass, serviceClass, controllerClass);

        // Act
        Graph<DependencyNode, DependencyEdge> graph = graphService.buildGraph(classes);

        // Assert
        // Nodes: 3 classes + 2 methods = 5 nodes
        assertEquals(5, graph.vertexSet().size());

        // Check dependencies for UserController
        Set<DependencyNode> controllerDeps = graphService.getDependencies(graph, "com.example.controller.UserController");
        assertEquals(1, controllerDeps.size());
        assertEquals("com.example.service.UserService", controllerDeps.iterator().next().getId());

        // Check dependents for UserService
        Set<DependencyNode> serviceDependents = graphService.getDependents(graph, "com.example.service.UserService");
        assertEquals(1, serviceDependents.size());
        assertEquals("com.example.controller.UserController", serviceDependents.iterator().next().getId());
        
        // Check dependencies for UserService
        Set<DependencyNode> serviceDeps = graphService.getDependencies(graph, "com.example.service.UserService");
        assertEquals(1, serviceDeps.size());
        assertEquals("com.example.repo.UserRepository", serviceDeps.iterator().next().getId());

        // Check caller/callee (Method calls)
        DependencyNode serviceMethod = graph.vertexSet().stream()
                .filter(n -> n.getId().equals("com.example.service.UserService.createUser"))
                .findFirst().orElseThrow();
                
        Set<DependencyEdge> outgoingEdges = graph.outgoingEdgesOf(serviceMethod);
        boolean callsSave = outgoingEdges.stream()
                .anyMatch(e -> e.getType() == DependencyEdge.EdgeType.CALLS &&
                        graph.getEdgeTarget(e).getId().equals("com.example.repo.UserRepository.save"));
        assertTrue(callsSave, "createUser should call save");
    }
}
