package com.mtaafix.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Reviewer returns a completion claim to active work (T12)")
public record ReturnReportRequest(
        @Schema(description = "Mandatory comment explaining what still needs work")
        @NotBlank String comment) {
}
