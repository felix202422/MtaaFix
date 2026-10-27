# Changelog

All notable changes to this project will be documented in this file.

Statuses below reflect what was **verified in this repository** (compile, tests,
analyzer, or file presence) — not intentions.

## [0.20.0] — 2026-10-27
### Phase 19 - Observability and Operations
Scope per owner decisions 2026-10-23: Q1 metrics = same port + nginx
allowlist for `/actuator/prometheus`; Q2 depth = full PITR + dump drill on
a scratch PG 18 cluster; Q3 alerts = rules as code + docs (no live
Prometheus/Grafana here). **Approved by owner 2026-10-27.**

**Backend - metrics (B-17.2)**
- `micrometer-registry-prometheus`; `management.endpoints.web.exposure:
  health,prometheus` (default + prod), `application` tag on every series,
  percentiles [0.5/0.95/0.99] + percentile histograms for
  `http.server.requests`; `SecurityConfig` permits `/actuator/prometheus`
- `PrometheusMetricsTest` x2: unauthenticated scrape 200 with
  `application="mtaafix"` + histogram buckets, 404s recorded under a bounded
  uri tag (raw path never leaks), `/actuator/env` still 401
- Live verification on PG 18 + PostGIS: scrape returns
  `http_server_requests_seconds_*`, `hikaricp_connections_*` series

**Backend - structured logging (request correlation)**
- `RequestTimingFilter` assigns `X-Request-Id`: inbound header honoured when
  it matches `[A-Za-z0-9._-]{1,64}` (else UUID), placed in the SLF4J MDC as
  `requestId`, echoed on the response, cleared after the request; prod logs
  are structured JSON (`logging.structured.format.console: logstash`) so
  `requestId` becomes a JSON key; dev console pattern carries `[requestId]`
- `RequestTimingFilterTest` x3 (generate / honour / reject injection); live
  WARN lines verified carrying the correlation id

**Ops - alerts as code + runbook**
- `ops/prometheus/alerting-rules.yml`: 9 rules (target down, scrape missing,
  5xx ratio > 5%, p95 > 1 s, Hikari saturation/queueing, heap > 90%,
  CPU > 85%, disk < 10%) with severities and runbook pointers; metric names
  verified against a live scrape
- `docs/operations/TROUBLESHOOTING.md` (new): alert triage table, log
  correlation via `X-Request-Id`, scrape security/nginx allowlist, slow
  request budgets, DB/backup/PITR runbook, quick reference

**Backup + PITR (B-16.3)**
- `backend/scripts/backup.ps1`: `pg_dump -Fc` + `uploads/` + SHA-256
  manifest + age retention (default 14 days); `PGPASSWORD` from env only
- `backend/scripts/restore-database.ps1`: restores into a scratch DB by
  default, refuses the live `mtaafix` without `-Force`
- **Dump/restore drill PASSED**: backup -> restore into `mtaafix_restore`
  (5,038 reports + PostGIS landed)
- **PITR drill PASSED** (`backend/scripts/pitr-drill.ps1`, scratch cluster on
  ports 55432/55433): WAL archiving -> `pg_basebackup -X stream` -> recovery
  with `restore_command` + `recovery_target_time` + promote -> markers A+B
  recovered, C absent; evidence incl. `SHOW archive_command` round-trip and
  per-segment archive/restore logs in `pitr-drill-evidence.txt`

**NFR-7 re-run (B-17.7)**
- `measure-perf.ps1` on the seeded 5,038-report dataset with the Phase 19
  build: **11/11 endpoints p95 < 500 ms, worst p95 152.2 ms** (vs 397.2 ms
  in Phase 17), no iteration above 500 ms; deltas + methodology in
  `docs/standards/PERFORMANCE_REVIEW.md` §8; results in
  `backend/scripts/perf-results.json`

**Docs**
- ROADMAP Phase 19 section + version history + delivery rows; BACKLOG
  B-17.2/B-16.3/B-17.7 closed + Phase 19 outcomes; BOARD Phase 19 table +
  delivery summary; DEPLOYMENT metrics allowlist, backup/PITR and alerting
  sections; DEVELOPMENT observability notes; `.env.example` Phase 19 block;
  `.gitignore` for `backend/scripts/backups/`

