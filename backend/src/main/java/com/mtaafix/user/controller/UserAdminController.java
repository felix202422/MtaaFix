package com.mtaafix.user.controller;

import com.mtaafix.common.response.PagedResponse;
import com.mtaafix.user.dto.SetActiveRequest;
import com.mtaafix.user.dto.UpdateRoleRequest;
import com.mtaafix.user.dto.UserDto;
import com.mtaafix.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.mtaafix.user.domain.User;

/**
 * FR-12.1 user management: search, role changes, activate/deactivate.
 * ADMINISTRATOR only (matrix area 12); every mutation is audited (FR-12.4).
 */
@Tag(name = "Users (administration)")
@RestController
@RequestMapping("/api/v1/users")
@PreAuthorize("hasRole('ADMINISTRATOR')")
public class UserAdminController {

    private final UserService userService;

    public UserAdminController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Search users (FR-12.1: text + role + active filters)")
    @GetMapping
    public ResponseEntity<PagedResponse<UserDto>> search(
            @Parameter(description = "Text search over email and full name")
            @RequestParam(required = false) String q,
            @Parameter(description = "Role filter") @RequestParam(required = false) String role,
            @Parameter(description = "Active-account filter") @RequestParam(required = false) Boolean active,
            @Parameter(description = "Page (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User actor) {
        int safeSize = Math.max(1, Math.min(size, 100));
        var result = userService.search(q, role, active,
                PageRequest.of(page, safeSize, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ResponseEntity.ok(PagedResponse.of((int) result.getTotalElements(),
                result.getTotalPages(), result.getNumber(), (int) result.getSize(),
                result.getContent()));
    }

    @Operation(summary = "Change a user's role (FR-5.4/FR-12.1, audited)")
    @PutMapping("/{id}/role")
    public ResponseEntity<UserDto> changeRole(@PathVariable String id,
            @Valid @RequestBody UpdateRoleRequest request,
            @AuthenticationPrincipal User actor) {
        return ResponseEntity.ok(userService.changeRole(id, request, actor));
    }

    @Operation(summary = "Activate or deactivate a user account (FR-12.1, audited)")
    @PutMapping("/{id}/active")
    public ResponseEntity<UserDto> setActive(@PathVariable String id,
            @Valid @RequestBody SetActiveRequest request,
            @AuthenticationPrincipal User actor) {
        return ResponseEntity.ok(userService.setActive(id, request, actor));
    }
}
