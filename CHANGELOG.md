# Changelog

All notable changes to this project will be documented in this file.

Statuses below reflect what was **verified in this repository** (compile, tests,
analyzer, or file presence) — not intentions.

## [0.9.0] — 2026-10-15
### Phase 8 - Geographic capabilities
Implemented geographic functionality per master spec Phase 8 (SRS FR-11.x, Part 6.4)
— coordinate validation, PostGIS spatial queries, map interactions, geographic filters,
location privacy, indexes, and tests. **Approval gate pending.**

**Backend — geographic primitives (new `com.mtaafix.common.geo`)**
- `LocationGeneralizer` — public location fuzzing (FR-11.4): deterministic
  HMAC-SHA256(reportId, `geo.fuzz-secret`) offset, distance uniform 150–600 m
  (`geo.fuzz-min-meters`/`geo.fuzz-max-meters`, env `GEO_FUZZ_*`), uniform bearing;
  same report → same public point on every request. Applied to **all** public
  locations (stronger than FR-11.4's "sensitive categories" wording — disclosed for
  owner review at the gate)
- `GeoJsonPoint` — GeoJSON Point (FR-11.5), coordinates `[lng, lat]`; exact on
  authorized `ReportDto`, fuzzed on both public DTOs
- `GeoBounds`/`NearSearch` — parse+validate `bbox=minLon,minLat,maxLon,maxLat` and
  `near=lat,lon,radiusKm` (max 500 km); mutually exclusive → 400
- Null-island (0,0) rejected → 400 `BAD_REQUEST` on report create/update
  (FR-11.2 implausibility guard)

**Backend — spatial queries (PostgreSQL-only native SQL)**
- `GET /api/v1/reports/public` gains `bbox` + `near` params: `ST_Intersects`/
  `ST_MakeEnvelope` (bbox) and `ST_DWithin` geography (near), each with countQuery;
  H2 suite covers param validation only
- **Bug found (IT hardening):** the controller always passes a sorted `Pageable`;
  Hibernate appends `order by r.reportedAt` after the native query's own order-by →
  `column r.reportedat does not exist` (500). Fixed by stripping sorts in
  `PublicReportService.unsorted()`; `PostgresIntegrationTest` now exercises the live
  path through the service, not the repository directly
- Flyway `V3__report_location_geography_index.sql` — GIST index on
  `(location::geography)`; `EXPLAIN` verifies both bbox (`idx_reports_location`) and
  near (`idx_reports_location_geography`) use index scans

**Web (`web/`) — map interactions**
- New `components/map/ReportMapView` (Leaflet, lazy chunk 151 kB) + `ReportMap`
  wrapper (Suspense + error boundary + always-present sr-only marker list; jsdom-safe)
- Explore: list/map toggle, "Search this area" bbox filter, FR-11.4 privacy note;
  detail: Location section with generalized marker; wizard step 3: tap-to-pick map
  feeding the location draft
- Deps: `leaflet` + `react-leaflet`; `npm audit` → 5 dev-only vulns (vite/vitest
  chain; runtime deps clean) — documented, no forced upgrade

**Tests / verification**
- Backend `mvn verify` (H2 + live PostGIS): **90/90 green** (new
  `LocationGeneralizerTest` 11; PG IT 5/5 incl. bbox/near assertions + sorted-Pageable
  path)
- Web: lint/typecheck/build green, **36/36 tests** (new: explore map toggle + bbox
  flow, wizard map pick, detail Location section)
- Live smoke PG :8085: fuzzing deterministic + differs-from-exact; bbox/near filter
  correctly (baseline 23 → 12 Dar / 6 Arusha); null-island 400; bbox+near 400;
  malformed bbox 400; exact `geoJson` on authorized DTO; OpenAPI regenerated with
  `bbox`/`near`/`geoJson`

**Known follow-ups:** production must set `GEO_FUZZ_SECRET`; `near` radius filter has
no citizen UI (API-level only); browser-based map verification deferred to Phase 15
(unit-tested in jsdom here).

## [0.8.0] — 2026-10-15
### Phase 7 - Citizen web application
**Approved by owner 2026-10-15.** Implemented the authenticated citizen surface (SRS FR-1.x/FR-2.x/FR-7.x/FR-9.x,
Part 10 pages 8–13) per the approved Phase 7 plan (6 owner-confirmed decisions).

