package com.example.Jark.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterUserRequest(
        @NotBlank(message = "Username is required") @Size(max = 255) String username,
        @NotBlank(message = "Email is required") @Email(message = "Email must be valid") @Size(max = 255) String email,
        @NotBlank(message = "Password is required") @Size(min = 12, max = 128, message = "Password must be 12 to 128 characters") String password
) {
}