**Suites**
- backend `mvn test` **151/151 H2** (160 run, 9 PG auto-skipped) incl.
  `PrometheusMetricsTest` x2 + `RequestTimingFilterTest` x3; PG IT
  `MTAAFIX_PG_IT=true` **9/9**; web `tsc --noEmit` 0 + vitest **68/68**;
  mobile analyze 0 + **171/171**
## [0.19.0] — 2026-10-22
### Phase 18 - Deployment and CI/CD
Deployment readiness per owner-approved scope Q1–Q4 (2026-10-22): prod
profile, runbook + local production-profile drill, hardened CI, release
pipeline. Deployment target remains a plain systemd/JAR host (readiness
only; no remote server provisioned).
**Approved by owner 2026-10-22.**

**Backend**
- `application-prod.yml`: `server.forward-headers-strategy: framework`
  (X-Forwarded-* honoured - B-16.4), `ddl-auto: validate` explicit,
  actuator exposes **health only** with `show-details: never`
- `ForwardedIpRateLimitTest`: proxy `X-Forwarded-For` rewrite keyed at
  `HIGHEST_PRECEDENCE` before the security chain - client A exhausts
  2/min on forgot-password (429 + Retry-After + `RATE_LIMITED`), client
  B behind the same socket passes (B-16.4 discriminator)
- `ProdSecretsGuardTest` x6: shipped defaults (incl. rotated), blanks,
  short JWT, prod-only `@Profile` scope, real values accepted

**CI/CD (.github/workflows)**
- `ci.yml` rewritten - four parallel jobs: **backend** full `mvn test`
  (H2 suite + PG IT `MTAAFIX_PG_IT=true` against `postgis/postgis:18-3.6`
  service, `JWT_SECRET` >=32 for the default-profile context), **web**
  (lint + vitest + tsc/vite build), **mobile** (`flutter analyze` +
  `flutter test`), **secrets** (gitleaks over full history); concurrency
  cancellation + workflow_dispatch
- `release.yml` new - on tag `v*`: package jar (tests rerun), build
  `web/dist`, attach executable jar + `web-dist.zip` to the GitHub Release

**Docs**
- `docs/operations/DEPLOYMENT.md` runbook (env/secrets, build, prod boot,
  nginx forwarded-header example, CI/release, rollback, scope notes;
  Docker skipped per owner - no container runtime in this environment)
- `.env.example`: corrected `RATE_LIMIT_AUTH_PER_MINUTE` /
  `RATE_LIMIT_REPORT_PER_MINUTE` names (documented vars did not match
  `application.yml`), added `GEO_FUZZ_SECRET` + `TIMING_WARN_THRESHOLD_MS`
- DEVELOPMENT.md CI section matches the new pipeline

**Live drill (local, evidence)**
- Refusal: `prod` + shipped JWT default -> boot fails with
  `JWT secret must be at least 32 characters`; with real JWT/DB but
  default `GEO_FUZZ_SECRET` -> `ProdSecretsGuard: Refusing to start with
  the prod profile: geo.fuzz-secret still holds a shipped development
  default` (exit 1)
- Boot: `prod` + real secrets vs fresh scratch DB -> Flyway **V1-V8**
  applied, `validate` passes, `/actuator/health` = **UP** in ~10 s,
  `GET /api/v1/reports/public?size=5` 200; scratch DB dropped after
- Suites: backend **155/155** (146 H2 + 9 PG IT) / web lint +
  **68/68** + production build / mobile analyze + **171/171**
- B-16.4 closed; D11 closed (suites wired into CI); Docker image skipped
  per owner decision; backups remain Phase 19; commits only - owner
  pushes to trigger CI
## [0.18.0] — 2026-10-22
### Phase 17 - Performance and reliability
N+1 elimination, index review, pagination of user-growable lists, justified
caching, and first NFR-7 measurement (report:
`docs/standards/PERFORMANCE_REVIEW.md`), per owner-approved scope Q1–Q4
(2026-10-22). Live measurement against a seeded 5,038-report dataset:
**11/11 endpoints p95 < 500 ms → NFR-7 MET** (local hardware; caveat and
methodology in the review).
**Approved by owner 2026-10-22.**

**Backend**
- N+1 fix: `ReportDto.listItem` lean factory (list rows carry batch-fetched
  media; empty statusHistory/comments — detail endpoint unchanged);
  `findByScope` fetch-joins assignment (assignee/org); media batch query
  `findMediaByReportIds`
