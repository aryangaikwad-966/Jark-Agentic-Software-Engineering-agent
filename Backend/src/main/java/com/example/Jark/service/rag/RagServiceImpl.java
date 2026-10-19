package com.example.Jark.service.rag;

import com.example.Jark.dto.index.RepositoryIndex;
import com.example.Jark.dto.rag.AssembledContext;
import com.example.Jark.dto.rag.CodeChunk;
import com.example.Jark.dto.rag.RagQuery;
import com.example.Jark.dto.rag.ScoredChunk;
import com.example.Jark.service.RepositoryIndexService;
import com.example.Jark.service.rag.retrieval.DependencyAwareRetriever;
import com.example.Jark.service.rag.retrieval.LexicalRetriever;
import com.example.Jark.service.rag.retrieval.SymbolRetriever;
import com.example.Jark.service.rag.retrieval.VectorRetriever;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of RagService coordinating ingestion, 4-channel hybrid retrieval,
 * RRF reranking, and context assembly.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagServiceImpl implements RagService {

    private final DocumentParserService documentParserService;
    private final VectorRetriever vectorRetriever;
    private final LexicalRetriever lexicalRetriever;
    private final SymbolRetriever symbolRetriever;
    private final DependencyAwareRetriever dependencyAwareRetriever;
    private final RerankerService rerankerService;
    private final ContextAssemblerService contextAssemblerService;
    private final RepositoryIndexService repositoryIndexService;

    private final Map<Long, List<CodeChunk>> indexedChunks = new ConcurrentHashMap<>();
    private final Map<Long, Map<String, CodeChunk>> chunkLookup = new ConcurrentHashMap<>();
    private final Map<Long, RepositoryIndex> indexedRepositories = new ConcurrentHashMap<>();

    @Override
    public List<CodeChunk> indexRepository(Long repositoryId, Path workspacePath) {
        log.info("Indexing repository {} at path {}", repositoryId, workspacePath);

        List<CodeChunk> chunks = documentParserService.parseRepository(repositoryId, workspacePath);
        RepositoryIndex repoIndex = repositoryIndexService.buildIndex(workspacePath);

        indexedChunks.put(repositoryId, List.copyOf(chunks));
        indexedRepositories.put(repositoryId, repoIndex);

        Map<String, CodeChunk> lookup = new HashMap<>();
        for (CodeChunk chunk : chunks) {
            lookup.put(chunk.getId(), chunk);
        }
        chunkLookup.put(repositoryId, lookup);

        // Populate vector store
        try {
            vectorRetriever.indexChunks(chunks);
        } catch (Exception e) {
            log.warn("Vector store indexing encountered issue: {}", e.getMessage());
        }

        log.info("Repository {} successfully indexed: {} chunks", repositoryId, chunks.size());
        return chunks;
    }

    @Override
    public List<ScoredChunk> retrieve(RagQuery query) {
        Long repositoryId = query.getRepositoryId();
        List<CodeChunk> chunks = repositoryId != null
                ? indexedChunks.getOrDefault(repositoryId, Collections.emptyList())
                : getAllIndexedChunks();

        Map<String, CodeChunk> lookup = repositoryId != null
                ? chunkLookup.getOrDefault(repositoryId, Collections.emptyMap())
                : getAllChunkLookup();

        RepositoryIndex repoIndex = repositoryId != null ? indexedRepositories.get(repositoryId) : null;

        if (chunks.isEmpty()) {
            log.warn("No indexed chunks found for repository {}", repositoryId);
            return Collections.emptyList();
        }

        // 1. Vector Semantic Retrieval
        List<ScoredChunk> vectorResults = vectorRetriever.retrieve(query, lookup);

        // 2. Lexical BM25 Retrieval
        List<ScoredChunk> lexicalResults = lexicalRetriever.retrieve(chunks, query);

        // 3. Symbol / AST Metadata Retrieval
        List<ScoredChunk> symbolResults = symbolRetriever.retrieve(chunks, query);

        // 4. Dependency-Aware Retrieval
        Set<String> focalClasses = extractFocalClasses(vectorResults, lexicalResults, symbolResults);
        List<ScoredChunk> dependencyResults = (repoIndex != null)
                ? dependencyAwareRetriever.retrieve(chunks, repoIndex, query, focalClasses)
                : Collections.emptyList();

        // 5. RRF Fusion & Reranking
        return rerankerService.rerank(query, vectorResults, lexicalResults, symbolResults, dependencyResults);
    }

    @Override
    public AssembledContext assembleContext(RagQuery query, int maxTokens) {
        List<ScoredChunk> candidates = retrieve(query);
        return contextAssemblerService.assemble(candidates, maxTokens);
    }

    @Override
    public List<CodeChunk> getIndexedChunks(Long repositoryId) {
        return indexedChunks.getOrDefault(repositoryId, Collections.emptyList());
    }

    private Set<String> extractFocalClasses(
            List<ScoredChunk> vectorHits,
            List<ScoredChunk> lexicalHits,
            List<ScoredChunk> symbolHits) {
        Set<String> classes = new HashSet<>();
        if (symbolHits != null && !symbolHits.isEmpty() && symbolHits.get(0).getChunk().getClassName() != null) {
            classes.add(symbolHits.get(0).getChunk().getClassName());
            return classes;
        }
        if (lexicalHits != null && !lexicalHits.isEmpty() && lexicalHits.get(0).getChunk().getClassName() != null) {
            classes.add(lexicalHits.get(0).getChunk().getClassName());
            return classes;
        }
        if (vectorHits != null && !vectorHits.isEmpty() && vectorHits.get(0).getChunk().getClassName() != null) {
            classes.add(vectorHits.get(0).getChunk().getClassName());
            return classes;
        }
        return classes;
    }

    private void addTopClasses(Set<String> target, List<ScoredChunk> hits, int limit) {
        if (hits == null) {
            return;
        }
        int count = 0;
        for (ScoredChunk sc : hits) {
            if (sc.getChunk().getClassName() != null && !sc.getChunk().getClassName().isBlank()) {
                target.add(sc.getChunk().getClassName());
                count++;
                if (count >= limit) {
                    break;
                }
            }
        }
    }

    private List<CodeChunk> getAllIndexedChunks() {
        return indexedChunks.values().stream()
                .flatMap(List::stream)
                .toList();
    }

    private Map<String, CodeChunk> getAllChunkLookup() {
        Map<String, CodeChunk> all = new HashMap<>();
        for (Map<String, CodeChunk> map : chunkLookup.values()) {
            all.putAll(map);
        }
        return all;
    }
}
