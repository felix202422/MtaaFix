package com.mtaafix.report.controller;

import com.mtaafix.common.response.PagedResponse;
import com.mtaafix.report.dto.PublicReportDetailDto;
import com.mtaafix.report.dto.PublicReportSummaryDto;
import com.mtaafix.report.service.PublicReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Public Reports", description = "Unauthenticated public transparency endpoints (FR-8.3, FR-8.4)")
@RestController
@RequestMapping("/api/v1/reports/public")
public class PublicReportController {

    private final PublicReportService publicReportService;

    public PublicReportController(PublicReportService publicReportService) {
        this.publicReportService = publicReportService;
    }

    @Operation(summary = "Public explore feed (search/category/status/date/bbox/near filters, "
            + "pagination; public fields only, generalized locations per FR-11.4)")
    @GetMapping
    public ResponseEntity<PagedResponse<PublicReportSummaryDto>> list(
            @Parameter(description = "Search in title/description") @RequestParam(required = false) String q,
            @Parameter(description = "Status filter") @RequestParam(required = false) String status,
            @Parameter(description = "Category id filter") @RequestParam(required = false) String categoryId,
            @Parameter(description = "Submitted-from date (YYYY-MM-DD)") @RequestParam(required = false) String dateFrom,
            @Parameter(description = "Submitted-to date (YYYY-MM-DD, inclusive)") @RequestParam(required = false) String dateTo,
            @Parameter(description = "Bounding box 'minLon,minLat,maxLon,maxLat' (PostgreSQL spatial query; "
                    + "cannot be combined with near)") @RequestParam(required = false) String bbox,
            @Parameter(description = "Radius search 'lat,lon,radiusKm' (PostgreSQL spatial query; "
                    + "cannot be combined with bbox)") @RequestParam(required = false) String near,
            @Parameter(description = "Page (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 100));
        return ResponseEntity.ok(publicReportService.list(status, categoryId, q, dateFrom, dateTo,
                bbox, near, PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "reportedAt"))));
    }

    @Operation(summary = "Public report detail by reference code (public fields only)")
    @GetMapping("/{code}")
    public ResponseEntity<PublicReportDetailDto> get(
            @Parameter(description = "Public reference code, e.g. MTF-2026-000042") @PathVariable String code) {
        return ResponseEntity.ok(publicReportService.getByCode(code));
    }
}
