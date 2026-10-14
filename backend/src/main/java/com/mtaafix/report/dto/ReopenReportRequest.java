package com.mtaafix.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Administrator reopen of a closed report (T16)")
public record ReopenReportRequest(
        @Schema(description = "Mandatory justification for reopening") @NotBlank String justification) {
}
