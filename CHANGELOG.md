# Changelog

All notable changes to this project will be documented in this file.

Statuses below reflect what was **verified in this repository** (compile, tests,
analyzer, or file presence) — not intentions.

## [0.12.0] — 2026-10-17
### Phase 11 - Flutter application foundation
Established the reliable mobile architecture per master spec Phase 11 — project
completeness, navigation, mobile theme, shared components, API integration,
authentication state, error handling, Android/iOS configuration, and the initial
test suite. **Approved by owner 2026-10-17.**

**Mobile — core foundation**
- Project completeness: `flutter create --org tz.co.mtaafix` scaffolding —
  Android Gradle (`.kts`) build + `MainActivity`, iOS `Runner/` project,
  `analysis_options.yaml`, project `README.md`
- `ApiClient`: base URL via `--dart-define=MTAAFIX_API_BASE` (default
  `http://10.0.2.2:8080/api/v1`), 15 s timeout, bearer headers, **one**
  401 → refresh-and-retry with rotated-token persistence, typed
  `ApiException` (`statusCode 0` = unreachable server), best-effort logout,
  multipart upload helper for evidence photos
- `TokenStore` abstraction — `flutter_secure_storage` (encrypted at rest) in
  production, `InMemoryTokenStore` injected in tests (no platform channels)
- `AuthController` + `AuthScope`: session restore (`GET /auth/me`), login,
  register→auto-login, logout; unreachable server during restore **keeps**
  tokens (connectivity error surfaced), rejected credentials **clear** them
- Navigation: central named-route table with guards (protected → sign-in,
  signed-in away from auth surfaces, unknown → `NotFoundScreen`), splash
  performs the real session restore before redirecting

**Mobile — screens and shared components**
- Real sign-in/register screens (password rules, confirm field, optional
  mobile), profile screen bound to the session with sign-out confirmation
- Report list/form and notifications migrated onto the MtaaFix theme (no more
  hardcoded `Colors.orange`); shared `LoadingView`, `FeedbackView`,
  `showAppSnack` components; placeholder `auth_page.dart` removed
- Forgot-password surfaces "not available yet" (reset flow → Phase 13);
  report submit/media/map are themed placeholders wired for Phase 12

**Platform configuration**
- Android: dev cleartext HTTP (`usesCleartextTraffic`), `configChanges`
  aligned to the Flutter template (the previous value contained
  `displaySize|displayCutout`, which no platform defines — AAPT resource
  linking failed until removed), NDK 28.2.13676358 installed (the deprecated
  `sdkmanager` shim crashes in this environment; installed via
  `android sdk install ndk/28.2.13676358`)
- iOS: display name "MtaaFix", camera/photo-library/location usage
  descriptions, ATS dev exception (`NSAllowsArbitraryLoads` +
  `NSAllowsLocalNetworking`)

**Verification**
- `flutter analyze` — 0 issues; `flutter test` — **29/29** (API client
  parse/error/network/refresh flows, auth controller lifecycle, route guards,
  boot→sign-in→"My Reports" widget journey, theme, token store)
- `flutter build apk --debug` — `app-debug.apk` built
- iOS build unexecutable on Windows (needs macOS/Xcode — documented
  limitation, backlog B-X.4)

## [0.11.0] — 2026-10-17
### Phase 10 - Field-worker web application
Implemented the field-worker web application per master spec Phase 10 (SRS FR-4.2–4.5,
FR-4.7, Part 10 pages 25–26) — assignment visibility, task details with progress
updates, completion evidence, reviewer return, and worker-authorization hardening.
**Approved by owner 2026-10-17.**

**Web — worker section (P25–P26)**
- `/worker` routes under `RequireAuth roles=['FIELD_WORKER']`: dashboard (New /
  In progress / Completed groups, counts, empty state) and a task details page
- Task details: instructions/category, location + map link, evidence grid with photo
  upload (20 MiB client check), status-driven actions (ASSIGNED → start work,
  IN_PROGRESS → completion claim with the T11 photo hint, RESOLUTION_REVIEW →
  read-only), progress comments, status history
