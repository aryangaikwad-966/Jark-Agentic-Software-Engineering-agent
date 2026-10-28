package com.example.Jark.dto;

public record AuthResponse(String accessToken, String tokenType, Long userId, String username) {
}
