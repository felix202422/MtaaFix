package com.mtaafix.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Partial update of notification preferences; omitted fields are unchanged")
public record NotificationPreferenceRequest(
        @Schema(description = "Enable/disable email notifications") Boolean emailEnabled,
        @Schema(description = "Enable/disable push notifications (no provider configured yet)") Boolean pushEnabled) {
}
