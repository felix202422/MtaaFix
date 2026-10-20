package com.mtaafix.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Change own password while authenticated")
public record ChangePasswordRequest(
        @Schema(description = "Current password") @NotBlank @Size(max = 128) String currentPassword,
        @Schema(description = "New password") @NotBlank @Size(min = 8, max = 128) String newPassword) {
}
