package com.mtaafix.notification.repository;

import com.mtaafix.notification.domain.Notification;
import com.mtaafix.user.domain.User;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, String> {
    List<Notification> findByUserOrderByCreatedAtDesc(User user);

    Page<Notification> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    /** Unread badge (mobile Alerts tab / web bell). */
    long countByUserAndIsReadFalse(User user);

    /** FR-6.2 mark-all-read as a single UPDATE instead of loading every row. */
    @Modifying(clearAutomatically = true)
    @Query("update Notification n set n.isRead = true where n.user = :user and n.isRead = false")
    int markAllReadForUser(@Param("user") User user);
}
