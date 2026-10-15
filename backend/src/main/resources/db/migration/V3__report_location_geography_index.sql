-- Phase 8 geographic capabilities.
--
-- The public feed supports two spatial filters:
--   * bbox -> ST_Intersects(location, ST_MakeEnvelope(...))
--     Uses the existing GIST index on the geometry column (idx_reports_location).
--   * near -> ST_DWithin(location::geography, ST_MakePoint(...)::geography, radius)
--     Casting to geography wraps the column in a function, so the planner cannot use
--     idx_reports_location and falls back to a sequential scan. Index the cast so the
--     radius search stays indexed as the reports table grows.
--
-- SRID 4326 with the geography type measures distances in metres, which is what
-- ST_DWithin's radiusMeters parameter expects.

CREATE INDEX IF NOT EXISTS idx_reports_location_geography
    ON reports USING GIST ((location::geography));
