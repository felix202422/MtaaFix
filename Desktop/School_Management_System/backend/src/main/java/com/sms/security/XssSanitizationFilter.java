package com.sms.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class XssSanitizationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        filterChain.doFilter(new XssRequestWrapper(request), response);
    }

    private static class XssRequestWrapper extends HttpServletRequestWrapper {
        XssRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String getParameter(String name) {
            String value = super.getParameter(name);
            return sanitize(value);
        }

        @Override
        public String[] getParameterValues(String name) {
            String[] values = super.getParameterValues(name);
            if (values == null) {
                return null;
            }
            String[] sanitized = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                sanitized[i] = sanitize(values[i]);
            }
            return sanitized;
        }

        @Override
        public String getHeader(String name) {
            String value = super.getHeader(name);
            return sanitize(value);
        }

        private String sanitize(String value) {
            if (value == null) {
                return null;
            }
            return value.replaceAll("(?i)<script[^>]*>.*?</script>", "")
                    .replaceAll("(?i)<script", "&lt;script")
                    .replaceAll("(?i)javascript:", "javascript_")
                    .replaceAll("(?i)onerror\\s*=", "onerror_=")
                    .replaceAll("(?i)onload\\s*=", "onload_=")
                    .replaceAll("(?i)onclick\\s*=", "onclick_=");
        }
    }
}