**Backend — citizen gaps**
- `PUT /api/v1/auth/me` — profile update (fullName + mobile only; blank name
  → 400; email/password not editable here)
- `POST /api/v1/reports/{id}/cancel` — T8 citizen cancel: creator-only (403),
  SUBMITTED only (409), → REJECTED with `CANCELLED_BY_CITIZEN`, history +
  creator notification + `REPORT_CANCELLED` audit
- `POST /api/v1/reports/{id}/media/upload` (multipart) — real photo upload:
  MIME allowlist (jpeg/png/webp), magic-byte check, ≤20 MiB, stored on local
  disk under `./uploads` (UUID names; documented Phase 13 placeholder);
  JSON-body `/media` kept for backward compatibility
- `GET /api/v1/media/{id}/content` — authenticated byte streaming with
  ownership/moderation checks (FR-9.3); `DELETE /media/{id}` now also removes
  the stored file
- `GET /api/v1/dashboard/me` extended (FR-7.4): `needsInfoCount`,
  `resolvedAwaitingConfirmCount`, `activeCount`, `attentionReports[]` (cap 5)
- `application.yml`: `media.storage-dir`, Spring multipart limits
  (21 MB file / 25 MB request)

**Web (`web/`) — auth layer + 6 citizen pages**
- `AuthProvider`/`useAuth()`, `RequireAuth` guard, header user menu; access +
  refresh tokens in localStorage with **silent refresh-once-on-401** in the API
  client (single in-flight refresh; session dropped on refresh failure) —
  token strategy documented per B-7.1 AC
- Pages: `/login`, `/register`, `/forgot-password` (info-only until Phase 13),
  and `/app` (dashboard with attention list + stat cards), `/app/reports/new`
  (4-step wizard: category → details → location/photo → review; draft in
  localStorage; geolocation "Use my location" + manual lat/lng; MTF reference
  on success), `/app/reports` (paginated), `/app/reports/:id` (timeline,
  comments, authenticated evidence blobs, cancel/confirm/dispute/resubmit
  eligibility), `/app/notifications` (read / read-all), `/app/profile`
  (name + mobile)
- Tests: `Phase7.test.tsx` (6: login success/error, profile update/blank,
  mark-read, 403 surfacing) + `ReportWizardPage.test.tsx` (5: step gating,
  submit payload, oversize rejection, ownership error); suite 33/33

**Bugs found by the live smoke (not visible under H2/unit tests)**
- Report-code counter collided with pre-existing seeded codes → 500 on create;
  fixed by bootstrapping the counter from the max existing `MTF-` serial at
  startup (`ReportRepository.findMaxReportSerial` + initializer)
- Oversize upload returned 500 — Tomcat's default 1 MB multipart limit threw
  before the service's 20 MiB check; fixed with Spring multipart limits and a
  `MaxUploadSizeExceededException` → 413 handler (service still returns 400
  for files that pass the container limit but exceed 20 MiB)

**Verified:** backend `mvn verify` green (73 H2 tests + 3/3 PG integration);
web lint/typecheck/build/test green (33 tests); live smoke on PG (port 8085):
register → login → create → photo upload → content stream → my reports →
dashboard → profile update → notifications → cancel → refresh, plus wrong
password 401, bad token 401, GIF/magic-byte/oversize 400 paths.
Deferrals (owner decisions): password change/reset → 13, notification prefs
→ 13, map → 8, org features → 9.


## [0.7.0] — 2026-10-15
### Phase 6 - Public website
**Approved by owner 2026-10-15.** Implemented the public transparency surface
(SRS FR-8.1–8.5, Part 10 pages 1–4).

**Backend (public read APIs, all `permitAll`)**
- `GET /api/v1/reports/public` — explore feed with `status`/`categoryId`/`q`/
  `dateFrom`/`dateTo` filters, pagination, `reportedAt DESC`; coordinates
  rounded to 2 decimals (interim until FR-11.4 fuzzing, Phase 8)
- `GET /api/v1/reports/public/{code}` — detail by `MTF-YYYY-NNNNNN` code:
  high-level public timeline (status + timestamp only), `duplicateOfCode`,
  `resolvedAt`/`closedAt`; 404 `RESOURCE_NOT_FOUND` for unknown codes
