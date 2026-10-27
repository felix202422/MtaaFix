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

## Phase 13 — Notifications and media (COMPLETE — approved 2026-10-18)

### B-13.1 Notification preferences + unread count - DONE
- **AC:** `GET/PUT /notifications/preferences` and
  `GET /notifications/unread-count` exist; web profile shows an
  auto-saving preferences card (push read-only with explanation) and a
  header unread badge; mobile has a preferences screen and an Alerts-tab
  badge fed by the server count.
- **Phase:** 13 — **Depends:** B-12.6, Phase 12 notifications feed
- **Delivered:** preference rows (`emailEnabled`, `pushEnabled`,
  `pushAvailable=false`); web `NotificationBell` + `ProfilePage` prefs card;
  mobile `UnreadCountScope`/`UnreadCountController` + badge refresh on shell
  mount and feed changes; `NotificationPreferencesScreen` with in-app
  always-on, email toggle (PUT + snackbar), push disabled until a provider
  exists; covered by web `Phase13.test.tsx`, mobile prefs/router/profile
  tests.

### B-13.2 Password reset + change (B-X.1) - DONE
- **AC:** forgot/reset with expiring single-use token and non-disclosing
  responses; change-password for the signed-in user; both web and mobile
  flows.
- **Phase:** 13 — **Depends:** B-12.7 profile surface
- **Delivered:** V5 `password_reset_tokens` (SHA-256, 30 min, single-use);
  `POST /auth/password/{forgot,reset,change}` with identical 202s and
  session-preserving change; web forgot/reset pages (confirmation copy,
  `?token=` prefilled); mobile forgot/reset/change screens + guarded routes;
  login "Forgot password?" wired; live E2E: bogus→400, planted→200,
  reuse→400, old password→401, change→200.

### B-13.3 Notification delivery separation (B-X.2) - DONE
- **AC:** delivery state recorded separately from notification creation;
  email failures handled with bounded retries; push explicitly absent until
  a provider is configured.
- **Phase:** 13 — **Depends:** B-13.1
- **Delivered:** `notification_deliveries` rows — IN_APP always DELIVERED;
  EMAIL created PENDING, attempted once, retried by `EmailRetryScheduler`
  ≤ `notifications.email.max-attempts` (3), terminal SENT/FAILED/SKIPPED;
  kill-switch `notifications.email.enabled` default false (no SMTP
  provider); no push rows (`pushAvailable=false`); live rows observed
  IN_APP DELIVERED + EMAIL SKIPPED ("Email channel disabled").

### B-13.4 Media hardening + attachment lifecycle - DONE
- **AC:** MIME allowlist + content (magic-byte) check, 20 MiB and
  10-attachments-per-report caps enforced server-side; attachments deletable
  by authorized users; clients validate before upload.
- **Phase:** 13 — **Depends:** Phase 7 upload endpoint
- **Delivered:** backend MIME/magic/size checks (magic-byte pre-existing)
  plus the new per-report cap; size/cap limits now config-driven
  (`media.*`), authorized `GET /media/{id}/content`; web wizard 5-photo with
  per-file validation + partial-failure toast, tracking/worker input
  checks, admin/worker media opened as authenticated blobs; mobile
  report-detail evidence (add via picker with MIME/20 MB/10 checks, remove
  with confirm → `DELETE /media/{id}`, `canEditMedia` gate);
  live battery: PNG 200 / .txt 400 / fake-JPEG magic 400 / 21 MB 400 /
  11th attachment 400 / DELETE 204.

### B-13.5 Verification: gates + live battery - DONE
- **AC:** every suite green and each new flow exercised against the live
  backend before the report.
- **Phase:** 13 — **Depends:** B-13.1–B-13.4
- **Delivered:** backend H2 **124/124** + PG IT **7/7** (V5 applied); web
  **68/68** + lint/tsc/build; mobile analyze 0 issues + **134/134** (tests
  caught `ModalRoute.of` in `ResetPasswordScreen.initState` — moved to
  `didChangeDependencies`); live :8085 battery (forgot non-disclosure,
  reset E2E, change-password, prefs GET/PUT, unread count, media caps,
  notifications on submit/cancel, delivery rows).

