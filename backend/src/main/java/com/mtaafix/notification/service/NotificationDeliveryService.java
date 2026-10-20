package com.mtaafix.notification.service;

import com.mtaafix.common.email.EmailSender;
import com.mtaafix.notification.domain.Notification;
import com.mtaafix.notification.domain.NotificationDelivery;
import com.mtaafix.notification.domain.NotificationDelivery.Channel;
import com.mtaafix.notification.domain.NotificationDelivery.Status;
import com.mtaafix.notification.repository.NotificationDeliveryRepository;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-6.3: keeps delivery state separate from notification creation.
 *
 * In-app delivery is recorded as DELIVERED (the feed row is the delivery for
 * the pull channel). Email deliveries are created PENDING, attempted once
 * immediately, and retried by {@link EmailRetryScheduler} while under the
 * configured attempt cap. Push has no provider configured yet, so no push
 * attempts are recorded.
 */
@Service
public class NotificationDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(NotificationDeliveryService.class);
    private static final int MAX_DETAIL_CHARS = 500;

    private final NotificationDeliveryRepository deliveryRepository;
    private final NotificationPreferenceService preferenceService;
    private final EmailSender emailSender;
    private final boolean emailEnabled;
    private final int emailMaxAttempts;
    private final String webBaseUrl;

    public NotificationDeliveryService(NotificationDeliveryRepository deliveryRepository,
            NotificationPreferenceService preferenceService, EmailSender emailSender,
            @Value("${notifications.email.enabled:false}") boolean emailEnabled,
            @Value("${notifications.email.max-attempts:3}") int emailMaxAttempts,
            @Value("${notifications.web-base-url:http://localhost:5173}") String webBaseUrl) {
        this.deliveryRepository = deliveryRepository;
        this.preferenceService = preferenceService;
        this.emailSender = emailSender;
        this.emailEnabled = emailEnabled;
        this.emailMaxAttempts = emailMaxAttempts;
        this.webBaseUrl = webBaseUrl;
    }

    /** Records per-channel delivery state for a freshly created notification. */
    @Transactional
    public void dispatch(Notification notification) {
        deliveryRepository.save(new NotificationDelivery(notification, Channel.IN_APP,
                Status.DELIVERED));
        if (notification.getUser() == null || notification.getUser().getEmail() == null) {
            return;
        }
        if (!preferenceService.preferenceFor(notification.getUser()).isEmailEnabled()) {
            return;
        }
        NotificationDelivery emailDelivery = deliveryRepository
                .save(new NotificationDelivery(notification, Channel.EMAIL, Status.PENDING));
        attemptEmail(emailDelivery);
    }

    /**
     * Retries FAILED (and interrupted PENDING) email deliveries while under
     * the attempt cap. SKIPPED deliveries are deliberate opt-out/config
     * records and are never retried; SENT deliveries are terminal.
     */
    @Transactional
    public void retryFailedEmails() {
        if (!emailEnabled) {
            return;
        }
        List<NotificationDelivery> candidates = deliveryRepository
                .findByChannelAndStatusInAndAttemptCountLessThan(Channel.EMAIL,
                        List.of(Status.FAILED, Status.PENDING), emailMaxAttempts);
        for (NotificationDelivery candidate : candidates) {
            attemptEmail(candidate);
        }
    }

    private void attemptEmail(NotificationDelivery delivery) {
        Notification notification = delivery.getNotification();
        try {
            if (!preferenceService.preferenceFor(notification.getUser()).isEmailEnabled()) {
                mark(delivery, Status.SKIPPED, "User disabled email notifications");
                return;
            }
            if (!emailSender.isConfigured()) {
                mark(delivery, Status.SKIPPED,
                        emailEnabled ? "SMTP host not configured" : "Email channel disabled");
                return;
            }
            emailSender.send(notification.getUser().getEmail(), subject(notification),
                    body(notification));
            mark(delivery, Status.SENT, null);
        } catch (RuntimeException ex) {
            log.warn("Email delivery {} failed: {}", delivery.getId(), ex.getMessage());
            mark(delivery, Status.FAILED, truncate(ex.getMessage()));
        }
    }

    private void mark(NotificationDelivery delivery, Status status, String detail) {
        delivery.setStatus(status);
        delivery.setDetail(detail);
        delivery.setAttemptCount(delivery.getAttemptCount() + 1);
        delivery.setAttemptedAt(Instant.now());
        deliveryRepository.save(delivery);
    }

    private String subject(Notification notification) {
        return "MtaaFix: " + notification.getTitle();
    }

    private String body(Notification notification) {
        StringBuilder text = new StringBuilder();
        text.append("Hello ").append(notification.getUser().getFullName()).append(",\n\n");
        text.append(notification.getTitle()).append('\n');
        if (notification.getBody() != null && !notification.getBody().isBlank()) {
            text.append(notification.getBody()).append('\n');
        }
        if (notification.getReport() != null) {
            text.append("\nReport: ").append(notification.getReport().getCode());
            if (notification.getReport().getTitle() != null) {
                text.append(" - ").append(notification.getReport().getTitle());
            }
            text.append("\nView: ").append(webBaseUrl).append("/app/reports/")
                    .append(notification.getReport().getId());
        }
        text.append("\n\nYou receive this because email notifications are enabled in your ")
                .append("MtaaFix preferences. Manage them in your profile.\n");
        return text.toString();
    }

    private static String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= MAX_DETAIL_CHARS ? value : value.substring(0, MAX_DETAIL_CHARS);
    }
}