- `GET /api/v1/categories/public`, `GET /api/v1/stats/public` (aggregate counts)
- Privacy: no reporter identity, phone, comments, or media serialized
- CORS from existing `cors.allowed-origins` property (was previously unused)
- **Bug found by real-PG smoke (not visible under H2):** `findPublic` used
  `lower(concat('%', :q, '%'))`, which PostgreSQL rejected (`function
  lower(bytea) does not exist` / `could not determine data type of parameter`),
  and null `Instant` filters were untyped in `is null` checks. Fixed by binding
  a pre-built LIKE pattern and non-null sentinel date bounds.

**Web (`web/`)**
- React Router app: landing, how-it-works, explore (search + filters +
  pagination), public report detail, `/report` info route, 404 page; shared
  Layout with skip-link + ARIA landmarks; `LinkButton` for valid link-buttons
- `lib/api.ts` typed client (envelope parsing, `ApiError`, timeout/network
  mapping); `lib/status.ts` status labels/tones
- Vitest + Testing Library added (`npm run test`): 22 tests across API client,
  status helpers, routes/404, explore filters, detail privacy/timeline
- CI web step now runs `npm ci && npm run lint && npm run test && npm run build`

**Verified:** backend `mvn verify` green (62 H2 tests + 3/3 PG integration);
web lint/typecheck/build/test green; live smoke on PG (port 8085) + Vite dev
(5173): all public endpoints, filters, 400/404 paths, CORS preflight, privacy
assertions, resolved-report timeline, stats. Owner decisions: map → Phase 8,
public media payloads → Phase 10/13, `/report` info-only until Phase 7 auth.

## [0.6.0] — 2026-10-14
### Phase 5 - Core reporting workflow
**Approved by owner 2026-10-14.** Implemented the
full submit → review → assign → resolve → confirm workflow per
REPORT_LIFECYCLE.md v1.0 (T1–T17) and ADR-0003 (category-driven reporting).

- Flyway `V2__category_driven_lifecycle.sql` (applied + validated on
  PostgreSQL 18): status extended to 12 states, `subject` dropped,
  `category_id` NOT NULL with 12 seeded categories, `duplicate_of` FK,
  `reason_code` on history, `uploaded_by` on media, `report_code_counters`
  (column `code_year` — `year` is reserved in H2)
- New `report/lifecycle` package: `LifecycleAction` + `ReportLifecycle`
  encoding all T1–T17 transition and actor rules; invalid transition → 409
  `INVALID_STATUS_TRANSITION`, wrong role → 403 (role gate checked before
  state gate); every transition appends a history row with its actor
  (latest-history == status invariant covered by tests)
- Category-driven creation: `categoryId` required (400), unknown → 404;
  unique code `MTF-<year>-<6-digit>` from a pessimistic-locked per-year
  counter (collision-free under 4-thread concurrency test); coordinate
  validation ±90/±180 → 400 (nested `@Valid`); point persisted with SRID 4326
  (previously written as SRID 0 — latent insert bug fixed)
- Endpoints: `GET /reports` (status/categoryId/orgId filters, role scoping,
  pagination), needs-info, resubmit, duplicate (canonical by id or code with
  history note on canonical), start, complete (≥1 PHOTO evidence, assignment
  → COMPLETED), return, confirm, dispute, reopen, comments, media attach
  (FR-9.2 MIME allowlist + ≤20 MiB), `DELETE /media/{id}`
- Editing restricted to SUBMITTED/NEEDS_INFORMATION; audit rows for
  verified/rejected/duplicate/assigned/completed/returned/resolved/reopened;
  notifications for needs-info/resubmit/duplicate/completion/return/reopen/
  dispute/comments
- DTO changes: `subject` removed (create/update/report DTOs),
  `RejectReportRequest(reasonCode, comment)`, lifecycle request records,
  `ReportDto.code` + `duplicateOfId`, `StatusHistoryDto.reasonCode`
- Deferrals: T8/T2r/T7r/T10r endpoints (no API_DESIGN §2/§3 contract rows),
  byte-level media content checks (storage in Phase 13), reopen-window
  enforcement (needs `closed_at`), org-wide T14 notifications
- Tests: `ReportLifecycleTest` (10: full journey, 409/403/400 negatives,
  history invariant, code format, scoping + filtering, duplicate linking,
  audit, comments/media, edit window), `ReportCodeGeneratorTest`
  (concurrent uniqueness); matrix/auth fixtures updated for the 12-state
  model; H2 49/49, PostgreSQL 3/3 with V2 applied; `openapi.json`
  regenerated; live boot smoke 16/16 on port 8085

