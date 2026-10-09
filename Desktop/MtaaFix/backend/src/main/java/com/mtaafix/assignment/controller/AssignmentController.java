package com.mtaafix.assignment.controller;

import com.mtaafix.common.response.SimpleResponse;
import com.mtaafix.assignment.dto.AssignmentDto;
import com.mtaafix.assignment.service.AssignmentService;
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

@Tag(name = "Assignments", description = "Assignment management endpoints")
@RestController
@RequestMapping("/api/v1/assignments")
public class AssignmentController {

    private final AssignmentService assignmentService;

    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @Operation(summary = "Get my assignments")
    @GetMapping("/me")
    public ResponseEntity<List<AssignmentDto>> myAssignments(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(assignmentService.getAssignmentsByAssignee(user.getId()));
    }

    @Operation(summary = "Get organisation's reports")
    @GetMapping("/organizations/{orgId}/reports")
    @PreAuthorize("hasRole('ORGANIZATION_ADMIN')")
    public ResponseEntity<List<AssignmentDto>> orgReports(@PathVariable String orgId) {
        return ResponseEntity.ok(assignmentService.getReportsByOrganisation(orgId));
    }
}
