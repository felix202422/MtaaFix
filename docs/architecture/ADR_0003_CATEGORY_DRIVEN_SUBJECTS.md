# ADR-0003: Category-Driven Reports (Deprecate Subject Enum)

- **Status:** Accepted (Phase 1, 2026-10-13) — pending owner approval of Phase 1 set
- **Context:** Master spec §2.2 mandates configurable categories ("Do not hardcode the
  application so that adding a category requires rewriting major parts of the system").
  The `Report` entity carries a hardcoded `Subject` enum (POTHOLE, …) **and** a
  `ReportCategory` FK — two competing classification sources.
- **Decision:** `ReportCategory` (DB table, admin-managed) is the single source of
  truth for classification. `Report.Subject` is deprecated immediately: no new usage;
  Phase 5 migration drops the column, seeds categories from legacy enum values, and
  makes `category_id` NOT NULL (D4).
- **Consequences:**
  - Category CRUD/activate/order APIs grow in Phase 9; public filter APIs in Phase 6.
  - `report_categories.parent_id` upgraded from String to UUID self-FK in same window.
  - Legacy `idx_reports_subject` dropped with the column.
- **Alternatives considered:** Keep both in sync (rejected: drift risk, spec-divergent);
  keep Subject as display-only cache (rejected: unnecessary until profiling proves need).
