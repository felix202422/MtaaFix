package com.mtaafix.user.dto;

import com.mtaafix.user.domain.User.Role;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Authentication response")
public record AuthResponse(
        @Schema(description = "Access token") String accessToken,
        @Schema(description = "Refresh token") String refreshToken,
        @Schema(description = "Access token expiration") Long accessTokenExpiresIn,
        @Schema(description = "User") UserDto user) {

    public static AuthResponse of(String accessToken, String refreshToken, Long expiresIn,
            UserDto user) {
        return new AuthResponse(accessToken, refreshToken, expiresIn, user);
    }
}
