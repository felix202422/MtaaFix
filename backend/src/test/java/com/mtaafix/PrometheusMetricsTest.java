package com.mtaafix;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Phase 19 (B-17.2): /actuator/prometheus is scrapeable without auth (owner
 * decision 2026-10-23 - same port + nginx allowlist), carries the application
 * tag, percentile/histogram distributions for per-endpoint latency, and stays
 * cardinality-safe: unknown paths collapse to uri="NOT_FOUND" instead of
 * leaking the raw URI into metric names. Everything else stays locked down.
 */
@SpringBootTest(classes = MtaaFixApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PrometheusMetricsTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void scrapeEndpointExposesTaggedPerEndpointMetrics() throws Exception {
        // A permitted path with no handler: reaches the dispatcher, 404s, and
        // must be recorded under the templated NOT_FOUND uri.
        mockMvc.perform(get("/api/v1/auth/no-such-route"))
                .andExpect(status().isNotFound());

        String scrape = mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Phase 19: keep a sample for authoring/verifying alert rules.
        Files.writeString(Path.of("target", "prometheus-sample.txt"), scrape,
                StandardCharsets.UTF_8);

        org.assertj.core.api.Assertions.assertThat(scrape)
                .contains("application=\"mtaafix\"")
                // Timer with percentile histogram buckets (p95 = histogram_quantile).
                .contains("http_server_requests_seconds_bucket")
                // Error paths are observable too: the unmatched route 404s and
                // is recorded - under a bounded uri tag, never the raw path.
                .contains("status=\"404\"")
                .contains("uri=\"/**\"")
                .contains("uri=\"/actuator/health\"")
                .contains("jvm_memory_used_bytes")
                .doesNotContain("no-such-route");
    }

    @Test
    void healthStaysOpenButUnlistedActuatorEndpointsStayLocked() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("UP")));

        // Not in management.endpoints.web.exposure.include and not permitted:
        // the security chain answers before the endpoint mapping would 404.
        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isUnauthorized());
    }
}
