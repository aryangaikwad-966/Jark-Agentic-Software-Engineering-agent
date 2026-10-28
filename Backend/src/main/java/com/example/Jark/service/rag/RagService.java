package com.example.Jark.service.rag;

import com.example.Jark.dto.rag.AssembledContext;
import com.example.Jark.dto.rag.CodeChunk;
import com.example.Jark.dto.rag.RagQuery;
import com.example.Jark.dto.rag.ScoredChunk;

import java.nio.file.Path;
import java.util.List;

/**
 * Primary interface for hybrid repository-aware RAG operations.
 */
public interface RagService {

    /**
     * Parses, indexes, and embeds repository content into the vector store
     * and in-memory lexical and structural caches.
     *
     * @param repositoryId unique identifier of the repository
     * @param workspacePath local path to the repository root
     * @return list of generated CodeChunks
     */
    List<CodeChunk> indexRepository(Long repositoryId, Path workspacePath);

    /**
     * Executes hybrid retrieval across vector, lexical, symbol, and dependency channels,
     * reranks the results with RRF, and returns the top-k scored chunks.
     *
     * @param query search query parameters
     * @return list of ScoredChunks sorted by relevance
     */
    List<ScoredChunk> retrieve(RagQuery query);

    /**
     * Performs hybrid retrieval and constructs a prompt-ready markdown context
     * respecting token limits and multi-source diversity.
     *
     * @param query search query parameters
     * @param maxTokens maximum context token budget
     * @return AssembledContext ready for LLM prompt injection
     */
    AssembledContext assembleContext(RagQuery query, int maxTokens);

    /**
     * Retrieves all in-memory indexed chunks for a given repository.
     *
     * @param repositoryId repository identifier
     * @return list of chunks currently in memory
     */
    List<CodeChunk> getIndexedChunks(Long repositoryId);
}
