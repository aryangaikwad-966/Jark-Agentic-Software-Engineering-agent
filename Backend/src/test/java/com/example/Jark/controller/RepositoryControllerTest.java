package com.example.Jark.controller;

import com.example.Jark.dto.RegisterRepositoryRequest;
import com.example.Jark.dto.RepositoryResponse;
import com.example.Jark.service.RepositoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Exclude Security for these basic web tests since it's permit-all currently anyway
@WebMvcTest(controllers = RepositoryController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class RepositoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RepositoryService repositoryService;

    @Test
    void registerRepository_ValidRequest_ShouldReturnCreated() throws Exception {
        // Arrange
        RegisterRepositoryRequest request = new RegisterRepositoryRequest();
        request.setUserId(1L);
        request.setUrl("https://github.com/spring-projects/spring-petclinic.git");
        
        RepositoryResponse mockResponse = RepositoryResponse.builder()
                .id(1L)
                .userId(1L)
                .name("spring-petclinic")
                .url("https://github.com/spring-projects/spring-petclinic.git")
                .status("READY")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
                
        when(repositoryService.registerRepository(any(RegisterRepositoryRequest.class))).thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/repositories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("spring-petclinic"))
                .andExpect(jsonPath("$.data.status").value("READY"));
    }

    @Test
    void registerRepository_InvalidUrl_ShouldReturnBadRequest() throws Exception {
        // Arrange
        RegisterRepositoryRequest request = new RegisterRepositoryRequest();
        request.setUserId(1L);
        request.setUrl("not-a-valid-url"); // Fails pattern validation
        
        // Act & Assert
        mockMvc.perform(post("/api/v1/repositories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0].field").value("url"));
    }
}