### Phase 13 known gaps
- Push provider (FCM/APNs) not configured — `pushAvailable=false`, no push
  delivery rows (owner decision flagged for a later phase).
- Email delivery behind `notifications.email.enabled` kill-switch (default
  false; no SMTP provider configured) — enabling it is an owner action.
- Public unauthenticated media endpoint **not** implemented (owner decision
  — do not open unilaterally).
- Backend attach/delete has no report-status guard; the active-status
  (`canEditMedia`) gate is enforced in the mobile UI only.
- JWTs are not revoked after a password reset (documented decision — tokens
  stay valid until expiry).
- FR-2.4 report date filter still unsupported by the backend; iOS build
  macOS-only (B-X.4).

## Phase 14 - Offline capabilities (COMPLETE - approved 2026-10-21)

### B-14.1 Backend idempotency (Option B) - DONE
- Flyway V6 `report_idem_keys` (key → `report_id` + `payload_hash`, 24 h
  TTL, hourly purge); optional `Idempotency-Key` `[A-Za-z0-9_-]{16,64}` on
  `POST /api/v1/reports`: 400 invalid format, 201 first use, 201 stored
  response replay (same id, no duplicate row), 409 `IDEMPOTENCY_CONFLICT`,
  409 `IDEMPOTENCY_IN_PROGRESS` (stale intent after 2 min); REQUIRES_NEW
  transactions; private `ObjectMapper` + `hashOf` (SHA-256).

### B-14.2 Mobile draft storage - DONE
- `DraftStore` (schema 1, atomic writes) + debounced 400 ms autosave +
  Restore/Discard offer; drafts never appear in My Reports and carry no
  MTF code; empty `categoryId` drafts restorable (product fix found by
  tests).

### B-14.3 Pending queue + foreground retry - DONE
- `OfflineController` outbox with `pending`/`attachingPhotos`/`failed`
  states; single-flight flush triggered by connectivity-online, shell
  mount, app resume, and manual Retry; create replayed with the same
  per-form-session idempotency key, then photos attach stepwise; honest
  photo-failure reporting; `PendingSubmissionsScreen` (Edit/Retry/Discard),
  offline strip, pending banner with just-submitted reload;
  `clearAll()` on sign-out.

### B-14.4 Categories cache - DONE
- Cache refreshed on every successful load; offline load falls back with a
  "may be outdated" hint.

### B-14.5 Verification: connectivity transitions + battery - DONE
- offline→online auto-flush, offline no-op flush, offline hint disables
  retry, banner appear/disappear (unit + widget); live battery on :8085 —
  replay same id, 409 conflict, 400 invalid key; backend 133 H2 + 8 PG,
  mobile 167/167, analyze 0.

### Phase 14 known gaps / deferrals (owner-approved scope)
- Offline browsing of existing reports and offline map tiles deferred.
- Background sync deferred — retry is foreground-only by design (no
  background isolates/platform work).
- iOS build remains macOS-only (B-X.4).

---

## Phase 15 - Quality assurance (COMPLETE - approved 2026-10-21)

### B-15.1 Full suite verification - DONE
- Backend `mvn test` **133/133** H2 (PostgresIntegrationTest auto-skips
  without env); PG `mvn test -Dtest=PostgresIntegrationTest` **8/8** with
  `MTAAFIX_PG_IT=true` + local PostgreSQL (V6 applied, PostGIS, spatial
  queries, idempotency); web `eslint .` 0 / `tsc --noEmit` 0 /
  `vitest run` **68/68** (10 files) / `vite build` green; Flutter
  `flutter analyze` 0 / `flutter test` **170/170** (+3).

### B-15.2 Critical-journey + gap-fill mobile tests - DONE
- New `mobile/test/phase15_quality_test.dart`: citizen critical journey
  (sign in, file report `MTF-2026-000042`, detail, back, refresh, report
  in My Reports), critical-screens no-overflow sweep at 320x640,
  accessible-semantics assertions; local harness (stateful mock HTTP
  client, geolocator/connectivity/path-provider channel mocks).

