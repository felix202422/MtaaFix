-- V7: Phase 16 security review (D3) - JWT revocation via token versioning.
-- Every outstanding access/refresh token carries a "tv" claim; a password
-- reset or change increments users.token_version, invalidating all tokens
-- issued before the change (stateless revocation without a token store).

ALTER TABLE users ADD COLUMN token_version INTEGER NOT NULL DEFAULT 0;
