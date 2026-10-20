package com.mtaafix.notification.domain;

import com.mtaafix.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * FR-6.3: a notification is never marked delivered merely because its
 * database row was created - each channel records its own delivery state.
 */
@Entity
@Table(name = "notification_deliveries", indexes = {
        @Index(name = "idx_notification_deliveries_status", columnList = "status"),
        @Index(name = "idx_notification_deliveries_channel", columnList = "channel")
})
public class NotificationDelivery extends BaseEntity {

    public enum Channel {
        IN_APP, EMAIL, PUSH
    }

    public enum Status {
        PENDING, SENT, DELIVERED, FAILED, SKIPPED
    }

    @ManyToOne
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;

    @Column(name = "channel", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Channel channel;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "detail", length = 500)
    private String detail;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "attempted_at")
    private Instant attemptedAt;

    public NotificationDelivery() {
    }

    public NotificationDelivery(Notification notification, Channel channel, Status status) {
        this.notification = notification;
        this.channel = channel;
        this.status = status;
    }

    public Notification getNotification() {
        return notification;
    }

    public void setNotification(Notification notification) {
        this.notification = notification;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
    }

    public Instant getAttemptedAt() {
        return attemptedAt;
    }

    public void setAttemptedAt(Instant attemptedAt) {
        this.attemptedAt = attemptedAt;
    }
}
