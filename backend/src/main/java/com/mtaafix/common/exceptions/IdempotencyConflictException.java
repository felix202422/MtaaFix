package com.mtaafix.common.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Phase 14: an Idempotency-Key cannot be honoured (reused with a different
 * payload, or a request with the same key is already in flight).
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class IdempotencyConflictException extends RuntimeException {

    private final String code;

    public IdempotencyConflictException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
