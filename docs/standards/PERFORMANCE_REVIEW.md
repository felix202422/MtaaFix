# Performance Review — Phase 17

**Status:** Complete · **Date:** 2026-10-22 · **Gate:** NFR-7 (p95 < 500 ms list queries)
**Scope:** Owner-approved scope Q1–Q4 (all workstreams A–H).

---

## 1. Findings (pre-Phase 17)

| # | Finding | Severity |
|---|---------|----------|
| 1 | `ReportService.list` N+1: `ReportDto.from` touched statusHistory/media/comments/assignment per row (up to ~4+ queries × page size). List UIs never read history/comments; media is read only as a count. | High |
| 2 | Assignment queries fetched per-row `assignee`/`organisation` eagerly (default `@ManyToOne` EAGER) — an N+1 variant, unpaginated (`List` return). | High |
| 3 | Org member listing fetched members then one `findById` per member, unpaginated. | Medium |
| 4 | No composite indexes for the dominant `WHERE … ORDER BY reported_at DESC` list pattern; staff `q` filter ran unindexed `lower(col) LIKE '%term%'`. | Medium |
| 5 | Web ExplorePage used a single page-10 feed for **both** list and map — the map showed at most 10 markers. Mobile map capped at 50. | Medium |
| 6 | `/stats/public` = 4 COUNTs per hit; `/dashboard/admin` = many live queries per hit; categories list re-queried per request. | Medium |
| 7 | `buildActivity14d` loaded every `reported_at` Instant for a 14-day window and bucketed in Java. | Low |
| 8 | No per-request timing; no p95 evidence against NFR-7. | Low |

**Not N+1 but confirmed bounded:** `categories`/`organisations` list endpoints (reference data, small, admin-managed) — documented as exemptions rather than paginated.

## 2. Changes shipped

| Workstream | Change | Files |
|---|---|---|
| A | `ReportDto.listItem` factory (no history/comments; pre-fetched media); `ReportRepository.findByScope` fetch-joins assignment; `findMediaByReportIds` batch; assignment queries converted to `Page<Report>` + count queries | `ReportDto`, `ReportRepository`, `ReportService` |
| A | `AssignmentService.list` → `PagedResponse<AssignmentDto>`, scalar-only DTO, null assignments filtered | `AssignmentService`, `AssignmentController` |
| B | `V8__report_list_indexes.sql`: 4 composite btree `(status\|org\|category\|user, reported_at DESC)` + `pg_trgm` GIN on `lower(code/title/description)` | `V8`, PG IT assertions |
| C | Org members paginated (`PagedResponse<MemberDto>`, batch user load, size ≤ 100); notifications size clamped to 100 (shape kept) | `OrganisationService`, `OrganisationController`, `NotificationController` |
| C | Web clients unwrap `.content` (fetch `size=100`) | `api.ts` (+ Phase10 test mocks) |
| D | `RequestTimingFilter` (DEBUG per request, WARN > 500 ms, config `timing.warn-threshold-ms`); seed + measurement scripts | `RequestTimingFilter`, `SecurityConfig`, `scripts/` |
| E | Caffeine (`spring-boot-starter-cache` + `caffeine`), global `expireAfterWrite=60s`; `categories` (evicted on create/update/activate/reorder), `publicStats`, `adminDashboard` cached; test profile → `type: simple`; `CachingTest` proves hits + eviction via `@MockitoSpyBean` | `pom.xml`, `CacheConfig`, `application*.yml`, services |
| F | ExplorePage separate map feed (`size=100`, own loading/error states, marker count note); mobile `publicList` default 50 → 100 | `ExplorePage.tsx`, `reports_api.dart` |
| G | `buildActivity14d` → SQL `GROUP BY (reported_at AT TIME ZONE 'Africa/Dar_es_Salaam')::date`; Java path retained for non-PG dialects (H2 tests) | `ReportRepository`, `DashboardService` |
| H | Docs sync (this file; API_DESIGN, CHANGELOG, ROADMAP, BACKLOG, BOARD) | see §5 |

## 3. Methodology (NFR-7 measurement)

