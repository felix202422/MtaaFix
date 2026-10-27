package com.mtaafix.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Phase 19: correlation id behaviour - generated when absent or malformed,
 * honoured when well-formed, echoed on the response, visible to logs of the
 * request only (MDC is cleared afterwards), never able to inject into the
 * log format.
 */
class RequestTimingFilterTest {

    private final RequestTimingFilter filter = new RequestTimingFilter(500);

    private static FilterChain recording(AtomicReference<String> seen) {
        return new FilterChain() {
            @Override
            public void doFilter(ServletRequest req, ServletResponse res) {
                seen.set(MDC.get("requestId"));
            }
        };
    }

    @Test
    void generatesRequestIdAndClearsMdcAfterwards() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/reports");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> seenInChain = new AtomicReference<>();

        filter.doFilter(request, response, recording(seenInChain));

        String echoed = response.getHeader(RequestTimingFilter.REQUEST_ID_HEADER);
        assertThat(echoed).isNotNull();
        assertThat(echoed).matches("[0-9a-f-]{36}");
        // The logs of this request see the id; nothing leaks after it.
        assertThat(seenInChain.get()).isEqualTo(echoed);
        assertThat(MDC.get("requestId")).isNull();
        assertThat(response.getHeader("X-Response-Time-Ms")).isNotNull();
    }

    @Test
    void honoursAClientSuppliedWellFormedRequestId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        request.addHeader(RequestTimingFilter.REQUEST_ID_HEADER, "trace-42.abc_DEF-1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> seenInChain = new AtomicReference<>();

        filter.doFilter(request, response, recording(seenInChain));

        assertThat(response.getHeader(RequestTimingFilter.REQUEST_ID_HEADER))
                .isEqualTo("trace-42.abc_DEF-1");
        assertThat(seenInChain.get()).isEqualTo("trace-42.abc_DEF-1");
    }

    @Test
    void rejectsMalformedInboundIdsSoHeadersCannotInjectIntoLogs() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        request.addHeader(RequestTimingFilter.REQUEST_ID_HEADER, "bad id\nERROR fake-entry");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> seenInChain = new AtomicReference<>();

        filter.doFilter(request, response, recording(seenInChain));

        String echoed = response.getHeader(RequestTimingFilter.REQUEST_ID_HEADER);
        assertThat(echoed).doesNotContain("\n").doesNotContain(" ");
        assertThat(echoed).matches("[0-9a-f-]{36}");
        assertThat(seenInChain.get()).isEqualTo(echoed);
    }
}
