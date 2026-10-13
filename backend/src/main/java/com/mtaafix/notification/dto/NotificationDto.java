package com.mtaafix.notification.dto;

import com.mtaafix.notification.domain.Notification;
import com.mtaafix.notification.domain.Notification.Type;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Notification representation")
public record NotificationDto(
        @Schema(description = "Notification id") String id,
        @Schema(description = "Title") String title,
        @Schema(description = "Body") String body,
        @Schema(description = "Type") Type type,
        @Schema(description = "Is read") Boolean isRead,
        @Schema(description = "Timestamp") String timestamp,
        @Schema(description = "Report id") String reportId) {

    public static NotificationDto from(Notification notification) {
        return new NotificationDto(notification.getId(), notification.getTitle(),
                notification.getBody(), notification.getType(), notification.getRead(),
                notification.getCreatedAt() != null ? notification.getCreatedAt().toString() : null,
                notification.getReport() != null ? notification.getReport().getId() : null);
    }
}
