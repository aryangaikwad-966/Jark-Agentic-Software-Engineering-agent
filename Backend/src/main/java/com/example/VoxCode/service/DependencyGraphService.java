package com.example.VoxCode.service;

import com.example.VoxCode.dto.ast.ClassInfo;
import com.example.VoxCode.dto.ast.FieldInfo;
import com.example.VoxCode.dto.ast.MethodInfo;
import com.example.VoxCode.dto.graph.DependencyEdge;
import com.example.VoxCode.dto.graph.DependencyNode;
import org.jgrapht.Graph;
import org.jgrapht.graph.DirectedMultigraph;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Constructs and queries a dependency graph using JGraphT based on AST data.
 */
@Slf4j
@Service
public class DependencyGraphService {

    /**
     * Builds a dependency graph from a list of ClassInfo objects extracted from AST.
     *
     * @param classes The classes to include in the graph.
     * @return The constructed directed graph.
     */
    public Graph<DependencyNode, DependencyEdge> buildGraph(List<ClassInfo> classes) {
        Graph<DependencyNode, DependencyEdge> graph = new DirectedMultigraph<>(DependencyEdge.class);
        Map<String, DependencyNode> nodeLookup = new HashMap<>();

        addNodesForClasses(graph, nodeLookup, classes);
        addDependencyEdges(graph, nodeLookup, classes);

        log.info("Built dependency graph with {} nodes and {} edges", graph.vertexSet().size(), graph.edgeSet().size());
        return graph;
    }

    /**
     * Gets all nodes that the specified class depends on directly.
     */
    public Set<DependencyNode> getDependencies(Graph<DependencyNode, DependencyEdge> graph, String classFqn) {
        DependencyNode node = findNodeById(graph, classFqn);
        if (node == null) return Set.of();
        return graph.outgoingEdgesOf(node).stream()
                .filter(this::isDependencyEdge)
                .map(graph::getEdgeTarget)
                .collect(Collectors.toSet());
    }

    /**
     * Gets all nodes that depend on the specified class directly (impact analysis).
     */
    public Set<DependencyNode> getDependents(Graph<DependencyNode, DependencyEdge> graph, String classFqn) {
        DependencyNode node = findNodeById(graph, classFqn);
        if (node == null) return Set.of();
        return graph.incomingEdgesOf(node).stream()
                .filter(this::isDependencyEdge)
                .map(graph::getEdgeSource)
                .collect(Collectors.toSet());
    }

    private void addNodesForClasses(
            Graph<DependencyNode, DependencyEdge> graph,
            Map<String, DependencyNode> nodeLookup,
            List<ClassInfo> classes
    ) {
        for (ClassInfo classInfo : classes) {
            DependencyNode classNode = createClassNode(classInfo);
            graph.addVertex(classNode);
            nodeLookup.put(classNode.getId(), classNode);

            for (MethodInfo methodInfo : classInfo.getMethods()) {
                DependencyNode methodNode = createMethodNode(classInfo, methodInfo);
                graph.addVertex(methodNode);
                nodeLookup.put(methodNode.getId(), methodNode);
                graph.addEdge(classNode, methodNode, new DependencyEdge(DependencyEdge.EdgeType.CONTAINS));
            }
        }
    }

    private void addDependencyEdges(
            Graph<DependencyNode, DependencyEdge> graph,
            Map<String, DependencyNode> nodeLookup,
            List<ClassInfo> classes
    ) {
        for (ClassInfo classInfo : classes) {
            DependencyNode sourceNode = nodeLookup.get(classInfo.getFullyQualifiedName());
            if (sourceNode == null) {
                continue;
            }
            addExtendsEdge(graph, nodeLookup, classInfo, sourceNode);
            addImplementsEdges(graph, nodeLookup, classInfo, sourceNode);
            addFieldDependencyEdges(graph, nodeLookup, classInfo, sourceNode);
            addMethodDependencyAndCallEdges(graph, nodeLookup, classInfo, sourceNode);
        }
    }

    private void addExtendsEdge(
            Graph<DependencyNode, DependencyEdge> graph,
            Map<String, DependencyNode> nodeLookup,
            ClassInfo classInfo,
            DependencyNode sourceNode
    ) {
        if (classInfo.getSuperClass() == null) {
            return;
        }
        String superClassName = resolveTypeName(
                classInfo.getSuperClass(),
                classInfo.getImports(),
                classInfo.getPackageName()
        );
        DependencyNode targetNode = nodeLookup.get(superClassName);
        if (targetNode != null) {
            graph.addEdge(sourceNode, targetNode, new DependencyEdge(DependencyEdge.EdgeType.EXTENDS));
        }
    }

    private void addImplementsEdges(
            Graph<DependencyNode, DependencyEdge> graph,
            Map<String, DependencyNode> nodeLookup,
            ClassInfo classInfo,
            DependencyNode sourceNode
    ) {
        if (classInfo.getImplementedInterfaces() == null) {
            return;
        }
        for (String interfaceName : classInfo.getImplementedInterfaces()) {
            String resolvedName = resolveTypeName(
                    interfaceName,
                    classInfo.getImports(),
                    classInfo.getPackageName()
            );
            DependencyNode targetNode = nodeLookup.get(resolvedName);
            if (targetNode != null) {
                graph.addEdge(sourceNode, targetNode, new DependencyEdge(DependencyEdge.EdgeType.IMPLEMENTS));
            }
        }
    }

