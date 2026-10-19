package com.example.VoxCode.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private static final String USER_ID_CLAIM = "uid";
    private final SecretKey signingKey;
    private final long expirationSeconds;

    public JwtService(@Value("${voxcode.security.jwt.secret}") String base64Secret,
                      @Value("${voxcode.security.jwt.expiration-seconds:3600}") long expirationSeconds) {
        byte[] keyBytes = Decoders.BASE64.decode(base64Secret);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("voxcode.security.jwt.secret must encode at least 256 bits");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationSeconds = expirationSeconds;
    }

    public String createToken(AuthenticatedUser user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.username())
                .claim(USER_ID_CLAIM, user.id())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirationSeconds)))
                .signWith(signingKey)
                .compact();
    }

    public AuthenticatedUser parse(String token) {
        Claims claims = Jwts.parser().verifyWith(signingKey).build()
                .parseSignedClaims(token).getPayload();
        Number userId = claims.get(USER_ID_CLAIM, Number.class);
        if (userId == null || claims.getSubject() == null || claims.getSubject().isBlank()) {
            throw new IllegalArgumentException("Token does not contain a valid user identity");
        }
        return new AuthenticatedUser(userId.longValue(), claims.getSubject());
    }
}