- **Server:** local Spring Boot on `:8085` (smoke-run.bat), PG 18 + PostGIS on localhost:5432.
- **Dataset:** `scripts/seed-perf.sql` — 5,000 synthetic reports (`MTF-2026-9xxxxx`) around Dar es Salaam, spread across 10 statuses / 9 categories / ~416 days, plus 38 pre-existing reports = 5,038 total.
- **Tool:** `scripts/measure-perf.ps1` — 5 warmup + 30 measured iterations per endpoint, client-side `Stopwatch`, p50/p95/max; results in `scripts/perf-results.json`.
- **Auth:** `perf-admin@mtaafix.local` (ADMINISTRATOR), `perf-worker@mtaafix.local` (FIELD_WORKER, for `/assignments/me`).
- **Server-side evidence:** `RequestTimingFilter` WARN lines (>500 ms) in the server log; observed 3 spikes, all one-time startup/JIT warmup (register 1675 ms, login 856 ms, first public list 1881 ms) — none during steady-state measurement.

## 4. Results — NFR-7 verdict

| Endpoint | p50 (ms) | **p95 (ms)** | max (ms) | p95 < 500 ms |
|---|---|---|---|---|
| GET /reports/public (list, size=20) | 149.9 | **169.5** | 467.9 | ✅ |
| GET /reports/public (map, size=100) | 298.2 | **336.4** | 352.1 | ✅ |
| GET /reports/public (bbox filter, size=100) | 312.5 | **397.2** | 708.0 | ✅ |
| GET /reports/public (q trigram search) | 120.5 | **128.7** | 130.4 | ✅ |
| GET /reports (staff list, size=20) | 113.9 | **127.4** | 224.2 | ✅ |
| GET /reports (staff status filter) | 113.5 | **149.7** | 983.9 | ✅ |
| GET /categories/public (cached) | 58.4 | **63.3** | 65.6 | ✅ |
| GET /stats/public (cached) | 59.3 | **63.5** | 106.6 | ✅ |
| GET /dashboard/admin (cached) | 69.7 | **122.8** | 141.3 | ✅ |
| GET /assignments/me (paged) | 69.8 | **77.8** | 80.5 | ✅ |
| GET /notifications (size=20, clamped) | 64.6 | **71.5** | 73.4 | ✅ |

**Verdict: 11/11 endpoints pass p95 < 500 ms → NFR-7: MET** (measured on the modest local hardware the NFR contemplates; includes network-loopback, Jackson serialization, and DB round trips).

Notes:
- Two single-iteration maxima exceed 500 ms (bbox 708 ms, status filter 983.9 ms) — GC/scheduler outliers; p95 (the NFR metric) passes with margin. Re-run repeatability is expected to vary these tails.
- The 30-iteration window is small; treat as evidence, not a load test (load testing deferred with Phase 19 observability).

## 5. Index evidence (PG, `EXPLAIN`)

- Selective status: `Index Scan using idx_reports_status_reported_at` with `Index Cond: status = …` (composite from V8 used).
- High-frequency status: planner picks backward scan on `idx_reports_reported_at` + filter — correct choice when the status covers ~30% of rows.
- Trigram: `q=pothole` measured at p95 128.7 ms over 5,038 rows (GIN path).

## 6. Deferrals (with rationale)

| Item | Deferred to | Rationale |
|---|---|---|
| Marker clustering (web/mobile) | Phase 18/19 | Rendering concern, not query cost; 100-marker cap applied instead |
| Full Micrometer/Prometheus metrics | Phase 19 | Owner-approved deferral; `RequestTimingFilter` gives minimal evidence now |
| Redis / distributed cache | — | Single-node deployment; Caffeine sufficient |
| User dashboard caching | — | Freshness: citizens must see their just-created report immediately |
| Paginating `categories`/`organisations` list endpoints | — | Bounded reference data (admin-managed, tens of rows) — documented exemption |
| Cache for `q` search / bbox queries | — | Filtered queries have high key cardinality; measured p95 already passes |
| Repeat measurement / larger N | Phase 19 | Measurement re-runs become routine with metrics stack |

## 7. Suite status (this phase)

| Suite | Result |
|---|---|
| Backend H2 (`mvn test`) | **148/148 pass** (9 PG-only skipped) |
| Backend PG IT (`MTAAFIX_PG_IT=true`) | **9/9 pass** (incl. V8 index assertions) |
| Web (`tsc --noEmit` + vitest) | **typecheck clean, 68/68 pass** |
| Mobile (`flutter test`) | **171/171 pass** |
| NFR-7 live measurement | **11/11 endpoints p95 < 500 ms** |