- Pagination (size ≤ 100): `GET /assignments/me` → `PagedResponse` (scalar
  DTO, fetch-joined repos, explicit count queries); `GET
  /organizations/{id}/members` → `PagedResponse` (batch user load);
  `GET /notifications` size clamped to 100 (Page shape kept)
- Flyway **V8**: composite btree `(status|organisation|category|user,
  reported_at DESC)` on `reports` + `pg_trgm` GIN on
  `lower(code/title/description)` (staff `q` filter); PG IT asserts all four
  + trigram indexes
- Caching (`spring-boot-starter-cache` + Caffeine, `expireAfterWrite=60s`):
  `categories` (evicted on create/update/activate/reorder), `publicStats`,
  `adminDashboard`; user dashboard deliberately uncached (freshness);
  test profile `spring.cache.type: simple`
- `RequestTimingFilter`: per-request DEBUG timing, WARN > `timing.warn-threshold-ms`
  (default 500); `X-Response-Time-Ms` best-effort
- `buildActivity14d`: SQL `GROUP BY (reported_at AT TIME ZONE
  'Africa/Dar_es_Salaam')::date` on PostgreSQL; Java bucketing kept for H2
- `scripts/seed-perf.sql` (5,000 reports, idempotent) +
  `scripts/measure-perf.ps1` (5 warmup + 30 iterations/endpoint, p50/p95/max,
  `perf-results.json`)
- Suites: `mvn test` **148/148** H2 (+CachingTest 3, pagination assertions
  updated); PG IT **9/9** (V8 assertions)

**Web**
- ExplorePage: map view fetches its own feed (`size=100`, own loading/error
  states, marker-count note) — list stays paginated at 10
- `api.getMyAssignments`/`listOrganisationMembers` unwrap `PagedResponse.content`
  (fetch `size=100`); Phase10 mocks return paged shape
- `tsc --noEmit` 0, `vitest run` **68/68**

**Mobile**
- `publicList` default `size` 50 → 100 (map markers)
- `flutter test` **171/171**

**NFR-7 measurement** (local PG 18 + PostGIS, 5,038 reports, loopback HTTP)
- 11/11 endpoints pass p95 < 500 ms; worst p95 = 397.2 ms (public bbox
  size=100); cached stats p95 63.5 ms; staff list p95 127.4 ms
- Server-side: 3 slow-request WARNs, all one-time startup/JIT warmup

**Docs**
- `docs/standards/PERFORMANCE_REVIEW.md` (findings, changes, methodology,
  results table, index EXPLAIN evidence, deferrals); API_DESIGN rate-limit row
  corrected (shipped Phase 16) + pagination exemptions + list-row note;
  ROADMAP/BACKLOG/BOARD synced

**Deferred (owner-endorsed)**: marker clustering (Phase 18/19), Micrometer/
Prometheus (Phase 19), Redis, user-dashboard caching, reference-data
pagination (bounded exemption), search/bbox caching (high key cardinality).

## [0.17.0] — 2026-10-22
### Phase 16 - Security review and hardening
Structured security review across the ten master-spec areas (review record:
`docs/standards/SECURITY_REVIEW.md`) with remediation of the flagged
Critical/High findings plus two cheap Mediums, per owner-approved scope
(2026-10-22): 415/405 error contracts (D1/D2), JWT revocation via
`token_version` (D3), server-side media status gate (D4), per-IP rate
limiting on auth + report submission (SECURITY_STANDARDS gap), prod-profile
secret fail-fast, health/mail indicator fix, dependency review, logging/privacy
audit, and a backup/restore runbook. Live battery **23/23** against the
rebuilt jar (fresh build - the stale Oct-20 jar had been serving the old
500s); offline-storage encryption deferred to Phase 20 by owner decision.
**Approved by owner 2026-10-22.**

**Backend**
- Flyway **V7** `users.token_version`; `tv` claim on access+refresh JWTs;
  filter + refresh reject stale versions; password reset **and** change bump
  the version; `/auth/password/change` returns a rotated `AuthResponse`
