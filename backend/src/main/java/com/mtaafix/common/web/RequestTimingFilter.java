package com.mtaafix.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Phase 17: per-request timing for NFR-7 evidence. Logs every request at
 * DEBUG and warns above {@code timing.warn-threshold-ms}. A
 * {@code X-Response-Time-Ms} header is set best-effort: it is only observable
 * when the response has not committed yet, so measurement scripts must time
 * client-side (measure-perf.ps1 does); the WARN log is the durable evidence.
 */
@Component
public class RequestTimingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestTimingFilter.class);

    private final long warnThresholdMs;

    public RequestTimingFilter(
            @Value("${timing.warn-threshold-ms:500}") long warnThresholdMs) {
        this.warnThresholdMs = warnThresholdMs;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
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
        }
    }
}
