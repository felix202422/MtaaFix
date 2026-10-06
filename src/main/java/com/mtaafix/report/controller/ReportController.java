package com.mtaafix.report.controller;

import com.mtaafix.common.response.SimpleResponse;
import com.mtaafix.report.dto.CreateReportRequest;
import com.mtaafix.report.dto.ReportDto;
import com.mtaafix.report.dto.UpdateReportRequest;
import com.mtaafix.report.service.ReportService;
import com.mtaafix.user.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Reports", description = "Report lifecycle: create, submit, verify, reject, assign, resolve, close")
@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @Operation(summary = "Create a report")
    @PostMapping
    public ResponseEntity<ReportDto> create(@Valid @RequestBody CreateReportRequest request,
            @AuthenticationPrincipal User user) {
        ReportDto report = reportService.create(request, user);
        return ResponseEntity.status(201).body(report);
    }

    @Operation(summary = "Get a report")
    @GetMapping("/{id}")
    public ResponseEntity<ReportDto> get(@Parameter(description = "Report id") @PathVariable String id) {
        return ResponseEntity.ok(reportService.get(id));
    }

    @Operation(summary = "Update a report (creator only)")
    @PutMapping("/{id}")
    public ResponseEntity<ReportDto> update(@Parameter(description = "Report id") @PathVariable String id,
            @Valid @RequestBody UpdateReportRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(reportService.create(request, user));
    }

    @Operation(summary = "Submit a report for review")
    @PostMapping("/{id}/submit")
    public ResponseEntity<ReportDto> submit(@Parameter(description = "Report id") @PathVariable String id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(reportService.submit(id, user));
    }

    @Operation(summary = "Verify a report")
    @PostMapping("/{id}/verify")
    public ResponseEntity<ReportDto> verify(@Parameter(description = "Report id") @PathVariable String id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(reportService.verify(id, user));
    }

    @Operation(summary = "Reject a report")
    @PostMapping("/{id}/reject")
    public ResponseEntity<ReportDto> reject(@Parameter(description = "Report id") @PathVariable String id,
            @AuthenticationPrincipal User user, @RequestBody String comment) {
        return ResponseEntity.ok(reportService.reject(id, user, comment));
    }

    @Operation(summary = "Assign a report")
    @PostMapping("/{id}/assign")
    public ResponseEntity<ReportDto> assign(@Parameter(description = "Report id") @PathVariable String id,
            @AuthenticationPrincipal User user, @RequestBody String assigneeId) {
        return ResponseEntity.ok(reportService.assign(id, user, assigneeId));
    }

    @Operation(summary = "Resolve a report")
    @PostMapping("/{id}/resolve")
    public ResponseEntity<ReportDto> resolve(@Parameter(description = "Report id") @PathVariable String id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(reportService.resolve(id, user));
    }

    @Operation(summary = "Close a report")
    @PostMapping("/{id}/close")
    public ResponseEntity<ReportDto> close(@Parameter(description = "Report id") @PathVariable String id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(reportService.close(id, user));
    }
}
