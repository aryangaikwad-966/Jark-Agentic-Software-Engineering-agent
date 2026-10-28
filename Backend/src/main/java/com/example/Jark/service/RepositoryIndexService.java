package com.example.Jark.service;

import com.example.Jark.dto.ast.ClassInfo;
import com.example.Jark.dto.ast.MethodInfo;
import com.example.Jark.dto.graph.DependencyEdge;
import com.example.Jark.dto.graph.DependencyNode;
import com.example.Jark.dto.index.GraphQueryResult;
import com.example.Jark.dto.index.RepositoryIndex;
import com.example.Jark.dto.index.StructuralQueryResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jgrapht.Graph;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Unified querying layer over AST structural data and the dependency graph.
 * Separates structural queries (classes, methods, annotations) from graph
 * queries (dependencies, dependents, callers, callees).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RepositoryIndexService {

    private final AstAnalysisService astAnalysisService;
    private final DependencyGraphService dependencyGraphService;

    /**
     * Builds a complete repository index from a workspace path.
     */
    public RepositoryIndex buildIndex(Path workspacePath) {
        List<ClassInfo> classes = astAnalysisService.extractAllClasses(workspacePath);
        Graph<DependencyNode, DependencyEdge> graph = dependencyGraphService.buildGraph(classes);

        log.info("Built repository index for {}: {} classes, {} graph nodes, {} edges",
                workspacePath, classes.size(), graph.vertexSet().size(), graph.edgeSet().size());

        return new RepositoryIndex(workspacePath, classes, graph);
    }

    // ── Structural (AST) queries ──────────────────────────────────────────

    public StructuralQueryResult findClass(RepositoryIndex index, String className) {
        List<ClassInfo> matches = index.getClasses().stream()
                .filter(c -> c.getClassName().equals(className))
                .toList();
        return StructuralQueryResult.of("FIND_CLASS", matches);
    }

    public StructuralQueryResult findClassesWithAnnotation(RepositoryIndex index, String annotationName) {
        List<ClassInfo> matches = index.getClasses().stream()
                .filter(c -> c.getAnnotations().stream()
                        .anyMatch(a -> matchesAnnotation(a, annotationName)))
                .toList();
        return StructuralQueryResult.of("FIND_CLASSES_WITH_ANNOTATION", matches);
    }

    public StructuralQueryResult findMethodsWithAnnotation(RepositoryIndex index, String annotationName) {
        List<ClassInfo> results = new ArrayList<>();

        for (ClassInfo classInfo : index.getClasses()) {
            List<MethodInfo> matchingMethods = classInfo.getMethods().stream()
                    .filter(m -> m.getAnnotations().stream()
                            .anyMatch(a -> matchesAnnotation(a, annotationName)))
                    .toList();

            if (!matchingMethods.isEmpty()) {
                results.add(copyClassWithMethods(classInfo, matchingMethods));
            }
        }
        return StructuralQueryResult.of("FIND_METHODS_WITH_ANNOTATION", results);
    }

    public StructuralQueryResult findMethodByName(RepositoryIndex index, String methodName) {
        List<ClassInfo> results = new ArrayList<>();

        for (ClassInfo classInfo : index.getClasses()) {
            List<MethodInfo> matchingMethods = classInfo.getMethods().stream()
                    .filter(m -> m.getName().equals(methodName))
                    .toList();

            if (!matchingMethods.isEmpty()) {
                results.add(copyClassWithMethods(classInfo, matchingMethods));
            }
        }
        return StructuralQueryResult.of("FIND_METHOD_BY_NAME", results);
    }

    // ── Graph (dependency) queries ──────────────────────────────────────

    public GraphQueryResult findDependencies(RepositoryIndex index, String classFqn) {
        Set<DependencyNode> nodes = dependencyGraphService.getDependencies(
                index.getDependencyGraph(), classFqn);
        return buildGraphResult("FIND_DEPENDENCIES", classFqn, index.getDependencyGraph(), nodes,
                DependencyEdge.EdgeType.DEPENDS_ON,
                DependencyEdge.EdgeType.EXTENDS,
                DependencyEdge.EdgeType.IMPLEMENTS);
    }

    public GraphQueryResult findDependents(RepositoryIndex index, String classFqn) {
        Set<DependencyNode> nodes = dependencyGraphService.getDependents(
                index.getDependencyGraph(), classFqn);
        return buildGraphResult("FIND_DEPENDENTS", classFqn, index.getDependencyGraph(), nodes,
                DependencyEdge.EdgeType.DEPENDS_ON,
                DependencyEdge.EdgeType.EXTENDS,
                DependencyEdge.EdgeType.IMPLEMENTS);
    }

    public GraphQueryResult findCallers(RepositoryIndex index, String methodId) {
        DependencyNode methodNode = findNodeById(index.getDependencyGraph(), methodId);
        if (methodNode == null) {
            return GraphQueryResult.of("FIND_CALLERS", methodId, List.of(), List.of());
        }

        Set<DependencyNode> callers = index.getDependencyGraph().incomingEdgesOf(methodNode).stream()
                .filter(e -> e.getType() == DependencyEdge.EdgeType.CALLS)
                .map(index.getDependencyGraph()::getEdgeSource)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return buildGraphResult("FIND_CALLERS", methodId, index.getDependencyGraph(), callers,
                DependencyEdge.EdgeType.CALLS);
    }

    public GraphQueryResult findCallees(RepositoryIndex index, String methodId) {
        DependencyNode methodNode = findNodeById(index.getDependencyGraph(), methodId);
        if (methodNode == null) {
            return GraphQueryResult.of("FIND_CALLEES", methodId, List.of(), List.of());
        }

        Set<DependencyNode> callees = index.getDependencyGraph().outgoingEdgesOf(methodNode).stream()
                .filter(e -> e.getType() == DependencyEdge.EdgeType.CALLS)
                .map(index.getDependencyGraph()::getEdgeTarget)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return buildGraphResult("FIND_CALLEES", methodId, index.getDependencyGraph(), callees,
                DependencyEdge.EdgeType.CALLS);
    }

    // ── Internal helpers ─────────────────────────────────────────────────

    private GraphQueryResult buildGraphResult(String queryType,
                                              String sourceId,
                                              Graph<DependencyNode, DependencyEdge> graph,
                                              Set<DependencyNode> targetNodes,
                                              DependencyEdge.EdgeType... edgeTypes) {
        Set<DependencyEdge.EdgeType> allowedTypes = Set.of(edgeTypes);
        List<GraphQueryResult.GraphRelationship> relationships = new ArrayList<>();

        DependencyNode sourceNode = findNodeById(graph, sourceId);
        if (sourceNode != null) {
            for (DependencyEdge edge : graph.outgoingEdgesOf(sourceNode)) {
                if (allowedTypes.contains(edge.getType())) {
                    DependencyNode target = graph.getEdgeTarget(edge);
                    if (targetNodes.contains(target)) {
                        relationships.add(GraphQueryResult.GraphRelationship.builder()
                                .sourceId(sourceNode.getId())
                                .targetId(target.getId())
                                .edgeType(edge.getType())
                                .build());
                    }
                }
            }
            for (DependencyEdge edge : graph.incomingEdgesOf(sourceNode)) {
                if (allowedTypes.contains(edge.getType())) {
                    DependencyNode source = graph.getEdgeSource(edge);
                    if (targetNodes.contains(source)) {
                        relationships.add(GraphQueryResult.GraphRelationship.builder()
                                .sourceId(source.getId())
                                .targetId(sourceNode.getId())
                                .edgeType(edge.getType())
                                .build());
                    }
                }
            }
        }

        return GraphQueryResult.of(queryType, sourceId, List.copyOf(targetNodes), relationships);
    }

    private DependencyNode findNodeById(Graph<DependencyNode, DependencyEdge> graph, String id) {
        return graph.vertexSet().stream()
                .filter(n -> n.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    private boolean matchesAnnotation(String annotation, String annotationName) {
        return annotation.equals(annotationName) || annotation.endsWith("." + annotationName);
    }

    private ClassInfo copyClassWithMethods(ClassInfo classInfo, List<MethodInfo> methods) {
        return ClassInfo.builder()
                .packageName(classInfo.getPackageName())
                .className(classInfo.getClassName())
                .fullyQualifiedName(classInfo.getFullyQualifiedName())
                .filePath(classInfo.getFilePath())
                .classType(classInfo.getClassType())
                .accessModifier(classInfo.getAccessModifier())
                .annotations(classInfo.getAnnotations())
                .implementedInterfaces(classInfo.getImplementedInterfaces())
                .superClass(classInfo.getSuperClass())
                .methods(methods)
                .fields(classInfo.getFields())
                .imports(classInfo.getImports())
                .startLine(classInfo.getStartLine())
                .endLine(classInfo.getEndLine())
                .build();
    }
}
