-- V6: Phase 14 - report-creation idempotency (offline retry, exactly-once).
-- Clients that retry POST /reports over flaky links send an Idempotency-Key;
-- a replay with the same user+key returns the stored response without
-- creating a second report. Keys expire after 24 hours (service-enforced).

CREATE TABLE idempotency_keys (
    id              VARCHAR(36) PRIMARY KEY,
    user_id         VARCHAR(36) NOT NULL,
    endpoint        VARCHAR(100) NOT NULL,
    idem_key        VARCHAR(64) NOT NULL,
    request_hash    VARCHAR(64) NOT NULL,
    response_status INTEGER,
    response_body   VARCHAR(8000),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ,
    CONSTRAINT fk_idempotency_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE UNIQUE INDEX uq_idempotency_user_endpoint_key
    ON idempotency_keys(user_id, endpoint, idem_key);
CREATE INDEX idx_idempotency_created ON idempotency_keys(created_at);