- `RateLimitFilter`: per-IP fixed one-minute windows on `POST /api/v1/auth/**`
  (30/min) and `POST /api/v1/reports` (20/min); 429 + `Retry-After` +
  ApiError `RATE_LIMITED`; configurable via `RATE_LIMIT_*`; disabled under
  the test profile
- `GlobalExceptionHandler`: explicit 415 (`UNSUPPORTED_MEDIA_TYPE`) and 405
  (`METHOD_NOT_ALLOWED`) ApiError handlers (D1/D2)
- `ReportService.assertMediaEditable`: attach/delete rejected with 409 once
  the report is RESOLVED/REJECTED/DUPLICATE/CLOSED (server-side mirror of
  the mobile gate)
- `ProdSecretsGuard` (`prod` profile): startup refuses shipped JWT/DB/geo
  secret defaults, blanks, and <32-char jwt/geo secrets
- `management.health.mail.enabled` mirrors `NOTIFICATIONS_EMAIL_ENABLED` so
  health stays UP while SMTP is intentionally unconfigured
- Suites: `mvn test` **144/144** H2 (+11 new tests: rate limit 4, error
  handling 4, token provider 2, media gate 1); PG IT **8/8**
  (`MTAAFIX_PG_IT=true`, V7 applied)

**Web**
- `api.changePassword` now returns `AuthResponse`; `ProfilePage` persists the
  rotated pair via `setAuthTokens` (pre-change tokens are revoked server-side)
- `tsc --noEmit` 0, `vitest run` **68/68** (rotation assertion added)

**Mobile**
- `auth_controller.changePassword` adopts rotated tokens from the response
  (`setSession`); new unit test asserts persistence
- `flutter test` **171/171**

**Live battery** (`backend/target/battery-p16.ps1`, rebuilt jar on :8085)
- **23/23**: health UP, auth + report-create regression, swagger admin-only,
  401 anonymous, 415/405/400 ApiError contracts, D4 gate (upload -> cancel ->
  upload 409), D3 revocation E2E (old access/refresh 401, new pair accepted),
  D6 rate limit (429 at the 31st forgot-password attempt + `Retry-After`)

**Docs**
- `docs/standards/SECURITY_REVIEW.md` (10 areas, findings F1-F12, evidence,
  deferrals, backup runbook); SECURITY.md checklist updated; ROADMAP/BACKLOG/
  BOARD synced; `.env.example` annotated with prod-required overrides and
  `RATE_LIMIT_*` variables

## [0.16.0] — 2026-10-21
### Phase 15 - Quality assurance
Full-suite validation per master spec Phase 15 (lines 2069-2089): backend,
web, Flutter, and PostgreSQL integration suites run green from clean
invocations; critical journey, role boundaries, and lifecycle transitions
exercised live against the backend on port 8085 (63/63 battery checks);
responsive layouts and accessibility verified for the mobile app (320x640
traversal + semantics assertions) and audited for web (jsx-a11y clean);
unresolved defects reviewed and documented with a mitigation plan in
BACKLOG ("Phase 15 known defects + mitigation plan"). The new small-screen
test found and drove fixes for **five RenderFlex overflow defects**. Mobile
test-infra defects surfaced by the journey test were fixed (map disk-cache
worker isolate disabled under `FLUTTER_TEST` via `appTileProvider()`; lazy
map mount on first tab activation). **Approved by owner 2026-10-21.**

**Backend**
- No source changes; suites re-run for the phase record: `mvn test` -
  **133/133** H2 (PG class auto-skipped without env), `mvn test
  -Dtest=PostgresIntegrationTest` with `MTAAFIX_PG_IT=true` + local
  PostgreSQL - **8/8** (Flyway V6 verified, PostGIS extension, spatial
  queries, idempotency)
- `docs/api/openapi.json` regenerated artifact synced (Analytics tag
  description ASCII hyphen matches the source `@Tag`)

**Web**
- No source changes; verification + audit only: `eslint .` 0,
  `tsc --noEmit` 0, `vitest run` **68/68** (10 files), `vite build` green
- Responsive/a11y audit: single structural breakpoint (`UserMenu`
  `hidden sm:inline`), header nav wraps at narrow widths (no hamburger;
  documented limitation), jsx-a11y rules pass, ARIA labels/roles covered
  by tests; jsdom cannot assert CSS-driven visibility - verification is
  lint + semantics tests + documented audit

