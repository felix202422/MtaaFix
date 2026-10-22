package com.mtaafix;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Phase 16 (SECURITY_STANDARDS): per-IP rate limiting on POST /auth/* and
 * report submission. Enabled only for this context - the test profile keeps
 * the limiter off so the rest of the suite is unaffected.
 */
@SpringBootTest(classes = MtaaFixApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "rate-limit.enabled=true")
class RateLimitTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    /** Sends POSTs until the limiter answers 429; returns the attempt count. */
    private int burstUntil429(MvcResult[] last, String path, String body,
            int expectedBeforeLimit) throws Exception {
        int attempts = 0;
        while (attempts < 60) {
            attempts++;
            MvcResult result = mockMvc.perform(post(path)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andReturn();
            last[0] = result;
            if (result.getResponse().getStatus() == 429) {
                return attempts;
            }
            assertEquals(expectedBeforeLimit, result.getResponse().getStatus(),
                    "attempt " + attempts + " must pass through before the limit");
        }
        throw new AssertionError("rate limiter never triggered within 60 attempts");
    }

    @Test
    void authEndpointsReturn429AfterTheAuthBurstLimit() throws Exception {
        MvcResult[] last = new MvcResult[1];
        // Non-disclosing forgot-password endpoint: always 202, no side effects.
        int attempts = burstUntil429(last, "/api/v1/auth/password/forgot",
                "{\"email\":\"nobody-" + System.nanoTime() + "@mtaafix.test\"}", 202);

        assertTrue(attempts <= 40, "limiter must fire within the configured window");
        assertNotNull(last[0]);
        assertEquals(429, last[0].getResponse().getStatus());
        String body = last[0].getResponse().getContentAsString();
        assertTrue(MAPPER.readTree(body).get("code").asText().equals("RATE_LIMITED"),
                "429 must use the ApiError shape, got: " + body);
        assertNotNull(last[0].getResponse().getHeader("Retry-After"),
                "429 must advertise Retry-After");
    }

    @Test
    void reportCreationIsRateLimited() throws Exception {
        MvcResult[] last = new MvcResult[1];
        int attempts = burstUntil429(last, "/api/v1/reports", "{}", 401);
        assertTrue(attempts <= 30, "report-creation limit must fire promptly");
        assertEquals("RATE_LIMITED",
                MAPPER.readTree(last[0].getResponse().getContentAsString())
                        .get("code").asText());
    }

    @Test
    void getRequestsAreNeverRateLimited() throws Exception {
        // More GETs than either configured limit: the filter only counts POSTs.
        for (int i = 0; i < 40; i++) {
            mockMvc.perform(get("/api/v1/reports/public"))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void postsOutsideTheLimitedPathsAreExempt() throws Exception {
        // Wrong report id: 401/404 every time - proves the bucket scope is
        // exactly /auth/** and POST /reports, not every authenticated POST.
        for (int i = 0; i < 40; i++) {
            MvcResult result = mockMvc.perform(post("/api/v1/reports/not-a-real-id/cancel")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andReturn();
            assertTrue(result.getResponse().getStatus() != 429,
                    "non-listed path must not be rate limited");
        }
    }
}
