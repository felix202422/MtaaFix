package com.mtaafix.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtaafix.common.response.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Phase 16 rate limiting (SECURITY_STANDARDS: brute-force protection on
 * POST /auth/* and report submission). Per-IP fixed one-minute windows,
 * in-memory, no external dependency. Windows are keyed by client IP + path
 * so hammering one endpoint cannot starve unrelated ones. Remote IP is taken
 * from the connection; deployments behind a proxy must configure forwarded
 * headers (Phase 18 deployment note, documented in SECURITY_REVIEW.md).
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final long WINDOW_SECONDS = 60;
    private static final int MAX_TRACKED_KEYS = 5000;

    // Same pattern as AuditService/IdempotencyService: the app context has no
    // ObjectMapper bean, so the filter owns a mapper configured for Instant.
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final boolean enabled;
    private final int authPerMinute;
    private final int reportPerMinute;
    private final Map<String, RateWindow> windows = new ConcurrentHashMap<>();

    public RateLimitFilter(
            @Value("${rate-limit.enabled:true}") boolean enabled,
            @Value("${rate-limit.auth-per-minute:30}") int authPerMinute,
            @Value("${rate-limit.report-per-minute:20}") int reportPerMinute) {
        this.enabled = enabled;
        this.authPerMinute = authPerMinute;
        this.reportPerMinute = reportPerMinute;
    }

    private static final class RateWindow {
        private final long start;
        private int count;

        private RateWindow(long start, int count) {
            this.start = start;
            this.count = count;
        }
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        if (!enabled || !"POST".equals(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }
        String path = request.getRequestURI();
        int limit;
        if (path.startsWith("/api/v1/auth/")) {
            limit = authPerMinute;
        } else if ("/api/v1/reports".equals(path)) {
            limit = reportPerMinute;
        } else {
            filterChain.doFilter(request, response);
            return;
        }

        long nowSeconds = System.currentTimeMillis() / 1000;
        long windowStart = nowSeconds - (nowSeconds % WINDOW_SECONDS);
        String key = request.getRemoteAddr() + '|' + path;
        purgeIfFull(windowStart);
        RateWindow window = windows.compute(key,
                (k, existing) -> existing == null || existing.start != windowStart
                        ? new RateWindow(windowStart, 1)
                        : new RateWindow(existing.start, existing.count + 1));
        if (window.count > limit) {
            long retryAfter = WINDOW_SECONDS - (nowSeconds % WINDOW_SECONDS);
            log.debug("Rate limit exceeded for {} ({}/{})", key, window.count, limit);
            ApiError body = ApiError.of(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED",
                    "Too many requests from this address. Retry after " + retryAfter + " seconds.",
                    path, Map.of());
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            response.getWriter().write(objectMapper.writeValueAsString(body));
            return;
        }
        filterChain.doFilter(request, response);
    }

    private void purgeIfFull(long currentWindowStart) {
        if (windows.size() <= MAX_TRACKED_KEYS) {
            return;
        }
        windows.values().removeIf(window -> window.start != currentWindowStart);
    }
}
