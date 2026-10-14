package com.mtaafix.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private static final String SECRET = "mtaafix-test-secret-min-32-characters-2026";

    private final JwtTokenProvider provider =
            new JwtTokenProvider(SECRET, 900_000L, 604_800_000L);

    @Test
    void accessTokenCarriesAccessTypAndRole() {
        String token = provider.generateAccessToken("user-1", "CITIZEN");

        assertTrue(provider.validateToken(token));
        assertEquals("user-1", provider.getSubject(token));
        assertEquals(JwtTokenProvider.TYPE_ACCESS, provider.getType(token));
        assertTrue(provider.isAccessToken(token));
        assertEquals("CITIZEN", provider.getRole(token));
    }

    @Test
    void refreshTokenCarriesRefreshTypAndNoRole() {
        String token = provider.generateRefreshToken("user-1");

        assertTrue(provider.validateToken(token));
        assertEquals("user-1", provider.getSubject(token));
        assertEquals(JwtTokenProvider.TYPE_REFRESH, provider.getType(token));
        assertFalse(provider.isAccessToken(token));
        assertNull(provider.getRole(token));
    }

    @Test
    void validateTokenRejectsGarbageAndBlank() {
        assertFalse(provider.validateToken(null));
        assertFalse(provider.validateToken(""));
        assertFalse(provider.validateToken("not-a-jwt"));
        assertFalse(provider.validateToken("a.b.c"));
    }

    @Test
    void constructorRejectsWeakSecrets() {
        assertThrows(IllegalArgumentException.class,
                () -> new JwtTokenProvider("short", 1L, 1L));
        assertThrows(IllegalArgumentException.class,
                () -> new JwtTokenProvider(null, 1L, 1L));
    }

    @Test
    void tokenSignedWithDifferentSecretIsRejected() {
        JwtTokenProvider other = new JwtTokenProvider(
                "another-secret-that-is-long-enough-2026", 900_000L, 604_800_000L);
        String foreign = other.generateAccessToken("user-1", "ADMINISTRATOR");
        assertFalse(provider.validateToken(foreign));
    }
}
