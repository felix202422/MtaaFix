package com.mtaafix.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Field-worker completion claim (T11); at least one PHOTO must already be attached")
public record CompleteReportRequest(
        @Schema(description = "Optional completion note") String completionNote) {
}
