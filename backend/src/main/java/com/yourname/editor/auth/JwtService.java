package com.yourname.editor.auth;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Per-tab login tokens: the browser keeps its own token in sessionStorage,
 * so every tab can be a different account — no shared-cookie hijacking.
 * Override {@code app.auth.jwt-secret} (env {@code JWT_SECRET}) in prod.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final Duration ttl;

    public JwtService(
            @Value("${app.auth.jwt-secret:dev-only-change-me-in-prod-0123456789abcdef}")
            String secret,
            @Value("${app.auth.jwt-ttl-days:7}") long ttlDays) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttl = Duration.ofDays(ttlDays);
        if (secret.startsWith("dev-only")) {
            System.err.println("WARN: using dev JWT secret — set JWT_SECRET env var in prod");
        }
    }

    public String issue(String email) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }

    /** Email if the token is valid, else null (expired, forged, garbage). */
    public String verify(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token.trim())
                    .getPayload()
                    .getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
}
