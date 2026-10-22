-- Phase 17 (NFR-7): index review for the dominant list/query patterns.
-- Composite btree indexes matching the staff/public list sort
-- (WHERE status/org/category/user = ? ORDER BY reported_at DESC).
CREATE INDEX IF NOT EXISTS idx_reports_status_reported_at
    ON reports (status, reported_at DESC);
CREATE INDEX IF NOT EXISTS idx_reports_organisation_reported_at
    ON reports (organisation_id, reported_at DESC);
CREATE INDEX IF NOT EXISTS idx_reports_category_reported_at
    ON reports (category_id, reported_at DESC);
CREATE INDEX IF NOT EXISTS idx_reports_user_reported_at
    ON reports (user_id, reported_at DESC);

-- pg_trgm GIN indexes for the staff `q` filter: lower(code/title/description)
-- LIKE '%term%' can never use a btree; trigram GIN makes it index-assisted.
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX IF NOT EXISTS idx_reports_code_trgm
    ON reports USING gin (lower(code) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_reports_title_trgm
    ON reports USING gin (lower(title) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_reports_description_trgm
    ON reports USING gin (lower(description) gin_trgm_ops);
