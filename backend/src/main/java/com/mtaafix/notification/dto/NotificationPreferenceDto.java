package com.mtaafix.notification.dto;

import com.mtaafix.notification.domain.NotificationPreference;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Notification preferences for the current user (FR-6.4)")
public record NotificationPreferenceDto(
        @Schema(description = "Email channel opted in") boolean emailEnabled,
        @Schema(description = "Push channel opted in (stored for future use)") boolean pushEnabled,
        @Schema(description = "Whether a push provider is configured on this deployment") boolean pushAvailable) {

    public static NotificationPreferenceDto from(NotificationPreference preference, boolean pushAvailable) {
        return new NotificationPreferenceDto(preference.isEmailEnabled(), preference.isPushEnabled(),
                pushAvailable);
    }
}
