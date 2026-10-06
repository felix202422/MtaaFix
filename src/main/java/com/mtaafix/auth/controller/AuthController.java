package com.mtaafix.auth.controller;

import com.mtaafix.common.response.SimpleResponse;
import com.mtaafix.user.domain.User;
import com.mtaafix.user.dto.AuthResponse;
import com.mtaafix.user.dto.LoginRequest;
import com.mtaafix.user.dto.RegisterRequest;
import com.mtaafix.user.dto.UserDto;
import com.mtaafix.user.service.UserService;
import com.mtaafix.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication", description = "Registration, login, refresh and logout")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserService userService;
    private final JwtTokenProvider tokenProvider;

    public AuthController(UserService userService, JwtTokenProvider tokenProvider) {
        this.userService = userService;
        this.tokenProvider = tokenProvider;
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
        UserDto user = userService.authenticate(request.email(), request.password());
        String accessToken = tokenProvider.generateAccessToken(user.id(), user.role().name());
        String refreshToken = tokenProvider.generateRefreshToken(user.id());
        return ResponseEntity.ok(AuthResponse.of(accessToken, refreshToken, 900L, user));
    }

    @Operation(summary = "Refresh an access token")
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody String refreshToken) {
        if (!tokenProvider.validateToken(refreshToken)) {
            return ResponseEntity.badRequest().build();
        }
        String subject = tokenProvider.getSubject(refreshToken);
        UserDto user = userService.findByEmail(subject);
        String accessToken = tokenProvider.generateAccessToken(user.id(), user.role().name());
        return ResponseEntity.ok(AuthResponse.of(accessToken, refreshToken, 900L, user));
    }

    @Operation(summary = "Log out")
    @PostMapping("/logout")
    public ResponseEntity<SimpleResponse> logout() {
        return ResponseEntity.ok(SimpleResponse.ok("Logged out"));
    }

    @PreAuthorize("hasRole('CITIZEN')")
    @GetMapping("/me")
    public ResponseEntity<UserDto> me(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(userService.findByEmail(user.getEmail()));
    }
}
