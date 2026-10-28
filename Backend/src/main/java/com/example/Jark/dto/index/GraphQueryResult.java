package com.example.Jark.dto.index;

import com.example.Jark.dto.graph.DependencyEdge;
import com.example.Jark.dto.graph.DependencyNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Result of a graph (dependency) query against the repository index.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphQueryResult {

    private String queryType;
    private String sourceId;
    private List<DependencyNode> nodes;
    private List<GraphRelationship> relationships;
    private int totalCount;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GraphRelationship {
        private String sourceId;
        private String targetId;
        private DependencyEdge.EdgeType edgeType;
    }

    public static GraphQueryResult of(String queryType, String sourceId,
                                      List<DependencyNode> nodes,
                                      List<GraphRelationship> relationships) {
        return GraphQueryResult.builder()
                .queryType(queryType)
                .sourceId(sourceId)
                .nodes(nodes)
                .relationships(relationships)
                .totalCount(nodes.size())
                .build();
    }
}
