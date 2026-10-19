package com.example.Jark.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {
    private String code;
    private String field;
    private String message;
    private String rejectedValue;

    public static ApiError of(String code, String message) {
        return ApiError.builder()
                .code(code)
                .message(message)
                .build();
    }

    public static ApiError of(String code, String field, String message) {
        return ApiError.builder()
                .code(code)
                .field(field)
                .message(message)
                .build();
    }

    public static ApiError of(String code, String field, String message, Object rejectedValue) {
        return ApiError.builder()
                .code(code)
                .field(field)
                .message(message)
                .rejectedValue(rejectedValue != null ? rejectedValue.toString() : null)
                .build();
    }
}
