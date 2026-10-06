package com.mtaafix.notification.repository;

import com.mtaafix.notification.domain.Notification;
import com.mtaafix.user.domain.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, String> {
    List<Notification> findByUserOrderByCreatedAtDesc(User user);
}
