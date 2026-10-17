package com.mtaafix.config.service;

import com.mtaafix.config.dto.SettingsDto;
import java.util.List;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/** P24/FR-12.3: builds the read-only settings view from the live environment. */
@Service
public class SettingsService {

    static final String DEFAULT_FUZZ_SECRET = "mtaafix-local-dev-geo-fuzz-secret";
    static final String APP_ZONE = "Africa/Dar_es_Salaam";

    private final Environment environment;

    public SettingsService(Environment environment) {
        this.environment = environment;
    }

    public SettingsDto getSettings() {
        String fuzzSecret = environment.getProperty("geo.fuzz-secret", String.class, "");
        boolean secretPresent = fuzzSecret != null && !fuzzSecret.isBlank();
        boolean secretIsDefault = DEFAULT_FUZZ_SECRET.equals(fuzzSecret);

        return new SettingsDto(
                new SettingsDto.General(
                        environment.getProperty("spring.application.name", String.class, "mtaafix"),
                        APP_ZONE),
                new SettingsDto.Reporting(
                        environment.getProperty("media.max-file-size-bytes", Long.class, 20971520L),
                        environment.getProperty("spring.servlet.multipart.max-file-size",
                                String.class, "21MB"),
                        List.of("image/jpeg", "image/png", "image/webp"),
                        new SettingsDto.Reporting.Notifications(true, false,
                                "Email delivery is post-MVP (Phase 13, FR-6.3)")),
                new SettingsDto.Security(
                        millisToSeconds(environment.getProperty("jwt.access-token-expiration",
                                Long.class, 900000L)),
                        millisToSeconds(environment.getProperty("jwt.refresh-token-expiration",
                                Long.class, 604800000L)),
                        environment.getProperty("cors.allowed-origins", String.class, "")),
                new SettingsDto.Privacy(
                        "Deterministic distance fuzzing applied to ALL public locations (FR-11.4)",
                        List.of("MODERATOR", "ADMINISTRATOR", "ORGANIZATION_ADMIN",
                                "FIELD_WORKER (assigned reports only)")),
                new SettingsDto.Geographic(
                        environment.getProperty("geo.fuzz-min-meters", Integer.class, 150),
                        environment.getProperty("geo.fuzz-max-meters", Integer.class, 600),
                        secretPresent && !secretIsDefault,
                        secretIsDefault),
                new SettingsDto.Integrations(
                        "OpenStreetMap raster tiles",
                        "Not connected (post-MVP, Phase 13)",
                        "Not connected (post-MVP, Phase 13)"));
    }

    private static long millisToSeconds(Long millis) {
        return millis == null ? 0L : millis / 1000L;
    }
}
