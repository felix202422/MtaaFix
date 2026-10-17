# MtaaFix — Implementation Backlog

> **Status:** v1.0 — APPROVED (owner, 2026-10-13) · Phase 1 · 2026-10-13
> Prioritized work items with acceptance criteria (master spec Part 14.2).
> Statuses: `BACKLOG` (ready), `BLOCKED` (waiting on dependency/gate).
> GitHub Projects unavailable; this file + [BOARD.md](BOARD.md) are the local
> source of truth until `gh` auth is fixed. Labels: `phase:N`, `be`/`web`/`mobile`,
> `docs`, `test`, `sec`.

Dependencies flow: Phase 2 ∥ Phase 3 → Phase 4 → Phase 5 → Phase 6 → Phase 7.
(Phases 2 and 3 can proceed in parallel; 4 and 5 are sequential on the backend;
6 and 7 need 4–5 for real integrations and 2 for the UI foundation.)

---

## Phase 2 — Design system (web + shared)

### B-2.1 Design tokens and theme
- **As** a team, **we** encode the approved palette, typography, spacing, radius,
  shadow tokens so all later UI is consistent.
- **AC:** Tokens file(s) in `web/` (CSS variables or Tailwind config) matching Part 9.1
  hex values; Storybook-or-docs page (or markdown visual spec) shows primary/semantic
  usage; contrast checks pass WCAG AA for text pairs; `npm run build` green.
- **Phase:** 2 · **Labels:** web, docs · **Tests:** none (visual review)

### B-2.2 Component primitives
- Button, input, card, badge/status pill, modal, toast, empty/loading/error states.
- **AC:** Each component keyboard-accessible with visible focus; status badges pair
  color with text/icon (NFR-9); components reused by a demo page; typecheck green.
- **Phase:** 2 · **Depends:** B-2.1

### B-2.3 Layout & responsive rules
- **AC:** Documented breakpoints; demo layouts verified at 360/768/1280/1920 px.
- **Phase:** 2 · **Depends:** B-2.1

---

## Phase 3 — Backend foundation completion

### B-3.1 PostgreSQL + PostGIS runtime verification
- Install PostGIS extension; create `mtaafix` DB; run Flyway V1; boot app; smoke
  health endpoint.
- **AC:** `flyway migrate` (or app boot with flyway enabled) succeeds on
  PostgreSQL+PostGIS; `GET /actuator/health` = UP; entity `validate` passes;
  commands + results recorded in phase report.
- **Phase:** 3 · **Labels:** be, test · **Blocked on:** PostGIS install (approved)

### B-3.2 Integration test profile
- **AC:** Maven profile (e.g. `-Pit`) runs context/repository tests against real
  Postgres; CI keeps H2 unit tier green; documented in DEVELOPMENT.md.
- **Phase:** 3 · **Depends:** B-3.1

### B-3.3 OpenAPI export
- **AC:** `openapi.yaml` (or json) generated from springdoc and committed;
  regeneration command documented.
- **Phase:** 3

### B-3.4 DEVELOPMENT.md runbook
- **AC:** Prereqs (JDK 25, Maven, PG+PostGIS, Node, Flutter), env vars from
  `.env.example`, run/test commands, troubleshooting; verified by following it on a
  clean shell.
- **Phase:** 3

---

## Phase 4 — AuthN/Z

### B-4.1 Method-level role enforcement
- Apply `@Precheck`/service guards per [role matrix](../design/ROLE_PERMISSION_MATRIX.md).
- **AC:** Every row of matrix §1 has at least one automated positive and one negative
  test; 401/403 semantics correct; no endpoint relies on `authenticated()` alone for
  role separation.
- **Phase:** 4 · **Labels:** be, sec, test

### B-4.2 Ownership & org scoping
- **AC:** Citizen cannot read/update another user's report (404/403 per convention);
  org admin cannot read other orgs' private data; field worker sees only own
  assignments — each with tests.
- **Phase:** 4 · **Depends:** B-4.1

### B-4.3 Audited A-scope operations
- **AC:** Role grants, org/category/settings changes write AuditLog rows containing
  actor, action, entity, outcome; test asserts row written; no secrets in payload logs.
- **Phase:** 4
- **Partial (Phase 4 done):** category create writes `CATEGORY_CREATED` audit rows;
  role grants / org / settings audit arrives with their Phase 9 endpoints (rows 8/9/14).

---

## Phase 5 — Core reporting workflow (COMPLETE - approved 2026-10-14)

