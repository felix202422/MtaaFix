-- V1: Base domain tables for MtaaFix
-- Coordinates are stored as PostGIS geography/geometry columns (SRID 4326).

CREATE SCHEMA IF NOT EXISTS mtaafix;

-- Users (CIVIS, moderators, admins, field workers, org admins)
CREATE TABLE mtaafix.users (
    id              UUID PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    full_name       VARCHAR(255) NOT NULL,
    mobile          VARCHAR(50),
    role            VARCHAR(30) NOT NULL,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    organisation_id UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ
);

CREATE INDEX idx_users_email ON mtaafix.users(email);
CREATE INDEX idx_users_role ON mtaafix.users(role);

-- Organisations
CREATE TABLE mtaafix.organisations (
    id              UUID PRIMARY KEY,
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

CREATE INDEX idx_organisations_slug ON mtaafix.organisations(slug);

-- Report categories
CREATE TABLE mtaafix.report_categories (
    id              UUID PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    slug            VARCHAR(100) NOT NULL,
    type            VARCHAR(20) NOT NULL,
    parent_id       UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ,
    CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES mtaafix.report_categories(id)
);

CREATE INDEX idx_categories_parent ON mtaafix.report_categories(parent_id);

-- Reports
CREATE TABLE mtaafix.reports (
    id              UUID PRIMARY KEY,
    code            VARCHAR(50) NOT NULL UNIQUE,
    subject         VARCHAR(50) NOT NULL,
    title           VARCHAR(255) NOT NULL,
    description     TEXT,
    location        GEOGRAPHY(POINT, 4326),
    location_area   GEOGRAPHY(POLYGON, 4326),
    reported_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    status          VARCHAR(30) NOT NULL DEFAULT 'SUBMITTED',
    score           INTEGER NOT NULL DEFAULT 0,
    user_id         UUID NOT NULL,
    category_id     UUID,
    assignment_id   UUID,
    organisation_id UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ,
    CONSTRAINT fk_report_user FOREIGN KEY (user_id) REFERENCES mtaafix.users(id),
    CONSTRAINT fk_report_category FOREIGN KEY (category_id) REFERENCES mtaafix.report_categories(id),
    CONSTRAINT fk_report_assignment FOREIGN KEY (assignment_id) REFERENCES mtaafix.assignments(id),
    CONSTRAINT fk_report_organisation FOREIGN KEY (organisation_id) REFERENCES mtaafix.organisations(id)
);

CREATE INDEX idx_reports_status ON mtaafix.reports(status);
CREATE INDEX idx_reports_category ON mtaafix.reports(category_id);
CREATE INDEX idx_reports_organisation ON mtaafix.reports(organisation_id);
CREATE INDEX idx_reports_subject ON mtaafix.reports(subject);
CREATE INDEX idx_reports_user ON mtaafix.reports(user_id);

-- Assignments
CREATE TABLE mtaafix.assignments (
    id              UUID PRIMARY KEY,
    assigned_at     TIMESTAMPTZ,
    completed_at    TIMESTAMPTZ,
    status          VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    report_id       UUID NOT NULL,
    assignee_id     UUID,
    organisation_id UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ,
    CONSTRAINT fk_assignment_report FOREIGN KEY (report_id) REFERENCES mtaafix.reports(id),
    CONSTRAINT fk_assignment_assignee FOREIGN KEY (assignee_id) REFERENCES mtaafix.users(id),
    CONSTRAINT fk_assignment_organisation FOREIGN KEY (organisation_id) REFERENCES mtaafix.organisations(id)
);

CREATE INDEX idx_assignments_status ON mtaafix.assignments(status);
CREATE INDEX idx_assignments_assignee ON mtaafix.assignments(assignee_id);
CREATE INDEX idx_assignments_organisation ON mtaafix.assignments(organisation_id);

-- Report status history
CREATE TABLE mtaafix.report_status_history (
    id              UUID PRIMARY KEY,
    from_status     VARCHAR(30) NOT NULL,
    to_status       VARCHAR(30) NOT NULL,
    user_id         UUID,
    report_id       UUID NOT NULL,
    comment         TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_status_history_report FOREIGN KEY (report_id) REFERENCES mtaafix.reports(id),
    CONSTRAINT fk_status_history_user FOREIGN KEY (user_id) REFERENCES mtaafix.users(id)
);

CREATE INDEX idx_status_history_report ON mtaafix.report_status_history(report_id);

-- Report comments
CREATE TABLE mtaafix.report_comments (
    id              UUID PRIMARY KEY,
    content         TEXT NOT NULL,
    report_id       UUID NOT NULL,
    user_id         UUID NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_comment_report FOREIGN KEY (report_id) REFERENCES mtaafix.reports(id),
    CONSTRAINT fk_comment_user FOREIGN KEY (user_id) REFERENCES mtaafix.users(id)
);

CREATE INDEX idx_comments_report ON mtaafix.report_comments(report_id);

-- Report media
CREATE TABLE mtaafix.report_media (
    id              UUID PRIMARY KEY,
    file_name       VARCHAR(500) NOT NULL,
    original_name   VARCHAR(500) NOT NULL,
    content_type    VARCHAR(100) NOT NULL,
    size_bytes      BIGINT NOT NULL,
    url             VARCHAR(500) NOT NULL,
    media_type      VARCHAR(30) NOT NULL,
    report_id       UUID NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_media_report FOREIGN KEY (report_id) REFERENCES mtaafix.reports(id)
);

CREATE INDEX idx_media_report ON mtaafix.report_media(report_id);

-- Notifications
CREATE TABLE mtaafix.notifications (
    id              UUID PRIMARY KEY,
    title           VARCHAR(255) NOT NULL,
    body            TEXT,
    type            VARCHAR(50) NOT NULL,
    is_read         BOOLEAN NOT NULL DEFAULT FALSE,
    user_id         UUID NOT NULL,
    report_id       UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES mtaafix.users(id),
    CONSTRAINT fk_notification_report FOREIGN KEY (report_id) REFERENCES mtaafix.reports(id)
);

CREATE INDEX idx_notifications_user ON mtaafix.notifications(user_id);
CREATE INDEX idx_notifications_read ON mtaafix.notifications(is_read);

-- Audit logs
CREATE TABLE mtaafix.audit_logs (
    id              UUID PRIMARY KEY,
    action          VARCHAR(100) NOT NULL,
    actor_id        UUID,
    entity_type     VARCHAR(100) NOT NULL,
    entity_id       VARCHAR(255),
    details         JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_logs_actor ON mtaafix.audit_logs(actor_id);
CREATE INDEX idx_audit_logs_entity ON mtaafix.audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_created ON mtaafix.audit_logs(created_at);

COMMENT ON TABLE mtaafix.users IS 'System users with RBAC roles';
COMMENT ON TABLE mtaafix.reports IS 'Community issue reports with geospatial location';
COMMENT ON TABLE mtaafix.report_status_history IS 'Immutable record of every status transition';
COMMENT ON TABLE mtaafix.audit_logs IS 'Audit trail for sensitive actions';
