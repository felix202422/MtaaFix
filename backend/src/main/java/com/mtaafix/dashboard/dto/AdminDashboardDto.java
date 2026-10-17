package com.mtaafix.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Schema(description = "Admin dashboard (P14): summary cards, activity, distribution, "
        + "moderation queue, workload, attention and recent reports — all derived from "
        + "real data (FR-7.4)")
public record AdminDashboardDto(
        @Schema(description = "Total reports") long totalReports,
        @Schema(description = "Counts by status") Map<String, Long> byStatus,
        @Schema(description = "Reports created in the last 7 days") long newLast7Days,
        @Schema(description = "Daily report counts for the last 14 days (Africa/Dar_es_Salaam)")
        List<DailyCount> activity14d,
        @Schema(description = "Counts by category") List<CategoryCount> byCategory,
        @Schema(description = "Moderation queue") ModerationQueue moderationQueue,
        @Schema(description = "Assignment workload") Workload workload,
        @Schema(description = "Attention-required counts and items") Attention attention,
        @Schema(description = "Most recent reports (with exact coordinates; staff only)")
        List<RecentReport> recent) {

    @Schema(description = "Count for one day")
    public record DailyCount(LocalDate date, long count) {
    }

    @Schema(description = "Count for one category")
    public record CategoryCount(String categoryId, String name, long count) {
    }

    @Schema(description = "Summary of pending review states")
    public record ModerationQueue(long submitted, long underReview, long needsInformation,
            long resolutionReview, List<RecentReport> items) {
    }

    @Schema(description = "Assignment workload")
    public record Workload(long unassigned, long assigned, List<OrgLoad> byOrganisation,
            List<WorkerLoad> byWorker) {
    }

    @Schema(description = "Assigned reports for one organisation")
    public record OrgLoad(String organisationId, String name, long assignedCount) {
    }

    @Schema(description = "Active assignments per field worker")
    public record WorkerLoad(String userId, String name, long activeAssignments) {
    }

    @Schema(description = "Attention-required items (needs info / awaiting confirmation)")
    public record Attention(long needsInformation, long resolvedAwaitingConfirm,
            List<RecentReport> needsInfoItems) {
    }

    @Schema(description = "Recent report row")
    public record RecentReport(String id, String code, String title, String status,
            String categoryId, String organisationId, Instant reportedAt, Double latitude,
            Double longitude) {
    }
}
