package com.mtaafix.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to add a member to an organisation (P21, B-X.3). Grants the "
        + "org role (FR-5.4, audited) and creates the membership in one step.")
public record AddMemberRequest(
        @Schema(description = "User id to add") @NotBlank @Size(max = 36) String userId,
        @Schema(description = "Org role: ORGANIZATION_ADMIN or FIELD_WORKER")
        @NotBlank @Pattern(regexp = "^(ORGANIZATION_ADMIN|FIELD_WORKER)$",
                message = "role must be ORGANIZATION_ADMIN or FIELD_WORKER")
        String role) {
}