## [0.5.0] — 2026-10-14
### Phase 4 - Authentication & authorization
**Approved by owner 2026-10-14.** Enforced the
five-role permission model (ROLE_PERMISSION_MATRIX) across the API per SRS
FR-1.1/FR-5.4, with role and account status resolved from the DB on every
request.

- `@EnableMethodSecurity` enabled - the 7 existing `@PreAuthorize`
  annotations were previously inert
- `JwtAuthenticationFilter` rewritten: requires `Bearer` + valid token
  with `typ=access`, loads the user from DB per request (role change /
  revocation effective immediately), rejects inactive accounts, authorities
  built from the loaded entity; principal is the `User` entity
- `JwtTokenProvider`: fixed `buildToken` (jjwt 0.12 claim map is
  immutable - token generation always threw `UnsupportedOperationException`,
  so login/refresh never worked); added `typ` claim (`access` /
  `refresh`) + `getType()` / `isAccessToken()`
- Registration forces `Role.CITIZEN` (`role` removed from
  `RegisterRequest`) per FR-5.4; `POST /auth/refresh` accepts
  `RefreshRequest`, validates `typ=refresh`, rotates the token pair;
  `GET /auth/me` requires an authenticated principal
- Error semantics: unknown email and wrong password both 401
  `INVALID_CREDENTIALS` (no user enumeration); inactive account 403
  `ACCOUNT_INACTIVE`; invalid/expired token 401 `INVALID_TOKEN`;
  `AccessDeniedException` 401 `UNAUTHORIZED` when anonymous, 403
  `FORBIDDEN` when authenticated; `IllegalStateException` 409
  `INVALID_STATUS_TRANSITION`; resource-not-found code now
  `RESOURCE_NOT_FOUND`
- Role gates (API_DESIGN §2 access column): submit/verify/reject/resolve →
  MODERATOR|ADMINISTRATOR; assign → ADMINISTRATOR|ORGANIZATION_ADMIN +
  org scope; close → ADMINISTRATOR; `/dashboard/stats` →
  MODERATOR|ADMINISTRATOR (matrix row 11); `/assignments/me` →
  FIELD_WORKER; category creation + global audit → ADMINISTRATOR; org
  audit/stats/reports → ORGANIZATION_ADMIN|ADMINISTRATOR + own-org scope
- Scoping enforced in code: report get/update/assign/comment/media check
  ownership, org, or moderation rights (`assertCanRead` /
  `assertInOrganisationScope`); notifications owner-only (reads, mark
  all); `ReportService.list(...)` scoped but deliberately unexposed (no
  list endpoint in API_DESIGN §2)
- Audit wiring: `AuditService.record(...)` implemented (own ObjectMapper -
  Boot 4 has no `com.fasterxml` bean); category creation emits
  `CATEGORY_CREATED` events (FR-10.1)
- Assignments now persisted (`AssignmentRepository.save`) - previously
  mutated in memory only
- Latent defects fixed: H2 test schema silently failed `audit_logs` DDL
  (`JSONB` domain via `INIT=CREATE DOMAIN`); notification list leaked
  all users' rows; `markAsRead` had no owner check; assignment scoping
  missing
- Tests: `JwtTokenProviderTest` (5), `AuthFlowTest` (16),
  `AuthorizationMatrixTest` (15); full suite **41/41 green** (3 PG skipped
  without env); PG integration **3/3 green** (`MTAAFIX_PG_IT=true`);
  live boot smoke on PG: register (role forced CITIZEN)/login/refresh
  rotation/401/403 paths verified
- Deferred (tracked in ROADMAP): lifecycle actor gates T1-T17 + full 409
  state machine (Phase 5), report list endpoint + comment/media routes
  (Phase 5), CORS (Phase 6/16), matrix rows 8/9/14 endpoints (Phase 9),
  refresh-token invalidation (Phase 16)
- Docs: API_DESIGN provisional notes → enforced, ROLE_PERMISSION_MATRIX
  note 6 decision recorded (DB lookup implemented), ROADMAP/BOARD/BACKLOG
  updated, `docs/api/openapi.json` regenerated

