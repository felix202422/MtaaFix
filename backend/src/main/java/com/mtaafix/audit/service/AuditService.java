package com.mtaafix.audit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtaafix.audit.domain.AuditLog;
import com.mtaafix.audit.dto.AuditLogEntry;
import com.mtaafix.audit.repository.AuditRepository;
import com.mtaafix.user.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditRepository auditRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuditService(AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    public List<AuditLogEntry> getOrganisationAuditLog(String orgId, User user) {
        if (user.getRole() == User.Role.ORGANIZATION_ADMIN
                && (user.getOrganisationId() == null
                        || !user.getOrganisationId().equals(orgId))) {
            throw new AccessDeniedException("Audit log is outside your organisation scope.");
        }
        return auditRepository.findByEntityId(orgId).stream()
                .map(AuditLogEntry::from)
                .collect(Collectors.toList());
    }

    public List<AuditLogEntry> getGlobalAuditLog() {
        return auditRepository.findAll().stream()
                .map(AuditLogEntry::from)
                .collect(Collectors.toList());
    }

    public AuditLogEntry record(String action, User actor, String entityType, String entityId,
            Map<String, String> details) {
        AuditLog entry = new AuditLog(action, actor != null ? actor.getId() : null, entityType,
                entityId, toJson(details));
        entry = auditRepository.save(entry);
        return AuditLogEntry.from(entry);
    }

    private String toJson(Map<String, String> details) {
        if (details == null || details.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(details);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize audit details: {}", ex.getMessage());
            return null;
        }
    }
}
