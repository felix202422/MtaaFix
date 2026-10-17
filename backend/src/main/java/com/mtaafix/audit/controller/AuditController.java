package com.mtaafix.audit.controller;

import com.mtaafix.audit.dto.AuditLogEntry;
import com.mtaafix.audit.dto.AuditLogFilter;
import com.mtaafix.audit.service.AuditService;
import com.mtaafix.common.response.PagedResponse;
import com.mtaafix.user.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Audit", description = "Audit log endpoints (FR-10.x, P22)")
@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @Operation(summary = "Get organisation audit log (ORG_ADMIN own scope, filtered)")
    @GetMapping("/organizations/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','ORGANIZATION_ADMIN')")
    public ResponseEntity<PagedResponse<AuditLogEntry>> orgAudit(
            @PathVariable String id,
            @Parameter(description = "Action filter (exact)")
            @RequestParam(required = false) String action,
            @Parameter(description = "Actor user id filter")
            @RequestParam(required = false) String actorId,
            @Parameter(description = "Entity type filter (exact)")
            @RequestParam(required = false) String entityType,
            @Parameter(description = "Entity id filter (exact)")
            @RequestParam(required = false) String entityId,
            @Parameter(description = "Outcome filter (SUCCESS/DENIED)")
            @RequestParam(required = false) String outcome,
            @Parameter(description = "Inclusive start timestamp (ISO-8601)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant from,
            @Parameter(description = "Inclusive end timestamp (ISO-8601)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant to,
            @Parameter(description = "Page (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(auditService.getOrganisationAuditLog(id, user,
                new AuditLogFilter(action, actorId, entityType, entityId, outcome, from, to),
                pageable(page, size)));
    }

    @Operation(summary = "Get global audit log (admin only, filtered)")
    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<PagedResponse<AuditLogEntry>> globalAudit(
            @Parameter(description = "Action filter (exact)")
            @RequestParam(required = false) String action,
            @Parameter(description = "Actor user id filter")
            @RequestParam(required = false) String actorId,
            @Parameter(description = "Entity type filter (exact)")
            @RequestParam(required = false) String entityType,
            @Parameter(description = "Entity id filter (exact)")
            @RequestParam(required = false) String entityId,
            @Parameter(description = "Outcome filter (SUCCESS/DENIED)")
            @RequestParam(required = false) String outcome,
            @Parameter(description = "Inclusive start timestamp (ISO-8601)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant from,
            @Parameter(description = "Inclusive end timestamp (ISO-8601)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant to,
            @Parameter(description = "Page (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(auditService.getGlobalAuditLog(
                new AuditLogFilter(action, actorId, entityType, entityId, outcome, from, to),
                pageable(page, size)));
    }

    private static PageRequest pageable(int page, int size) {
        int safeSize = Math.max(1, Math.min(size, 200));
        return PageRequest.of(Math.max(0, page), safeSize,
                Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
