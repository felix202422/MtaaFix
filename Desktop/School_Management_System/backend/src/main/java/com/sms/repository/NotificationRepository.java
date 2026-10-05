package com.sms.repository;

import com.sms.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByTargetRoleOrderByCreatedAtDesc(String targetRole);

    List<Notification> findByIsReadFalseAndTargetRole(String targetRole);

    Long countByIsReadFalseAndTargetRole(String targetRole);
}
