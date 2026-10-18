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

All four items **delivered and approved 2026-10-15** (see CHANGELOG 0.8.0).

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

All four items **delivered and approved 2026-10-15** (see CHANGELOG 0.9.0).

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

## Phase 9 - Administration and moderation (COMPLETE - approved 2026-10-17)

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

## Phase 10 - Field-worker web application (COMPLETE - approved 2026-10-17)

### B-10.1 Worker dashboard, pages 25 - DONE
- **AC:** FIELD_WORKER lands on a role-guarded `/worker` dashboard fed by
  `GET /assignments/me`, grouped New / In progress / Completed with counts and an
  empty state; "My tasks" appears in the account menu for field workers only.
- **Phase:** 10 — **Depends:** B-5.x (assignment endpoints), B-7.1
- **Delivered:** `pages/worker/WorkerDashboardPage.tsx`, `RequireAuth
  roles=['FIELD_WORKER']` nested routes, `UserMenu` menuitem, `api.getMyAssignments`
  + `Assignment` types.

### B-10.2 Task details with progress updates (P26, FR-4.3) - DONE
- **AC:** The task details page shows instructions/category and location + map link;
  the worker starts (ASSIGNED → IN_PROGRESS), posts progress comments, and sees
  status-appropriate controls only (no review-only actions at any state).
- **Phase:** 10 — **Depends:** B-10.1, B-5.x (start/comments)
- **Delivered:** `pages/worker/WorkerTaskDetailsPage.tsx` (status-driven actions,
  comments form, status history); web tests assert start → completion-form flow and
  absence of review-only controls.

### B-10.3 Completion evidence (FR-4.4, T11) - DONE
- **AC:** Completion submission is blocked by the backend's photo requirement and
  the web surfaces the 400 message; evidence can be uploaded from the task page
  (multipart, 20 MiB client check) and is displayed as a media grid.
- **Phase:** 10 — **Depends:** B-10.2, B-7.4 (upload)
- **Delivered:** upload action + media grid on the task page; smoke proves the
  full path (no-photo 400 → multipart `FF D8 FF` upload 200 → assignee media read
  200 → claim 200).

### B-10.4 Reviewer return + authorization hardening (FR-4.5/4.7, T12) - DONE
- **AC:** Reviewers return a report from RESOLUTION_REVIEW to IN_PROGRESS with a
  mandatory comment from the admin report details page; a non-assignee worker
  cannot read or act on another worker's assignment; `PublicApiTest` privacy
  assertions are deterministic.
- **Phase:** 10 — **Depends:** B-10.2, B-9.5
- **Delivered:** admin "Return to work" action (`api.reportReturn`),
  `ReportLifecycleTest.workerOnlySeesAndActsOnTheirOwnAssignments` (403 ×4 +
  self-scoped list), `PublicApiTest` ≥100 m haversine assertions (flake fixed);
  backend 106/106, web 57/57, live smoke 50/50.

---

## Phase 11 - Flutter application foundation (COMPLETE - approved 2026-10-17)

### B-11.1 Navigation with auth guards - DONE
- **AC:** Every screen is reachable through a central named-route table;
  protected routes redirect signed-out users to sign-in, signed-in users are
  redirected away from auth surfaces, and unknown routes render a 404 screen
  (never a red error screen); the splash screen restores the session before
  choosing the destination.
- **Phase:** 11 — **Depends:** B-11.2
- **Delivered:** `lib/app/router.dart` (`Routes` + `generateRoute` guards),
  `NotFoundScreen`, `SplashScreen` bootstrap; `router_test.dart` covers
  guards, splash flow and the 404 route.

### B-11.2 API client + secure token storage - DONE
- **AC:** All HTTP goes through one client with a configurable base URL
  (`--dart-define=MTAAFIX_API_BASE`), bearer auth, a single refresh-and-retry
  on 401 that persists rotated tokens, and typed errors (`statusCode 0` =
  unreachable); tokens persist encrypted at rest and are injectable in tests
  without platform channels.
