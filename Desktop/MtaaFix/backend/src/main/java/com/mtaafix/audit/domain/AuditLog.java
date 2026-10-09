package com.mtaafix.audit.domain;

import com.mtaafix.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_logs_actor", columnList = "actor_id"),
        @Index(name = "idx_audit_logs_entity", columnList = "entity_type, entity_id"),
        @Index(name = "idx_audit_logs_created", columnList = "created_at")
})
public class AuditLog extends BaseEntity {

    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @Column(name = "actor_id")
    private String actorId;

    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;

    @Column(name = "entity_id", length = 255)
    private String entityId;

    @Column(name = "details", columnDefinition = "text")
    private String details;

    public AuditLog() {
    }

    public AuditLog(String action, String actorId, String entityType, String entityId, String details) {
        this.action = action;
        this.actorId = actorId;
        this.entityType = entityType;
        this.entityId = entityId;
        this.details = details;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getActorId() {
        return actorId;
    }

    public void setActorId(String actorId) {
        this.actorId = actorId;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
