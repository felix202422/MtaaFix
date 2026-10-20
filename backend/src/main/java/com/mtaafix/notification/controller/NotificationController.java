package com.mtaafix.notification.controller;

import com.mtaafix.common.response.SimpleResponse;
import com.mtaafix.notification.domain.Notification;
import com.mtaafix.notification.dto.NotificationDto;
import com.mtaafix.notification.dto.NotificationPreferenceDto;
import com.mtaafix.notification.dto.NotificationPreferenceRequest;
import com.mtaafix.notification.dto.UnreadCountResponse;
import com.mtaafix.notification.service.NotificationPreferenceService;
import com.mtaafix.notification.service.NotificationService;
import com.mtaafix.user.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Notifications", description = "Notification endpoints for users")
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationPreferenceService preferenceService;
    private final boolean pushAvailable;

    public NotificationController(NotificationService notificationService,
            NotificationPreferenceService preferenceService,
            @org.springframework.beans.factory.annotation.Value("${notifications.push.enabled:false}")
            boolean pushAvailable) {
        this.notificationService = notificationService;
        this.preferenceService = preferenceService;
        this.pushAvailable = pushAvailable;
    }

    @Operation(summary = "Get my notifications")
    @GetMapping
    public ResponseEntity<Page<NotificationDto>> list(
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(notificationService.listByUser(user, pageable));
    }

    @Operation(summary = "Get my unread notification count (badges)")
    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponse> unreadCount(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(new UnreadCountResponse(notificationService.unreadCount(user)));
    }

    @Operation(summary = "Get my notification preferences (FR-6.4)")
    @GetMapping("/preferences")
    public ResponseEntity<NotificationPreferenceDto> getPreferences(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(preferenceService.get(user, pushAvailable));
    }

    @Operation(summary = "Update my notification preferences (FR-6.4)")
    @PutMapping("/preferences")
    public ResponseEntity<NotificationPreferenceDto> updatePreferences(
            @AuthenticationPrincipal User user,
            @RequestBody NotificationPreferenceRequest request) {
        return ResponseEntity.ok(preferenceService.update(user, request, pushAvailable));
    }

    @Operation(summary = "Mark a notification as read")
    @PostMapping("/{id}/read")
    public ResponseEntity<SimpleResponse> markRead(
            @AuthenticationPrincipal User user,
            @PathVariable @NotNull String id) {
        notificationService.markAsRead(user, id);
        return ResponseEntity.ok(SimpleResponse.ok("Marked as read"));
    }

    @Operation(summary = "Mark all notifications as read")
    @PostMapping("/read-all")
    public ResponseEntity<SimpleResponse> markAllRead(@AuthenticationPrincipal User user) {
        notificationService.markAllAsRead(user);
        return ResponseEntity.ok(SimpleResponse.ok("Marked all as read"));
    }
}
