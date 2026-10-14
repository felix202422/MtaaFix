package com.mtaafix.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to reject a report (reason code + comment mandatory, T6/T8)")
public record RejectReportRequest(
        @Schema(description = "Reason code, e.g. REJECTED_AS_POLICY or CANCELLED_BY_CITIZEN")
        @NotBlank String reasonCode,
        @Schema(description = "Comment explaining the rejection") @NotBlank String comment) {
}
