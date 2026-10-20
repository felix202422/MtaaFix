package com.mtaafix.notification.repository;

import com.mtaafix.notification.domain.NotificationDelivery;
import com.mtaafix.notification.domain.NotificationDelivery.Channel;
import com.mtaafix.notification.domain.NotificationDelivery.Status;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, String> {
    List<NotificationDelivery> findByNotificationId(String notificationId);

    /** Retry candidates: unfinished or failed email deliveries under the cap. */
    List<NotificationDelivery> findByChannelAndStatusInAndAttemptCountLessThan(
            Channel channel, List<Status> statuses, int attemptCount);
}