**Mobile** (Flutter)
- **Responsive fixes (found by the new 320x640 test, all fixed):**
  `login_screen` auth actions `Row` -> `Wrap`; `home_screen`
  `_SectionHeader` title -> `Expanded` + ellipsis;
  `report_form_screen` category dropdown `isExpanded: true`; photo attach
  buttons `Row` -> `Wrap`
- **Map:** `MapScreen` mounts `FlutterMap`/`TileLayer` only after first tab
  activation (`_mapBooted`, static placeholder before); new
  `core/map/app_tile_provider.dart` - `appTileProvider()` returns
  `NetworkTileProvider` with `DisabledMapCachingProvider` when
  `FLUTTER_TEST` is set (the disk-cache worker isolate stalled the
  fake-async test runner) and keeps the production disk cache otherwise;
  wired into `MapScreen`, `ReportDetailScreen`, `MapPickerScreen`
- **Tests:** new `test/phase15_quality_test.dart` (+3): citizen critical
  journey (sign in -> create `MTF-2026-000042` -> detail -> back -> refresh
  -> My Reports), critical-screens no-overflow sweep at 320x640, accessible
  semantics labels (login fields, NavigationBar destinations, form field
  labels, refresh tooltip); `flutter analyze` 0 issues, `flutter test`
  **170/170**

**Live battery** (:8085, PostgreSQL) - **63/63**
- Fresh accounts registered via API; admin promoted via SQL seed, moderator
  promoted via the admin `PUT /users/{id}/role` endpoint; org-role grant
  without membership -> 400 (FR-5.4 rule verified live)
- Critical journey x2 chains: create (SUBMITTED) -> claim/verify -> assign
  (photo uploaded) -> start -> complete -> resolve -> close -> reopen, then
  a second pass ending with citizen **confirm** - every status transition
  observed in the response
- Role boundaries: citizen/mod/worker -> 403 on admin+moderation endpoints,
  anonymous -> 401, worker `GET /assignments/me` -> 200, admin
  `GET /users` -> 200; state-gate 409 (mod /resolve while SUBMITTED),
  ownership guard 403 (worker /start unassigned)
- Known-defect probes reproduced live: non-JSON POST -> 500, GET on
  POST-only endpoint -> 500 (see mitigation plan)

**Docs**: this entry; ROADMAP Phase 15 + version history; BACKLOG B-15.1..6
+ defects mitigation plan; BOARD Phase 15 table.

## [0.15.0] — 2026-10-21
### Phase 14 - Offline capabilities
Implemented master spec Phase 14 under the owner-approved scope: Option B
backend idempotency, report-form draft storage, a pending-submission queue
with foreground retry, a categories cache for offline form loads, and
local-state cleanup on sign-out. Offline browsing/map tiles and background
sync are deferred (approved). Exit criterion enforced end to end: **unsent
data is never presented as submitted.** Live battery run against the backend
on port 8085 before this entry was written. **Approved by owner 2026-10-21.**

**Backend**
- Flyway **V6**: `report_idem_keys` (key → `report_id` + `payload_hash`,
  24 h TTL, hourly purge)
- `Idempotency-Key` on `POST /api/v1/reports`: format
  `^[A-Za-z0-9_-]{16,64}$`, anything else → **400 BAD_REQUEST**; first use
  creates the report in a **REQUIRES_NEW** transaction; same key +
  byte-identical payload replays the stored **201 (same report id, no
  duplicate)**; same key + different payload → **409
  `IDEMPOTENCY_CONFLICT`**; concurrent first use → **409
  `IDEMPOTENCY_IN_PROGRESS`** (stale intent after `STALE_INTENT_MINUTES=2`)
- `IdempotencyService` with a private `ObjectMapper` + `JavaTimeModule`
  (`hashOf` = SHA-256 over the canonical payload), conflict exception +
  handler; header optional — legacy clients unaffected
- Tests: `IdempotencyTest` (8) → H2 **133/133**; `PostgresIntegrationTest`
  **8/8** on PostgreSQL with V6; jar packaged; `docs/api/openapi.json`
  regenerated
- Live battery (:8085, PG): new key → `201` `MTF-2026-000035`; same key +
  same body → `201` **same id**; same key + different body → `409
  IDEMPOTENCY_CONFLICT`; key `short` → `400`

