package com.mtaafix.auth.controller;

import com.mtaafix.auth.service.PasswordResetService;
import com.mtaafix.common.response.SimpleResponse;
import com.mtaafix.user.domain.User;
import com.mtaafix.user.dto.AuthResponse;
import com.mtaafix.user.dto.ChangePasswordRequest;
import com.mtaafix.user.dto.ForgotPasswordRequest;
import com.mtaafix.user.dto.LoginRequest;
import com.mtaafix.user.dto.RegisterRequest;
import com.mtaafix.user.dto.RefreshRequest;
import com.mtaafix.user.dto.ResetPasswordRequest;
import com.mtaafix.user.dto.UpdateProfileRequest;
import com.mtaafix.user.dto.UserDto;
import com.mtaafix.user.service.UserService;
import com.mtaafix.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication", description = "Registration, login, refresh and logout")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserService userService;
    private final JwtTokenProvider tokenProvider;
    private final PasswordResetService passwordResetService;

    public AuthController(UserService userService, JwtTokenProvider tokenProvider,
            PasswordResetService passwordResetService) {
        this.userService = userService;
        this.tokenProvider = tokenProvider;
        this.passwordResetService = passwordResetService;
    }

    @Operation(summary = "Register a new user")
    @PostMapping("/register")
    public ResponseEntity<UserDto> register(@Valid @RequestBody RegisterRequest request) {
        UserDto user = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @Operation(summary = "Log in")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(userService.authenticate(request.email(), request.password()));
    }

    @Operation(summary = "Refresh an access token")
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @Valid @RequestBody RefreshRequest request) {
        User user = userService.refresh(request.refreshToken());
        String accessToken = tokenProvider.generateAccessToken(user.getId(), user.getRole().name());
        String newRefreshToken = tokenProvider.generateRefreshToken(user.getId());
        return ResponseEntity.ok(AuthResponse.of(accessToken, newRefreshToken, 900L,
                userService.toDto(user)));
    }

    @Operation(summary = "Log out")
    @PostMapping("/logout")
    public ResponseEntity<SimpleResponse> logout() {
        return ResponseEntity.ok(SimpleResponse.ok("Logged out"));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<UserDto> me(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(userService.toDto(user));
    }

    @Operation(summary = "Update own profile (full name / mobile only)")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/me")
    public ResponseEntity<UserDto> updateMe(@AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(user, request));
    }

    @Operation(summary = "Request a password reset link (always 202, non-disclosing - FR-1.7)")
    @PostMapping("/password/forgot")
    public ResponseEntity<SimpleResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestReset(request.email());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(SimpleResponse.ok("If that email address is registered, a password reset "
                        + "link has been sent."));
    }

    @Operation(summary = "Complete a password reset with a single-use token (FR-1.7)")
    @PostMapping("/password/reset")
    public ResponseEntity<SimpleResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.reset(request.token(), request.password());
        return ResponseEntity.ok(SimpleResponse.ok("Password has been reset"));
    }

    @Operation(summary = "Change own password while authenticated")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/password/change")
    public ResponseEntity<SimpleResponse> changePassword(@AuthenticationPrincipal User user,
            @Valid @RequestBody ChangePasswordRequest request) {
        passwordResetService.change(user, request.currentPassword(), request.newPassword());
        return ResponseEntity.ok(SimpleResponse.ok("Password changed"));
    }
}
