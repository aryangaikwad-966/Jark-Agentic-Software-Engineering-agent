package com.example.Jark.mcp;

import com.example.Jark.agent.tools.AstTools;
import com.example.Jark.agent.tools.GraphTools;
import com.example.Jark.agent.tools.RagTools;
import com.example.Jark.agent.tools.RepositoryTools;
import com.example.Jark.entity.CodeRepository;
import com.example.Jark.repository.CodeRepositoryRepository;
import com.example.Jark.security.AuthenticatedUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;

/**
 * Opt-in, streamable-HTTP MCP server. It intentionally exposes only repository
 * inspection, AST analysis, dependency analysis, and semantic retrieval tools;
 * build, test, and remediation remain internal approval-gated flows.
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "jark.mcp.enabled", havingValue = "true")
public class McpServerConfig {
    private static final String USER_ID = "jark.userId";

    private final RepositoryTools repositoryTools;
    private final AstTools astTools;
    private final GraphTools graphTools;
    private final RagTools ragTools;
    private final CodeRepositoryRepository repositoryRepository;
    private final ObjectMapper objectMapper;

    @Bean
    public HttpServletStreamableServerTransportProvider mcpTransportProvider() {
        return HttpServletStreamableServerTransportProvider.builder()
                .jsonMapper(McpJsonDefaults.getMapper())
                .mcpEndpoint("/mcp")
                .maxRequestSize(64 * 1024)
                .contextExtractor(this::authenticatedContext)
                .build();
    }

    @Bean
    public ServletRegistrationBean<HttpServletStreamableServerTransportProvider> mcpServlet(
            HttpServletStreamableServerTransportProvider provider) {
        return new ServletRegistrationBean<>(provider, "/mcp/*");
    }

    @Bean(destroyMethod = "close")
    public McpSyncServer jarkMcpServer(HttpServletStreamableServerTransportProvider provider) {
        return McpServer.sync(provider)
                .serverInfo("jark", "1.0.0")
                .capabilities(McpSchema.ServerCapabilities.builder().tools(true).build())
                .tools(
                        // Repository tools
                        readFileTool(),
                        listFilesTool(),
                        // AST analysis tools
                        findClassTool(),
                        findMethodTool(),
                        findAnnotationTool(),
                        findMethodAnnotationTool(),
                        // Dependency analysis tools
                        findDependenciesTool(),
                        findDependentsTool(),
                        findCallersTool(),
                        findCalleesTool(),
                        // Semantic retrieval tools
                        searchSemanticContextTool(),
                        assembleContextTool()
                )
                .build();
    }

    private McpServerFeatures.SyncToolSpecification readFileTool() {
        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool("repository_read_file", "Read a non-sensitive file from an owned repository",
                        List.of("repositoryId", "filePath"), Map.of("filePath", Map.of("type", "string"))))
                .callHandler((exchange, request) -> invoke(exchange, request.arguments(), true))
                .build();
    }

    private McpServerFeatures.SyncToolSpecification listFilesTool() {
        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool("repository_list_files", "List files in an owned repository directory",
                        List.of("repositoryId"), Map.of("directoryPath", Map.of("type", "string"))))
                .callHandler((exchange, request) -> invoke(exchange, request.arguments(), false))
                .build();
    }

    // AST Analysis Tools

    private McpServerFeatures.SyncToolSpecification findClassTool() {
        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool("ast_find_class", "Find a class by name in the repository",
                        List.of("repositoryId", "className"), Map.of("className", Map.of("type", "string"))))
                .callHandler((exchange, request) -> invokeAstTool(exchange, request.arguments(), "findClass"))
                .build();
    }

    private McpServerFeatures.SyncToolSpecification findMethodTool() {
        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool("ast_find_method", "Find methods by name across all classes in the repository",
                        List.of("repositoryId", "methodName"), Map.of("methodName", Map.of("type", "string"))))
                .callHandler((exchange, request) -> invokeAstTool(exchange, request.arguments(), "findMethod"))
                .build();
    }

    private McpServerFeatures.SyncToolSpecification findAnnotationTool() {
        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool("ast_find_annotation", "Find all classes annotated with a specific annotation",
                        List.of("repositoryId", "annotationName"), Map.of("annotationName", Map.of("type", "string"))))
                .callHandler((exchange, request) -> invokeAstTool(exchange, request.arguments(), "findAnnotation"))
                .build();
    }

    private McpServerFeatures.SyncToolSpecification findMethodAnnotationTool() {
        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool("ast_find_method_annotation", "Find all methods annotated with a specific annotation",
                        List.of("repositoryId", "annotationName"), Map.of("annotationName", Map.of("type", "string"))))
                .callHandler((exchange, request) -> invokeAstTool(exchange, request.arguments(), "findMethodAnnotation"))
                .build();
    }

    // Dependency Analysis Tools

    private McpServerFeatures.SyncToolSpecification findDependenciesTool() {
        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool("graph_find_dependencies", "Find all classes that the specified class depends on",
                        List.of("repositoryId", "classFqn"), Map.of("classFqn", Map.of("type", "string"))))
                .callHandler((exchange, request) -> invokeGraphTool(exchange, request.arguments(), "findDependencies"))
                .build();
    }

    private McpServerFeatures.SyncToolSpecification findDependentsTool() {
        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool("graph_find_dependents", "Find all classes that depend on the specified class (impact analysis)",
                        List.of("repositoryId", "classFqn"), Map.of("classFqn", Map.of("type", "string"))))
                .callHandler((exchange, request) -> invokeGraphTool(exchange, request.arguments(), "findDependents"))
                .build();
    }

    private McpServerFeatures.SyncToolSpecification findCallersTool() {
        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool("graph_find_callers", "Find all methods that call the specified method",
                        List.of("repositoryId", "methodId"), Map.of("methodId", Map.of("type", "string"))))
                .callHandler((exchange, request) -> invokeGraphTool(exchange, request.arguments(), "findCallers"))
                .build();
    }

    private McpServerFeatures.SyncToolSpecification findCalleesTool() {
        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool("graph_find_callees", "Find all methods that are called by the specified method",
                        List.of("repositoryId", "methodId"), Map.of("methodId", Map.of("type", "string"))))
                .callHandler((exchange, request) -> invokeGraphTool(exchange, request.arguments(), "findCallees"))
                .build();
    }

    // Semantic Retrieval Tools

    private McpServerFeatures.SyncToolSpecification searchSemanticContextTool() {
        Map<String, Object> properties = new java.util.HashMap<>();
        properties.put("repositoryId", Map.of("type", "integer", "minimum", 1));
        properties.put("query", Map.of("type", "string"));
        properties.put("targetClass", Map.of("type", "string"));
        properties.put("targetMethod", Map.of("type", "string"));
        properties.put("documentTypes", Map.of("type", "array", "items", Map.of("type", "string")));
        properties.put("topK", Map.of("type", "integer", "minimum", 1, "default", 5));

        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(McpSchema.Tool.builder()
                        .name("rag_search_semantic_context")
                        .description("Search for semantic context using hybrid retrieval (vector + lexical + metadata + dependency-aware)")
                        .inputSchema(McpSchema.JsonSchema.builder()
                                .type("object")
                                .properties(properties)
                                .required(List.of("repositoryId", "query"))
                                .additionalProperties(false)
                                .build())
                        .build())
                .callHandler((exchange, request) -> invokeRagTool(exchange, request.arguments(), "searchSemanticContext"))
                .build();
    }

    private McpServerFeatures.SyncToolSpecification assembleContextTool() {
        Map<String, Object> properties = new java.util.HashMap<>();
        properties.put("repositoryId", Map.of("type", "integer", "minimum", 1));
        properties.put("query", Map.of("type", "string"));
        properties.put("targetClass", Map.of("type", "string"));
        properties.put("targetMethod", Map.of("type", "string"));
        properties.put("documentTypes", Map.of("type", "array", "items", Map.of("type", "string")));
        properties.put("topK", Map.of("type", "integer", "minimum", 1, "default", 10));
        properties.put("maxTokens", Map.of("type", "integer", "minimum", 1, "default", 2000));

        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(McpSchema.Tool.builder()
                        .name("rag_assemble_context")
                        .description("Assemble semantic context with token budget management")
                        .inputSchema(McpSchema.JsonSchema.builder()
                                .type("object")
                                .properties(properties)
                                .required(List.of("repositoryId", "query"))
                                .additionalProperties(false)
                                .build())
                        .build())
                .callHandler((exchange, request) -> invokeRagTool(exchange, request.arguments(), "assembleContext"))
                .build();
    }

    private McpSchema.Tool tool(String name, String description, List<String> required, Map<String, Object> extraProperties) {
        Map<String, Object> properties = new java.util.HashMap<>(extraProperties);
        properties.put("repositoryId", Map.of("type", "integer", "minimum", 1));
        return McpSchema.Tool.builder().name(name).description(description)
                .inputSchema(McpSchema.JsonSchema.builder().type("object").properties(properties)
                        .required(required).additionalProperties(false).build())
                .build();
    }

    private McpSchema.CallToolResult invoke(io.modelcontextprotocol.server.McpSyncServerExchange exchange,
                                             Map<String, Object> arguments, boolean readFile) {
        try {
            Long repositoryId = ((Number) arguments.get("repositoryId")).longValue();
            assertOwner(exchange, repositoryId);
            Object response = readFile
                    ? repositoryTools.readFile().apply(new RepositoryTools.ReadFileRequest(repositoryId, (String) arguments.get("filePath")))
                    : repositoryTools.listFiles().apply(new RepositoryTools.ListFilesRequest(repositoryId, (String) arguments.get("directoryPath")));
            return McpSchema.CallToolResult.builder().addTextContent(objectMapper.writeValueAsString(response)).build();
        } catch (Exception exception) {
            return McpSchema.CallToolResult.builder().isError(true)
                    .addTextContent("Tool request denied or invalid").build();
        }
    }

    private McpSchema.CallToolResult invokeAstTool(io.modelcontextprotocol.server.McpSyncServerExchange exchange,
                                                   Map<String, Object> arguments, String toolName) {
        try {
            Long repositoryId = ((Number) arguments.get("repositoryId")).longValue();
            assertOwner(exchange, repositoryId);
            Object response = switch (toolName) {
                case "findClass" -> astTools.findClass().apply(new AstTools.FindClassRequest(repositoryId, (String) arguments.get("className")));
                case "findMethod" -> astTools.findMethod().apply(new AstTools.FindMethodRequest(repositoryId, (String) arguments.get("methodName")));
                case "findAnnotation" -> astTools.findAnnotation().apply(new AstTools.FindAnnotationRequest(repositoryId, (String) arguments.get("annotationName")));
                case "findMethodAnnotation" -> astTools.findMethodAnnotation().apply(new AstTools.FindMethodAnnotationRequest(repositoryId, (String) arguments.get("annotationName")));
                default -> throw new IllegalArgumentException("Unknown AST tool: " + toolName);
            };
            return McpSchema.CallToolResult.builder().addTextContent(objectMapper.writeValueAsString(response)).build();
        } catch (Exception exception) {
            return McpSchema.CallToolResult.builder().isError(true)
                    .addTextContent("AST tool request denied or invalid: " + exception.getMessage()).build();
        }
    }

    private McpSchema.CallToolResult invokeGraphTool(io.modelcontextprotocol.server.McpSyncServerExchange exchange,
                                                     Map<String, Object> arguments, String toolName) {
        try {
            Long repositoryId = ((Number) arguments.get("repositoryId")).longValue();
            assertOwner(exchange, repositoryId);
            Object response = switch (toolName) {
                case "findDependencies" -> graphTools.findDependencies().apply(new GraphTools.FindDependenciesRequest(repositoryId, (String) arguments.get("classFqn")));
                case "findDependents" -> graphTools.findDependents().apply(new GraphTools.FindDependentsRequest(repositoryId, (String) arguments.get("classFqn")));
                case "findCallers" -> graphTools.findCallers().apply(new GraphTools.FindCallersRequest(repositoryId, (String) arguments.get("methodId")));
                case "findCallees" -> graphTools.findCallees().apply(new GraphTools.FindCalleesRequest(repositoryId, (String) arguments.get("methodId")));
                default -> throw new IllegalArgumentException("Unknown Graph tool: " + toolName);
            };
            return McpSchema.CallToolResult.builder().addTextContent(objectMapper.writeValueAsString(response)).build();
        } catch (Exception exception) {
            return McpSchema.CallToolResult.builder().isError(true)
                    .addTextContent("Graph tool request denied or invalid: " + exception.getMessage()).build();
        }
    }

    private McpSchema.CallToolResult invokeRagTool(io.modelcontextprotocol.server.McpSyncServerExchange exchange,
                                                    Map<String, Object> arguments, String toolName) {
        try {
            Long repositoryId = ((Number) arguments.get("repositoryId")).longValue();
            assertOwner(exchange, repositoryId);
            Object response = switch (toolName) {
                case "searchSemanticContext" -> ragTools.searchSemanticContext().apply(new RagTools.SearchSemanticContextRequest(
                        repositoryId,
                        (String) arguments.get("query"),
                        (String) arguments.get("targetClass"),
                        (String) arguments.get("targetMethod"),
                        arguments.get("documentTypes") != null ? ((java.util.List<?>) arguments.get("documentTypes")).stream()
                                .map(Object::toString)
                                .collect(java.util.stream.Collectors.toSet()) : null,
                        arguments.get("topK") != null ? ((Number) arguments.get("topK")).intValue() : null
                ));
                case "assembleContext" -> ragTools.assembleContext().apply(new RagTools.AssembleContextRequest(
                        repositoryId,
                        (String) arguments.get("query"),
                        (String) arguments.get("targetClass"),
                        (String) arguments.get("targetMethod"),
                        arguments.get("documentTypes") != null ? ((java.util.List<?>) arguments.get("documentTypes")).stream()
                                .map(Object::toString)
                                .collect(java.util.stream.Collectors.toSet()) : null,
                        arguments.get("topK") != null ? ((Number) arguments.get("topK")).intValue() : null,
                        arguments.get("maxTokens") != null ? ((Number) arguments.get("maxTokens")).intValue() : null
                ));
                default -> throw new IllegalArgumentException("Unknown RAG tool: " + toolName);
            };
            return McpSchema.CallToolResult.builder().addTextContent(objectMapper.writeValueAsString(response)).build();
        } catch (Exception exception) {
            return McpSchema.CallToolResult.builder().isError(true)
                    .addTextContent("RAG tool request denied or invalid: " + exception.getMessage()).build();
        }
    }

    private void assertOwner(io.modelcontextprotocol.server.McpSyncServerExchange exchange, Long repositoryId) {
        Object userId = exchange.transportContext().get(USER_ID);
        if (!(userId instanceof Long id)) throw new SecurityException("Unauthenticated MCP request");
        CodeRepository repository = repositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new SecurityException("Unknown repository"));
        if (repository.getUser() == null || !id.equals(repository.getUser().getId())) throw new SecurityException("Unauthorized repository");
    }

    private McpTransportContext authenticatedContext(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return McpTransportContext.create(Map.of(USER_ID, user.id()));
        }
        return McpTransportContext.EMPTY;
    }
}
