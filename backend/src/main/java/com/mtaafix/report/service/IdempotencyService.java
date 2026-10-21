package com.mtaafix.report.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtaafix.common.exceptions.IdempotencyConflictException;
import com.mtaafix.report.domain.IdempotencyKey;
import com.mtaafix.report.repository.IdempotencyKeyRepository;
import com.mtaafix.user.domain.User;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.ResponseEntity;

/**
 * Phase 14: exactly-once semantics for retry-safe creates. The client sends
 * an `Idempotency-Key` header; a replay with the same user+key+payload
 * returns the stored response, a replay with a different payload is a 409,
 * and expired records are ignored (and purged hourly).
 *
 * Transaction boundaries matter: the intent insert runs in its own
 * transaction (REQUIRES_NEW) so a unique-constraint race on PostgreSQL does
 * not poison the caller's transaction.
 */
@Service
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);

    public static final String CREATE_REPORT = "POST /reports";

    private static final Pattern KEY_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{16,64}$");

    private final IdempotencyKeyRepository repository;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public IdempotencyService(IdempotencyKeyRepository repository) {
        this.repository = repository;
    }

    /**
     * Runs [action] at most once per user+endpoint+key. Successful results
     * are stored and replayed verbatim on later attempts with the same key.
     */
    public <T> ResponseEntity<T> execute(User user, String endpoint, String key,
            Object requestPayload, Supplier<T> action, Class<T> responseType) {
        String normalized = key.trim();
        if (!KEY_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException(
                    "Idempotency-Key must be 16-64 characters of [A-Za-z0-9_-].");
        }
        String hash = hashOf(requestPayload);
        Instant cutoff = Instant.now().minus(Duration.ofHours(IdempotencyKey.TTL_HOURS));
        for (int attempt = 0; attempt < 2; attempt++) {
            IdempotencyKey existing =
                    repository.findActive(user.getId(), endpoint, normalized, cutoff);
            if (existing == null) {
                IdempotencyKey intent = tryBegin(user, endpoint, normalized, hash);
                if (intent == null) {
                    continue; // Lost a race — re-lookup the winner's row.
                }
                return runAndStore(intent, action, responseType);
            }
            if (!hash.equals(existing.getRequestHash())) {
                throw new IdempotencyConflictException(
                        "IDEMPOTENCY_CONFLICT",
                        "This Idempotency-Key was already used with a different request payload.");
            }
            if (existing.hasResponse()) {
                return replay(existing, responseType);
            }
            if (isStaleIntent(existing)) {
                discard(existing.getId());
                continue; // Crashed attempt — treat as absent.
            }
            throw new IdempotencyConflictException(
                    "IDEMPOTENCY_IN_PROGRESS",
                    "A request with this Idempotency-Key is already in flight; retry shortly.");
        }
        throw new IdempotencyConflictException(
                "IDEMPOTENCY_CONFLICT",
                "The idempotent request could not be resolved; retry with a new Idempotency-Key.");
    }

    /** Inserts the intent row in its own transaction; null on lost race. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    IdempotencyKey tryBegin(User user, String endpoint, String key, String hash) {
        try {
            return repository.save(new IdempotencyKey(user, endpoint, key, hash));
        } catch (DataIntegrityViolationException ex) {
            return null;
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void storeResponse(String id, int status, String body) {
        IdempotencyKey row = repository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Idempotency row vanished: " + id));
        row.setResponseStatus(status);
        row.setResponseBody(body);
        repository.save(row);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void discard(String id) {
        repository.deleteById(id);
    }

    private <T> ResponseEntity<T> runAndStore(IdempotencyKey intent, Supplier<T> action,
            Class<T> responseType) {
        T result;
        try {
            result = action.get();
        } catch (RuntimeException ex) {
            discard(intent.getId()); // Nothing was created — the key stays usable.
            throw ex;
        }
        String body;
        try {
            body = objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException ex) {
            discard(intent.getId());
            throw new IllegalStateException("Could not serialize the created resource.", ex);
        }
        storeResponse(intent.getId(), 201, body);
        return ResponseEntity.status(201).body(result);
    }

    private <T> ResponseEntity<T> replay(IdempotencyKey row, Class<T> responseType) {
        try {
            T body = objectMapper.readValue(row.getResponseBody(), responseType);
            log.info("Replaying idempotent response for key {} ({} {})",
                    row.getIdemKey(), row.getResponseStatus(), row.getEndpoint());
            return ResponseEntity.status(row.getResponseStatus()).body(body);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Stored idempotent response is unreadable.", ex);
        }
    }

    private static boolean isStaleIntent(IdempotencyKey row) {
        return row.getCreatedAt() != null
                && row.getCreatedAt().plus(Duration.ofMinutes(IdempotencyKey.STALE_INTENT_MINUTES))
                        .isBefore(Instant.now());
    }

    public static String hashOf(Object payload) {
        try {
            String json = new ObjectMapper()
                    .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
                    .writeValueAsString(payload);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of()
                    .formatHex(digest.digest(json.getBytes(StandardCharsets.UTF_8)));
        } catch (JsonProcessingException | NoSuchAlgorithmException ex) {
            throw new IllegalArgumentException("Could not fingerprint the request payload.", ex);
        }
    }

    /** Hourly purge of expired records (TTL 24h). */
    @Scheduled(fixedDelayString = "PT1H")
    @Transactional
    public void purgeExpired() {
        int removed = repository.deleteExpired(
                Instant.now().minus(Duration.ofHours(IdempotencyKey.TTL_HOURS)));
        if (removed > 0) {
            log.debug("Purged {} expired idempotency records", removed);
        }
    }
}
