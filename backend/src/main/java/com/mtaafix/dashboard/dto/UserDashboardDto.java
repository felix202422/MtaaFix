package com.mtaafix.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "User dashboard summary with attention-required data (FR-7.1, FR-7.4)")
public record UserDashboardDto(
        @Schema(description = "User initials") String initials,
        @Schema(description = "User full name") String name,
        @Schema(description = "User role") String role,
        @Schema(description = "Total reports") long totalReports,
        @Schema(description = "Submitted reports") long submittedReports,
        @Schema(description = "Verified reports") long verifiedReports,
        @Schema(description = "Closed reports") long closedReports,
        @Schema(description = "Reports in NEEDS_INFORMATION (creator must act)") long needsInfoCount,
        @Schema(description = "RESOLVED reports awaiting citizen confirm/dispute") long resolvedAwaitingConfirmCount,
        @Schema(description = "Reports currently active (not closed/rejected/duplicate)") long activeCount,
        @Schema(description = "Attention-required reports (needs-info + resolved), capped at 5")
        List<AttentionReport> attentionReports) {

    @Schema(description = "Attention-required report reference")
    public record AttentionReport(String id, String code, String title, String status) {
    }
}