**Mobile** (Flutter)
- `features/offline/`: `OfflineController` (single-flight `flush()`,
  honesty-first queue states), atomic **`Zone.root`-wrapped**
  `OfflineStorage` (`schema: 1` everywhere, tmp+rename, graceful
  degradation), `DraftStore` / `OutboxStore` / `CategoryCache`,
  `ConnectivityService`, `OfflineScope`, `PendingSubmissionsScreen`
- Report form: debounced (400 ms) draft autosave + Restore/Discard offer
  (drafts with an empty `categoryId` are restorable), offline submit →
  queue, failed-entry Edit → prefill + re-queue (old entry replaced, photo
  files kept via `keepPaths`), photos copied into offline storage at pick,
  **one idempotency key per form session** reused for retries, category
  cache fallback with stale hint
- Shell/list/profile: offline strip with just-submitted reload banner,
  `clearAll()` on sign-out, guarded `/pending-submissions` route
- `ApiClient.post(path, body, idempotencyKey)` → `Idempotency-Key` header
  carried across 401→refresh retries; new deps `path_provider`,
  `connectivity_plus`
- Tests: **167/167 green** (+33: offline controller/storage, pending
  screen, form offline flows, offline banner), `flutter analyze` → 0
  issues; tests drove two product fixes: post-`dispose()` notify guard in
  `OfflineController` and empty-`categoryId` draft rejection

**Docs**
- `docs/design/OFFLINE_CAPABILITIES.md` (new): honesty invariant,
  architecture, data model, queue lifecycle, idempotency semantics,
  evidence, deferrals
- API_DESIGN `POST /reports` idempotency row; SRS FR-2.11; ROADMAP /
  BACKLOG / BOARD Phase 14 sections

## [0.14.0] — 2026-10-18
### Phase 13 - Notifications and media
Implemented master spec Phase 13: notification preferences and unread counts,
delivery-state separation (B-X.2), media validation/attachment lifecycle
hardening, and the B-X.1 password-reset flow — across backend, web, and
mobile. All flows exercised against the live backend on port 8085 before
this entry was written. **Approved by owner 2026-10-18.**

**Backend**
- Flyway **V5**: `notification_preferences` (per-user email/push opt-in),
  `notification_deliveries` (channel/status/attempt/detail), and
  `password_reset_tokens` (SHA-256 hash, single-use, 30-minute expiry)
- `GET /notifications/unread-count` and `GET/PUT /notifications/preferences`
  (`emailEnabled`, `pushEnabled`, `pushAvailable`); preference rows created
  lazily with defaults
- `POST /auth/password/forgot|reset|change`: forgot/reset always answer the
  same **202** (non-disclosing); change requires an authenticated session
  and preserves it (also on failure); reset marks the token used atomically
  and rejects reuse with a generic 400
- `NotificationDeliveryService` + `EmailRetryScheduler` (B-X.2): every
  notification records IN_APP **DELIVERED**; EMAIL is created PENDING,
  attempted immediately, and retried on a schedule while under
  `notifications.email.max-attempts` (3), terminating SENT/FAILED/SKIPPED;
  **push has no provider** — `pushAvailable=false`, no push rows
- Email content mirrors the in-app title/body plus report link, sent through
  `common/email` (Spring Mail) behind the **`notifications.email.enabled`
  kill-switch, default false** (no SMTP provider configured) — owner action
  to enable
- Media hardening (FR-9.2): MIME allowlist + magic-byte content checks
  (pre-existing) now joined by config-driven `media.max-file-size-bytes`
  (default 20 MiB) and a new `media.max-per-report` (10) cap enforced on
  both the metadata and multipart paths, plus metadata URL format
  validation; authorized `GET /media/{id}/content`; multipart oversize →
  413; **public unauthenticated media deliberately NOT implemented**
  (owner decision — do not open unilaterally)
- Delivery wiring at the single `NotificationService.notifyUser` funnel:
  every lifecycle event (submit, cancel, assign, status, dispute,
  needs-info) records deliveries via `dispatch()` with no per-call-site
  rewiring, and dispatch failure never fails the triggering workflow; two
  event fixes — dispute now also notifies the report owner, cancel emits
  `REPORT_CANCELLED`; `markAllAsRead` reduced to a single UPDATE; settings
  surfaced via `SettingsService`; `.env.example`/`application.yml`
  document the new keys
