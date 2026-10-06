package com.mtaafix.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final Key key;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("JWT secret must not be blank or null.");
        }
        if (secret.length() < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 characters.");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    public String generateAccessToken(String subject, String role) {
        return buildToken(subject, role, accessTokenExpiration);
    }

    public String generateRefreshToken(String subject) {
        return buildToken(subject, null, refreshTokenExpiration);
    }

    private String buildToken(String subject, String role, long validityInMilliseconds) {
        Instant now = Instant.now();
        Claims claims = Jwts.claims().setSubject(subject).build();
        if (role != null) {
            claims.put("role", role);
        }
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(DateFromInstant(now))
                .setExpiration(DateFromInstant(now.plus(validityInMilliseconds, ChronoUnit.MILLIS)))
                .signWith(key)
                .compact();
    }

    private static Instant DateFromInstant(Instant instant) {
        return java.util.Date.from(instant);
    }

    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Token validation failed: {}", ex.getMessage());
            return false;
        }
    }

    public String getSubject(String token) {
        return parse(token).getPayload().getSubject();
    }

    public String getRole(String token) {
        Claims claims = parse(token).getPayload();
        return claims.get("role", String.class);
    }

    public Instant getIssuedAt(String token) {
        return parse(token).getPayload().getIssuedAt().toInstant();
    }

    public Instant getExpiration(String token) {
        return parse(token).getPayload().getExpiration().toInstant();
    }

    private Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