## [0.4.0] — 2026-10-14
### Phase 3 — Backend foundation completion
**Approved by owner 2026-10-14.** Verified the backend against real PostgreSQL 18.6 +
PostGIS 3.6.2 per master spec Phase 3; fixed the defects that made that
verification impossible. No API behavior changes beyond correctness fixes
listed below.

- Installed PostGIS 3.6.2 for PG18 (official OSGeo bundle overlay, elevated);
  `CREATE EXTENSION postgis` + `postgis_version()` verified (USE_GEOS=1, USE_PROJ=1)
- **V1 migration rewritten** (never applied to any shared environment; dev DBs
  recreated): `CREATE EXTENSION IF NOT EXISTS postgis`; tables moved from
  `mtaafix.` schema to `public` (matches `currentSchema=public` and entities);
  `GEOGRAPHY` → `GEOMETRY(POINT|POLYGON, 4326)` to match entity
  columnDefinitions; GIST indexes on report geometries; reports↔assignments
  FK cycle resolved (deferred `ALTER TABLE` after both tables exist);
  `updated_at` added to all BaseEntity-backed tables; `from_status` nullable
  (initial SUBMITTED transition has no previous state); UUID columns →
  `VARCHAR(36)` to match String entity ids
- Dependencies: added `org.hibernate.orm:hibernate-spatial` (JTS geometry
  mapping), `spring-boot-flyway` (Spring Boot 4 module split — Flyway
  auto-configuration was silently absent, migrations never ran),
  `spring-boot-webmvc-test` (Boot 4 MockMvc test package); removed unused
  Testcontainers artifacts (no Docker in this environment)
- Entities: `BaseEntity` — `@PrePersist` default for `createdAt` (auditing
  annotations were inert without `@EnableJpaAuditing`), removed conflicting
  `@GeneratedValue` (ids are assigned random UUIDs at construction);
  `AuditLog.details` mapped as jsonb via `@JdbcTypeCode(SqlTypes.JSON)`
- Verified end-to-end on PG+PostGIS: boot, Flyway V1 apply, `ddl-auto:
  validate`, `GET /actuator/health` = UP
- Tests: `PostgresIntegrationTest` (env-gated `MTAAFIX_PG_IT=true`):
  Flyway history, PostGIS availability, geometry insert/read-back +
  `ST_AsText`; `OpenApiExportTest` exports `docs/api/openapi.json` on every
  test run. Full suite 5/5 green with PG; without env var PG tests skip
  cleanly (CI path)
- `docs/api/openapi.json` — committed OpenAPI spec (admin-role endpoints
  included; springdoc-generated)
- `DEVELOPMENT.md` — runbook: prerequisites, PostGIS install, config matrix,
  H2-vs-PG test matrix, troubleshooting

## [0.3.0] — 2026-10-13
### Phase 2 — Design system & UI foundation
**Approved by owner 2026-10-13.** Established the
web design foundation (tokens, components, gallery) and minimal Flutter theme
groundwork per master spec Phase 2 and SRS §9. No product pages yet (Phases 6–7).

- Fixed corrupted `web/tsconfig.json` / `tsconfig.node.json` (literal `\n`
  sequences from old-repo extraction)
- Tailwind v4 via `@tailwindcss/vite`; design tokens in
  `web/src/styles/tokens.css` (spec §9.1 palette); global styles + Inter in
  `web/src/styles/global.css`, `web/index.html`
- ESLint 9 flat config with `eslint-plugin-jsx-a11y`; `npm run lint` script; CI
  web step now runs lint + build
- Component library `web/src/design-system/`: Button, Field, TextAreaField,
  Card, Badge, Modal (Escape/outside-click/focus management), Toast +
  ToastProvider (`aria-live`), EmptyState, LoadingState, ErrorState — all
  exported from the barrel; a11y wiring (labels, aria-invalid, roles)
- In-repo design gallery at `/design` (`web/src/gallery/`) with color/type/
  component/status showcases and presentational layout previews: landing, auth,
  citizen dashboard, report wizard (clearly labeled design validation; no
  interactive dead controls)
- `docs/design/DESIGN_SYSTEM.md` — tokens, typography, status→badge mapping
  (12 states), responsive breakpoints, manual accessibility checklist
- Minimal brand assets: `web/public/favicon.svg` (green pin + check)
- Flutter groundwork: `mobile/lib/app/theme.dart` (Tanzanian palette,
  M3 ThemeData) wired into `mobile/lib/app.dart` (screens still use hardcoded
  `Colors.orange` — migration scheduled Phase 11)
