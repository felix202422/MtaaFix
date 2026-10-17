package com.mtaafix.audit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtaafix.audit.domain.AuditLog;
import com.mtaafix.audit.dto.AuditLogEntry;
import com.mtaafix.audit.dto.AuditLogFilter;
import com.mtaafix.audit.repository.AuditRepository;
import com.mtaafix.common.response.PagedResponse;
import com.mtaafix.user.domain.User;
import com.mtaafix.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditRepository auditRepository;
    private final com.mtaafix.user.service.OrganisationMembershipService membershipService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuditService(AuditRepository auditRepository,
            com.mtaafix.user.service.OrganisationMembershipService membershipService,
            UserRepository userRepository) {
        this.auditRepository = auditRepository;
        this.membershipService = membershipService;
        this.userRepository = userRepository;
    }

    /**
     * P22/FR-10.2: org-scoped audit — organisation events plus events on the
     * organisation's members (role grants, membership changes).
     */
    @Transactional(readOnly = true)
    public PagedResponse<AuditLogEntry> getOrganisationAuditLog(String orgId, User user,
            AuditLogFilter filter, Pageable pageable) {
        if (user.getRole() == User.Role.ORGANIZATION_ADMIN
                && (membershipService.orgIdOfOrNull(user) == null
                        || !membershipService.orgIdOfOrNull(user).equals(orgId))) {
            throw new AccessDeniedException("Audit log is outside your organisation scope.");
        }
        List<String> scopeIds = new ArrayList<>();
        scopeIds.add(orgId);
        membershipService.membersOf(orgId)
                .forEach(m -> scopeIds.add(m.getUserId()));
        Page<AuditLog> page = auditRepository.searchInEntities(scopeIds, filter.action(),
                filter.actorId(), filter.entityType(), filter.entityId(), filter.outcome(),
                filter.from(), filter.to(), pageable);
        return toPaged(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<AuditLogEntry> getGlobalAuditLog(AuditLogFilter filter,
            Pageable pageable) {
        Page<AuditLog> page = auditRepository.search(filter.action(), filter.actorId(),
                filter.entityType(), filter.entityId(), filter.outcome(), filter.from(),
                filter.to(), pageable);
        return toPaged(page);
    }

    /** Resolves actor emails for display (P22 "actor" column). */
    private PagedResponse<AuditLogEntry> toPaged(Page<AuditLog> page) {
        List<String> actorIds = page.getContent().stream()
                .map(AuditLog::getActorId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<String, String> emails = new HashMap<>();
        if (!actorIds.isEmpty()) {
            userRepository.findAllById(actorIds)
                    .forEach(u -> emails.put(u.getId(), u.getEmail()));
        }
        List<AuditLogEntry> content = page.getContent().stream()
                .map(entry -> {
                    AuditLogEntry dto = AuditLogEntry.from(entry);
                    return dto.actorId() != null && emails.containsKey(dto.actorId())
                            ? dto.withActorEmail(emails.get(dto.actorId()))
                            : dto;
                })
                .toList();
        return PagedResponse.of((int) page.getTotalElements(), page.getTotalPages(),
                page.getNumber(), (int) page.getSize(), content);
    }

    public AuditLogEntry record(String action, User actor, String entityType, String entityId,
            Map<String, String> details) {
        return record(action, actor, entityType, entityId, details, "SUCCESS");
    }

    public AuditLogEntry record(String action, User actor, String entityType, String entityId,
            Map<String, String> details, String outcome) {
        AuditLog entry = new AuditLog(action, actor != null ? actor.getId() : null, entityType,
                entityId, toJson(details));
        entry.setOutcome(outcome);
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
