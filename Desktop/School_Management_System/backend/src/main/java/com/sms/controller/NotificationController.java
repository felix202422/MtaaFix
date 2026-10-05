package com.sms.controller;

import com.sms.dto.response.ApiResponse;
import com.sms.entity.Notification;
import com.sms.repository.NotificationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;

    @GetMapping("/{role}")
    public ResponseEntity<ApiResponse<List<Notification>>> getNotifications(@PathVariable String role) {
        return ResponseEntity.ok(ApiResponse.success(notificationRepository.findByTargetRoleOrderByCreatedAtDesc(role)));
    }

    @GetMapping("/unread/{role}")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(@PathVariable String role) {
        return ResponseEntity.ok(ApiResponse.success(notificationRepository.countByIsReadFalseAndTargetRole(role)));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long id) {
        notificationRepository.findById(id).ifPresent(notification -> {
            notification.setIsRead(true);
            notificationRepository.save(notification);
        });
        return ResponseEntity.ok(ApiResponse.success("Marked as read", null));
    }

    public NotificationController(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

}
