package com.example.VoxCode.service;

import com.example.VoxCode.dto.AuthRequest;
import com.example.VoxCode.dto.AuthResponse;
import com.example.VoxCode.dto.RegisterUserRequest;
import com.example.VoxCode.entity.User;
import com.example.VoxCode.repository.UserRepository;
import com.example.VoxCode.security.AuthenticatedUser;
import com.example.VoxCode.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterUserRequest request) {
        if (userRepository.existsByUsername(request.username()) || userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Username or email is already registered");
        }
        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        return responseFor(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse authenticate(AuthRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));
        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid username or password");
        }
        return responseFor(user);
    }

    private AuthResponse responseFor(User user) {
        String token = jwtService.createToken(new AuthenticatedUser(user.getId(), user.getUsername()));
        return new AuthResponse(token, "Bearer", user.getId(), user.getUsername());
    }
}
