package com.mtaafix.stats.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Public platform statistics (aggregate counts only; FR-8.1)")
public record PublicStatsDto(
        @Schema(description = "Total reports") long totalReports,
        @Schema(description = "Reports in SUBMITTED status") long submittedReports,
        @Schema(description = "Reports in RESOLVED status") long resolvedReports,
        @Schema(description = "Report categories") long categories) {
}
