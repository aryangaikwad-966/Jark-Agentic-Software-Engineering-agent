package com.example.Jark.mcp;

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
 * inspection tools; build, test, and remediation remain internal approval-gated flows.
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "jark.mcp.enabled", havingValue = "true")
public class McpServerConfig {
    private static final String USER_ID = "jark.userId";

    private final RepositoryTools repositoryTools;
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
                .tools(readFileTool(), listFilesTool())
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