    private void addFieldDependencyEdges(
            Graph<DependencyNode, DependencyEdge> graph,
            Map<String, DependencyNode> nodeLookup,
            ClassInfo classInfo,
            DependencyNode sourceNode
    ) {
        if (classInfo.getFields() == null) {
            return;
        }
        for (FieldInfo field : classInfo.getFields()) {
            String resolvedName = resolveTypeName(
                    field.getType(),
                    classInfo.getImports(),
                    classInfo.getPackageName()
            );
            addDependsOnEdge(graph, nodeLookup.get(resolvedName), sourceNode);
        }
    }

    private void addMethodDependencyAndCallEdges(
            Graph<DependencyNode, DependencyEdge> graph,
            Map<String, DependencyNode> nodeLookup,
            ClassInfo classInfo,
            DependencyNode sourceNode
    ) {
        for (MethodInfo methodInfo : classInfo.getMethods()) {
            DependencyNode methodNode = nodeLookup.get(getMethodId(classInfo, methodInfo));
            addMethodReturnTypeDependencyEdge(graph, nodeLookup, classInfo, methodInfo, sourceNode);
            addMethodParameterDependencyEdges(graph, nodeLookup, classInfo, methodInfo, sourceNode);
            addMethodCallEdges(graph, nodeLookup, methodInfo, methodNode);
        }
    }

    private void addMethodReturnTypeDependencyEdge(
            Graph<DependencyNode, DependencyEdge> graph,
            Map<String, DependencyNode> nodeLookup,
            ClassInfo classInfo,
            MethodInfo methodInfo,
            DependencyNode sourceNode
    ) {
        String returnType = resolveTypeName(
                methodInfo.getReturnType(),
                classInfo.getImports(),
                classInfo.getPackageName()
        );
        addDependsOnEdge(graph, nodeLookup.get(returnType), sourceNode);
    }

    private void addMethodParameterDependencyEdges(
            Graph<DependencyNode, DependencyEdge> graph,
            Map<String, DependencyNode> nodeLookup,
            ClassInfo classInfo,
            MethodInfo methodInfo,
            DependencyNode sourceNode
    ) {
        for (String paramType : methodInfo.getParameterTypes()) {
            String resolvedParamType = resolveTypeName(
                    paramType,
                    classInfo.getImports(),
                    classInfo.getPackageName()
            );
            addDependsOnEdge(graph, nodeLookup.get(resolvedParamType), sourceNode);
        }
    }

    private void addMethodCallEdges(
            Graph<DependencyNode, DependencyEdge> graph,
            Map<String, DependencyNode> nodeLookup,
            MethodInfo methodInfo,
            DependencyNode methodNode
    ) {
        if (methodInfo.getMethodCalls() == null || methodNode == null) {
            return;
        }
        for (String calledMethodName : methodInfo.getMethodCalls()) {
            for (DependencyNode possibleTarget : nodeLookup.values()) {
                if (possibleTarget.getType() == DependencyNode.NodeType.METHOD
                        && possibleTarget.getName().equals(calledMethodName)
                        && !possibleTarget.getId().equals(methodNode.getId())) {
                    graph.addEdge(methodNode, possibleTarget, new DependencyEdge(DependencyEdge.EdgeType.CALLS));
                }
            }
        }
    }

    private void addDependsOnEdge(
            Graph<DependencyNode, DependencyEdge> graph,
            DependencyNode targetNode,
            DependencyNode sourceNode
    ) {
        if (targetNode != null && !targetNode.getId().equals(sourceNode.getId())) {
            graph.addEdge(sourceNode, targetNode, new DependencyEdge(DependencyEdge.EdgeType.DEPENDS_ON));
        }
    }

    private boolean isDependencyEdge(DependencyEdge edge) {
        return edge.getType() == DependencyEdge.EdgeType.DEPENDS_ON
                || edge.getType() == DependencyEdge.EdgeType.EXTENDS
                || edge.getType() == DependencyEdge.EdgeType.IMPLEMENTS;
    }

    private DependencyNode findNodeById(Graph<DependencyNode, DependencyEdge> graph, String id) {
        return graph.vertexSet().stream()
                .filter(n -> n.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    private DependencyNode createClassNode(ClassInfo classInfo) {
        DependencyNode.NodeType type = DependencyNode.NodeType.CLASS;
        if ("INTERFACE".equals(classInfo.getClassType())) type = DependencyNode.NodeType.INTERFACE;
        if ("ENUM".equals(classInfo.getClassType())) type = DependencyNode.NodeType.ENUM;

        return DependencyNode.builder()
                .id(classInfo.getFullyQualifiedName())
                .name(classInfo.getClassName())
                .type(type)
                .build();
    }

    private DependencyNode createMethodNode(ClassInfo classInfo, MethodInfo methodInfo) {
        return DependencyNode.builder()
                .id(getMethodId(classInfo, methodInfo))
                .name(methodInfo.getName())
                .type(DependencyNode.NodeType.METHOD)
                .build();
    }

    private String getMethodId(ClassInfo classInfo, MethodInfo methodInfo) {
        return classInfo.getFullyQualifiedName() + "." + methodInfo.getName();
    }

    /**
     * Attempts to resolve a simple type name to a fully qualified name using imports.
     */
    private String resolveTypeName(String typeName, List<String> imports, String currentPackage) {
        if (typeName == null) return null;
        // Strip generics if any
        if (typeName.contains("<")) {
            typeName = typeName.substring(0, typeName.indexOf("<"));
        }
        for (String imp : imports) {
            if (imp.endsWith("." + typeName)) {
                return imp;
            }
        }
        // If not found in imports, assume it's in the same package (naive, but works for simple cases)
        if (!currentPackage.isEmpty() && !typeName.contains(".")) {
             return currentPackage + "." + typeName;
        }
        return typeName;
    }
}
