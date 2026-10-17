package com.mtaafix.audit.dto;

import java.time.Instant;

/** P22 audit filter parameters (all optional; null = no filtering). */
public record AuditLogFilter(
        String action,
        String actorId,
        String entityType,
        String entityId,
        String outcome,
        Instant from,
        Instant to) {

    public static AuditLogFilter none() {
        return new AuditLogFilter(null, null, null, null, null, null, null);
    }
}
