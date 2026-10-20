package com.mtaafix.notification.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Delivery failure handling (Phase 13): retries FAILED/PENDING email
 * deliveries on a fixed interval. A no-op while the email channel is
 * disabled, so local/dev deployments are unaffected.
 */
@Component
public class EmailRetryScheduler {

    private final NotificationDeliveryService deliveryService;

    public EmailRetryScheduler(NotificationDeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @Scheduled(fixedDelayString = "${notifications.email.retry-interval-ms:300000}",
            initialDelayString = "${notifications.email.retry-initial-delay-ms:60000}")
    public void retryFailedDeliveries() {
        deliveryService.retryFailedEmails();
    }
}