- Verified: `npm run lint` (0 issues), `npm run build` (tsc + vite, exit 0),
  `flutter analyze` (no issues)
- Known: 2 npm audit findings (esbuild ≤0.24.2 via vite ≤6.4.2 — dev-server
  request vulnerability, GHSA-67mh-4wv8-2f99); fix requires vite 8 breaking
  upgrade — deferred, decision pending owner

## [0.2.0] — 2026-10-13
### Phase 1 — Requirements & system design (docs only; no code changes)
**Approved by owner 2026-10-13.** Established the requirements baseline before further implementation, per master
spec Phase 1. Owner-approved decisions D1–D4 (SRS §1.5): master phase numbering
0–23 with MVP = Phases 0–7; five-role model with system-admin as scoped
ADMINISTRATOR; 12-state report lifecycle with CLOSED terminal (CANCELLED deferred);
category-driven reports with `Report.Subject` deprecated.

- `docs/requirements/SRS.md` — v1.0 draft: scope, FR-1.x–FR-12.x, NFR-1–14,
  privacy boundaries, MVP definition, 36-page inventory mapping, code gap summary
- `docs/design/ROLE_PERMISSION_MATRIX.md` — all roles × 14 permission areas
  (spec §3.8) + enforcement requirements for Phase 4
- `docs/design/REPORT_LIFECYCLE.md` — 12 states, transitions T1–T17 with actors/
  reasons/notifications/audit/reversibility, invariants, `MTF-YYYY-NNNNNN`
  reference rules, resolution & duplicate policies, code-gap list for Phase 5
- `docs/design/DOMAIN_MODEL.md` — 10 present entities reconciled with code +
  Flyway V1; deferred conceptual entities justified; planned migrations table
- `docs/api/API_DESIGN.md` — conventions (envelope, pagination, errors, auth),
  implemented-vs-planned endpoint tables, corrections to legacy API.md
- ADRs: `ADR_0002_REPORT_LIFECYCLE.md`, `ADR_0003_CATEGORY_DRIVEN_SUBJECTS.md`,
  `ADR_0004_ROLE_MODEL.md`
- `docs/project-management/ROADMAP.md` — restructured to master Phases 0–23
  (replaces legacy 14-phase numbering per D1)
- `docs/project-management/BACKLOG.md` — prioritized items B-2.x…B-7.x with
  acceptance criteria and dependencies
- `docs/project-management/BOARD.md` — updated to master numbering

## [0.1.0] — 2026-10-13
### Phase 0 — Repository remediation & verified baseline
This repository was re-initialized from a contaminated history (repo rooted at
a home directory, mixed with unrelated projects). All claims from the previous
CHANGELOG were re-audited against the actual tree; unverified claims were
removed. What exists and was verified today:

- Canonical tree: `backend/`, `web/`, `mobile/`, `docs/`, `.github/`
- Backend (Spring Boot, Java 25, Maven): merged from three divergent copies
  into one module; `mvn compile` succeeds; `mvn test` green (1 context-load
  test, H2, Flyway disabled in test profile)
- Backend API surface present: Auth (JWT), Report lifecycle, Categories,
  Assignments, Dashboard, Audit, Notifications, Media, Springdoc OpenAPI
- Flyway migration `V1__create_base_tables.sql` present (PostGIS columns)
- Mobile (Flutter): screens for splash/login/register/report list/report
  form/notifications/profile; `flutter pub get` and `flutter analyze` clean
  after fixes (removed nonexistent `boat_dirt_track` dep, `intl` bumped to
  `^0.20.3`, analyzer errors fixed)
- Web: **dependency manifest and TypeScript configs only** — no `src/`, no
  `index.html`, no `vite.config`; nothing buildable yet
- CI workflow (`.github/workflows/ci.yml`): backend compile+test; web and
  mobile steps are guarded and skip when scaffolds are incomplete
- Root `.gitignore` and `.env.example` added

Known gaps (deliberately not claimed complete):
- Web application not started (Phase 5)
- Backend test coverage is a single context-load test; no service/controller
  or PostGIS integration tests yet (Phase 10)
- Mobile has no iOS project folder, no offline sync, no push notifications
- Role-specific experiences (Phase 3) not built
- Local PostgreSQL has no PostGIS extension installed yet (Phase 3 task)
