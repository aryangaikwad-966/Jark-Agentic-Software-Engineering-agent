package com.example.Jark.dto.graph;

import org.jgrapht.graph.DefaultEdge;
import lombok.Getter;

/**
 * Represents an edge in the repository dependency graph.
 */
@Getter
public class DependencyEdge extends DefaultEdge {

    public enum EdgeType {
        DEPENDS_ON,  // Class depends on another class (field, param, etc.)
        IMPLEMENTS,  // Class implements an interface
        EXTENDS,     // Class extends another class
        CALLS,       // Method calls another method
        CONTAINS     // Class contains a method
    }

    private final EdgeType type;

    public DependencyEdge(EdgeType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "(" + getSource() + " -[" + type + "]-> " + getTarget() + ")";
    }
}
