package com.mtaafix.assignment.controller;

import com.mtaafix.assignment.dto.AssignmentDto;
import com.mtaafix.assignment.service.AssignmentService;
import com.mtaafix.common.response.PagedResponse;
import com.mtaafix.user.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Assignments", description = "Assignment management endpoints")
@RestController
@RequestMapping("/api/v1/assignments")
public class AssignmentController {

    private final AssignmentService assignmentService;

    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @Operation(summary = "Get my assignments (paginated, NFR-7)")
    @GetMapping("/me")
    @PreAuthorize("hasRole('FIELD_WORKER')")
    public ResponseEntity<PagedResponse<AssignmentDto>> myAssignments(
            @Parameter(description = "Page (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 100)") @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User user) {
        int safeSize = Math.max(1, Math.min(size, 100));
        return ResponseEntity.ok(assignmentService.getAssignmentsByAssignee(user.getId(),
                PageRequest.of(page, safeSize)));
    }

    @Operation(summary = "Get organisation's reports (paginated, NFR-7)")
    @GetMapping("/organizations/{orgId}/reports")
    @PreAuthorize("hasAnyRole('ORGANIZATION_ADMIN','ADMINISTRATOR')")
    public ResponseEntity<PagedResponse<AssignmentDto>> orgReports(@PathVariable String orgId,
            @Parameter(description = "Page (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 100)") @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User user) {
        int safeSize = Math.max(1, Math.min(size, 100));
        return ResponseEntity.ok(assignmentService.getReportsByOrganisation(orgId, user,
                PageRequest.of(page, safeSize)));
    }
}