### B-15.3 Responsive fixes - DONE
- Five RenderFlex overflows found at 320x640 and fixed: login auth actions
  Row -> Wrap; home `_SectionHeader` title -> Expanded + ellipsis;
  report-form category dropdown `isExpanded: true`; photo attach buttons
  Row -> Wrap.

### B-15.4 Map + test infrastructure - DONE
- `MapScreen` lazy `_mapBooted` tile mount (placeholder until first
  activation); new `core/map/app_tile_provider.dart` - `appTileProvider()`
  disables flutter_map's disk-cache worker isolate only when
  `FLUTTER_TEST` is set (the isolate stalled fake-async test completion);
  production disk cache unchanged; wired into all three `TileLayer` sites.

### B-15.5 Live battery: journeys, role boundaries, lifecycle - DONE
- :8085 **63/63** - two full lifecycle chains (claim/verify, assign with
  photo evidence, start, complete, resolve, close, reopen; second pass
  ending in citizen confirm); 403 matrix (citizen/mod/worker vs
  admin/moderation), 401 anonymous, FR-5.4 org-role-without-membership ->
  400, 409 state gate, 403 unassigned-ownership guard; known-defect probes
  reproduced (non-JSON POST -> 500, GET on POST-only -> 500).

### B-15.6 Accessibility + responsive verification - DONE
- Mobile: `ensureSemantics` assertions for login fields, NavigationBar
  destinations, form field labels, refresh tooltip; 320x640 sweep. Web:
  jsx-a11y clean; audit documents single structural breakpoint + wrapping
  header nav; jsdom cannot assert CSS-driven visibility (tooling
  limitation recorded, not a product defect).

### Phase 15 known defects + mitigation plan
Exit criterion: critical defects resolved or explicitly documented with an
approved mitigation plan. Fixed this phase: five 320x640 overflow defects
(B-15.3). Carried items (D1-D4 resolved in Phase 16; see the Phase 16
section below for the verification evidence):

| # | Defect / limitation | Evidence | Severity | Mitigation / disposition (target) |
| - | ------------------- | -------- | -------- | --------------------------------- |
| D1 | Non-JSON body on `POST /api/v1/reports` -> 500 instead of 4xx | Live probe Phase 15 (reproduced; stale jar) | Low (error hygiene) | **RESOLVED Phase 16** - source already mapped malformed JSON -> 400; rebuilt jar verified live (400 `MALFORMED_REQUEST`) + `WebErrorHandlingTest` |
| D2 | Wrong HTTP method on POST-only endpoints -> 500 instead of 405 | Live probe Phase 15 (reproduced; stale jar) | Low (error hygiene) | **RESOLVED Phase 16** - explicit 405 handler + live probe + tests |
| D3 | JWTs survive password reset (no revocation) | Phase 13 flagged decision | Medium (security) | **RESOLVED Phase 16** - `users.token_version` (V7), `tv` claim, filter/refresh checks; reset and change bump; change rotates the pair (web/mobile persist it) |
| D4 | Media attach/delete has no backend report-status guard (mobile UI gates only) | Phase 13 | Medium (security) | **RESOLVED Phase 16** - `assertMediaEditable` 409 on terminal statuses; tests + live probe |
| D5 | Offline storage (drafts/outbox) unencrypted at rest | Phase 14 | Medium (device security) | **DEFERRED by owner (Phase 16)** - tokens already Keystore-protected via `flutter_secure_storage`; drafts/outbox encryption = B-16.1 (Phase 20) |
| D6 | No push provider (FCM/APNs); `pushAvailable=false` | Phase 13 | Medium (feature) | Needs external provider credentials; Phase 20/21 |
| D7 | Email notifications behind kill-switch, no SMTP provider | Phase 13 | Low | EMAIL rows recorded SKIPPED; configure SMTP before Phase 20 beta |
| D8 | FR-2.4 report date filter unsupported by backend | Since Phase 12 | Low (feature gap) | Deferred; Phase 21 advanced features |
| D9 | Web narrow-width header nav wraps instead of collapsing (no hamburger) | Phase 15 audit | Low (UX polish) | All destinations remain reachable via wrap; refine with real-device feedback in Phase 20 |
| D10 | iOS build unexecutable on Windows host | B-X.4 | Medium (platform) | `ios/Runner` scaffolded; macOS CI/device verification required before Phase 20 release |
| D11 | Suites not wired into CI (backend/web/mobile run manually) | Structural | Medium (process) | **Resolved Phase 18** - `ci.yml`: backend job runs H2 suite + PG IT against a PostGIS service, web lint/test/build, mobile analyze/test, gitleaks; `release.yml` on `v*` tags |
| D12 | jsdom cannot assert CSS-driven responsive visibility | Phase 15 audit | Info (tooling) | Covered by lint + semantics tests + documented audit; real-browser check in Phase 20 |
| D13 | SRS table rows 14-26 (device/section listing) stale vs current feature set | Carried | Low (docs) | SRS sync pass in Phase 20 release prep |
| D14 | Phase 14 deferrals: offline browsing/map tiles, background sync | Owner-approved deferral | Low | Remains approved scope; reconsider Phase 21 |

