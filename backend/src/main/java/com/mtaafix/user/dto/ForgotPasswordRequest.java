package com.mtaafix.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request a password reset link. The response is identical whether or "
        + "not the email exists (FR-1.7 non-disclosure).")
public record ForgotPasswordRequest(
        @Schema(description = "Account email address") @Email @NotBlank @Size(max = 255) String email) {
}
