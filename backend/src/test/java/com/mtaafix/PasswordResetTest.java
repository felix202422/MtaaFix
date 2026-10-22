package com.mtaafix;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtaafix.auth.domain.PasswordResetToken;
import com.mtaafix.auth.repository.PasswordResetTokenRepository;
import com.mtaafix.auth.service.PasswordResetService;
import com.mtaafix.user.domain.User;
import com.mtaafix.user.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** Phase 13 / FR-1.7 / B-X.1: password reset and change-password flows. */
@SpringBootTest(classes = MtaaFixApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PasswordResetTest {

    private static final String PASSWORD = "password123";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordResetTokenRepository tokenRepository;
    @Autowired
    private PasswordResetService passwordResetService;

    private User seedUser() {
        return userRepository.save(new User("pw-" + System.nanoTime() + "@mtaafix.test",
                BCrypt.hashpw(PASSWORD, BCrypt.gensalt()), "Reset Candidate", "+255755555555",
                User.Role.CITIZEN));
    }

    private MvcResult forgot(String email) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\"}"))
                .andExpect(status().isAccepted())
                .andReturn();
    }

    private String login(User user, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + user.getEmail()
                                + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return MAPPER.readTree(result.getResponse().getContentAsString()).get("accessToken")
                .asText();
    }

    private static String sha256Hex(String value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of()
                .formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void forgotIsNonDisclosingForUnknownEmails() throws Exception {
        String unknownBody = forgot("definitely-not-registered-" + System.nanoTime()
                + "@mtaafix.test").getResponse().getContentAsString();
        User user = seedUser();
        String knownBody = forgot(user.getEmail()).getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertEquals(knownBody, unknownBody,
                "responses must not disclose whether an account exists (FR-1.7)");
    }

    @Test
    void malformedEmailIsRejectedWithoutAccountProbing() throws Exception {
        mockMvc.perform(post("/api/v1/auth/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void forgotCreatesHashedExpiringTokenAndResetRoundTrips() throws Exception {
        User user = seedUser();
        String rawToken = passwordResetService.requestReset(user.getEmail());
        org.junit.jupiter.api.Assertions.assertNotNull(rawToken);

        // Phase 16 (D3): capture a live session issued before the reset.
        MvcResult preResetLogin = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + user.getEmail()
                                + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        var preResetTokens = MAPPER.readTree(preResetLogin.getResponse().getContentAsString());
        String oldAccess = preResetTokens.get("accessToken").asText();
        String oldRefresh = preResetTokens.get("refreshToken").asText();
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + oldAccess))
                .andExpect(status().isOk());

        List<PasswordResetToken> tokens = tokenRepository.findByUserIdAndUsedAtIsNull(user.getId());
        org.junit.jupiter.api.Assertions.assertEquals(1, tokens.size());
        PasswordResetToken stored = tokens.get(0);
        org.junit.jupiter.api.Assertions.assertEquals(64, stored.getTokenHash().length(),
                "only the SHA-256 hash may be stored");
        org.junit.jupiter.api.Assertions.assertNotEquals(rawToken, stored.getTokenHash());
        org.junit.jupiter.api.Assertions.assertTrue(
                stored.getExpiresAt().isAfter(Instant.now().plusSeconds(25 * 60L)),
                "token must expire in ~30 minutes");

        mockMvc.perform(post("/api/v1/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + rawToken
                                + "\",\"password\":\"brandnewpass1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").isNotEmpty());

        login(user, "brandnewpass1");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + user.getEmail()
                                + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isUnauthorized());

        // Phase 16 (D3): the reset revoked every token issued before it.
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + oldAccess))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + oldRefresh + "\"}"))
                .andExpect(status().isUnauthorized());

        // Single-use: the same token must not work twice.
        mockMvc.perform(post("/api/v1/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + rawToken
                                + "\",\"password\":\"anotherpass1\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void expiredTokensAreRejected() throws Exception {
        User user = seedUser();
        String rawToken = "expired-token-value-0123456789";
        tokenRepository.save(new PasswordResetToken(user, sha256Hex(rawToken),
                Instant.now().minusSeconds(60)));

        mockMvc.perform(post("/api/v1/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + rawToken + "\",\"password\":\"brandnewpass1\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requestingAgainInvalidatesEarlierOpenTokens() throws Exception {
        User user = seedUser();
        String first = passwordResetService.requestReset(user.getEmail());
        String second = passwordResetService.requestReset(user.getEmail());

        mockMvc.perform(post("/api/v1/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + first + "\",\"password\":\"brandnewpass1\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + second + "\",\"password\":\"brandnewpass1\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void unknownTokensAreRejectedGenerically() throws Exception {
        mockMvc.perform(post("/api/v1/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"0123456789abcdef\",\"password\":\"brandnewpass1\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changePasswordVerifiesCurrentAndRotates() throws Exception {
        User user = seedUser();
        MvcResult initial = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + user.getEmail()
                                + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        var preChange = MAPPER.readTree(initial.getResponse().getContentAsString());
        String token = "Bearer " + preChange.get("accessToken").asText();
        String oldRefresh = preChange.get("refreshToken").asText();

        mockMvc.perform(post("/api/v1/auth/password/change")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"wrongpass1\","
                                + "\"newPassword\":\"brandnewpass1\"}"))
                .andExpect(status().isBadRequest());

        // Phase 16 (D3): a successful change revokes the pre-change token pair
        // and returns a fresh one for the session that performed the change.
        MvcResult changed = mockMvc.perform(post("/api/v1/auth/password/change")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"" + PASSWORD
                                + "\",\"newPassword\":\"brandnewpass1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();
        var rotated = MAPPER.readTree(changed.getResponse().getContentAsString());
        String oldAccess = token.substring("Bearer ".length());
        String newAccess = rotated.get("accessToken").asText();
        String newRefresh = rotated.get("refreshToken").asText();

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + oldAccess))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + oldRefresh + "\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + newAccess))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + newRefresh + "\"}"))
                .andExpect(status().isOk());

        login(user, "brandnewpass1");
        mockMvc.perform(post("/api/v1/auth/password/change")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"" + PASSWORD
                                + "\",\"newPassword\":\"anotherpass1\"}"))
                .andExpect(status().isUnauthorized());
    }
}