- Account menu gains a "My tasks" entry for FIELD_WORKER; new API client methods
  `getMyAssignments`, `reportComplete`, `reportReturn`
- Admin report details gains the T12 "Return to work" action
  (`returnComment` → `POST /reports/{id}/return`)

**Backend — test hardening (no production-code change)**
- `ReportLifecycleTest.workerOnlySeesAndActsOnTheirOwnAssignments`: worker B gets
  an empty `/assignments/me` and 403 on GET/start/complete/comments; the assignee's
  list is self-scoped (106/106 total)
- `PublicApiTest` flaky-coordinate fix: precise-coordinate assertions now require a
  generalization distance ≥ 100 m (haversine) instead of digit substrings —
  deterministic HMAC fuzzing preserves latitude digits on E/W bearings (root-caused
  ~0.2 %/run flake; failed once with identical code, passed after the fix)

**Verification**
- Web: lint, typecheck, 57/57 tests (+10 Phase 10), production build
- Backend: `mvn verify` 106/106 (H2 + live PG ITs)
- Live smoke PG :8085 — 50/50 worker journey: self-scoped assignment lists (A=1,
  B=0), unassigned worker 403 on GET/start/complete/comments, complete without
  photo → 400 (T11 evidence), multipart `FF D8 FF` upload → 200 + assignee media
  read, progress comment, completion → RESOLUTION_REVIEW / assignment COMPLETED,
  return without comment → 400, return with comment → IN_PROGRESS, re-claim,
  worker verify/resolve/return → 403, resolve → citizen confirm → CLOSED
- Known gap (deferred, same family as the Phase 9 405 note): a POST with a
  non-JSON content type (e.g. `application/x-www-form-urlencoded`) still maps to
  500 via the unhandled `HttpMediaTypeNotSupportedException`; JSON clients sending
  an empty body correctly receive 400 — outside Phase 10 scope, flagged in the
  report

## [0.10.0] — 2026-10-17
### Phase 9 - Administration and moderation
Implemented administration, moderation, and organisation management per master spec
Phase 9 (SRS FR-3.x, FR-4.1/4.6, FR-5.3/5.4, FR-7.3, FR-10.2, FR-12.x, Part 10
pages 14–24) — membership model, user/organisation/category administration, filtered
audit, admin dashboard, analytics, read-only settings, and the 11-page admin web
section. **Approved by owner 2026-10-17.**

**Backend — membership model (owner decision D-P9.1)**
- Flyway `V4__administration_and_organization_members.sql` — new
  `organization_members` table (unique user, single org per user), backfills
  `users.organisation_id`, then drops that column; adds
  `categories.display_order`/`is_active`, `organizations.is_active`, and
  `audit_logs.outcome`
- `OrganisationMembership` domain + repository + `OrganisationMembershipService`
  (org role grant requires membership; role change away from an org role removes the
  membership; `orgIdOfOrNull` drives ORG_ADMIN scoping)

**Backend — administration endpoints**
- `GET /users?q=&role=&active=` (paged search), `PUT /users/{id}/role`,
  `PUT /users/{id}/active` — ADMINISTRATOR only, audited
  (`USER_ROLE_CHANGED`, `USER_ACTIVATED`/`USER_DEACTIVATED`); self role change → 400;
  demoting/deactivating the last active admin → 409
- `GET/POST/PUT /organizations`, `PUT /organizations/{id}/active`,
  `GET/POST /organizations/{id}/members`, `DELETE .../members/{userId}` — admin CRUD +
  membership add (grants org role) / remove (affiliation only, role kept, documented);
  audited (`ORGANIZATION_*`, `MEMBERSHIP_*`); inactive org → 409 on assignment
- Categories: `GET /categories?includeInactive=true`,
  `PUT /categories/{id}/active?value=`, `PUT /categories/{id}/order`;
  public list hides inactive; inactive category on report create → 400; audited
  (`CATEGORY_*`)
- `GET /dashboard/admin` (MOD/ADMIN): byStatus, 14-day activity, byCategory,
  moderation queue, unassigned/assigned workload, attention, recent reports
