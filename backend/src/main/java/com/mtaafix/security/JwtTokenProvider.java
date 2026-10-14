package com.mtaafix.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import javax.crypto.SecretKey;
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

    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    public String generateAccessToken(String subject, String role) {
        return buildToken(subject, TYPE_ACCESS, role, accessTokenExpiration);
    }

    public String generateRefreshToken(String subject) {
        return buildToken(subject, TYPE_REFRESH, null, refreshTokenExpiration);
    }

    private String buildToken(String subject, String type, String role,
            long validityInMilliseconds) {
        Instant now = Instant.now();
        JwtBuilder builder = Jwts.builder()
                .subject(subject)
                .claim("typ", type)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(validityInMilliseconds, ChronoUnit.MILLIS)));
        if (role != null) {
            builder = builder.claim("role", role);
        }
        return builder.signWith((javax.crypto.SecretKey) key).compact();
    }

    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            Jwts.parser().verifyWith((javax.crypto.SecretKey) key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Token validation failed: {}", ex.getMessage());
            return false;
        }
    }

    public String getSubject(String token) {
        return parse(token).getSubject();
    }

    public String getRole(String token) {
        Claims claims = parse(token);
        return claims.get("role", String.class);
    }

    public String getType(String token) {
        Claims claims = parse(token);
        return claims.get("typ", String.class);
    }

    public boolean isAccessToken(String token) {
        return TYPE_ACCESS.equals(getType(token));
    }

    public Instant getIssuedAt(String token) {
        return parse(token).getIssuedAt().toInstant();
    }

    public Instant getExpiration(String token) {
        return parse(token).getExpiration().toInstant();
    }

    private Claims parse(String token) {
        return Jwts.parser().verifyWith((javax.crypto.SecretKey) key).build().parseSignedClaims(token).getPayload();
    }
}
