package com.mtaafix.notification.service;

import com.mtaafix.notification.domain.Notification;
import com.mtaafix.notification.domain.Notification.Type;
import com.mtaafix.notification.dto.NotificationDto;
import com.mtaafix.notification.repository.NotificationRepository;
import com.mtaafix.report.domain.Report;
import com.mtaafix.user.domain.User;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
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
        return NotificationDto.from(notification);
    }

    public NotificationDto markRead(String id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException("Notification not found"));
        notification.setRead(true);
        notification = notificationRepository.save(notification);
        return NotificationDto.from(notification);
    }
}
