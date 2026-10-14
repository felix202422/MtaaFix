package com.mtaafix;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtaafix.security.JwtTokenProvider;
import com.mtaafix.user.domain.User;
import com.mtaafix.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest(classes = MtaaFixApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowTest {

    private static final String PASSWORD = "password123";

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private UserRepository userRepository;

    private String uniqueEmail() {
        return "auth-" + System.nanoTime() + "@mtaafix.test";
    }

    private MockHttpServletRequestBuilder loginRequest(String email, String password) {
        String body = "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
        return post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private MvcResult registerUser(String email) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD
                                + "\",\"fullName\":\"Auth Test User\",\"mobile\":\"+255712345678\"}"))
                .andExpect(status().isCreated())
                .andReturn();
    }

    private String loginOk(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(loginRequest(email, password))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("accessToken").asText();
    }

    @Test
    void registerAlwaysCreatesCitizenEvenWhenRoleSubmitted() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD
                                + "\",\"fullName\":\"Role Attempt\",\"mobile\":\"+255712345678\","
                                + "\"role\":\"ADMINISTRATOR\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("CITIZEN"));
    }

    @Test
    void registerRejectsDuplicateEmail() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD
                                + "\",\"fullName\":\"First User\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD
                                + "\",\"fullName\":\"Second User\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void loginReturnsTypedTokenPair() throws Exception {
        String email = uniqueEmail();
        registerUser(email);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();

        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        String access = node.get("accessToken").asText();
        String refresh = node.get("refreshToken").asText();
        org.junit.jupiter.api.Assertions.assertEquals(JwtTokenProvider.TYPE_ACCESS,
                tokenProvider.getType(access));
        org.junit.jupiter.api.Assertions.assertEquals(JwtTokenProvider.TYPE_REFRESH,
                tokenProvider.getType(refresh));
        org.junit.jupiter.api.Assertions.assertEquals("CITIZEN",
                tokenProvider.getRole(access));
    }

    @Test
    void loginWithWrongPasswordReturns401() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD
                                + "\",\"fullName\":\"Wrong Password\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(loginRequest(email, "wrong-password-123"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void loginWithUnknownEmailReturns401WithoutEnumeration() throws Exception {
        mockMvc.perform(loginRequest(uniqueEmail() + "-missing", PASSWORD))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void loginForInactiveAccountReturns403() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD
                                + "\",\"fullName\":\"Inactive User\"}"))
                .andExpect(status().isCreated());
        User user = userRepository.findByEmail(email).orElseThrow();
        user.setActive(false);
        userRepository.save(user);

        mockMvc.perform(loginRequest(email, PASSWORD))
                .andExpect(status().isForbidden());
    }

    @Test
    void refreshRotatesTokenPair() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD
                                + "\",\"fullName\":\"Refresh Flow\"}"))
                .andExpect(status().isCreated());
        JsonNode loginNode = objectMapper.readTree(
                mockMvc.perform(loginRequest(email, PASSWORD)).andReturn()
                        .getResponse().getContentAsString());
        String refresh = loginNode.get("refreshToken").asText();

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refresh + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    void refreshRejectsAccessTokenAsRefreshToken() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD
                                + "\",\"fullName\":\"Typ Confusion\"}"))
                .andExpect(status().isCreated());
        JsonNode loginNode = objectMapper.readTree(
                mockMvc.perform(loginRequest(email, PASSWORD)).andReturn()
                        .getResponse().getContentAsString());
        String access = loginNode.get("accessToken").asText();

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + access + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }

    @Test
    void refreshRejectsGarbageToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"garbage-token\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }

    @Test
    void meRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meRejectsRefreshTokenUsedAsBearer() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD
                                + "\",\"fullName\":\"Bearer Confusion\"}"))
                .andExpect(status().isCreated());
        JsonNode loginNode = objectMapper.readTree(
                mockMvc.perform(loginRequest(email, PASSWORD)).andReturn()
                        .getResponse().getContentAsString());
        String refresh = loginNode.get("refreshToken").asText();

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + refresh))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meReturnsProfileForValidAccessToken() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD
                                + "\",\"fullName\":\"Profile User\"}"))
                .andExpect(status().isCreated());
        String token = loginOk(email, PASSWORD);

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("CITIZEN"));
    }

    @Test
    void protectedEndpointWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenOfDeactivatedUserStopsWorkingImmediately() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD
                                + "\",\"fullName\":\"Revoked User\"}"))
                .andExpect(status().isCreated());
        String token = loginOk(email, PASSWORD);

        mockMvc.perform(get("/api/v1/dashboard/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        User user = userRepository.findByEmail(email).orElseThrow();
        user.setActive(false);
        userRepository.save(user);

        mockMvc.perform(get("/api/v1/dashboard/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void roleChangeAppliesWithoutReauthentication() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD
                                + "\",\"fullName\":\"Role Change\"}"))
                .andExpect(status().isCreated());
        String token = loginOk(email, PASSWORD);

        mockMvc.perform(get("/api/v1/dashboard/stats")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        User user = userRepository.findByEmail(email).orElseThrow();
        user.setRole(User.Role.MODERATOR);
        userRepository.save(user);

        mockMvc.perform(get("/api/v1/dashboard/stats")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void passwordIsStoredHashedNotInPlaintext() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD
                                + "\",\"fullName\":\"Hash Check\"}"))
                .andExpect(status().isCreated());

        User user = userRepository.findByEmail(email).orElseThrow();
        org.junit.jupiter.api.Assertions.assertNotEquals(PASSWORD, user.getPasswordHash());
        org.junit.jupiter.api.Assertions.assertTrue(
                BCrypt.checkpw(PASSWORD, user.getPasswordHash()));
    }
}
