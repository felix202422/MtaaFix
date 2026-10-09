package com.mtaafix.notification.controller;

import com.mtaafix.common.response.SimpleResponse;
import com.mtaafix.notification.domain.Notification;
import com.mtaafix.notification.dto.NotificationDto;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Notifications", description = "Notification endpoints for users")
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Operation(summary = "Get my notifications")
    @GetMapping
    public ResponseEntity<Page<NotificationDto>> list(
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(notificationService.listByUser(user, pageable));
    }

    @Operation(summary = "Mark a notification as read")
    @PostMapping("/{id}/read")
    public ResponseEntity<SimpleResponse> markRead(
            @AuthenticationPrincipal User user,
            @PathVariable @NotNull Long id) {
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
