package com.example.Jark.dto.graph;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

/**
 * Represents a node in the repository dependency graph.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DependencyNode {

    public enum NodeType {
        CLASS, INTERFACE, ENUM, METHOD
    }

    /**
     * Unique identifier for the node.
     * For classes: Fully qualified class name.
     * For methods: Fully qualified class name + "." + method signature.
     */
    private String id;

    private String name;

    private NodeType type;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DependencyNode that = (DependencyNode) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
