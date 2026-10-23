package com.mtaafix;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mtaafix.config.ProdSecretsGuard;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;

/**
 * Phase 18 (F7 remediation evidence): the prod profile must refuse to boot
 * while JWT_SECRET / SPRING_DATASOURCE_PASSWORD / GEO_FUZZ_SECRET hold
 * shipped defaults, are blank, or (where required) are too short - and must
 * accept real environment-provided values.
 */
class ProdSecretsGuardTest {

    private static final String REAL_JWT = "0123456789abcdef0123456789abcdef-really-long";
    private static final String REAL_DB = "environment-provided-db-password";
    private static final String REAL_GEO = "geo-fuzz-secret-for-production-rotated";

    private static ProdSecretsGuard guard(String jwt, String dbPassword, String geo) {
        return new ProdSecretsGuard(jwt, dbPassword, geo);
    }

    @Test
    void guardIsScopedToTheProdProfileOnly() {
        Profile profile = ProdSecretsGuard.class.getAnnotation(Profile.class);
        assertEquals("prod", profile.value()[0]);
    }

    @Test
    void refusesEveryShippedDevelopmentDefault() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> guard("min-32-char-secret", "change-me",
                        "mtaafix-local-dev-geo-fuzz-secret").afterPropertiesSet());
        assertTrue(ex.getMessage().contains(
                        "jwt.secret still holds a shipped development default"),
                "got: " + ex.getMessage());
        assertTrue(ex.getMessage().contains(
                        "spring.datasource.password still holds a shipped development default"),
                "got: " + ex.getMessage());
        assertTrue(ex.getMessage().contains(
                        "geo.fuzz-secret still holds a shipped development default"),
                "got: " + ex.getMessage());
    }

    @Test
    void refusesTheRotatedJwtDefaultToo() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> guard("min-32-char-secret-restart-2026-more", REAL_DB, REAL_GEO)
                        .afterPropertiesSet());
        assertTrue(ex.getMessage().contains(
                        "jwt.secret still holds a shipped development default"),
                "got: " + ex.getMessage());
    }

    @Test
    void refusesBlankSecrets() {
        IllegalStateException blankJwt = assertThrows(IllegalStateException.class,
                () -> guard(null, REAL_DB, REAL_GEO).afterPropertiesSet());
        assertTrue(blankJwt.getMessage().contains("jwt.secret is blank (set it via environment)"),
                "got: " + blankJwt.getMessage());
        IllegalStateException blankDb = assertThrows(IllegalStateException.class,
                () -> guard(REAL_JWT, "", REAL_GEO).afterPropertiesSet());
        assertTrue(blankDb.getMessage().contains(
                        "spring.datasource.password is blank (set it via environment)"),
                "got: " + blankDb.getMessage());
    }

    @Test
    void refusesAShortJwtSecret() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> guard("too-short-jwt", REAL_DB, REAL_GEO).afterPropertiesSet());
        assertTrue(ex.getMessage().contains("jwt.secret must be at least 32 characters"),
                "got: " + ex.getMessage());
    }

    @Test
    void acceptsRealEnvironmentProvidedSecrets() {
        assertDoesNotThrow(() -> guard(REAL_JWT, REAL_DB, REAL_GEO).afterPropertiesSet());
    }
}