### Phase 16 outcomes + deferrals
Exit criterion met 2026-10-22 (review record:
`docs/standards/SECURITY_REVIEW.md`; live battery 23/23). Deferrals and
follow-ups owned by later phases:

| ID | Item | Target | AC summary |
| -- | ---- | ------ | ---------- |
| B-16.1 | Encrypt mobile offline drafts/outbox at rest (D5) | Phase 20 | Keystore-backed key; encrypted `DraftStore`/`OutboxStore` files; device testing; tokens already covered by `flutter_secure_storage` |
| B-16.2 | Web dev-toolchain majors (`npm audit` dev-only advisories: vitest 5, vite 8 / esbuild, tinypool) | Scheduled toolchain task | `npm audit` clean including dev; suites stay green after the breaking upgrades |
| B-16.3 | Backup automation + PITR (runbook documented in SECURITY_REVIEW) | Ops/deploy phase | Scheduled `pg_dump` + `uploads/` backup; restore drill evidence; retention policy - **DONE Phase 19** (`backend/scripts/backup.ps1` + `restore-database.ps1`; dump/restore drill PASSED into `mtaafix_restore` with 5,038 reports + PostGIS; PITR drill PASSED, evidence `backend/scripts/pitr-drill-evidence.txt`; retention default 14 days) |
| B-16.4 | Phase 18 handoffs: activate `prod` profile with real secrets (`ProdSecretsGuard`); forwarded-header/proxy config so the rate limiter sees real client IPs | Phase 18 | Boot under `prod` refuses weak defaults; 429 buckets keyed by true client IP behind the proxy - **DONE Phase 18** (`application-prod.yml` + drills: refusal proven live; `ForwardedIpRateLimitTest` proves XFF bucket isolation) |
| B-16.5 | Full NVD dependency scan (skipped per owner: network/API friction) | Ops phase if scanner provisioned | Recorded scan output attached to a future review refresh |

### Phase 17 outcomes + deferrals
Exit criterion met 2026-10-22 (review record:
`docs/standards/PERFORMANCE_REVIEW.md`; NFR-7 measured: 11/11 endpoints
p95 < 500 ms on the seeded 5,038-report dataset). Deferrals and follow-ups
owned by later phases:

