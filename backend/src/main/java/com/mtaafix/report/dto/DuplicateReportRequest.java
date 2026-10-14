package com.mtaafix.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Mark a report as duplicate of a canonical report (T7)")
public record DuplicateReportRequest(
        @Schema(description = "Canonical report code (MTF-YYYY-NNNNNN) or id")
        @NotBlank String canonicalReportRef) {
}
