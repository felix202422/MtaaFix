package com.mtaafix.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Phase 16 secrets review: refuses to start under the "prod" profile while
 * any security-relevant secret still holds a shipped development default.
 * The guard is inert outside the profile (local/test runs keep working);
 * activating prod (Phase 18 deployment) without real secrets fails fast
 * with a precise message instead of running insecurely.
 */
@Component
@Profile("prod")
public class ProdSecretsGuard implements InitializingBean {

    static final Set<String> FORBIDDEN_DEFAULTS = Set.of(
            "min-32-char-secret",
            "min-32-char-secret-restart-2026-more",
            "change-me",
            "mtaafix-local-dev-geo-fuzz-secret");

    private final String jwtSecret;
    private final String datasourcePassword;
    private final String geoFuzzSecret;

    public ProdSecretsGuard(@Value("${jwt.secret}") String jwtSecret,
            @Value("${spring.datasource.password}") String datasourcePassword,
            @Value("${geo.fuzz-secret}") String geoFuzzSecret) {
        this.jwtSecret = jwtSecret;
        this.datasourcePassword = datasourcePassword;
        this.geoFuzzSecret = geoFuzzSecret;
    }

    @Override
    public void afterPropertiesSet() {
        List<String> problems = new ArrayList<>();
        check(problems, "jwt.secret", jwtSecret, true);
        check(problems, "spring.datasource.password", datasourcePassword, false);
        check(problems, "geo.fuzz-secret", geoFuzzSecret, true);
        if (!problems.isEmpty()) {
            throw new IllegalStateException(
                    "Refusing to start with the prod profile: " + String.join("; ", problems));
        }
    }

    private static void check(List<String> problems, String name, String value,
            boolean requireLength) {
        if (value == null || value.isBlank()) {
            problems.add(name + " is blank (set it via environment)");
            return;
        }
        if (FORBIDDEN_DEFAULTS.contains(value)) {
            problems.add(name + " still holds a shipped development default");
            return;
        }
        if (requireLength && value.length() < 32) {
            problems.add(name + " must be at least 32 characters");
        }
    }
}
