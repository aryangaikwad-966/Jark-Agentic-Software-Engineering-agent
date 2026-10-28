package com.example.Jark.exception;

import com.example.Jark.dto.ApiError;
import com.example.Jark.dto.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final WebRequest request = mock(WebRequest.class);

    @Test
    void handleValidationException_ShouldReturnBadRequestWithErrors() throws Exception {
        org.springframework.validation.BeanPropertyBindingResult bindingResult =
                new org.springframework.validation.BeanPropertyBindingResult(new Object(), "testObject");
        FieldError fieldError = new FieldError("testObject", "fieldName", "invalidValue", 
                false, null, null, "must not be null");
        bindingResult.addError(fieldError);

        java.lang.reflect.Method dummyMethod = GlobalExceptionHandlerTest.class.getDeclaredMethod("handleValidationException_ShouldReturnBadRequestWithErrors");
        org.springframework.core.MethodParameter methodParameter = new org.springframework.core.MethodParameter(dummyMethod, -1);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(methodParameter, bindingResult);

        ResponseEntity<ApiResponse<Void>> response = handler.handleValidationException(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Validation failed", response.getBody().getMessage());
        assertNotNull(response.getBody().getErrors());
        assertEquals(1, response.getBody().getErrors().size());
        
        ApiError error = response.getBody().getErrors().get(0);
        assertEquals("VALIDATION_ERROR", error.getCode());
        assertEquals("fieldName", error.getField());
        assertEquals("must not be null", error.getMessage());
        assertEquals("invalidValue", error.getRejectedValue());
    }

    @Test
    void handleIllegalArgumentException_ShouldReturnBadRequest() {
        IllegalArgumentException ex = new IllegalArgumentException("Invalid argument");

        ResponseEntity<ApiResponse<Void>> response = handler.handleIllegalArgumentException(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Invalid argument", response.getBody().getMessage());
    }

    @Test
    void handleIllegalStateException_ShouldReturnConflict() {
        IllegalStateException ex = new IllegalStateException("Invalid state");

        ResponseEntity<ApiResponse<Void>> response = handler.handleIllegalStateException(ex, request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Invalid state", response.getBody().getMessage());
    }

    @Test
    void handleGenericException_ShouldReturnInternalServerError() {
        Exception ex = new Exception("Unexpected error");

        ResponseEntity<ApiResponse<Void>> response = handler.handleGenericException(ex, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("An unexpected error occurred", response.getBody().getMessage());
    }
}
