package com.mtaafix.dashboard.controller;

import com.mtaafix.dashboard.dto.AnalyticsSummaryDto;
import com.mtaafix.dashboard.service.AnalyticsService;
import com.mtaafix.user.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Analytics", description = "Analytics and reports (P23; PostgreSQL-only spatial "
        + "filtering — H2 covers parameter validation only)")
@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @Operation(summary = "Analytics summary with date/category/status/organisation/geo filters")
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('MODERATOR','ADMINISTRATOR','ORGANIZATION_ADMIN')")
    public ResponseEntity<AnalyticsSummaryDto> summary(
            @Parameter(description = "Inclusive start (ISO-8601)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant from,
            @Parameter(description = "Inclusive end (ISO-8601)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant to,
            @Parameter(description = "Category id filter") @RequestParam(required = false) String categoryId,
            @Parameter(description = "Status filter") @RequestParam(required = false) String status,
            @Parameter(description = "Organisation id filter (ORG_ADMIN is forced to own org)")
            @RequestParam(required = false) String orgId,
            @Parameter(description = "Bounding box: minLon,minLat,maxLon,maxLat")
            @RequestParam(required = false) String bbox,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(
                analyticsService.summarize(from, to, categoryId, status, orgId, bbox, user));
    }
}
