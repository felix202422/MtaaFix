package com.mtaafix.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "User dashboard summary")
public record UserDashboardDto(
        @Schema(description = "User initials") String initials,
        @Schema(description = "User full name") String name,
        @Schema(description = "User role") String role,
        @Schema(description = "Total reports") long totalReports,
        @Schema(description = "Submitted reports") long submittedReports,
        @Schema(description = "Verified reports") long verifiedReports,
        @Schema(description = "Closed reports") long closedReports) {
}