> **Status (2026-10-14):** B-5.1–B-5.5 all implemented and verified (H2 49/49,
> PG 3/3 with V2 applied, live smoke 16/16) — approved by owner 2026-10-14.
> Deferrals documented in the phase report: T8 (citizen cancel), T2r, T7r, T10r
> have no API_DESIGN §2/§3 contract rows (no endpoint implemented); byte-level
> media content checks wait for Phase 13 storage; reopen-window enforcement
> needs `closed_at` capture; org-wide notifications for T14 confirmation.

### B-5.1 Lifecycle migration + state machine
- Flyway: add 4 statuses, `duplicate_of`, history nullability fix; central transition
  guard implementing T1–T17.
- **AC:** Invalid transition → 409 `INVALID_STATUS_TRANSITION`; every legal
  transition covered by test; history row appended per transition with actor;
  invariant test (latest history == current status).
- **Phase:** 5 · **Depends:** B-3.1, B-4.1 · **Labels:** be, test

### B-5.2 Category-driven reports (ADR-0003)
- **AC:** Legacy subjects seeded into `report_categories`; `subject` column dropped;
  `category_id` NOT NULL; create-report requires categoryId; adding a category needs
  no code change (verified by create-via-API test).
- **Phase:** 5 · **Depends:** B-5.1

### B-5.3 Report code generation
- **AC:** `MTF-<year>-<6-digit>` generated at creation; DB unique constraint;
  collision-safe under concurrent creates (test); stable across transitions.
- **Phase:** 5

### B-5.4 Missing endpoints (comments, media, lifecycle actions)
- Expose Planned routes in [API_DESIGN §3](../api/API_DESIGN.md) needed for MVP
  lifecycle: comments, media attach, needs-info, resubmit, duplicate, start,
  complete+evidence, return, confirm, dispute, reopen.
- **AC:** Each route matches API_DESIGN contract; role/state guards tested;
  media validates MIME/size (FR-9.2).
- **Phase:** 5 · **Depends:** B-5.1

### B-5.5 Coordinate validation
- **AC:** Out-of-range lat/long rejected 400; valid point round-trips EPSG:4326;
  test with boundary values.
- **Phase:** 5

---

## Phase 6 — Public website

### B-6.1 Web app scaffold ✅ DONE (Phase 6)
- Vite + React + TS + Tailwind + Router wired; env config; API client with error/
  auth handling (Part 11.1).
- **AC:** `npm ci && npm run build` green; CI web step activates (web/src exists);
  404 route works.
- **Phase:** 6 · **Depends:** B-2.1

### B-6.2 Public APIs ✅ DONE (Phase 6)
- **AC:** `/reports/public`, `/reports/public/{code}`, `/categories/public`,
  `/stats/public` return only public fields (SRS §5); reporter identity/phone/
  media/comments never serialized (test with full-data report); pagination +
  filters per FR-8.3; coords rounded to 2dp (replaced by FR-11.4 fuzzing in
  Phase 8); CORS from `CORS_ALLOWED_ORIGINS`.
- **Phase:** 6 · **Depends:** B-5.2, B-5.3

### B-6.3 Pages 1–4 ✅ DONE (Phase 6)
- **AC:** Landing (hero + stats + how-it-works summary + recent reports),
  how-it-works (7-step lifecycle + roles), explore (search/filter/paginate),
  public detail (generalized location, high-level timeline, honest no-media
  state), `/report` info route (citizen sign-in arrives Phase 7), 404 route.
  Responsive, a11y spot-checked, privacy reviewed vs SRS §5.
- **Phase:** 6 · **Depends:** B-6.1, B-6.2
- **Deferrals (owner decisions 2026-10-15):** map view → Phase 8; public media
  payloads → Phase 10/13 (needs visibility classification + storage); coordinate
  fuzzing → Phase 8 (2dp rounding interim). No report-level visibility flag
  exists yet — all reports are public at field level only; public timeline
  excludes T8 (citizen cancel) and T9r (reassign) per REPORT_LIFECYCLE "Public"
  column.

---

## Phase 7 — Citizen web

All four items **delivered 2026-10-15** (see CHANGELOG 0.8.0; approval gate pending).

### B-7.1 Auth pages (login/register) — DONE
- **AC:** Real `/auth/*` integration; token storage strategy documented; duplicate
  email error surfaced; logout clears session.
- **Phase:** 7 · **Depends:** B-6.1, B-4.1
- **Delivered:** `AuthProvider` + `/login` + `/register`; access+refresh in localStorage
  with silent refresh-once-on-401 (single in-flight refresh; session dropped when
  refresh fails); server error messages (incl. duplicate email 409) surfaced inline;
  logout calls `/auth/logout` and clears tokens. `/forgot-password` is info-only
  (password self-service → Phase 13 / B-X.1).

