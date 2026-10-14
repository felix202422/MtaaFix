package com.mtaafix.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request more information from the report creator (T3)")
public record NeedsInfoRequest(
        @Schema(description = "Message describing what information is required")
        @NotBlank String message) {
}
