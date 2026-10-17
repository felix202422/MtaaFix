package com.mtaafix.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to change a user's role (FR-5.4, FR-12.1; ADMINISTRATOR only, "
        + "audited). Organisation roles (ORGANIZATION_ADMIN, FIELD_WORKER) must be granted "
        + "through an organisation membership instead.")
public record UpdateRoleRequest(
        @Schema(description = "New role") @NotBlank @Size(max = 30) String role) {
}
