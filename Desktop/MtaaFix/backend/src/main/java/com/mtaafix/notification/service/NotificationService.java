package com.mtaafix.notification.service;

import com.mtaafix.notification.domain.Notification;
import com.mtaafix.notification.domain.Notification.Type;
import com.mtaafix.notification.dto.NotificationDto;
import com.mtaafix.notification.repository.NotificationRepository;
import com.mtaafix.report.domain.Report;
import com.mtaafix.user.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

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

    public Page<NotificationDto> listByUser(User user, Pageable pageable) {
        return notificationRepository.findAll(pageable).map(NotificationDto::from);
    }

    public NotificationDto markRead(String id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException("Notification not found"));
        notification.setRead(true);
        notification = notificationRepository.save(notification);
        return NotificationDto.from(notification);
    }

    public void markAsRead(User user, Long id) {
        Notification notification = notificationRepository.findById(id.toString())
                .orElseThrow(() -> new java.util.NoSuchElementException("Notification not found"));
        if (notification.getUser().getId().equals(user.getId())) {
            notification.setRead(true);
            notificationRepository.save(notification);
        }
    }

    public void markAllAsRead(User user) {
        notificationRepository.findByUserOrderByCreatedAtDesc(user).forEach(n -> n.setRead(true));
        notificationRepository.saveAll(notificationRepository.findAll());
    }
}
