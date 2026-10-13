package com.mtaafix.audit.service;

import com.mtaafix.audit.dto.AuditLogEntry;
import com.mtaafix.audit.repository.AuditRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuditService {

    private final AuditRepository auditRepository;

    public AuditService(AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    public List<AuditLogEntry> getOrganisationAuditLog(String orgId) {
        return auditRepository.findByEntityId(orgId).stream()
                .map(AuditLogEntry::from)
                .collect(Collectors.toList());
    }

    public List<AuditLogEntry> getGlobalAuditLog() {
        return auditRepository.findAll().stream()
                .map(AuditLogEntry::from)
                .collect(Collectors.toList());
    }
}