### B-7.2 Report wizard (4 steps) — DONE
- **AC:** Category from API; location via map or pin; photo upload with client+server
  validation; review step; success shows MTF reference; draft protection against
  double-submit.
- **Phase:** 7 · **Depends:** B-5.4, B-2.2
- **Delivered:** categories from `GET /categories`; location via browser geolocation
  ("Use my location") + manual lat/lng (map tiles → Phase 8, owner decision); photo
  client validation (type + 20 MiB) + server MIME/magic-byte/size checks; review step;
  success shows MTF code; draft persists in localStorage and the submit button is
  disabled while in flight (double-submit guarded).

### B-7.3 My Reports / Tracking / Notifications / Profile — DONE
- **AC:** List paginated+filterable; timeline shows full history; confirm/dispute
  actions appear only in eligible states; notifications mark-read works; profile edit
  own-only.
- **Phase:** 7 · **Depends:** B-7.1, B-5.1
- **Delivered:** paginated `/app/reports`; full `statusHistory` timeline + comments +
  evidence (authenticated blobs); action eligibility driven by status (cancel in
  SUBMITTED, confirm/dispute in RESOLVED, resubmit in NEEDS_INFORMATION); mark-read
  + mark-all; `PUT /auth/me` edits name/mobile only, own account only (403 tests).

### B-7.4 Citizen dashboard — DONE
- **AC:** Real stats from `/dashboard/me`; attention-required panel driven by
  NEEDS_INFORMATION/dispute states; no fabricated numbers (FR-7.4).
- **Phase:** 7 · **Depends:** B-7.1
- **Delivered:** backend extended with `needsInfoCount`, `resolvedAwaitingConfirmCount`,
  `activeCount`, `attentionReports[]` (cap 5, needs-info + resolved awaiting confirm)
  from real queries; dashboard renders only API data.

---

## Phase 8 — Geographic capabilities

All four items **delivered 2026-10-15** (see CHANGELOG 0.9.0; approval gate pending).

### B-8.1 Coordinate validation + GeoJSON representation — DONE
- **AC:** Lat/lng range validation enforced (FR-11.2) incl. implausible null-island
  (0,0) → 400; API exposes GeoJSON Point `[lng, lat]` alongside lat/long (FR-11.5);
  exact coordinates only on authorized surfaces.
- **Phase:** 8 · **Depends:** B-5.5
- **Delivered:** `GeoJsonPoint` on `ReportDto` (exact) and both public DTOs (fuzzed);
  `ReportService` null-island guard on create+update; range validation already from
  B-5.5 retained.

### B-8.2 Location privacy (fuzzing) — DONE
- **AC:** Public surfaces never reveal exact coordinates (FR-11.4/NFR-5); same
  report yields a stable public point (no request-to-request jitter); fuzz radius
  bounded and configurable; exact location readable only by authorized roles.
- **Phase:** 8 · **Depends:** B-8.1
- **Delivered:** `LocationGeneralizer` — deterministic HMAC-SHA256(reportId,
  `geo.fuzz-secret`) offset, uniform distance 150–600 m + uniform bearing
  (`GEO_FUZZ_MIN/MAX_METERS`); replaces Phase 6's 2dp rounding. Applied to **all**
  public locations — a superset of FR-11.4's "sensitive categories" wording (owner
  decision recorded at gate). Production must set `GEO_FUZZ_SECRET`.

### B-8.3 Spatial queries + geographic filters — DONE
- **AC:** Bounding-box and radius filters on the public feed return correct subsets;
  invalid/mutually-exclusive filters → 400; queries execute on PostgreSQL+PostGIS;
  H2 suite stays green (validation-only for the native queries).
- **Phase:** 8 · **Depends:** B-8.1
- **Delivered:** `bbox` (`ST_Intersects`/`ST_MakeEnvelope`) + `near`
  (`ST_DWithin`, ≤500 km) on `GET /reports/public`, validated by
  `GeoBounds`/`NearSearch`; PG IT asserts Dar/Arusha filtering incl. the controller's
  sorted-Pageable path (live bug found: Hibernate appended invalid
  `order by reportedat` → fixed via `PublicReportService.unsorted()`).

### B-8.4 Map interactions + indexes — DONE
- **AC:** Map renders report locations with markers, click/pan interactions and
  graceful fallback when tiles/map unavailable; explore filter by visible extent;
  wizard location pickable from map; GIST index used for spatial filters (EXPLAIN);
  tests cover geographic operations.
- **Phase:** 8 · **Depends:** B-8.2, B-8.3
- **Delivered:** `ReportMapView` (Leaflet, lazy chunk) + `ReportMap` wrapper
  (error boundary + sr-only marker list); explore list/map toggle + "Search this
  area"; detail Location section; wizard tap-to-pick. V3 migration adds GIST on
  `(location::geography)` — `EXPLAIN` shows index scans for bbox **and** near.
  Web 36/36 tests; backend 90/90 (H2+PG); live smoke green.

