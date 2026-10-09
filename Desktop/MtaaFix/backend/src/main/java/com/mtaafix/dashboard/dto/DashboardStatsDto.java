package com.mtaafix.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "Dashboard statistics")
public record DashboardStatsDto(
        @Schema(description = "Organisation ID (null for global)") String organisationId,
        @Schema(description = "Statistics by status") Map<String, Long> stats) {
}
