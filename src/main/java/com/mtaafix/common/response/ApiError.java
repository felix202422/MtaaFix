package com.mtaafix.common.response;

import java.time.Instant;
import java.util.Map;

public record ApiError(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        Map<String, String> errors) {

    public static ApiError of(HttpStatus status, String code, String message, String path,
            Map<String, String> errors) {
        return new ApiError(Instant.now(), status.value(), code, message, path, errors);
    }
}
