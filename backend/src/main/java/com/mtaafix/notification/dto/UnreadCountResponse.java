package com.mtaafix.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Unread notification count for badges")
public record UnreadCountResponse(
        @Schema(description = "Number of unread notifications") long count) {
}