- Tests: `NotificationPhase13Test` (7), `PasswordResetTest` (7),
  `MediaLimitsTest` (4), lifecycle media-url updates — H2 **124/124**,
  PostgreSQL IT **7/7** with V5 applied; OpenAPI regenerated

**Web**
- `NotificationBell` in the header: unread badge fed by
  `GET /notifications/unread-count`, refreshed on sign-in/route changes
  (counts >99 clamped; hidden for anonymous visitors)
- Forgot/reset password: rewritten `ForgotPasswordPage` (always-same
  "Check your inbox" confirmation with non-disclosing copy) and new
  `ResetPasswordPage` (`/reset-password`, token prefilled from `?token=`,
  ≥8-char + match validation, success state)
- Profile: notification-preferences card (auto-save email toggle, in-app
  checked and locked, push disabled with "No push provider is configured…")
  and change-password card that preserves auth state; `Change password`
  API methods (`forgotPassword`, `resetPassword`, `changePassword`)
- Notifications pagination controls when `totalPages > 1`; admin/worker
  report media now opened via authenticated blob fetch (no long-lived
  signed URLs); report wizard supports **5 photos** with per-file
  validation and a partial-failure toast; tracking/worker photo inputs
  enforce type/size/10-photo limits client-side
- Tests: new `Phase13.test.tsx` (11) — suite **68/68**; `npm run lint`,
  `tsc --noEmit`, `npm run build` all green

**Mobile**
- Unread badge: `UnreadCountController`/`UnreadCountScope` above
  `MaterialApp`; `MainShellScreen` refreshes once on mount, the feed
  refreshes after load/mark-open/mark-all (nullable lookups keep minimal
  test harnesses working)
- `NotificationPreferencesScreen` (route `/notification-preferences`,
  guarded): in-app always-on, email switch with PUT + confirmation snackbar,
  push read-only until a provider exists; profile rows navigate to it and
  to change-password
- Password recovery: `ForgotPasswordScreen`, `ResetPasswordScreen` (reads
  `token` route args), `ChangePasswordScreen`; login "Forgot password?"
  wired; auth controller methods emit error states without dropping the
  session (change) / navigate to sign-in on success (reset)
- Report detail evidence: add photo via `image_picker` (MIME, 20 MB,
  10-photo checks), remove with confirmation dialog →
  `DELETE /media/{id}` then reload; `canEditMedia` hides editing for
  resolved/rejected/duplicate/closed reports (mobile-side gate — backend
  attach/delete has no status guard)
- Defect found & fixed by the new tests: `ModalRoute.of` was called in
  `ResetPasswordScreen.initState` (framework assertion whenever the route
  pushed) — moved token argument reading into `didChangeDependencies`
- Tests: `flutter analyze` 0 issues; `flutter test` **134/134** (+23:
  password recovery 5, preferences screen 3, profile rows 2, router 4,
  shell badge, notifications API 4, media delete, evidence 3)

