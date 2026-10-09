package com.mtaafix.audit.controller;

import com.mtaafix.common.response.SimpleResponse;
import com.mtaafix.audit.dto.AuditLogEntry;
import com.mtaafix.audit.service.AuditService;
import com.mtaafix.user.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Audit", description = "Audit log endpoints")
@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @Operation(summary = "Get organisation audit log")
    @GetMapping("/organizations/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('ORGANIZATION_ADMIN')")
    public ResponseEntity<List<AuditLogEntry>> orgAudit(
            @PathVariable String id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(auditService.getOrganisationAuditLog(id));
    }

    @Operation(summary = "Get global audit log (admin only)")
    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<List<AuditLogEntry>> globalAudit() {
        return ResponseEntity.ok(auditService.getGlobalAuditLog());
    }
}
