package com.mtaafix;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtaafix.report.domain.IdempotencyKey;
import com.mtaafix.report.repository.IdempotencyKeyRepository;
import com.mtaafix.report.repository.ReportCategoryRepository;
import com.mtaafix.user.domain.User;
import com.mtaafix.user.repository.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** Phase 14: Idempotency-Key support on POST /reports (offline retry, exactly-once). */
@SpringBootTest(classes = MtaaFixApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IdempotencyTest {

    private static final String PASSWORD = "password123";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ReportCategoryRepository categoryRepository;
    @Autowired
    private IdempotencyKeyRepository idempotencyKeyRepository;
    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private User seedUser() {
        return userRepository.save(new User("idem-" + System.nanoTime() + "@mtaafix.test",
                BCrypt.hashpw(PASSWORD, BCrypt.gensalt()), "Idem Citizen", "+255744444444",
                User.Role.CITIZEN));
    }

    private String seedCategory() {
        String suffix = System.nanoTime() + "";
        return categoryRepository.save(new com.mtaafix.report.domain.ReportCategory(
                "Idem category " + suffix, "idem-cat-" + suffix,
                com.mtaafix.report.domain.ReportCategory.Type.PRIMARY)).getId();
    }

    private String bearer(User user) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + user.getEmail()
                                + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return "Bearer " + MAPPER.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
    }

    private String body(String categoryId, String title) {
        return "{\"categoryId\":\"" + categoryId + "\",\"title\":\"" + title
                + "\",\"description\":\"idem test\","
                + "\"location\":{\"latitude\":-6.82,\"longitude\":39.21,\"srid\":4326}}";
    }

    private org.springframework.test.web.servlet.ResultActions create(String token,
            String categoryId, String title, String key) throws Exception {
        var request = post("/api/v1/reports")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(categoryId, title));
        if (key != null) {
            request = request.header("Idempotency-Key", key);
        }
        return mockMvc.perform(request);
    }

    private static String key() {
        return UUID.randomUUID().toString();
    }

    @Test
    void sameKeyReplaysSameReportWithoutDuplicates() throws Exception {
        User user = seedUser();
        String token = bearer(user);
        String categoryId = seedCategory();
        String idemKey = key();

        MvcResult first = create(token, categoryId, "Pothole on Main", idemKey)
                .andExpect(status().isCreated())
                .andReturn();
        String firstBody = first.getResponse().getContentAsString();

        MvcResult second = create(token, categoryId, "Pothole on Main", idemKey)
                .andExpect(status().isCreated())
                .andReturn();
        org.junit.jupiter.api.Assertions.assertEquals(firstBody,
                second.getResponse().getContentAsString(),
                "replay must return the stored response verbatim");

        mockMvc.perform(get("/api/v1/reports").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void sameKeyDifferentPayloadConflicts() throws Exception {
        User user = seedUser();
        String token = bearer(user);
        String categoryId = seedCategory();
        String idemKey = key();

        create(token, categoryId, "First title", idemKey)
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/reports")
                        .header("Authorization", token)
                        .header("Idempotency-Key", idemKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(categoryId, "Different title")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_CONFLICT"));
    }

    @Test
    void differentKeysCreateSeparateReports() throws Exception {
        User user = seedUser();
        String token = bearer(user);
        String categoryId = seedCategory();

        create(token, categoryId, "Same content", key());
        create(token, categoryId, "Same content", key());

        mockMvc.perform(get("/api/v1/reports").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void keysAreScopedPerUser() throws Exception {
        User userA = seedUser();
        User userB = seedUser();
        String categoryId = seedCategory();
        String sharedKey = key();

        create(bearer(userA), categoryId, "Report A", sharedKey)
                .andExpect(status().isCreated());
        create(bearer(userB), categoryId, "Report B", sharedKey)
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/reports").header("Authorization", bearer(userB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void malformedKeyIsRejected() throws Exception {
        User user = seedUser();
        String token = bearer(user);
        String categoryId = seedCategory();
        mockMvc.perform(post("/api/v1/reports")
                        .header("Authorization", token)
                        .header("Idempotency-Key", "short")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(categoryId, "Bad key")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void inFlightKeyReturnsConflictInProgress() throws Exception {
        User user = seedUser();
        String token = bearer(user);
        String categoryId = seedCategory();
        String idemKey = key();
        idempotencyKeyRepository.save(
                new IdempotencyKey(user, "POST /reports", idemKey, hashFor(categoryId, "In flight")));

        mockMvc.perform(post("/api/v1/reports")
                        .header("Authorization", token)
                        .header("Idempotency-Key", idemKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(categoryId, "In flight")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_IN_PROGRESS"));
    }

    @Test
    void staleIntentIsTreatedAsAbsent() throws Exception {
        User user = seedUser();
        String token = bearer(user);
        String categoryId = seedCategory();
        String title = "Crashed attempt";
        String idemKey = key();
        IdempotencyKey intent = idempotencyKeyRepository.save(
                new IdempotencyKey(user, "POST /reports", idemKey, hashFor(categoryId, title)));
        jdbcTemplate.update(
                "UPDATE idempotency_keys SET created_at = ? WHERE id = ?",
                java.sql.Timestamp.from(Instant.now().minus(10, ChronoUnit.MINUTES)),
                intent.getId());

        MvcResult result = create(token, categoryId, title, idemKey)
                .andExpect(status().isCreated())
                .andReturn();
        org.junit.jupiter.api.Assertions.assertNotNull(result);
        mockMvc.perform(get("/api/v1/reports").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void requestWithoutHeaderIsUnaffected() throws Exception {
        User user = seedUser();
        String token = bearer(user);
        String categoryId = seedCategory();
        create(token, categoryId, "No key", null)
                .andExpect(status().isCreated());
        create(token, categoryId, "No key", null)
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/v1/reports").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    private static String hashFor(String categoryId, String title) {
        return com.mtaafix.report.service.IdempotencyService.hashOf(
                com.mtaafix.report.dto.CreateReportRequest.of(categoryId, title, "idem test",
                        new com.mtaafix.report.dto.LocationDto(-6.82, 39.21, 4326)));
    }
}