**Live verification (port 8085, PostgreSQL)**
- Forgot: identical 202 for known/unknown email; token row with 30-minute
  expiry; reset E2E — bogus token 400 (validation / "Invalid or expired
  reset token"), planted token 200, reuse 400, old password 401 after
  reset, new password 200, change-password 200 and restored login
- Preferences GET/PUT persisted; unread count reflects new notifications
  (submit + cancel events observed in the feed)
- Media: real PNG 200; `.txt` 400 (MIME); fake JPEG 400 (magic bytes);
  21 MB 400; 11th attachment 400 (cap); `DELETE /media/{id}` 204 (count
  10→9); delivery rows IN_APP DELIVERED and EMAIL SKIPPED
  ("Email channel disabled") while the kill-switch is off

**Decisions & gaps flagged for the report:** email mirrors in-app content
behind the kill-switch (default off); push not configured; JWTs not revoked
after password reset; public media endpoint absent (owner decision); backend
media attach/delete lacks a status guard (active-status gate is UI-side);
FR-2.4 date filter still backend-unsupported; iOS build macOS-only (B-X.4).

## [0.13.0] — 2026-10-18
### Phase 12 - Flutter citizen features
Implemented the Phase 12 citizen experience (Pages 27–36) per master spec
Phase 12 — onboarding, home dashboard, report creation with photos and
location, report list/detail with lifecycle actions, community map,
notifications and profile — verified end-to-end against the live backend on
an Android emulator. **Approved by owner 2026-10-18.**

**Mobile — shared layer**
- `paged.dart` (generic paged-response helper), `formatters.dart`
  (date/date-time formatting), `status_theme.dart` (status meta, labels,
  colour map, `StatusChip`), `report.dart` domain (statuses, lifecycle
  helpers, location, timeline, comments), `notification_item.dart`
- `ReportsApi` (dashboard stats, CRUD, filters/pagination, lifecycle
  actions, comments, multipart media upload, public bbox feed, category
  list, `resolveMediaUrl`) and `NotificationsApi` (list, mark-read,
  mark-all-read)

**Mobile — screens (Pages 27–36)**
- Onboarding (3 slides, persisted `onboarding.seen` flag) → login/home
  routing; splash now honours the onboarding flag before session restore
  redirect; login/register land on the home shell after authentication
- Home shell (`MainShellScreen`): IndexedStack + NavigationBar (Home,
  My Reports, Map, Alerts, Profile); **tabs refresh their data whenever
  they are re-selected** (first-mount-only data was showing stale empty
  states after submitting a report — found in emulator verification)
- Home: welcome hero, dashboard stats (flat `/dashboard/me` counters mapped
  to status tiles), recent reports, recent notifications, primary report CTA
- Report list: status/category/search filters, pagination + load-more, FAB
- Report creation/edit: category dropdown (public API), title/description
  validation, geolocator "Current location" with permission-denial handling,
  map-picker location, up to 5 photos via `image_picker` (camera/gallery,
  MIME + 20 MB/5-photo checks), live review card, backend error surfacing
- Report detail: summary, static map snapshot, evidence, status timeline,
  comments, and status-gated actions (cancel/edit for SUBMITTED,
  confirm/dispute for RESOLVED, resubmit for NEEDS_INFORMATION)
- Community map: flutter_map + OSM tiles, status/category filters, viewport
  bbox fetch (debounced camera moves), my-location, marker bottom-sheet,
  reports-on-screen count
- Notifications: real feed with unread styling, mark-read/mark-all-read,
  deep link into report details
- Profile: session details, edit-profile dialog (`PUT /auth/me`), privacy
  card, informational notification-preferences row, about, sign-out

**Defects found and fixed during verification**
- `uses-material-design: true` was missing from `pubspec.yaml` — every
  Material icon rendered as tofu on device (tests don't catch glyph
  rendering); added, APK rebuilt, icons verified on emulator
- `GET /categories/public` returns a bare JSON array — `ApiClient` now
  wraps top-level arrays (`_tryDecodeList`), fixing the category dropdown
  (and any future bare-array endpoint) in production, not just tests
- First map fetch ran before the camera existed, silently dropping the
  viewport `bbox`; deferred to the first post-frame callback
- `ReportsApi.dashboardStats` expected a nested `stats` map that
  `/dashboard/me` does not send; now maps the flat citizen counters
  (`submittedReports`, `verifiedReports`, …) to status tiles
- Shell tabs kept first-mount data forever (IndexedStack): Home/Reports/Map/
  Alerts now reload via a `MainShellTab` activation notifier

**Verification**
- `flutter analyze` — 0 issues; `flutter test` — **111/111**
  (15 widget/unit test files: router guards, shell, paged helper, APIs,
  onboarding, home, list, form, detail, map, notifications, profile)
- `flutter build apk --debug` — built (with
  `--dart-define=MTAAFIX_API_BASE=http://10.0.2.2:8085/api/v1` for the
  live-backend run; host port 8080 was occupied by an unrelated service)
- Emulator run (Medium_Phone_API_36.1, live PostgreSQL backend): register →
  sign-in → home → create report (category + title + description + picked
  pin −1.25697, 36.81722) → detail `MTF-2026-000032` (SUBMITTED) → list
  card, community map marker after refresh, notifications "Report
  submitted", profile; screenshots captured for the Phase 12 report
- Known gaps flagged: FR-2.4 report date filter has no backend parameter
  (status/category/q only); no change-password endpoint (Phase 13);
  iOS build requires macOS (B-X.4)


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
