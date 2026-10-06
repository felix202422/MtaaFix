package com.mtaafix.user.dto;

import com.mtaafix.user.domain.User.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to register a new user")
public record RegisterRequest(
        @Schema(description = "Email address") @Email @NotBlank @Size(max = 255) String email,
        @Schema(description = "Password") @NotBlank @Size(min = 8, max = 128) String password,
        @Schema(description = "Full name") @NotBlank @Size(max = 255) String fullName,
        @Schema(description = "Mobile number") @Size(max = 50) String mobile,
        @Schema(description = "Role") @NotBlank Role role) {

    public static RegisterRequest of(String email, String password, String fullName, String mobile,
            Role role) {
        return new RegisterRequest(email, password, fullName, mobile, role);
    }
}
