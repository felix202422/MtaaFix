-- Phase 5: category-driven reports (ADR-0003), lifecycle columns, unique code counters

-- 1. Seed categories from the legacy subject enum (fixed ids, slug = lower(subject))
INSERT INTO report_categories (id, name, slug, type) VALUES
    ('5f0a0001-0000-4000-8000-000000000001', 'Pothole', 'pothole', 'PRIMARY'),
    ('5f0a0001-0000-4000-8000-000000000002', 'Broken streetside light', 'broken_streetside_light', 'PRIMARY'),
    ('5f0a0001-0000-4000-8000-000000000003', 'Damaged road', 'damaged_road', 'PRIMARY'),
    ('5f0a0001-0000-4000-8000-000000000004', 'Garbage accumulation', 'garbage_accumulation', 'PRIMARY'),
    ('5f0a0001-0000-4000-8000-000000000005', 'Blocked drainage', 'blocked_drainage', 'PRIMARY'),
    ('5f0a0001-0000-4000-8000-000000000006', 'Water leak', 'water_leak', 'PRIMARY'),
    ('5f0a0001-0000-4000-8000-000000000007', 'Flooding', 'flooding', 'PRIMARY'),
    ('5f0a0001-0000-4000-8000-000000000008', 'Damaged public infrastructure', 'damaged_public_infrastructure', 'PRIMARY'),
    ('5f0a0001-0000-4000-8000-000000000009', 'Fallen tree', 'fallen_tree', 'PRIMARY'),
    ('5f0a0001-0000-4000-8000-000000000010', 'Damaged electrical infrastructure', 'damaged_electrical_infrastructure', 'PRIMARY'),
    ('5f0a0001-0000-4000-8000-000000000011', 'Unsafe public area', 'unsafe_public_area', 'PRIMARY'),
    ('5f0a0001-0000-4000-8000-000000000012', 'Other', 'other', 'PRIMARY');

-- 2. Backfill legacy reports from subject to category
UPDATE reports r
   SET category_id = (SELECT c.id FROM report_categories c
                       WHERE c.slug = lower(r.subject))
 WHERE r.category_id IS NULL;

-- Unmatched fallback (should be none; subject values match seeded slugs)
UPDATE reports SET category_id = '5f0a0001-0000-4000-8000-000000000012'
 WHERE category_id IS NULL;

-- 3. Category is now mandatory on reports
ALTER TABLE reports ALTER COLUMN category_id SET NOT NULL;

-- 4. Drop legacy subject column + its index
DROP INDEX IF EXISTS idx_reports_subject;
ALTER TABLE reports DROP COLUMN subject;

-- 5. Duplicate linkage (T7)
ALTER TABLE reports ADD COLUMN duplicate_of VARCHAR(36);
ALTER TABLE reports
    ADD CONSTRAINT fk_report_duplicate FOREIGN KEY (duplicate_of) REFERENCES reports(id);

-- 6. Status history reason codes (T6/T8/T12/T15/T16)
ALTER TABLE report_status_history ADD COLUMN reason_code VARCHAR(50);

-- 7. Media uploader (FR-9.2 removal rights)
ALTER TABLE report_media ADD COLUMN uploaded_by VARCHAR(36);
ALTER TABLE report_media
    ADD CONSTRAINT fk_media_uploader FOREIGN KEY (uploaded_by) REFERENCES users(id);

-- 8. Unique report code counters (MTF-YYYY-NNNNNN, collision-safe)
CREATE TABLE report_code_counters (
    code_year   INTEGER PRIMARY KEY,
    last_serial BIGINT NOT NULL DEFAULT 0
);

INSERT INTO report_code_counters (code_year, last_serial) VALUES
    (2025, 0), (2026, 0), (2027, 0);
