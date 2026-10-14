package com.mtaafix.report.controller;

import com.mtaafix.report.service.ReportService;
import com.mtaafix.user.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Media", description = "Report media removal")
@RestController
@RequestMapping("/api/v1/media")
public class MediaController {

    private final ReportService reportService;

    public MediaController(ReportService reportService) {
        this.reportService = reportService;
    }

    @Operation(summary = "Remove own media from a report (FR-9.2)")
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(@Parameter(description = "Media id") @PathVariable String id,
            @AuthenticationPrincipal User user) {
        reportService.deleteMedia(id, user);
        return ResponseEntity.noContent().build();
    }
}