**Resolves Phase 6/7 deferrals:** map view (Phase 6), coordinate fuzzing (Phase 6),
wizard map tiles (B-7.2). **Remaining deferral:** `near` radius filter has no citizen
UI (API-level only); browser-based map verification → Phase 15.

---

## Phase 9 - Administration and moderation (COMPLETE - 2026-10-17, approval gate pending)

### B-9.1 Organisation membership model (D-P9.1) - DONE
- **AC:** Team membership persists in `organization_members` (single org per user),
  backfilled from `users.organisation_id` then dropped; org-role grant requires
  membership; removing membership keeps the user's role (documented); ORG_ADMIN
  scoping reads membership.
- **Phase:** 9 — **Depends:** B-4.2, B-4.3
- **Delivered:** Flyway V4 migration + `OrganisationMembership` domain/repository/
  service; PG IT asserts V4 applied, column dropped, table + `audit_logs.outcome`
  present.

### B-9.2 User administration + filtered audit (FR-12.1, FR-10.2, P19/P22) - DONE
- **AC:** ADMINISTRATOR searches users and changes roles/active state, all audited;
  self role change → 400, last-active-admin demote/deactivate → 409; audit log
  filters by action/actor/entity/outcome/from/to with actor emails; invalid timestamp
  → 400.
- **Phase:** 9 — **Depends:** B-9.1, B-4.3
- **Delivered:** `UserAdminController` (`/users` search, `/{id}/role`, `/{id}/active`),
  audit filters on `GET /audit` + `GET /audit/organizations/{id}`; PG null-`Instant`
  filter bug fixed (`cast(:from as timestamp)`).

### B-9.3 Organisation + category management (FR-5.3, FR-12.2, P20/P21) - DONE
- **AC:** Admin creates/updates/activates organisations and manages members (grant
  org role audited); categories activate/deactivate + reorder, public list hides
  inactive, inactive category rejected on report create; inactive org cannot receive
  assignments (409).
- **Phase:** 9 — **Depends:** B-9.1
- **Delivered:** `OrganisationController` + `OrganisationService`; category
  `PUT /{id}/active?value=` + `PUT /{id}/order` + `?includeInactive=true`;
  `ORGANIZATION_*`/`MEMBERSHIP_*`/`CATEGORY_*` audit actions.

### B-9.4 Admin dashboard, analytics, settings (P14/P23/P24, D-P9.2) - DONE
- **AC:** `GET /dashboard/admin` returns moderation/workload/attention/recent panels
  for MOD/ADMIN; analytics summary filters by date/category/status/org/bbox
  (PostgreSQL-only spatial SQL; ORG_ADMIN forced to own org, non-member → 403);
  settings endpoint ADMIN-only, read-only, secrets masked.
- **Phase:** 9 — **Depends:** B-9.2, B-9.3
- **Delivered:** `DashboardService.adminSummary`, `AnalyticsService` (native SQL),
  `config` package (`SettingsDto`/`SettingsService`/`SettingsController`); PG IT
  executes analytics on PostgreSQL.

### B-9.5 Admin web section, pages 14-24 - DONE
- **AC:** Eleven role-guarded `/admin` pages (dashboard, report list + details,
  moderation queue, assignments, users, categories, organisations, audit, analytics,
  settings) with per-role route guards and staff entry from the account menu;
  lifecycle actions wired to backend with dialogs for reject/duplicate/needs-info.
- **Phase:** 9 — **Depends:** B-9.2–B-9.4, B-7.1
- **Delivered:** `pages/admin/*` (shared `AdminPageShell` + `adminNav`), `RequireAuth
  roles=`, `api.ts` admin methods; web tests 47/47 (11 new Phase 9 tests); live smoke
  PG 68/68.

---

## Cross-cutting (scheduled, not MVP-blocking)

| ID | Item | Phase | AC summary |
| -- | ---- | ----- | ---------- |
| B-X.1 | Password reset flow | 13 | Token expiring, non-disclosing responses, email provider integration |
| B-X.2 | Notification delivery separation | 13 | deliveredAt ≠ createdAt; failure handling |
| B-X.3 | `organization_members` + org scoping UI | 9 | Membership CRUD; matrix O-scopes enforced |
| B-X.4 | iOS project scaffold | 11 | `flutter build ios` succeeds on macOS CI-or-local; until then documented limitation |
| B-X.5 | Mobile token persistence + wiring | 11–12 | Login→session→report flow against real backend |
