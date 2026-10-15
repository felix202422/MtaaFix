package com.mtaafix.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to update the signed-in user's profile (full name and mobile only; "
        + "email and password are not editable here)")
public record UpdateProfileRequest(
        @Schema(description = "Full name") @Size(max = 255) String fullName,
        @Schema(description = "Mobile number") @Size(max = 50) String mobile) {
}
