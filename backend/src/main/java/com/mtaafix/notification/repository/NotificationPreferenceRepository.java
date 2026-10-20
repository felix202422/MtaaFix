package com.mtaafix.notification.repository;

import com.mtaafix.notification.domain.NotificationPreference;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, String> {
    Optional<NotificationPreference> findByUserId(String userId);
}
