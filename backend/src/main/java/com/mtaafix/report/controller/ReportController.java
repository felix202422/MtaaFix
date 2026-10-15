package com.mtaafix.report.controller;

import com.mtaafix.common.response.PagedResponse;
import com.mtaafix.report.dto.AssignReportRequest;
import com.mtaafix.report.dto.ConfirmReportRequest;
import com.mtaafix.report.dto.CompleteReportRequest;
import com.mtaafix.report.dto.CreateCommentRequest;
import com.mtaafix.report.dto.CreateMediaRequest;
import com.mtaafix.report.dto.CreateReportRequest;
import com.mtaafix.report.dto.DisputeReportRequest;
import com.mtaafix.report.dto.DuplicateReportRequest;
import com.mtaafix.report.dto.NeedsInfoRequest;
import com.mtaafix.report.dto.RejectReportRequest;
import com.mtaafix.report.dto.ReportDto;
import com.mtaafix.report.dto.ReopenReportRequest;
import com.mtaafix.report.dto.ReturnReportRequest;
import com.mtaafix.report.dto.UpdateReportRequest;
import com.mtaafix.report.service.ReportService;
import com.mtaafix.user.domain.User;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Reports", description = "Report lifecycle: create, review, work, resolution, closure")
@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @Operation(summary = "Create a report (category required; unique code generated)")
    @PostMapping
    public ResponseEntity<ReportDto> create(@Valid @RequestBody CreateReportRequest request,
            @AuthenticationPrincipal User user) {
        ReportDto report = reportService.create(request, user);
        return ResponseEntity.status(201).body(report);
    }

    @Operation(summary = "List reports (role-scoped; status/categoryId filters, pagination)")
    @GetMapping
    public ResponseEntity<PagedResponse<ReportDto>> list(
            @Parameter(description = "Status filter") @RequestParam(required = false) String status,
            @Parameter(description = "Category id filter") @RequestParam(required = false) String categoryId,
            @Parameter(description = "Organisation id filter (staff only)") @RequestParam(required = false) String orgId,
            @Parameter(description = "Page (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User user) {
        int safeSize = Math.max(1, Math.min(size, 100));
        return ResponseEntity.ok(reportService.list(status, categoryId, orgId, user,
                PageRequest.of(page, safeSize, Sort.by(Sort.Direction.DESC, "reportedAt"))));
    }

    @Operation(summary = "Get a report (ownership / organisation scope enforced)")
    @GetMapping("/{id}")
    public ResponseEntity<ReportDto> get(@Parameter(description = "Report id") @PathVariable String id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(reportService.get(id, user));
    }

    @Operation(summary = "Update a report (creator only; SUBMITTED or NEEDS_INFORMATION)")
    @PutMapping("/{id}")
    public ResponseEntity<ReportDto> update(@Parameter(description = "Report id") @PathVariable String id,
            @Valid @RequestBody UpdateReportRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(reportService.update(id, request, user));
    }

    @Operation(summary = "Claim a report for review (T2/T17)")
    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('MODERATOR','ADMINISTRATOR')")
    public ResponseEntity<ReportDto> submit(@Parameter(description = "Report id") @PathVariable String id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(reportService.submit(id, user));
    }

    @Operation(summary = "Request more information from the creator (T3)")
    @PostMapping("/{id}/needs-info")
    @PreAuthorize("hasAnyRole('MODERATOR','ADMINISTRATOR')")
    public ResponseEntity<ReportDto> needsInfo(@Parameter(description = "Report id") @PathVariable String id,
            @Valid @RequestBody NeedsInfoRequest request, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(reportService.needsInfo(id, user, request));
    }

    @Operation(summary = "Creator resubmits after providing information (T4)")
    @PostMapping("/{id}/resubmit")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReportDto> resubmit(@Parameter(description = "Report id") @PathVariable String id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(reportService.resubmit(id, user));
    }

    @Operation(summary = "Verify a report (T5)")
    @PostMapping("/{id}/verify")
    @PreAuthorize("hasAnyRole('MODERATOR','ADMINISTRATOR')")
    public ResponseEntity<ReportDto> verify(@Parameter(description = "Report id") @PathVariable String id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(reportService.verify(id, user));
    }

    @Operation(summary = "Reject a report (T6; reasonCode + comment mandatory)")
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('MODERATOR','ADMINISTRATOR')")
    public ResponseEntity<ReportDto> reject(@Parameter(description = "Report id") @PathVariable String id,
            @AuthenticationPrincipal User user, @Valid @RequestBody RejectReportRequest request) {
        return ResponseEntity.ok(reportService.reject(id, user, request));
    }

    @Operation(summary = "Mark a report as duplicate (T7; canonical report link mandatory)")
    @PostMapping("/{id}/duplicate")
    @PreAuthorize("hasAnyRole('MODERATOR','ADMINISTRATOR')")
    public ResponseEntity<ReportDto> duplicate(@Parameter(description = "Report id") @PathVariable String id,
            @AuthenticationPrincipal User user, @Valid @RequestBody DuplicateReportRequest request) {
        return ResponseEntity.ok(reportService.markDuplicate(id, user, request));
    }

    @Operation(summary = "Assign a report (T9/T9r; administrator or owning organisation)")
    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','ORGANIZATION_ADMIN')")
    public ResponseEntity<ReportDto> assign(@Parameter(description = "Report id") @PathVariable String id,
            @AuthenticationPrincipal User user, @Valid @RequestBody AssignReportRequest request) {
        return ResponseEntity.ok(reportService.assign(id, user, request.assigneeId(), request.organisationId()));
    }

    @Operation(summary = "Start work on an assigned report (T10)")
    @PostMapping("/{id}/start")
    @PreAuthorize("hasAnyRole('FIELD_WORKER','ORGANIZATION_ADMIN')")
    public ResponseEntity<ReportDto> start(@Parameter(description = "Report id") @PathVariable String id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(reportService.startWork(id, user));
    }

    @Operation(summary = "Claim completion with evidence (T11; at least one photo required)")
    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('FIELD_WORKER','ORGANIZATION_ADMIN')")
    public ResponseEntity<ReportDto> complete(@Parameter(description = "Report id") @PathVariable String id,
            @Valid @RequestBody CompleteReportRequest request, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(reportService.complete(id, user, request));
    }

    @Operation(summary = "Return a completion claim to active work (T12; comment mandatory)")
    @PostMapping("/{id}/return")
    @PreAuthorize("hasAnyRole('MODERATOR','ADMINISTRATOR','ORGANIZATION_ADMIN')")
    public ResponseEntity<ReportDto> returnToWork(@Parameter(description = "Report id") @PathVariable String id,
            @Valid @RequestBody ReturnReportRequest request, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(reportService.returnToWork(id, user, request));
    }

    @Operation(summary = "Approve a completion claim (T13)")
    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('MODERATOR','ADMINISTRATOR')")
    public ResponseEntity<ReportDto> resolve(@Parameter(description = "Report id") @PathVariable String id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(reportService.resolve(id, user));
    }

    @Operation(summary = "Close a report as administrator (T14)")
    @PostMapping("/{id}/close")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<ReportDto> close(@Parameter(description = "Report id") @PathVariable String id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(reportService.close(id, user));
    }

    @Operation(summary = "Citizen confirms resolution and closes (T14; optional feedback)")
    @PostMapping("/{id}/confirm")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReportDto> confirm(@Parameter(description = "Report id") @PathVariable String id,
            @RequestBody(required = false) @Valid ConfirmReportRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(reportService.confirm(id, user, request));
    }

    @Operation(summary = "Dispute a resolution (T15; disputeReason mandatory)")
    @PostMapping("/{id}/dispute")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReportDto> dispute(@Parameter(description = "Report id") @PathVariable String id,
            @Valid @RequestBody DisputeReportRequest request, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(reportService.dispute(id, user, request));
    }

    @Operation(summary = "Reopen a closed report (T16; justification mandatory)")
    @PostMapping("/{id}/reopen")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<ReportDto> reopen(@Parameter(description = "Report id") @PathVariable String id,
            @Valid @RequestBody ReopenReportRequest request, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(reportService.reopen(id, user, request));
    }

    @Operation(summary = "Comment on a report (FR-2.8)")
    @PostMapping("/{id}/comments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReportDto> comment(@Parameter(description = "Report id") @PathVariable String id,
            @Valid @RequestBody CreateCommentRequest request, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(reportService.addComment(id, user, request));
    }

    @Operation(summary = "Attach photo evidence (FR-9.1; MIME allowlist + 20 MiB limit, FR-9.2)")
    @PostMapping("/{id}/media")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReportDto> media(@Parameter(description = "Report id") @PathVariable String id,
            @Valid @RequestBody CreateMediaRequest request, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(reportService.addMedia(id, user, request.fileName(),
                request.originalName(), request.contentType(), request.sizeBytes(), request.url(),
                request.mediaType()));
    }

    @Operation(summary = "Upload a photo file (multipart; Phase 7 citizen wizard, FR-9.1/9.2)")
    @PostMapping(value = "/{id}/media/upload", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReportDto> uploadMedia(
            @Parameter(description = "Report id") @PathVariable String id,
            @RequestPart("file") org.springframework.web.multipart.MultipartFile file,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(reportService.uploadMedia(id, user, file));
    }

    @Operation(summary = "Cancel own report before review (T8; creator + SUBMITTED only)")
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('CITIZEN')")
    public ResponseEntity<ReportDto> cancel(@Parameter(description = "Report id") @PathVariable String id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(reportService.cancel(id, user));
    }
}