| ID | Item | Target | AC summary |
| -- | ---- | ------ | ---------- |
| B-17.1 | Marker clustering (web + mobile maps) | Phase 18/19 | Cluster at low zoom; 100-marker fetch cap in place meanwhile (ExplorePage map feed, mobile `publicList`) |
| B-17.2 | Micrometer/Prometheus metrics + histogram per endpoint | Phase 19 | Scrape endpoint live; p50/p95 dashboards replace ad-hoc `measure-perf.ps1` runs; `RequestTimingFilter` is the interim evidence - **DONE Phase 19** (`/actuator/prometheus` live with `application` tag, percentiles + histograms, `PrometheusMetricsTest` x2; nginx-allowlisted per owner decision; alert rules read these series) |
| B-17.3 | Redis / distributed cache | Ops scale-out if multi-node | Only when running >1 node; Caffeine TTL sufficient single-node |
| B-17.4 | User dashboard caching | — (rejected by design) | Citizens must see just-created reports immediately; revisit only if `/dashboard/me` measurement regresses |
| B-17.5 | Paginate reference data (`/categories*`, `/organizations` list) | Only if datasets grow | Currently bounded admin-managed rows (tens) — documented exemption in API_DESIGN |
| B-17.6 | Cache high-cardinality queries (`q` search, bbox/map) | Only if measurement regresses | Filtered keys defeat TTL caching; current p95 already within NFR-7 |
| B-17.7 | Larger-N / repeat NFR-7 measurement | Phase 19 | Re-run `scripts/measure-perf.ps1` after schema/dep changes; record deltas in PERFORMANCE_REVIEW - **DONE Phase 19** (re-run 11/11 p95 < 500 ms, worst 152.2 ms vs 397.2 ms in Phase 17, no iteration > 500 ms; deltas + methodology in PERFORMANCE_REVIEW §8) |

### Phase 18 outcomes + deferrals
Exit criterion met 2026-10-22 (runbook: `docs/operations/DEPLOYMENT.md`;
live drills: prod refusal + fresh-DB boot with health UP; suites 155/155
backend (146 H2 + 9 PG) / 68 web / 171 mobile). Deferrals and follow-ups
owned by later phases:

| ID | Item | Target | AC summary |
| -- | ---- | ------ | ---------- |
| B-18.1 | Docker image + compose (owner decision: skip - no container runtime in this environment) | Only if a container platform appears | Image builds reproducibly; compose up honours the DEPLOYMENT.md env contract |
| B-18.2 | Remote host provisioning + first production deploy | When a host exists | Execute DEPLOYMENT.md end-to-end; health UP behind TLS proxy; rate limiter keys on real client IP |

### Phase 19 outcomes + deferrals
Exit criterion met 2026-10-27 (metrics, structured logging, alerts as code,
backup + PITR drills PASSED with evidence, NFR-7 re-run, TROUBLESHOOTING.md;
suites 151 H2 + 9 PG / 68 web / 171 mobile). Deferrals and follow-ups owned
by later phases:

| ID | Item | Target | AC summary |
| -- | ---- | ------ | ---------- |
| B-19.1 | Live Prometheus + Grafana deployment (owner decision Q3: rules as code + docs only, no live instance here) | When a metrics host exists | Load `ops/prometheus/alerting-rules.yml`, scrape job per TROUBLESHOOTING.md, dashboards from `http_server_requests_seconds_*` percentiles |
| B-19.2 | Backup scheduling (scripts are manual today) | With B-18.2 host provisioning | `backup.ps1` on a cron/systemd timer + off-host copy; alert `MtaaFixDiskAlmostFull` watches archive space; retention per `-RetentionDays` |
| B-19.3 | Alert routing (email/chat receivers) | With B-19.1 | Prometheus `alerting` receivers wired to the 9 rules; runbook links resolve to TROUBLESHOOTING.md |

---

## Cross-cutting (scheduled, not MVP-blocking)

| ID | Item | Phase | AC summary |
| -- | ---- | ----- | ---------- |
| B-X.1 | Password reset flow | 13 | Token expiring, non-disclosing responses, email provider integration — **DONE Phase 13** (token flow complete; email send behind `notifications.email.enabled` kill-switch, no SMTP provider yet) |
| B-X.2 | Notification delivery separation | 13 | deliveredAt ≠ createdAt; failure handling — **DONE Phase 13** (IN_APP DELIVERED; EMAIL PENDING→SENT/FAILED/SKIPPED ≤3 attempts; no push rows) |
| B-X.3 | `organization_members` + org scoping UI | 9 | Membership CRUD; matrix O-scopes enforced |
| B-X.4 | iOS project scaffold | 11 | `flutter build ios` succeeds on macOS CI-or-local; until then documented limitation — `ios/Runner` scaffolded Phase 11, build unexecutable on Windows |
| B-X.5 | Mobile token persistence + wiring | 11–12 | Login→session→report flow against real backend — session restore/login/logout done Phase 11; report submission against the API lands with Phase 12 |
