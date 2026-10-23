package com.mtaafix;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.filter.ForwardedHeaderFilter;

/**
 * Phase 18 (B-16.4): behind a forwarding proxy the per-IP limiter must key
 * on the client address from X-Forwarded-For, not the proxy's socket IP.
 * The ForwardedHeaderFilter is registered at HIGHEST_PRECEDENCE so it
 * rewrites the remote address before the security chain (RateLimitFilter)
 * sees the request - the same wiring application-prod.yml activates with
 * server.forward-headers-strategy=framework.
 */
@SpringBootTest(classes = {MtaaFixApplication.class, ForwardedIpRateLimitTest.ForwardedConfig.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "rate-limit.enabled=true",
        "rate-limit.auth-per-minute=2"
})
class ForwardedIpRateLimitTest {

    @TestConfiguration(proxyBeanMethods = false)
    static class ForwardedConfig {

        @Bean
        FilterRegistrationBean<ForwardedHeaderFilter> forwardedHeaderFilter() {
            FilterRegistrationBean<ForwardedHeaderFilter> registration =
                    new FilterRegistrationBean<>(new ForwardedHeaderFilter());
            registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
            return registration;
        }
    }

    private static final String FORGOT_PATH = "/api/v1/auth/password/forgot";

    @Autowired
    private MockMvc mockMvc;

    /** Fresh builder per call - .with() accumulates post-processors. */
    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder forgot(
            String body) {
        return post(FORGOT_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private static RequestPostProcessor forwardedFrom(String clientIp) {
        return request -> {
            request.setRemoteAddr("10.0.0.1");
            request.addHeader("X-Forwarded-For", clientIp);
            return request;
        };
    }

    @Test
    void rateLimitBucketsFollowTheForwardedClientIp() throws Exception {
        String body = "{\"email\":\"xfwd-" + System.nanoTime() + "@mtaafix.test\"}";

        // Client A exhausts its 2/min bucket: two pass, the third is 429.
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(forgot(body).with(forwardedFrom("203.0.113.50")))
                    .andExpect(status().isAccepted());
        }
        mockMvc.perform(forgot(body).with(forwardedFrom("203.0.113.50")))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", notNullValue()))
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"));

        // Client B behind the same proxy socket still passes: buckets are
        // keyed by the forwarded IP, not by the proxy address.
        mockMvc.perform(forgot(body).with(forwardedFrom("203.0.113.51")))
                .andExpect(status().isAccepted());
    }
}
