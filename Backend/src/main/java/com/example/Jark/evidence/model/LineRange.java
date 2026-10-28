package com.example.Jark.evidence.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Immutable value object representing a 1-based line range in a source file.
 */
public record LineRange(
        @JsonProperty("startLine") int startLine,
        @JsonProperty("endLine") int endLine
) {

    @JsonCreator
    public LineRange {
        if (startLine <= 0) {
            throw new IllegalArgumentException("startLine must be positive, got: " + startLine);
        }
        if (endLine < startLine) {
            throw new IllegalArgumentException("endLine (" + endLine + ") cannot be less than startLine (" + startLine + ")");
        }
    }

    public static LineRange of(int startLine, int endLine) {
        return new LineRange(startLine, endLine);
    }

    public static LineRange singleLine(int line) {
        return new LineRange(line, line);
    }

    public int lineCount() {
        return endLine - startLine + 1;
    }

    public boolean contains(int line) {
        return line >= startLine && line <= endLine;
    }

    @Override
    public String toString() {
        return startLine == endLine ? String.valueOf(startLine) : startLine + "-" + endLine;
    }
}
