package com.mtaafix.common.exceptions;

import com.mtaafix.common.response.ApiError;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, WebRequest request) {
        log.warn("Validation failed for path {}", request.getDescription(false));
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(() -> new java.util.LinkedHashMap<>(),
                        (map, error) -> map.put(error.getField(), error.getDefaultMessage()),
                        java.util.Map::putAll);
        ApiError apiError = ApiError.of(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "The request failed validation.",
                request.getDescription(false),
                errors);
        return ResponseEntity.badRequest().body(apiError);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex,
            WebRequest request) {
        log.warn("Bad request: {} - {}", ex.getMessage(), request.getDescription(false));
        ApiError apiError = ApiError.of(
                HttpStatus.BAD_REQUEST,
                "BAD_REQUEST",
                ex.getMessage(),
                request.getDescription(false),
                Map.of());
        return ResponseEntity.badRequest().body(apiError);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleResourceNotFound(ResourceNotFoundException ex,
            WebRequest request) {
        log.warn("Not found: {} - {}", ex.getMessage(), request.getDescription(false));
        ApiError apiError = ApiError.of(
                HttpStatus.NOT_FOUND,
                "NOT_FOUND",
                ex.getMessage(),
                request.getDescription(false),
                Map.of());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiError);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex,
            WebRequest request) {
        log.warn("Access denied: {} - {}", ex.getMessage(), request.getDescription(false));
        ApiError apiError = ApiError.of(
                HttpStatus.FORBIDDEN,
                "FORBIDDEN",
                "You are not allowed to perform this action.",
                request.getDescription(false),
                Map.of());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(apiError);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleAll(Exception ex, WebRequest request) {
        log.error("Unhandled error", ex);
        ApiError apiError = ApiError.of(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "An unexpected error occurred.",
                request.getDescription(false),
                Map.of());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiError);
    }
}
