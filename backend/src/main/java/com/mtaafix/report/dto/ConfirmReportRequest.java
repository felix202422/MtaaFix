package com.mtaafix.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Citizen/administrator confirmation closing a resolved report (T14)")
public record ConfirmReportRequest(
        @Schema(description = "Optional citizen feedback about the resolution") String feedback) {
}
