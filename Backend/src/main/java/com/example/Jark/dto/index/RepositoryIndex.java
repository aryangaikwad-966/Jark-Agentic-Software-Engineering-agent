package com.example.Jark.dto.index;

import com.example.Jark.dto.ast.ClassInfo;
import com.example.Jark.dto.graph.DependencyEdge;
import com.example.Jark.dto.graph.DependencyNode;
import lombok.Getter;
import org.jgrapht.Graph;

import java.nio.file.Path;
import java.util.List;

/**
 * Unified in-memory index combining AST structural data and the dependency graph.
 * Built once per workspace and reused for subsequent queries.
 */
@Getter
public class RepositoryIndex {

    private final Path workspacePath;
    private final List<ClassInfo> classes;
    private final Graph<DependencyNode, DependencyEdge> dependencyGraph;
    private final int classCount;
    private final int nodeCount;
    private final int edgeCount;

    public RepositoryIndex(Path workspacePath,
                           List<ClassInfo> classes,
                           Graph<DependencyNode, DependencyEdge> dependencyGraph) {
        this.workspacePath = workspacePath;
        this.classes = List.copyOf(classes);
        this.dependencyGraph = dependencyGraph;
        this.classCount = classes.size();
        this.nodeCount = dependencyGraph.vertexSet().size();
        this.edgeCount = dependencyGraph.edgeSet().size();
    }
}
