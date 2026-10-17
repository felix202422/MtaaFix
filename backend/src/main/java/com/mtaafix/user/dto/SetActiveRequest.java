package com.mtaafix.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to activate or deactivate a user account (FR-12.1; "
        + "ADMINISTRATOR only, audited). Deactivated users are rejected on every request.")
public record SetActiveRequest(
        @Schema(description = "Whether the account is active") @NotNull Boolean active) {
}
