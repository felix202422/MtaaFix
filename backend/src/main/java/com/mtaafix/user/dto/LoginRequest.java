package com.mtaafix.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to log in")
public record LoginRequest(
        @Schema(description = "Email address") @Email @NotBlank String email,
        @Schema(description = "Password") @NotBlank String password) {
}
