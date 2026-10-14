package com.mtaafix.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to exchange a refresh token for a new token pair")
public record RefreshRequest(
        @Schema(description = "Refresh token") @NotBlank String refreshToken) {
}
