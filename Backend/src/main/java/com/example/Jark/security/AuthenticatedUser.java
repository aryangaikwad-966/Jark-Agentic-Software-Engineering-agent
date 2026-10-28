package com.example.Jark.security;

/** Immutable identity established from a verified bearer token. */
public record AuthenticatedUser(Long id, String username) {
}
