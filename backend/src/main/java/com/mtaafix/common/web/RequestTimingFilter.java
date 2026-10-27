package com.mtaafix.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Phase 17: per-request timing for NFR-7 evidence. Logs every request at
 * DEBUG and warns above {@code timing.warn-threshold-ms}. A
 * {@code X-Response-Time-Ms} header is set best-effort: it is only observable
 * when the response has not committed yet, so measurement scripts must time
 * client-side (measure-perf.ps1 does); the WARN log is the durable evidence.
 *
 * Phase 19: also assigns a correlation id - an inbound {@code X-Request-Id}
 * is honoured when it is well-formed, otherwise a fresh one is generated. It
 * is echoed on the response and put in the MDC so every log line of the
 * request carries it (dev console pattern and prod structured JSON alike).
 * Inbound values are validated against {@code [A-Za-z0-9._-]{1,64}} so a
 * hostile header cannot inject newlines or MDC keys into the logs.
 */
@Component
public class RequestTimingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestTimingFilter.class);

    static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String SAFE_REQUEST_ID = "[A-Za-z0-9._-]{1,64}";

    private final long warnThresholdMs;

    public RequestTimingFilter(
            @Value("${timing.warn-threshold-ms:500}") long warnThresholdMs) {
        this.warnThresholdMs = warnThresholdMs;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || !requestId.matches(SAFE_REQUEST_ID)) {
            requestId = UUID.randomUUID().toString();
        }
        MDC.put("requestId", requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);
        long start = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            response.setHeader("X-Response-Time-Ms", String.valueOf(durationMs));
            if (durationMs >= warnThresholdMs) {
                log.warn("Slow request: {} {} took {} ms (threshold {} ms)", request.getMethod(),
                        request.getRequestURI(), durationMs, warnThresholdMs);
            } else {
                log.debug("{} {} took {} ms", request.getMethod(), request.getRequestURI(),
                        durationMs);
            }
            MDC.remove("requestId");
        }
    }
}
