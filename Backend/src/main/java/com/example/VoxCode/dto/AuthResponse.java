package com.example.VoxCode.dto;

public record AuthResponse(String accessToken, String tokenType, Long userId, String username) {
}
