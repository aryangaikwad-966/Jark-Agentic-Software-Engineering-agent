package com.example.Jark.dto.ast;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents method-level AST metadata extracted by JavaParser.
 * Used by agent tools for deterministic structural queries.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MethodInfo {

    private String name;
    private String returnType;
    private List<String> parameterTypes;
    private List<String> parameterNames;
    private List<String> annotations;
    private List<String> methodCalls;
    private String accessModifier;
    private boolean isStatic;
    private boolean isAbstract;
    private int startLine;
    private int endLine;
}
