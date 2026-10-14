package com.mtaafix.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Citizen dispute of a resolved report (T15)")
public record DisputeReportRequest(
        @Schema(description = "Mandatory reason for disputing the resolution")
        @NotBlank String disputeReason) {
}
