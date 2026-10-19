package com.example.Jark.agent.config;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.Jark.agent.tools.AstTools;
import com.example.Jark.agent.tools.GraphTools;
import com.example.Jark.agent.tools.RagTools;
import com.example.Jark.agent.tools.RepositoryTools;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Configuration for Spring AI agent tools.
 * Registers all READ-ONLY investigation tools for the LLM to use.
 * All tools enforce strict security boundaries and do not modify source code.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AgentToolConfig {

    private final AstTools astTools;
    private final GraphTools graphTools;
    private final RagTools ragTools;
    private final RepositoryTools repositoryTools;

    /**
     * Registers all AST-based structural query tools.
     * These tools provide deterministic class, method, and annotation information.
     */
    @Bean
    public List<Function<?, ?>> astToolFunctions() {
        log.info("Registering AST investigation tools");
        return List.of(
                astTools.findClass(),
                astTools.findMethod(),
                astTools.findAnnotation(),
                astTools.findMethodAnnotation()
        );
    }

    /**
     * Registers all dependency graph query tools.
     * These tools provide dependency analysis and impact analysis capabilities.
     */
    @Bean
    public List<Function<?, ?>> graphToolFunctions() {
        log.info("Registering Graph investigation tools");
        return List.of(
                graphTools.findDependencies(),
                graphTools.findDependents(),
                graphTools.findCallers(),
                graphTools.findCallees()
        );
    }

    /**
     * Registers all RAG-based semantic retrieval tools.
     * These tools provide semantic context and hybrid retrieval capabilities.
     */
    @Bean
    public List<Function<?, ?>> ragToolFunctions() {
        log.info("Registering RAG investigation tools");
        return List.of(
                ragTools.searchSemanticContext(),
                ragTools.assembleContext()
        );
    }

    /**
     * Registers all repository file operation tools.
     * These tools provide READ-ONLY file access with strict security boundaries.
     */
    @Bean
    public List<Function<?, ?>> repositoryToolFunctions() {
        log.info("Registering Repository investigation tools");
        return List.of(
                repositoryTools.readFile(),
                repositoryTools.listFiles()
        );
    }

    /**
     * Consolidated list of all investigation tools.
     * This is the main bean that Spring AI will use to discover available tools.
     */
    @Bean
    public List<Function<?, ?>> allInvestigationTools() {
        List<Function<?, ?>> allTools = new ArrayList<>();
        allTools.addAll(astToolFunctions());
        allTools.addAll(graphToolFunctions());
        allTools.addAll(ragToolFunctions());
        allTools.addAll(repositoryToolFunctions());
        
        log.info("Registered {} total investigation tools for agent", allTools.size());
        return allTools;
    }
}