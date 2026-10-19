package com.example.Jark.dto.ast;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents field-level AST metadata extracted by JavaParser.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldInfo {

    private String name;
    private String type;
    private String accessModifier;
    private List<String> annotations;
    private boolean isStatic;
    private boolean isFinal;
    private int lineNumber;
}
