package com.mtaafix.user.dto;

import com.mtaafix.user.domain.User.Role;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "User representation")
public record UserDto(
        @Schema(description = "User id") String id,
        @Schema(description = "Email") String email,
        @Schema(description = "Full name") String fullName,
        @Schema(description = "Mobile number") String mobile,
        @Schema(description = "Role") Role role,
        @Schema(description = "Organisation id") String organisationId) {

    public static UserDto from(UserDto dto) {
        return dto;
    }
}