- **Phase:** 11 — **Depends:** auth endpoints (Phase 4)
- **Delivered:** `lib/core/network/api_client.dart` + `api_exception.dart`,
  `lib/core/storage/token_store.dart` (`SecureTokenStore` /
  `InMemoryTokenStore`, `flutter_secure_storage`); 8 API-client tests cover
  success/error/network/empty-body/bearer/refresh/rotation/clear-vs-keep.

### B-11.3 Authentication state + real auth screens - DONE
- **AC:** The app restores a persisted session (`GET /auth/me`) at boot;
  login/register/logout drive a single observable auth state; unreachable
  server during restore keeps tokens (error surfaced), rejected credentials
  clear them; the register flow establishes a session immediately after
  account creation.
- **Phase:** 11 — **Depends:** B-11.1, B-11.2
- **Delivered:** `AuthController` + `AuthScope`, real sign-in/register screens
  (password rules, confirm, optional mobile), profile screen bound to the
  session with sign-out confirmation; 8 auth-controller tests + the
  boot→sign-in→"My Reports" widget test.

### B-11.4 Theme migration + shared components - DONE
- **AC:** No screen uses hardcoded `Colors.orange`; loading, empty/error and
  toast/feedback states come from shared components; unknown routes show a
  friendly 404 with a way back.
- **Phase:** 11 — **Depends:** Phase 2 theme groundwork
- **Delivered:** `lib/core/widgets/{feedback_view,loading_view,snackbars}.dart`;
  report list/form, notifications, login/register/profile migrated to the
  M3 theme; placeholder `auth_page.dart` removed; `theme_test.dart`.

### B-11.5 Platform configuration + initial tests + build - DONE
- **AC:** Android manifest is build-valid (dev cleartext HTTP, valid
  `configChanges` flags) with the plugin-required NDK installed; iOS carries
  display name, usage descriptions and a dev ATS exception; `flutter analyze`
  is clean, the test suite passes, and a debug APK builds.
- **Phase:** 11 — **Depends:** B-11.1–B-11.4
- **Delivered:** Android/iOS manifests per above (NDK 28.2.13676358 installed
  via `android sdk install` — `sdkmanager` shim crashes in this environment);
  `flutter analyze` 0 issues, `flutter test` 29/29,
  `flutter build apk --debug` → `app-debug.apk`; iOS build requires macOS
  (documented limitation, B-X.4).

---

## Phase 12 - Flutter citizen features (COMPLETE - approved 2026-10-18)

### B-12.1 Onboarding + shell navigation - DONE
- **AC:** First launch shows persisted onboarding; a signed-in user reaches
  a five-tab shell (Home, My Reports, Map, Alerts, Profile) with route
  guards intact.
- **Phase:** 12 — **Depends:** B-11.1, B-11.3
- **Delivered:** 3-slide `OnboardingScreen` (`onboarding.seen` flag, skip),
  splash honouring the flag, `MainShellScreen` (IndexedStack +
  NavigationBar), `main_shell_test.dart`.

### B-12.2 Dashboard home - DONE
- **AC:** Home shows welcome, dashboard counters from `/dashboard/me`,
  recent reports/notifications and a primary report CTA.
- **Phase:** 12 — **Depends:** B-12.1, B-11.2
- **Delivered:** `HomeScreen` + `ReportsApi.dashboardStats` (flat-counter
  mapping, verified against live backend), stats tiles, recent lists; tab
  re-activation refresh via `MainShellTab` (stale-data defect found and
  fixed in verification).

### B-12.3 Report creation with photos + location - DONE
- **AC:** Citizen creates/edits a report with category, title, description,
  location (GPS with permission-denial handling or map pin) and up to 5
  photos (camera/gallery, MIME + 20 MB checks), sees backend validation
  errors.
- **Phase:** 12 — **Depends:** B-12.1, B-5.x backend, B-7.4 media family
- **Delivered:** `ReportFormScreen`, `MapPickerScreen`, `image_picker`
  evidence sheet, geolocator path with denied/permanently-denied dialogs,
  bare-array response wrapping for `GET /categories/public` (production bug
  found in verification), submit → detail navigation with MTF code.

