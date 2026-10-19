package com.example.VoxCode.agent.tools;

import java.util.function.Function;

import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import com.example.VoxCode.dto.index.RepositoryIndex;
import com.example.VoxCode.dto.index.StructuralQueryResult;
import com.example.VoxCode.service.RepositoryIndexService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Spring AI tool functions for AST-based structural queries.
 * All tools are READ-ONLY and do not modify source code.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AstTools {

    private final RepositoryIndexService repositoryIndexService;

    /**
     * Tool function to find a class by name in the repository.
     */
    @Description(
            "Find a class by its simple name in the repository. Returns class information "
                    + "including package, file path, annotations, methods, and fields."
    )
    public Function<FindClassRequest, FindClassResponse> findClass() {
        return request -> {
            log.info("Tool call: findClass with className='{}' in repository {}", 
                    request.className(), request.repositoryId());
            
            try {
                RepositoryIndex index = getRepositoryIndex(request.repositoryId());
                StructuralQueryResult result = repositoryIndexService.findClass(index, request.className());
                
                return successFindClass(result);
            } catch (Exception e) {
                log.error("Error in findClass tool", e);
                return errorFindClass(e.getMessage());
            }
        };
    }

    /**
     * Tool function to find methods by name in the repository.
     */
    @Description("Find methods by name across all classes in the repository. Returns class information containing only the matching methods.")
    public Function<FindMethodRequest, FindMethodResponse> findMethod() {
        return request -> {
            log.info("Tool call: findMethod with methodName='{}' in repository {}", 
                    request.methodName(), request.repositoryId());
            
            try {
                RepositoryIndex index = getRepositoryIndex(request.repositoryId());
                StructuralQueryResult result = repositoryIndexService.findMethodByName(index, request.methodName());
                
                return successFindMethod(result);
            } catch (Exception e) {
                log.error("Error in findMethod tool", e);
                return errorFindMethod(e.getMessage());
            }
        };
    }

    /**
     * Tool function to find classes annotated with a specific annotation.
     */
    @Description(
            "Find all classes annotated with a specific annotation name "
                    + "(e.g., 'RestController', 'Service'). Returns matching class information."
    )
    public Function<FindAnnotationRequest, FindAnnotationResponse> findAnnotation() {
        return request -> {
            log.info("Tool call: findAnnotation with annotationName='{}' in repository {}", 
                    request.annotationName(), request.repositoryId());
            
            try {
                RepositoryIndex index = getRepositoryIndex(request.repositoryId());
                StructuralQueryResult result = repositoryIndexService.findClassesWithAnnotation(index, request.annotationName());
                
                return successFindAnnotation(result);
            } catch (Exception e) {
                log.error("Error in findAnnotation tool", e);
                return errorFindAnnotation(e.getMessage());
            }
        };
    }

    /**
     * Tool function to find methods annotated with a specific annotation.
     */
    @Description(
            "Find all methods annotated with a specific annotation name "
                    + "(e.g., 'GetMapping', 'PostMapping'). Returns class information containing only matching methods."
    )
    public Function<FindMethodAnnotationRequest, FindMethodAnnotationResponse> findMethodAnnotation() {
        return request -> {
            log.info("Tool call: findMethodAnnotation with annotationName='{}' in repository {}", 
                    request.annotationName(), request.repositoryId());
            
            try {
                RepositoryIndex index = getRepositoryIndex(request.repositoryId());
                StructuralQueryResult result = repositoryIndexService.findMethodsWithAnnotation(index, request.annotationName());
                
                return successFindMethodAnnotation(result);
            } catch (Exception e) {
                log.error("Error in findMethodAnnotation tool", e);
                return errorFindMethodAnnotation(e.getMessage());
            }
        };
    }

    private RepositoryIndex getRepositoryIndex(Long repositoryId) {
        // In a real implementation, this would load the index from a cache or database
        // For now, we'll throw an exception indicating this needs to be implemented
        throw new UnsupportedOperationException("Repository index loading not yet implemented. Need to integrate with repository indexing service.");
    }

    // Request/Response DTOs for tool functions

    public record FindClassRequest(
            Long repositoryId,
            String className
    ) {}

    public record FindClassResponse(
            boolean success,
            String message,
            StructuralQueryResult result
    ) {}

    public record FindMethodRequest(
            Long repositoryId,
            String methodName
    ) {}

    public record FindMethodResponse(
            boolean success,
            String message,
            StructuralQueryResult result
    ) {}

    public record FindAnnotationRequest(
            Long repositoryId,
            String annotationName
    ) {}

    public record FindAnnotationResponse(
            boolean success,
            String message,
            StructuralQueryResult result
    ) {}

    public record FindMethodAnnotationRequest(
            Long repositoryId,
            String annotationName
    ) {}

    public record FindMethodAnnotationResponse(
            boolean success,
            String message,
            StructuralQueryResult result
    ) {}

    // Helper methods for creating responses
    private static FindClassResponse successFindClass(StructuralQueryResult result) {
        return new FindClassResponse(true, "Class found successfully", result);
    }

    private static FindClassResponse errorFindClass(String message) {
        return new FindClassResponse(false, message, null);
    }

    private static FindMethodResponse successFindMethod(StructuralQueryResult result) {
        return new FindMethodResponse(true, "Method found successfully", result);
    }

    private static FindMethodResponse errorFindMethod(String message) {
        return new FindMethodResponse(false, message, null);
    }

    private static FindAnnotationResponse successFindAnnotation(StructuralQueryResult result) {
        return new FindAnnotationResponse(true, "Annotation found successfully", result);
    }

    private static FindAnnotationResponse errorFindAnnotation(String message) {
        return new FindAnnotationResponse(false, message, null);
    }

    private static FindMethodAnnotationResponse successFindMethodAnnotation(StructuralQueryResult result) {
        return new FindMethodAnnotationResponse(true, "Method annotation found successfully", result);
    }

    private static FindMethodAnnotationResponse errorFindMethodAnnotation(String message) {
        return new FindMethodAnnotationResponse(false, message, null);
    }
}