package com.mtaafix.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to assign a report to a user")
public record AssignReportRequest(
        @Schema(description = "User id of the assignee") @NotBlank String assigneeId,
        @Schema(description = "Organisation id, when assigning on behalf of an organisation") String organisationId) {
}
