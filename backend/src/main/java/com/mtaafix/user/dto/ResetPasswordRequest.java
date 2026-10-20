package com.mtaafix.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Complete a password reset with a single-use expiring token (FR-1.7)")
public record ResetPasswordRequest(
        @Schema(description = "Raw reset token from the email link") @NotBlank @Size(min = 10, max = 200) String token,
        @Schema(description = "New password") @NotBlank @Size(min = 8, max = 128) String password) {
}
