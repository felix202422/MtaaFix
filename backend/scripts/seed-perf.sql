-- Phase 17 performance seed (NFR-7 measurement dataset).
-- Inserts ~5,000 synthetic reports near Dar es Salaam owned by the perf user
-- (created by measure-perf.ps1 via /auth/register). Idempotent: delete-then-insert.
--
-- Run:  psql -U postgres -d mtaafix -f scripts/seed-perf.sql
--
-- Codes use the MTF-2026-9xxxxx range so the delete predicate never touches
-- real reports (real codes are MTF-2026-1xxxx/2xxxx — check before changing).

BEGIN;

DELETE FROM reports WHERE code LIKE 'MTF-2026-9%';

WITH perf_user AS (
    SELECT id FROM users WHERE email = 'perf-admin@mtaafix.local'
),
cats AS (
    SELECT ARRAY[
        '5f0a0001-0000-4000-8000-000000000001',
        '5f0a0001-0000-4000-8000-000000000002',
        '5f0a0001-0000-4000-8000-000000000003',
        '5f0a0001-0000-4000-8000-000000000004',
        '5f0a0001-0000-4000-8000-000000000005',
        '5f0a0001-0000-4000-8000-000000000006',
        '5f0a0001-0000-4000-8000-000000000007',
        '5f0a0001-0000-4000-8000-000000000008',
        '5f0a0001-0000-4000-8000-000000000009'
    ]::varchar[] AS ids
),
words AS (
    SELECT ARRAY[
        'Deep pothole on a busy street',
        'Blocked drainage causing flooding',
        'Broken streetlight after the storm',
        'Garbage accumulation near the market',
        'Water leak soaking the pavement',
        'Fallen tree blocking the road',
        'Damaged road surface after rains',
        'Waste collection missed again',
        'Street sign missing at the junction'
    ]::varchar[] AS items
)
INSERT INTO reports (
    id, code, title, description, location, reported_at, status, score,
    user_id, category_id, created_at
)
SELECT
    gen_random_uuid()::text,
    'MTF-2026-9' || lpad(i::text, 5, '0'),
    w.items[(i % 9) + 1] || ' #' || i,
    'Synthetic performance-test report #' || i || '. ' || w.items[((i + 4) % 9) + 1]
        || '. Seeded by Phase 17 NFR-7 measurement.',
    ST_SetSRID(
        ST_MakePoint(
            39.05 + ((i * 37) % 500) / 1000.0,
            -6.95 + ((i * 53) % 500) / 1000.0
        ), 4326),
    NOW() - ((i * 2) || ' hours')::interval,
    (ARRAY['SUBMITTED','SUBMITTED','SUBMITTED','UNDER_REVIEW','VERIFIED',
           'ASSIGNED','IN_PROGRESS','RESOLVED','CLOSED','REJECTED'])[1 + (i % 10)],
    (i * 7) % 100,
    (SELECT id FROM perf_user),
    c.ids[(i % 9) + 1],
    NOW() - ((i * 2) || ' hours')::interval
FROM generate_series(1, 5000) AS i
CROSS JOIN cats AS c, words AS w
WHERE (SELECT id FROM perf_user) IS NOT NULL;

COMMIT;

SELECT count(*) AS total_reports,
       count(*) FILTER (WHERE code LIKE 'MTF-2026-9%') AS perf_reports
FROM reports;