### B-12.4 Report list + detail with lifecycle actions - DONE
- **AC:** List filters/paginates and refreshes on tab activation; detail
  shows summary, evidence, timeline, comments and status-gated actions
  (cancel/edit, confirm/dispute, resubmit).
- **Phase:** 12 — **Depends:** B-12.3, B-5.x lifecycle endpoints
- **Delivered:** `ReportListScreen` (status/category/search + load-more),
  `ReportDetailScreen` with `StatusChip` timeline, comments, action sheet
  wired to backend transitions; emulator-verified against
  `MTF-2026-000032` (SUBMITTED).

### B-12.5 Community map - DONE
- **AC:** Map loads, fetches reports in the viewport bbox (after camera
  ready), shows markers/count with status/category filters and my-location.
- **Phase:** 12 — **Depends:** B-12.1, Phase 8 public bbox API
- **Delivered:** `MapScreen` (flutter_map + OSM), post-frame deferred first
  bbox fetch (pre-camera bug found in verification), debounced camera-move
  refetch, marker bottom-sheet, filters; marker verified on device.

### B-12.6 Notifications feed - DONE
- **AC:** Real notifications list with unread styling; mark-one/mark-all
  read; tapping a linked report opens its detail.
- **Phase:** 12 — **Depends:** B-12.1, Phase 13 in-app backend
- **Delivered:** `NotificationsScreen` + `NotificationsApi`,
  `Report submitted` notification created by the backend during the
  emulator run, deep link to `ReportDetailScreen`.

### B-12.7 Profile + sign-out - DONE
- **AC:** Profile shows the session; edit dialog calls `PUT /auth/me`;
  sign-out with confirmation returns to sign-in; privacy/about rows present.
- **Phase:** 12 — **Depends:** B-11.3
- **Delivered:** `ProfileScreen`, edit-profile dialog, sign-out confirmation
  (widget-tested with `ensureVisible`), informational notification-
  preferences row (backend preference API is Phase 13).

### B-12.8 Verification: gates + device run - DONE
- **AC:** `flutter analyze` 0 issues, full test suite green, debug APK
  builds, core journeys run on an emulator against the live backend.
- **Phase:** 12 — **Depends:** B-12.1–B-12.7
- **Delivered:** analyze 0 issues, **111/111** tests, APK built with
  `--dart-define=MTAAFIX_API_BASE=http://10.0.2.2:8085/api/v1` (host 8080
  occupied by an unrelated service), emulator walkthrough with screenshots
  (`%TEMP%\phase12\`), five verification defects fixed (tofu icons /
  `uses-material-design`, bare-array decoding, map bbox timing, dashboard
  stats shape, tab staleness).

### Phase 12 known gaps
- FR-2.4 report **date** filter: backend supports status/category/q only —
  no date parameter (flagged, not implemented; needs backend change).
- No change-password endpoint (mobile edit profile cannot rotate password)
  → Phase 13 alongside B-X.1 password reset.
- iOS build/run unexecutable on Windows (B-X.4).

---

## Cross-cutting (scheduled, not MVP-blocking)

| ID | Item | Phase | AC summary |
| -- | ---- | ----- | ---------- |
| B-X.1 | Password reset flow | 13 | Token expiring, non-disclosing responses, email provider integration |
| B-X.2 | Notification delivery separation | 13 | deliveredAt ≠ createdAt; failure handling |
| B-X.3 | `organization_members` + org scoping UI | 9 | Membership CRUD; matrix O-scopes enforced |
| B-X.4 | iOS project scaffold | 11 | `flutter build ios` succeeds on macOS CI-or-local; until then documented limitation — `ios/Runner` scaffolded Phase 11, build unexecutable on Windows |
| B-X.5 | Mobile token persistence + wiring | 11–12 | Login→session→report flow against real backend — session restore/login/logout done Phase 11; report submission against the API lands with Phase 12 |
