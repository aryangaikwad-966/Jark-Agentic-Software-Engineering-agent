package com.example.VoxCode.agent.tools;

import com.example.VoxCode.dto.index.GraphQueryResult;
import com.example.VoxCode.dto.index.RepositoryIndex;
import com.example.VoxCode.service.RepositoryIndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import java.util.function.Function;

/**
 * Spring AI tool functions for dependency graph queries.
 * All tools are READ-ONLY and do not modify source code.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GraphTools {

    private final RepositoryIndexService repositoryIndexService;

    /**
     * Tool function to find dependencies of a class.
     */
    @Description("Find all classes that the specified class depends on (dependencies). Returns dependency nodes and relationship information.")
    public Function<FindDependenciesRequest, FindDependenciesResponse> findDependencies() {
        return request -> {
            log.info("Tool call: findDependencies for classFqn='{}' in repository {}", 
                    request.classFqn(), request.repositoryId());
            
            try {
                RepositoryIndex index = getRepositoryIndex(request.repositoryId());
                GraphQueryResult result = repositoryIndexService.findDependencies(index, request.classFqn());
                
                return successFindDependencies(result);
            } catch (Exception e) {
                log.error("Error in findDependencies tool", e);
                return errorFindDependencies(e.getMessage());
            }
        };
    }

    /**
     * Tool function to find dependents of a class (impact analysis).
     */
    @Description(
            "Find all classes that depend on the specified class (impact analysis). Returns "
                    + "dependent nodes and relationship information for understanding change impact."
    )
    public Function<FindDependentsRequest, FindDependentsResponse> findDependents() {
        return request -> {
            log.info("Tool call: findDependents for classFqn='{}' in repository {}", 
                    request.classFqn(), request.repositoryId());
            
            try {
                RepositoryIndex index = getRepositoryIndex(request.repositoryId());
                GraphQueryResult result = repositoryIndexService.findDependents(index, request.classFqn());
                
                return successFindDependents(result);
            } catch (Exception e) {
                log.error("Error in findDependents tool", e);
                return errorFindDependents(e.getMessage());
            }
        };
    }

    /**
     * Tool function to find callers of a method.
     */
    @Description(
            "Find all methods that call the specified method. Returns caller nodes and "
                    + "relationship information for understanding method call chains."
    )
    public Function<FindCallersRequest, FindCallersResponse> findCallers() {
        return request -> {
            log.info("Tool call: findCallers for methodId='{}' in repository {}", 
                    request.methodId(), request.repositoryId());
            
            try {
                RepositoryIndex index = getRepositoryIndex(request.repositoryId());
                GraphQueryResult result = repositoryIndexService.findCallers(index, request.methodId());
                
                return successFindCallers(result);
            } catch (Exception e) {
                log.error("Error in findCallers tool", e);
                return errorFindCallers(e.getMessage());
            }
        };
    }

    /**
     * Tool function to find callees of a method.
     */
    @Description(
            "Find all methods that are called by the specified method. Returns callee nodes "
                    + "and relationship information for understanding method call chains."
    )
    public Function<FindCalleesRequest, FindCalleesResponse> findCallees() {
        return request -> {
            log.info("Tool call: findCallees for methodId='{}' in repository {}", 
                    request.methodId(), request.repositoryId());
            
            try {
                RepositoryIndex index = getRepositoryIndex(request.repositoryId());
                GraphQueryResult result = repositoryIndexService.findCallees(index, request.methodId());
                
                return successFindCallees(result);
            } catch (Exception e) {
                log.error("Error in findCallees tool", e);
                return errorFindCallees(e.getMessage());
            }
        };
    }

    private RepositoryIndex getRepositoryIndex(Long repositoryId) {
        // In a real implementation, this would load the index from a cache or database
        // For now, we'll throw an exception indicating this needs to be implemented
        throw new UnsupportedOperationException("Repository index loading not yet implemented. Need to integrate with repository indexing service.");
    }

    // Request/Response DTOs for tool functions

    public record FindDependenciesRequest(
            Long repositoryId,
            String classFqn
    ) {}

    public record FindDependenciesResponse(
            boolean success,
            String message,
            GraphQueryResult result
    ) {}

    public record FindDependentsRequest(
            Long repositoryId,
            String classFqn
    ) {}

    public record FindDependentsResponse(
            boolean success,
            String message,
            GraphQueryResult result
    ) {}

    public record FindCallersRequest(
            Long repositoryId,
            String methodId
    ) {}

    public record FindCallersResponse(
            boolean success,
            String message,
            GraphQueryResult result
    ) {}

    public record FindCalleesRequest(
            Long repositoryId,
            String methodId
    ) {}

    public record FindCalleesResponse(
            boolean success,
            String message,
            GraphQueryResult result
    ) {}

    // Helper methods for creating responses
    private static FindDependenciesResponse successFindDependencies(GraphQueryResult result) {
        return new FindDependenciesResponse(true, "Dependencies found successfully", result);
    }

    private static FindDependenciesResponse errorFindDependencies(String message) {
        return new FindDependenciesResponse(false, message, null);
    }

    private static FindDependentsResponse successFindDependents(GraphQueryResult result) {
        return new FindDependentsResponse(true, "Dependents found successfully", result);
    }

    private static FindDependentsResponse errorFindDependents(String message) {
        return new FindDependentsResponse(false, message, null);
    }

    private static FindCallersResponse successFindCallers(GraphQueryResult result) {
        return new FindCallersResponse(true, "Callers found successfully", result);
    }

    private static FindCallersResponse errorFindCallers(String message) {
        return new FindCallersResponse(false, message, null);
    }

    private static FindCalleesResponse successFindCallees(GraphQueryResult result) {
        return new FindCalleesResponse(true, "Callees found successfully", result);
    }

    private static FindCalleesResponse errorFindCallees(String message) {
        return new FindCalleesResponse(false, message, null);
    }
}