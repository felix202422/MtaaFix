package com.sms.service.impl;

import com.sms.config.AppProperties;
import com.sms.dto.request.ChangePasswordRequest;
import com.sms.dto.request.ForgotPasswordRequest;
import com.sms.dto.request.LoginRequest;
import com.sms.dto.request.ResetPasswordRequest;
import com.sms.dto.response.LoginResponse;
import com.sms.entity.User;
import com.sms.exception.BadRequestException;
import com.sms.exception.ResourceNotFoundException;
import com.sms.repository.UserRepository;
import com.sms.security.CustomUserDetails;
import com.sms.security.JwtTokenProvider;
import com.sms.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final AppProperties appProperties;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        if (!user.getIsAccountNonLocked()) {
            throw new LockedException("Account is locked. Please contact administrator.");
        }

        if (!user.getIsEnabled()) {
            throw new BadCredentialsException("Account is disabled.");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

            String accessToken = tokenProvider.generateToken(authentication);
            String refreshToken = request.getRememberMe()
                    ? tokenProvider.generateRefreshToken(authentication, appProperties.getJwt().getRememberMeRefreshExpirationMs())
                    : tokenProvider.generateRefreshToken(authentication);

            user.setLastLogin(LocalDateTime.now());
            user.setFailedLoginAttempts(0);
            userRepository.save(user);

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            // Derive from user.getRoles(): permissions named ROLE_* would otherwise
            // collide with the ROLE_ authority prefix.
            String role = user.getRoles().stream()
                    .map(com.sms.entity.Role::getName)
                    .findFirst().orElse("");

            return LoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType(appProperties.getJwt().getTokenPrefix())
                    .expiresIn(tokenProvider.getExpirationMs())
                    .username(user.getUsername())
                    .fullName(user.getFullName())
                    .email(user.getEmail())
                    .role(role)
                    .userId(user.getId())
                    .build();

        } catch (BadCredentialsException e) {
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
            if (user.getFailedLoginAttempts() >= 5) {
                user.setIsAccountNonLocked(false);
            }
            userRepository.save(user);
            throw new BadCredentialsException("Invalid username or password");
        }
    }

    @Override
    public void logout(String token) {
        log.debug("Logout requested for token");
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.getEmail()));

        String resetToken = UUID.randomUUID().toString();
        user.setResetToken(resetToken);
        user.setResetTokenExpires(LocalDateTime.now().plusHours(24));
        userRepository.save(user);

        log.info("Password reset token generated for user {}: {}", user.getEmail(), resetToken);
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByResetToken(request.getToken())
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset token"));

        if (user.getResetTokenExpires() == null || user.getResetTokenExpires().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Reset token has expired");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setResetToken(null);
        user.setResetTokenExpires(null);
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setFailedLoginAttempts(0);
        user.setIsAccountNonLocked(true);
        userRepository.save(user);
    }

    @Override
    public LoginResponse refreshToken(String refreshToken) {
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new BadRequestException("Invalid refresh token");
        }

        String username = tokenProvider.getUsernameFromToken(refreshToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        CustomUserDetails userDetails = new CustomUserDetails(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());

        String newAccessToken = tokenProvider.generateToken(authentication);

        String role = user.getRoles().stream()
                .map(com.sms.entity.Role::getName)
                .findFirst().orElse("");

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .tokenType(appProperties.getJwt().getTokenPrefix())
                .expiresIn(tokenProvider.getExpirationMs())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(role)
                .userId(user.getId())
                .build();
    }

    public AuthServiceImpl(AuthenticationManager authenticationManager, JwtTokenProvider tokenProvider, AppProperties appProperties, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.appProperties = appProperties;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }


    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuthServiceImpl.class);
}
