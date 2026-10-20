package com.mtaafix.auth.service;

import com.mtaafix.auth.domain.PasswordResetToken;
import com.mtaafix.auth.repository.PasswordResetTokenRepository;
import com.mtaafix.common.email.EmailSendException;
import com.mtaafix.common.email.EmailSender;
import com.mtaafix.user.domain.User;
import com.mtaafix.user.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-1.7 / B-X.1: password reset with expiring, single-use, hashed tokens
 * and non-disclosing responses. The raw token leaves the backend only via
 * the reset email and is never persisted or logged.
 */
@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final String INVALID_MESSAGE = "Invalid or expired reset token.";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailSender emailSender;
    private final String webBaseUrl;

    public PasswordResetService(UserRepository userRepository,
            PasswordResetTokenRepository tokenRepository, EmailSender emailSender,
            @Value("${notifications.web-base-url:http://localhost:5173}") String webBaseUrl) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.emailSender = emailSender;
        this.webBaseUrl = webBaseUrl;
    }

    /**
     * Starts a reset for the given email. Returns the raw token only so
     * tests can exercise the full flow; production callers (controllers)
     * must ignore the return value - delivery happens via email only.
     * Returns null when no account matches (callers respond identically
     * either way to avoid disclosing account existence).
     */
    @Transactional
    public String requestReset(String email) {
        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isEmpty()) {
            return null;
        }
        User user = existing.get();
        List<PasswordResetToken> openTokens = tokenRepository.findByUserIdAndUsedAtIsNull(user.getId());
        for (PasswordResetToken openToken : openTokens) {
            openToken.setUsedAt(Instant.now());
            tokenRepository.save(openToken);
        }
        String rawToken = generateRawToken();
        tokenRepository.save(new PasswordResetToken(user, sha256Hex(rawToken),
                Instant.now().plusSeconds(PasswordResetToken.VALIDITY_SECONDS)));
        if (emailSender.isConfigured()) {
            try {
                emailSender.send(user.getEmail(), "Reset your MtaaFix password",
                        resetEmailBody(user, rawToken));
            } catch (EmailSendException ex) {
                log.warn("Password reset email delivery failed (token not logged): {}",
                        ex.getMessage());
            }
        } else {
            log.info("Password reset email skipped: email channel not configured");
        }
        return rawToken;
    }

    /** Consumes a valid token and sets the new password (FR-1.7). */
    @Transactional
    public void reset(String rawToken, String newPassword) {
        PasswordResetToken token = tokenRepository.findByTokenHash(sha256Hex(rawToken))
                .orElseThrow(PasswordResetService::invalidToken);
        if (token.isUsed() || token.getExpiresAt().isBefore(Instant.now())) {
            throw invalidToken();
        }
        User user = token.getUser();
        user.setPasswordHash(hashPassword(newPassword));
        userRepository.save(user);
        token.setUsedAt(Instant.now());
        tokenRepository.save(token);
        // Note: already-issued JWTs stay valid until their natural expiry
        // (stateless tokens, no revocation list) - documented limitation.
    }

    /** Authenticated password change (Phase 13 companion to FR-1.7). */
    @Transactional
    public void change(User user, String currentPassword, String newPassword) {
        if (!org.springframework.security.crypto.bcrypt.BCrypt.checkpw(currentPassword,
                user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }
        user.setPasswordHash(hashPassword(newPassword));
        userRepository.save(user);
    }

    private static String hashPassword(String newPassword) {
        return org.springframework.security.crypto.bcrypt.BCrypt.hashpw(newPassword,
                org.springframework.security.crypto.bcrypt.BCrypt.gensalt());
    }

    private static IllegalArgumentException invalidToken() {
        return new IllegalArgumentException(INVALID_MESSAGE);
    }

    private static String generateRawToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    private String resetEmailBody(User user, String rawToken) {
        return "Hello " + user.getFullName() + ",\n\n"
                + "Someone requested a password reset for your MtaaFix account.\n\n"
                + "Reset link: " + webBaseUrl + "/reset-password?token=" + rawToken + "\n"
                + "The link expires in 30 minutes and can be used once.\n\n"
                + "If you did not request this, you can ignore this message - your password "
                + "has not changed.\n";
    }
}
