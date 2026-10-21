package com.mtaafix.report.domain;

import com.mtaafix.common.domain.BaseEntity;
import com.mtaafix.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Phase 14: idempotency record for retry-safe endpoints. A completed row
 * (response filled) replays its stored response; an intent row (response
 * NULL) marks an in-flight request. Rows expire after
 * {@link #TTL_HOURS} and are purged by the cleanup scheduler.
 */
@Entity
@Table(name = "idempotency_keys")
public class IdempotencyKey extends BaseEntity {

    public static final long TTL_HOURS = 24;

    /** An intent row without a response older than this is considered crashed. */
    public static final long STALE_INTENT_MINUTES = 2;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "endpoint", nullable = false, length = 100)
    private String endpoint;

    @Column(name = "idem_key", nullable = false, length = 64)
    private String idemKey;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(name = "response_body", length = 8000)
    private String responseBody;

    public IdempotencyKey() {
    }

    public IdempotencyKey(User user, String endpoint, String idemKey, String requestHash) {
        this.user = user;
        this.endpoint = endpoint;
        this.idemKey = idemKey;
        this.requestHash = requestHash;
    }

    public User getUser() {
        return user;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getIdemKey() {
        return idemKey;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public Integer getResponseStatus() {
        return responseStatus;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseStatus(Integer responseStatus) {
        this.responseStatus = responseStatus;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public boolean hasResponse() {
        return responseStatus != null && responseBody != null;
    }
}
