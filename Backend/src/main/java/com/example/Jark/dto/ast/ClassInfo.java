package com.example.Jark.dto.ast;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents class-level AST metadata extracted by JavaParser.
 * Used by agent tools for deterministic structural queries.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassInfo {

    private String packageName;
    private String className;
    private String fullyQualifiedName;
    private String filePath;
    private String classType; // CLASS, INTERFACE, ENUM, RECORD, ANNOTATION
    private String accessModifier;
    private List<String> annotations;
    private List<String> implementedInterfaces;
    private String superClass;
    private List<MethodInfo> methods;
    private List<FieldInfo> fields;
    private List<String> imports;
    private int startLine;
    private int endLine;
}
