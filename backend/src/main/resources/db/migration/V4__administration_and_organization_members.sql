-- V4: Phase 9 administration and moderation
--     (FR-5.1 categories, FR-5.3 organisations, FR-10.1 audit outcome,
--      FR-12.1 user management, B-X.3 organization_members)
--
-- organization_members is the source of truth for user <-> organisation
-- affiliation (single organisation per user). The legacy users.organisation_id
-- column is backfilled into it and then dropped.

CREATE TABLE organization_members (
    id              VARCHAR(36) PRIMARY KEY,
    user_id         VARCHAR(36) NOT NULL,
    organisation_id VARCHAR(36) NOT NULL,
    member_role     VARCHAR(30) NOT NULL,
    joined_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ,
    CONSTRAINT fk_member_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_member_organisation FOREIGN KEY (organisation_id) REFERENCES organisations(id),
    CONSTRAINT ck_member_role CHECK (member_role IN ('ORGANIZATION_ADMIN', 'FIELD_WORKER'))
);

-- Backfill from the legacy column. id = user_id is deterministic (one row per
-- user), which keeps the migration portable across PostgreSQL and the H2 test
-- database instead of relying on a DB-specific UUID() function. Only staff
-- roles are members; a legacy organisation_id on a non-staff user carries no
-- permissions (membership implies a staff role) and is intentionally dropped
-- together with the column.
INSERT INTO organization_members (id, user_id, organisation_id, member_role, joined_at, created_at)
SELECT u.id, u.id, u.organisation_id, u.role, u.created_at, u.created_at
FROM users u
WHERE u.organisation_id IS NOT NULL
  AND u.role IN ('ORGANIZATION_ADMIN', 'FIELD_WORKER');

-- Single organisation per user (Part 3.5/3.6 model)
CREATE UNIQUE INDEX uq_organization_members_user ON organization_members(user_id);
CREATE INDEX idx_organization_members_org ON organization_members(organisation_id);

-- Single source of truth: drop the denormalised legacy column
ALTER TABLE users DROP COLUMN organisation_id;

-- FR-5.1: category activation and ordering without code changes
ALTER TABLE report_categories ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE report_categories ADD COLUMN display_order INTEGER NOT NULL DEFAULT 0;

-- P21: organisation account status (soft deactivate; FKs forbid hard delete)
ALTER TABLE organisations ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;

-- FR-10.1: audit records carry an outcome (legacy rows keep NULL = unknown)
ALTER TABLE audit_logs ADD COLUMN outcome VARCHAR(20);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);

-- P23: date-range analytics filter
CREATE INDEX idx_reports_reported_at ON reports(reported_at);
