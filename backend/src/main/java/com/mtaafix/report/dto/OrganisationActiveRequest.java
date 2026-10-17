package com.mtaafix.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to activate or deactivate an organisation account (P21; "
        + "ADMINISTRATOR only, audited). Deactivated orgs cannot receive new assignments.")
public record OrganisationActiveRequest(
        @Schema(description = "Whether the organisation is active") @NotNull Boolean active) {
}
