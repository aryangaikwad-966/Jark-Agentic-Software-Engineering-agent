package com.example.VoxCode.agent.tools;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import com.example.VoxCode.dto.rag.AssembledContext;
import com.example.VoxCode.dto.rag.DocumentType;
import com.example.VoxCode.dto.rag.RagQuery;
import com.example.VoxCode.dto.rag.ScoredChunk;
import com.example.VoxCode.service.rag.RagService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Spring AI tool functions for RAG-based semantic context retrieval.
 * All tools are READ-ONLY and do not modify source code.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RagTools {

    private final RagService ragService;

    /**
     * Tool function to search for semantic context using hybrid retrieval.
     */
    @Description(
            "Search for semantic context in the repository using hybrid retrieval "
                    + "(vector + lexical + metadata + dependency-aware). Returns relevant code chunks with scores."
    )
    public Function<SearchSemanticContextRequest, SearchSemanticContextResponse> searchSemanticContext() {
        return request -> {
            log.info("Tool call: searchSemanticContext with query='{}' in repository {}", 
                    request.query(), request.repositoryId());
            
            try {
                Set<DocumentType> documentTypes = request.documentTypes() != null 
                        ? request.documentTypes().stream()
                                .map(dt -> {
                                    try {
                                        return DocumentType.valueOf(dt.toUpperCase());
                                    } catch (IllegalArgumentException e) {
                                        log.warn("Unknown document type: {}, skipping", dt);
                                        return null;
                                    }
                                })
                                .filter(Objects::nonNull)
                                .collect(Collectors.toSet())
                        : null;
                
                RagQuery ragQuery = RagQuery.builder()
                        .repositoryId(request.repositoryId())
                        .query(request.query())
                        .targetClass(request.targetClass())
                        .targetMethod(request.targetMethod())
                        .documentTypes(documentTypes)
                        .topK(request.topK() != null ? request.topK() : 5)
                        .build();
                
                var retrievedChunks = ragService.retrieve(ragQuery);
                
                return successSearchSemanticContext(retrievedChunks);
            } catch (Exception e) {
                log.error("Error in searchSemanticContext tool", e);
                return errorSearchSemanticContext(e.getMessage());
            }
        };
    }

    /**
     * Tool function to assemble context with token budget management.
     */
    @Description(
            "Assemble semantic context with token budget management. Returns an optimized context "
                    + "that fits within the specified token limit while maximizing relevance."
    )
    public Function<AssembleContextRequest, AssembleContextResponse> assembleContext() {
        return request -> {
            log.info("Tool call: assembleContext with query='{}', maxTokens={} in repository {}", 
                    request.query(), request.maxTokens(), request.repositoryId());
            
            try {
                Set<DocumentType> documentTypes = request.documentTypes() != null 
                        ? request.documentTypes().stream()
                                .map(dt -> {
                                    try {
                                        return DocumentType.valueOf(dt.toUpperCase());
                                    } catch (IllegalArgumentException e) {
                                        log.warn("Unknown document type: {}, skipping", dt);
                                        return null;
                                    }
                                })
                                .filter(Objects::nonNull)
                                .collect(Collectors.toSet())
                        : null;
                
                RagQuery ragQuery = RagQuery.builder()
                        .repositoryId(request.repositoryId())
                        .query(request.query())
                        .targetClass(request.targetClass())
                        .targetMethod(request.targetMethod())
                        .documentTypes(documentTypes)
                        .topK(request.topK() != null ? request.topK() : 10)
                        .build();
                
                int maxTokens = request.maxTokens() != null ? request.maxTokens() : 2000;
                AssembledContext context = ragService.assembleContext(ragQuery, maxTokens);
                
                return successAssembleContext(context);
            } catch (Exception e) {
                log.error("Error in assembleContext tool", e);
                return errorAssembleContext(e.getMessage());
            }
        };
    }

    // Request/Response DTOs for tool functions

    public record SearchSemanticContextRequest(
            Long repositoryId,
            String query,
            String targetClass,
            String targetMethod,
            Set<String> documentTypes,
            Integer topK
    ) {}

    public record SearchSemanticContextResponse(
            boolean success,
            String message,
            List<ScoredChunk> chunks
    ) {}

    public record AssembleContextRequest(
            Long repositoryId,
            String query,
            String targetClass,
            String targetMethod,
            Set<String> documentTypes,
            Integer topK,
            Integer maxTokens
    ) {}

    public record AssembleContextResponse(
            boolean success,
            String message,
            AssembledContext context
    ) {}

    // Helper methods for creating responses
    private static SearchSemanticContextResponse successSearchSemanticContext(List<ScoredChunk> chunks) {
        return new SearchSemanticContextResponse(true, "Semantic context retrieved successfully", chunks);
    }

    private static SearchSemanticContextResponse errorSearchSemanticContext(String message) {
        return new SearchSemanticContextResponse(false, message, Collections.emptyList());
    }

    private static AssembleContextResponse successAssembleContext(AssembledContext context) {
        return new AssembleContextResponse(true, "Context assembled successfully", context);
    }

    private static AssembleContextResponse errorAssembleContext(String message) {
        return new AssembleContextResponse(false, message, null);
    }
}