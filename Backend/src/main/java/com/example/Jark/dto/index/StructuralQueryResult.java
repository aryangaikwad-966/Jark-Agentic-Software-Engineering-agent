package com.example.Jark.dto.index;

import com.example.Jark.dto.ast.ClassInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Result of a structural (AST-based) query against the repository index.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StructuralQueryResult {

    private String queryType;
    private List<ClassInfo> classes;
    private int totalCount;

    public static StructuralQueryResult of(String queryType, List<ClassInfo> classes) {
        return StructuralQueryResult.builder()
                .queryType(queryType)
                .classes(classes)
                .totalCount(classes.size())
                .build();
    }
}