- `GET /analytics/summary` (MOD/ADMIN/ORG_ADMIN): from/to/categoryId/status/orgId/bbox
  filters — **PostgreSQL-native spatial SQL** (H2 covers validation 400s only); ORG_ADMIN
  without membership → 403; foreign `orgId` forced to own org
- `GET /audit` (ADMIN global) + `GET /audit/organizations/{id}` (ORG_ADMIN own scope)
  gain `action`/`actorId`/`entityType`/`entityId`/`outcome`/`from`/`to` filters with
  `actorEmail` enrichment
- `GET /settings` — ADMIN only, read-only effective configuration (FR-12.3, owner
  decision D-P9.2): reporting rules, security TTLs, privacy/geographic, integrations;
  secrets masked (`fuzzSecretConfigured`/`fuzzSecretIsDefault` booleans, no values)
- `GET /reports` staff list gains `q` (code/title/description) + `assignment`
  (assigned|unassigned) filters (P15); `UserDto` exposes `active`
- **Bugs found & fixed during live smoke:** audit filters with null `Instant`
  params → PG `could not determine data type of parameter $13` (fixed via
  `cast(:from as timestamp)` pattern); unknown paths → 500 (now 404
  `RESOURCE_NOT_FOUND` via `NoResourceFoundException` handler); malformed JSON bodies
  → 500 (now 400 `MALFORMED_REQUEST`); path-variable type mismatch already → 400
  `INVALID_PARAMETER`

**Web (`web/`) — admin section (pages 14–24)**
- Nested role-guarded routes under `/admin` (`RequireAuth roles=…`; citizen → `/app`,
  unauthenticated → `/login`); `Admin` entry in the account menu for staff roles
- New `pages/admin/`: dashboard (P14), report management + full report details with
  lifecycle actions (P15/P16), review queue with reject-reason/duplicate dialogs
  (P17), assignment board (P18), user management with role/active controls (P19),
  categories incl. deactivate/reorder (P20), organisations + members (P21), filtered
  audit log (P22), analytics with date/category/status/org/geographic filters (P23),
  read-only settings (P24); shared `AdminPageShell` + role-filtered `adminNav`
- `api.ts` gains 30+ admin methods; admin DTOs added to `types.ts`

**Tests / verification**
- Backend `mvn verify` (H2 + live PostGIS): **105/105 green** (new
  `AdministrationApiTest` 13; PG IT +2: V4 migration shape incl. dropped
  `users.organisation_id`, analytics summary executes on PostgreSQL)
- Web: lint/typecheck/build green, **47/47 tests** (new `Phase9.test.tsx` 11: route
  guards, staff menu link, user role/active flows, category deactivate, audit
  filters, settings read-only/no-leak, staff list; test timeouts raised for
  parallel-load flakiness)
- Live smoke PG :8085: **68/68 green** — registration→SQL promotion→login; settings
  masked; dashboard+analytics on PG; citizen 403s (users/settings/audit/dashboard/
  analytics) + anonymous 401 + unknown path 404; role change audited with
  actorEmail/outcome + filters + bad-timestamp 400; category deactivate→hidden→
  create-400→reactivate; org create + member add granting FIELD_WORKER + org-scoped
  audit; full lifecycle create(SUBMITTED)→claim→verify→assign (worker sees it via
  `/assignments/me`), needs-info→resubmit, reject; inactive-org assign → 409 with org
  guard message

**Known follow-ups:** settings are read-only by design (mutations env-driven,
post-MVP editor); `MODERATOR` assign stays **N** (matrix §2.3 revisited, kept — owner
decision D-P9.3); GET-on-nonexistent-method 405 gap still returns 500 (405 outside
allowed status list, pre-existing decision).

## [0.9.0] — 2026-10-15
### Phase 8 - Geographic capabilities
Implemented geographic functionality per master spec Phase 8 (SRS FR-11.x, Part 6.4)
— coordinate validation, PostGIS spatial queries, map interactions, geographic filters,
location privacy, indexes, and tests. **Approved 2026-10-15.**

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
