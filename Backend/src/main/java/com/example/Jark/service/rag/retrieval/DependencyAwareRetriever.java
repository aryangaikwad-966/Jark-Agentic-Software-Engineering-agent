package com.example.Jark.service.rag.retrieval;

import com.example.Jark.dto.graph.DependencyNode;
import com.example.Jark.dto.index.GraphQueryResult;
import com.example.Jark.dto.index.RepositoryIndex;
import com.example.Jark.dto.rag.CodeChunk;
import com.example.Jark.dto.rag.RagQuery;
import com.example.Jark.dto.rag.ScoredChunk;
import com.example.Jark.service.RepositoryIndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Retrieves structurally connected code chunks by traversing the dependency graph
 * from the unified RepositoryIndex.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DependencyAwareRetriever {

    private final RepositoryIndexService repositoryIndexService;

    /**
     * Retrieves code chunks of components directly related (dependencies and dependents)
     * to classes mentioned in the query or focal chunks.
     *
     * @param chunks repository code chunks
     * @param index repository index containing the dependency graph
     * @param query search query
     * @param focalClasses primary classes identified from query or top vector/lexical matches
     * @return list of ScoredChunks representing related components
     */
    public List<ScoredChunk> retrieve(
            List<CodeChunk> chunks,
            RepositoryIndex index,
            RagQuery query,
            Set<String> focalClasses) {

        if (chunks == null || chunks.isEmpty() || index == null) {
            return Collections.emptyList();
        }

        Set<String> targetClasses = new HashSet<>();
        if (query.getTargetClass() != null && !query.getTargetClass().isBlank()) {
            targetClasses.add(query.getTargetClass());
        }
        if (focalClasses != null) {
            targetClasses.addAll(focalClasses);
        }

        // Also detect class names mentioned in the query
        String rawQuery = query.getQuery() != null ? query.getQuery().toLowerCase(Locale.ROOT) : "";
        for (CodeChunk chunk : chunks) {
            if (chunk.getClassName() != null) {
                String simpleName = getSimpleName(chunk.getClassName()).toLowerCase(Locale.ROOT);
                if (simpleName.length() > 2 && rawQuery.contains(simpleName)) {
                    targetClasses.add(chunk.getClassName());
                }
            }
        }

        if (targetClasses.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, Double> connectedClassScores = resolveConnectedClassScores(targetClasses, index);

        List<ScoredChunk> results = new ArrayList<>();
        for (CodeChunk chunk : chunks) {
            if (chunk.getClassName() == null) {
                continue;
            }

            // Exclude the focal classes themselves (they are retrieved by primary channels)
            if (targetClasses.contains(chunk.getClassName())
                    || targetClasses.contains(getSimpleName(chunk.getClassName()))) {
                continue;
            }

            double score = matchConnectedClass(chunk.getClassName(), connectedClassScores);
            if (score > 0.0 && score >= query.getMinScore()) {
                Map<String, Double> channelScores = new HashMap<>();
                channelScores.put("dependency", score);
                results.add(ScoredChunk.builder()
                        .chunk(chunk)
                        .finalScore(score)
                        .channelScores(channelScores)
                        .primarySource("DEPENDENCY")
                        .build());
            }
        }

        Collections.sort(results);
        int limit = Math.min(query.getTopK(), results.size());
        return results.subList(0, limit);
    }

    private double matchConnectedClass(String className, Map<String, Double> connectedMap) {
        if (connectedMap.containsKey(className)) {
            return connectedMap.get(className);
        }
        String simple = getSimpleName(className);
        for (Map.Entry<String, Double> entry : connectedMap.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(className) || entry.getKey().endsWith("." + simple)) {
                return entry.getValue();
            }
        }
        return 0.0;
    }

    private String getSimpleName(String className) {
        return className.contains(".") ? className.substring(className.lastIndexOf('.') + 1) : className;
    }

    private String resolveFqcn(String target, RepositoryIndex index) {
        if (target.contains(".")) {
            return target;
        }
        for (com.example.Jark.dto.ast.ClassInfo ci : index.getClasses()) {
            if (ci.getClassName().equalsIgnoreCase(target)) {
                return ci.getFullyQualifiedName();
            }
        }
        return target;
    }

    private Map<String, Double> resolveConnectedClassScores(Set<String> targetClasses, RepositoryIndex index) {
        Map<String, Double> connectedClassScores = new HashMap<>();
        for (String target : targetClasses) {
            try {
                String fqcn = resolveFqcn(target, index);
                GraphQueryResult deps = repositoryIndexService.findDependencies(index, fqcn);
                for (DependencyNode node : deps.getNodes()) {
                    if (node.getType() == DependencyNode.NodeType.CLASS || node.getType() == DependencyNode.NodeType.INTERFACE) {
                        connectedClassScores.put(node.getName(), 0.85);
                        if (node.getId() != null) {
                            connectedClassScores.put(node.getId(), 0.85);
                        }
                    }
                }

                GraphQueryResult dependents = repositoryIndexService.findDependents(index, fqcn);
                for (DependencyNode node : dependents.getNodes()) {
                    if (node.getType() == DependencyNode.NodeType.CLASS || node.getType() == DependencyNode.NodeType.INTERFACE) {
                        connectedClassScores.put(node.getName(), 0.75);
                        if (node.getId() != null) {
                            connectedClassScores.put(node.getId(), 0.75);
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("Could not resolve dependencies for class {}: {}", target, e.getMessage());
            }
        }
        return connectedClassScores;
    }
}
