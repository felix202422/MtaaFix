package com.mtaafix.audit.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Audit log entry")
public record AuditLogEntry(
        @Schema(description = "Audit log entry ID") String id,
        @Schema(description = "Action performed") String action,
        @Schema(description = "Actor user ID") String actorId,
        @Schema(description = "Actor email (resolved; null when the actor no longer exists)")
        String actorEmail,
        @Schema(description = "Entity type") String entityType,
        @Schema(description = "Entity ID") String entityId,
        @Schema(description = "Outcome (SUCCESS/DENIED); null on legacy rows") String outcome,
        @Schema(description = "Additional details (JSON)") String details,
        @Schema(description = "Timestamp") Instant createdAt) {

    public static AuditLogEntry from(com.mtaafix.audit.domain.AuditLog log) {
        return new AuditLogEntry(
                log.getId().toString(),
                log.getAction(),
                log.getActorId() != null ? log.getActorId().toString() : null,
                null,
                log.getEntityType(),
                log.getEntityId(),
                log.getOutcome(),
                log.getDetails() != null ? log.getDetails().toString() : null,
                log.getCreatedAt()
        );
    }

    public AuditLogEntry withActorEmail(String email) {
        return new AuditLogEntry(id, action, actorId, email, entityType, entityId, outcome,
                details, createdAt);
    }
}
