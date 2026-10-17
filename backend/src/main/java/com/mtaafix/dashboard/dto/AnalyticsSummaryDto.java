package com.mtaafix.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Schema(description = "Analytics summary (P23): every metric is computed from real report "
        + "data with the applied filters echoed back (FR-7.4)")
public record AnalyticsSummaryDto(
        @Schema(description = "Applied start filter (inclusive)") Instant from,
        @Schema(description = "Applied end filter (inclusive)") Instant to,
        @Schema(description = "Applied organisation filter") String organisationId,
        @Schema(description = "Applied category filter") String categoryId,
        @Schema(description = "Applied status filter") String status,
        @Schema(description = "Applied geographic bounding box") String bbox,
        @Schema(description = "Reports matching the filters") long total,
        @Schema(description = "Counts by status") Map<String, Long> byStatus,
        @Schema(description = "Counts by category") List<Breakdown> byCategory,
        @Schema(description = "Weekly submission counts (Monday buckets, Africa/Dar_es_Salaam)")
        List<WeekCount> weekly,
        @Schema(description = "Average hours from submission to first RESOLVED/CLOSED "
                + "(closed reports only)") Double avgResolutionHours,
        @Schema(description = "Matching reports without an assignment") long unassigned,
        @Schema(description = "Top organisations by matching assigned reports")
        List<OrgRank> organisations) {

    @Schema(description = "Category breakdown row")
    public record Breakdown(String categoryId, String name, long count) {
    }

    @Schema(description = "Weekly count")
    public record WeekCount(LocalDate weekStart, long count) {
    }

    @Schema(description = "Organisation ranking row")
    public record OrgRank(String organisationId, String name, long assignedReports) {
    }
}
