package com.mtaafix.notification.service;

import com.mtaafix.common.exceptions.ResourceNotFoundException;
import com.mtaafix.notification.domain.Notification;
import com.mtaafix.notification.domain.Notification.Type;
import com.mtaafix.notification.dto.NotificationDto;
import com.mtaafix.notification.repository.NotificationRepository;
import com.mtaafix.report.domain.Report;
import com.mtaafix.user.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final NotificationDeliveryService deliveryService;

    public NotificationService(NotificationRepository notificationRepository,
            NotificationDeliveryService deliveryService) {
        this.notificationRepository = notificationRepository;
        this.deliveryService = deliveryService;
    }

    public List<NotificationDto> getByUser(User user) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(NotificationDto::from)
                .toList();
    }

    public NotificationDto send(Type type, User user, Report report, String title, String body) {
        Notification notification = new Notification(title, body, type, user);
        notification.setReport(report);
        notification = notificationRepository.save(notification);
        try {
            deliveryService.dispatch(notification);
        } catch (RuntimeException ex) {
            // Delivery bookkeeping must never fail the triggering workflow (FR-6.3).
            log.warn("Delivery dispatch failed for notification {}: {}", notification.getId(),
                    ex.getMessage());
        }
        return NotificationDto.from(notification);
    }

    public Page<NotificationDto> listByUser(User user, Pageable pageable) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user, pageable)
                .map(NotificationDto::from);
    }

    /** Unread badge count (mobile Alerts tab, web bell). */
    public long unreadCount(User user) {
        return notificationRepository.countByUserAndIsReadFalse(user);
    }

    public void markAsRead(User user, String id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", id));
        if (!notification.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You can only mark your own notifications as read.");
        }
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(User user) {
        notificationRepository.markAllReadForUser(user);
    }
}
