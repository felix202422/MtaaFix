package com.mtaafix.audit.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

@Schema(description = "Audit log entry")
public record AuditLogEntry(
        @Schema(description = "Audit log entry ID") String id,
        @Schema(description = "Action performed") String action,
        @Schema(description = "Actor user ID") String actorId,
        @Schema(description = "Entity type") String entityType,
        @Schema(description = "Entity ID") String entityId,
        @Schema(description = "Additional details (JSON)") String details,
        @Schema(description = "Timestamp") Instant createdAt) {

    public static AuditLogEntry from(com.mtaafix.audit.domain.AuditLog log) {
        return new AuditLogEntry(
                log.getId().toString(),
                log.getAction(),
                log.getActorId() != null ? log.getActorId().toString() : null,
                log.getEntityType(),
                log.getEntityId(),
                log.getDetails() != null ? log.getDetails().toString() : null,
                log.getCreatedAt()
        );
    }
}
