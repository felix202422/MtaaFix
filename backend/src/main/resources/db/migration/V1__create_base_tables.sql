-- V1: Base domain tables for MtaaFix
-- Coordinates are stored as PostGIS geometry columns (SRID 4326).
-- Tables live in the public schema to match datasource currentSchema=public
-- and entities (no @Table schema attribute).

CREATE EXTENSION IF NOT EXISTS postgis;

-- Users (citizens, moderators, administrators, field workers, org admins)
CREATE TABLE users (
    id              VARCHAR(36) PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    full_name       VARCHAR(255) NOT NULL,
    mobile          VARCHAR(50),
    role            VARCHAR(30) NOT NULL,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    organisation_id VARCHAR(36),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);

-- Organisations
CREATE TABLE organisations (
    id              VARCHAR(36) PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    slug            VARCHAR(100) NOT NULL UNIQUE,
    email           VARCHAR(255),
    phone           VARCHAR(50),
    address         TEXT,
    description     TEXT,
    logo_url        VARCHAR(500),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ
);

CREATE INDEX idx_organisations_slug ON organisations(slug);

-- Report categories
CREATE TABLE report_categories (
    id              VARCHAR(36) PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    slug            VARCHAR(100) NOT NULL,
    type            VARCHAR(20) NOT NULL,
    parent_id       VARCHAR(36),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ,
    CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES report_categories(id)
);

CREATE INDEX idx_categories_parent ON report_categories(parent_id);

-- Reports (assignment_id FK added after assignments is created ??? cyclic reference)
CREATE TABLE reports (
    id              VARCHAR(36) PRIMARY KEY,
    code            VARCHAR(50) NOT NULL UNIQUE,
    subject         VARCHAR(50) NOT NULL,
    title           VARCHAR(255) NOT NULL,
    description     TEXT,
    location        GEOMETRY(POINT, 4326),
    location_area   GEOMETRY(POLYGON, 4326),
    reported_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    status          VARCHAR(30) NOT NULL DEFAULT 'SUBMITTED',
    score           INTEGER NOT NULL DEFAULT 0,
    user_id         VARCHAR(36) NOT NULL,
    category_id     VARCHAR(36),
    assignment_id   VARCHAR(36),
    organisation_id VARCHAR(36),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ,
    CONSTRAINT fk_report_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_report_category FOREIGN KEY (category_id) REFERENCES report_categories(id),
    CONSTRAINT fk_report_organisation FOREIGN KEY (organisation_id) REFERENCES organisations(id)
);

CREATE INDEX idx_reports_status ON reports(status);
CREATE INDEX idx_reports_category ON reports(category_id);
CREATE INDEX idx_reports_organisation ON reports(organisation_id);
CREATE INDEX idx_reports_subject ON reports(subject);
CREATE INDEX idx_reports_user ON reports(user_id);
CREATE INDEX idx_reports_location ON reports USING GIST (location);
CREATE INDEX idx_reports_location_area ON reports USING GIST (location_area);

-- Assignments
CREATE TABLE assignments (
    id              VARCHAR(36) PRIMARY KEY,
    assigned_at     TIMESTAMPTZ,
    completed_at    TIMESTAMPTZ,
    status          VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    report_id       VARCHAR(36) NOT NULL,
    assignee_id     VARCHAR(36),
    organisation_id VARCHAR(36),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ,
    CONSTRAINT fk_assignment_report FOREIGN KEY (report_id) REFERENCES reports(id),
    CONSTRAINT fk_assignment_assignee FOREIGN KEY (assignee_id) REFERENCES users(id),
    CONSTRAINT fk_assignment_organisation FOREIGN KEY (organisation_id) REFERENCES organisations(id)
);

CREATE INDEX idx_assignments_status ON assignments(status);
CREATE INDEX idx_assignments_assignee ON assignments(assignee_id);
CREATE INDEX idx_assignments_organisation ON assignments(organisation_id);

-- Close the reports <-> assignments cycle
ALTER TABLE reports
    ADD CONSTRAINT fk_report_assignment FOREIGN KEY (assignment_id) REFERENCES assignments(id);

-- Report status history
CREATE TABLE report_status_history (
    id              VARCHAR(36) PRIMARY KEY,
    from_status     VARCHAR(30),
    to_status       VARCHAR(30) NOT NULL,
    user_id         VARCHAR(36),
    report_id       VARCHAR(36) NOT NULL,
    comment         TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ,
    CONSTRAINT fk_status_history_report FOREIGN KEY (report_id) REFERENCES reports(id),
    CONSTRAINT fk_status_history_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_status_history_report ON report_status_history(report_id);

-- Report comments
CREATE TABLE report_comments (
    id              VARCHAR(36) PRIMARY KEY,
    content         TEXT NOT NULL,
    report_id       VARCHAR(36) NOT NULL,
    user_id         VARCHAR(36) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ,
    CONSTRAINT fk_comment_report FOREIGN KEY (report_id) REFERENCES reports(id),
    CONSTRAINT fk_comment_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_comments_report ON report_comments(report_id);

-- Report media
CREATE TABLE report_media (
    id              VARCHAR(36) PRIMARY KEY,
    file_name       VARCHAR(500) NOT NULL,
    original_name   VARCHAR(500) NOT NULL,
    content_type    VARCHAR(100) NOT NULL,
    size_bytes      BIGINT NOT NULL,
    url             VARCHAR(500) NOT NULL,
    media_type      VARCHAR(30) NOT NULL,
    report_id       VARCHAR(36) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ,
    CONSTRAINT fk_media_report FOREIGN KEY (report_id) REFERENCES reports(id)
);

CREATE INDEX idx_media_report ON report_media(report_id);

-- Notifications
CREATE TABLE notifications (
    id              VARCHAR(36) PRIMARY KEY,
    title           VARCHAR(255) NOT NULL,
    body            TEXT,
    type            VARCHAR(50) NOT NULL,
    is_read         BOOLEAN NOT NULL DEFAULT FALSE,
    user_id         VARCHAR(36) NOT NULL,
    report_id       VARCHAR(36),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ,
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_notification_report FOREIGN KEY (report_id) REFERENCES reports(id)
);

CREATE INDEX idx_notifications_user ON notifications(user_id);
CREATE INDEX idx_notifications_read ON notifications(is_read);

-- Audit logs
CREATE TABLE audit_logs (
    id              VARCHAR(36) PRIMARY KEY,
    action          VARCHAR(100) NOT NULL,
    actor_id        VARCHAR(36),
    entity_type     VARCHAR(100) NOT NULL,
    entity_id       VARCHAR(255),
    details         JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ
);

CREATE INDEX idx_audit_logs_actor ON audit_logs(actor_id);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_created ON audit_logs(created_at);

COMMENT ON TABLE users IS 'System users with RBAC roles';
COMMENT ON TABLE reports IS 'Community issue reports with geospatial location';
COMMENT ON TABLE report_status_history IS 'Immutable record of every status transition';
COMMENT ON TABLE audit_logs IS 'Audit trail for sensitive actions';
