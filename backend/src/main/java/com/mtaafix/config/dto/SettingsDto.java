package com.mtaafix.config.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * P24/FR-12.3: read-only effective configuration (MVP is config-only; the full
 * settings editor is post-MVP). Sensitive values (JWT secret, datasource
 * password) are never exposed — booleans only.
 */
@Schema(description = "Effective system settings (read-only in MVP, FR-12.3)")
public record SettingsDto(
        General general,
        Reporting reporting,
        Security security,
        Privacy privacy,
        Geographic geographic,
        Integrations integrations) {

    @Schema(description = "General settings")
    public record General(String applicationName, String applicationTimeZone) {
    }

    @Schema(description = "Reporting rules and upload limits (FR-12.3)")
    public record Reporting(long maxUploadBytes, String multipartMaxFileSize,
            List<String> allowedMediaTypes, Notifications notifications) {

        @Schema(description = "Notification channel status")
        public record Notifications(boolean inAppEnabled, boolean emailEnabled,
                String emailNote) {
        }
    }

    @Schema(description = "Security settings")
    public record Security(long accessTokenTtlSeconds, long refreshTokenTtlSeconds,
            String corsAllowedOrigins) {
    }

    @Schema(description = "Privacy policy in effect (FR-11.4)")
    public record Privacy(String locationFuzzing,
            List<String> rolesSeeingPreciseLocations) {
    }

    @Schema(description = "Geographic configuration")
    public record Geographic(int fuzzMinMeters, int fuzzMaxMeters,
            boolean fuzzSecretConfigured, boolean fuzzSecretIsDefault) {
    }

    @Schema(description = "Integrations status")
    public record Integrations(String mapTiles, String email, String push) {
    }
}
