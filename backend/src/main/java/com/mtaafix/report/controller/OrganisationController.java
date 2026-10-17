package com.mtaafix.report.controller;

import com.mtaafix.report.dto.AddMemberRequest;
import com.mtaafix.report.dto.CreateOrganisationRequest;
import com.mtaafix.report.dto.MemberDto;
import com.mtaafix.report.dto.OrganisationActiveRequest;
import com.mtaafix.report.dto.OrganisationDto;
import com.mtaafix.report.dto.UpdateOrganisationRequest;
import com.mtaafix.report.service.OrganisationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.mtaafix.user.domain.User;

/**
 * FR-5.3/P21: organisation management and team membership (B-X.3).
 * Writes are ADMINISTRATOR (A-scope, audited); ORGANIZATION_ADMIN may read
 * own organisation and manage own members only (matrix areas 8/9).
 */
@Tag(name = "Organizations (administration)")
@RestController
@RequestMapping("/api/v1/organizations")
public class OrganisationController {

    private final OrganisationService organisationService;

    public OrganisationController(OrganisationService organisationService) {
        this.organisationService = organisationService;
    }

    @Operation(summary = "List organisations (ADMIN all; ORG_ADMIN own only)")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','ORGANIZATION_ADMIN')")
    public ResponseEntity<List<OrganisationDto>> list(@AuthenticationPrincipal User actor) {
        return ResponseEntity.ok(organisationService.list(actor));
    }

    @Operation(summary = "Create an organisation (FR-5.3, audited)")
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<OrganisationDto> create(@Valid @RequestBody CreateOrganisationRequest request,
            @AuthenticationPrincipal User actor) {
        return ResponseEntity.status(201).body(organisationService.create(request, actor));
    }

    @Operation(summary = "Get an organisation (ADMIN all; ORG_ADMIN own only)")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','ORGANIZATION_ADMIN')")
    public ResponseEntity<OrganisationDto> get(@PathVariable String id,
            @AuthenticationPrincipal User actor) {
        return ResponseEntity.ok(organisationService.get(id, actor));
    }

    @Operation(summary = "Update an organisation (FR-5.3, audited)")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<OrganisationDto> update(@PathVariable String id,
            @Valid @RequestBody UpdateOrganisationRequest request,
            @AuthenticationPrincipal User actor) {
        return ResponseEntity.ok(organisationService.update(id, request, actor));
    }

    @Operation(summary = "Activate or deactivate an organisation (audited)")
    @PutMapping("/{id}/active")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<OrganisationDto> setActive(@PathVariable String id,
            @Valid @RequestBody OrganisationActiveRequest request,
            @AuthenticationPrincipal User actor) {
        return ResponseEntity.ok(organisationService.setActive(id, request.active(), actor));
    }

    @Operation(summary = "List organisation members (P21)")
    @GetMapping("/{id}/members")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','ORGANIZATION_ADMIN')")
    public ResponseEntity<List<MemberDto>> listMembers(@PathVariable String id,
            @AuthenticationPrincipal User actor) {
        return ResponseEntity.ok(organisationService.listMembers(id, actor));
    }

    @Operation(summary = "Add a member (grants org role, audited)")
    @PostMapping("/{id}/members")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','ORGANIZATION_ADMIN')")
    public ResponseEntity<MemberDto> addMember(@PathVariable String id,
            @Valid @RequestBody AddMemberRequest request,
            @AuthenticationPrincipal User actor) {
        return ResponseEntity.status(201).body(organisationService.addMember(id, request, actor));
    }

    @Operation(summary = "Remove a member (affiliation only; role unchanged, audited)")
    @DeleteMapping("/{id}/members/{userId}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','ORGANIZATION_ADMIN')")
    public ResponseEntity<Void> removeMember(@PathVariable String id, @PathVariable String userId,
            @AuthenticationPrincipal User actor) {
        organisationService.removeMember(id, userId, actor);
        return ResponseEntity.noContent().build();
    }
}
