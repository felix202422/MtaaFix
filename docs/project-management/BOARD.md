# MtaaFix — Local Task Board

Local tracking board (`gh` CLI still unauthenticated; run status read via the unauthenticated GitHub REST API).
Status values: `TODO` / `IN PROGRESS` / `DONE` / `BLOCKED`.
Phase numbering: master spec Phases 0–23 (decision D1). Sections follow
phase number ascending, matching ROADMAP.md.
Last updated: 2026-10-23.

## Current phase: Phase 19 IN PROGRESS (started 2026-10-23) — Phase 18 approved 2026-10-22

Phases 0–18 delivered and approved (all pushed to `origin/master`).

**CI (GitHub Actions): run #36 (2026-10-23, `78a90ee`) — all four green ✅**
backend (H2 + PostGIS service IT) · web (lint + tests + build) · mobile
(flutter analyze + 171/171) · gitleaks. History: first 4-job run #31
(`a76c470`) failed mobile 170/171; runs #31–35 each failed a *different*
single test → flaky CI-scheduling races, not product bugs. Diagnosed
without auth via public check-runs annotations (JSON-reporter parser in
`ci.yml`; `flutter-version` pinned to 3.47.7 — version drift ruled out).
Root causes fixed in `78a90ee`: (1) the queue-persistence test restarted
while *online*, so the fresh controller's fire-and-forget auto-flush raced
its own assertions — CI's stretched event loop let the drain win; the test
now restarts offline. (2) `settleReal`'s fixed 320 ms real budget couldn't
absorb CI I/O latency at the three submit sites — it now supports an
optional condition wait bounded at 1 s (defaults unchanged). Local after
fix: analyze clean, mobile 171/171 ×2.

## Phase 0 — Project audit & foundation (COMPLETE)

| Task | Status | Notes |
|------|--------|-------|
| Audit contaminated home-dir repo | DONE | Rooted at `C:\Users\PC`; unrelated projects mixed in |
| Extract MtaaFix files from old HEAD | DONE | `git archive` staging |
| Assemble canonical tree | DONE | Junk dropped; three backend copies merged |
| Backend compile + tests green | DONE | `mvn test` 1/1 on H2 |
| Mobile pub get + analyze clean | DONE | 15 analyzer issues fixed; bogus dep removed |
| Root `.gitignore` / `.env.example` / CI guards | DONE | |
| Truth-ify docs; this board | DONE | |
| `git init` + push to MtaaFix remote | DONE | Commits `a18288d`, `69056ea` |

## Phase 1 — Requirements & system design (COMPLETE — APPROVED 2026-10-13)

| Deliverable | Status | File |
|-------------|--------|------|
| Requirements specification | DONE | `docs/requirements/SRS.md` |
| Role-permission matrix | DONE | `docs/design/ROLE_PERMISSION_MATRIX.md` |
| Report lifecycle specification (12 states, T1–T17) | DONE | `docs/design/REPORT_LIFECYCLE.md` |
| Initial domain model (code-reconciled) | DONE | `docs/design/DOMAIN_MODEL.md` |
| API design outline + conventions | DONE | `docs/api/API_DESIGN.md` |
| Decisions D1–D4 | DONE | SRS §1.5 + ADRs 0002–0004 |
| Backlog with acceptance criteria | DONE | `docs/project-management/BACKLOG.md` |
| ROADMAP restructured to Phases 0–23 | DONE | `docs/project-management/ROADMAP.md` |
| Commit + push | DONE | Commits `e0cad27` + approval bookkeeping pushed to `origin/master` |
| Phase 1 report + approval gate | DONE | Approved by owner 2026-10-13; docs flipped to APPROVED |

## Phase 2 — Design system & UI foundation (COMPLETE — APPROVED 2026-10-13)

| Task | Status | Notes |
|------|--------|-------|
| Fix corrupted tsconfig files | DONE | Real newlines; valid JSON |
| Tailwind v4 + @tailwindcss/vite | DONE | `tokens.css` `@theme`, `global.css` |
| ESLint 9 flat + jsx-a11y | DONE | Pinned eslint@^9 (v10 incompatible) |
| npm install (tailwind, eslint, jsx-a11y) | DONE | Clean reinstall after corruption; 2 vulns noted (esbuild/vite dev-only) |
| Design tokens + typography + favicon | DONE | Spec §9.1 palette; Inter; SVG pin+check |
| Component library | DONE | Button, Field, TextAreaField, Card, Badge, Modal, Toast, EmptyState, LoadingState, ErrorState |
| `/design` gallery + layout previews | DONE | Landing, auth, dashboard, report wizard — presentational only |
| DESIGN_SYSTEM.md | DONE | Tokens, status map, breakpoints, manual a11y checklist |
| Flutter theme groundwork | DONE | `mobile/lib/app/theme.dart` wired in; `flutter analyze` green; screens migrated onto it in Phase 11 |
| CI lint step | DONE | `npm run lint` in web CI step |
| Verify lint + build + flutter | DONE | `eslint .` 0 issues; `tsc` + `vite build` green; analyze clean |
| Phase 2 report + approval gate | DONE | Approved by owner 2026-10-13 |

## Phase 3 — Backend foundation completion (COMPLETE — APPROVED 2026-10-14)

| Task | Status | Notes |
|------|--------|-------|
| Record Phase 2 approval | DONE | |
| Inventory backend config/profiles/Flyway/entities | DONE | Found V1 defects + missing spatial deps |
| Install PostGIS for PostgreSQL 18 | DONE | 3.6.2 OSGeo bundle, elevated overlay; DLL deps verified loadable |
| Create `mtaafix` DB + PostGIS extension | DONE | `postgis_version()` = 3.6 USE_GEOS=1 USE_PROJ=1 |
| Fix V1 migration | DONE | Extension, public schema, geometry+GIST, FK cycle, updated_at, from_status nullable, varchar(36) ids |
| Fix Boot 4 deps | DONE | +spring-boot-flyway (was silently not running), +hibernate-spatial, +spring-boot-webmvc-test; −Testcontainers |
| Fix entities | DONE | BaseEntity PrePersist + assigned ids; AuditLog jsonb |
| Verify app on PG+PostGIS | DONE | Boot OK, Flyway applied, ddl validate passes, health UP (port 8085; 8080 held by system service) |
| H2 vs PG test matrix | DONE | 5/5 green with MTAAFIX_PG_IT; PG suite skips cleanly without |
| Export OpenAPI spec | DONE | `docs/api/openapi.json` via OpenApiExportTest |
| DEVELOPMENT.md runbook | DONE | |
| Phase 3 report + approval gate | DONE | Approved by owner 2026-10-14; docs flipped to APPROVED |

## Phase 4 — Authentication and authorization (COMPLETE — APPROVED 2026-10-14)

| Task | Status | Notes |
|------|--------|-------|
| Record Phase 3 approval | DONE | |
| Enable method security | DONE | `@EnableMethodSecurity` - existing `@PreAuthorize` annotations were inert |
| JWT filter hardening | DONE | DB user lookup per request, `ROLE_*` authorities, User principal, `typ` claim, no path bypass |
| Register is CITIZEN-only | DONE | `role` dropped from RegisterRequest (FR-1.1/FR-5.4) |
| Fix /auth/refresh | DONE | was `findByEmail(subject=id)`; now typ=refresh + findById + active check |
| Login/refresh error semantics | DONE | 401 INVALID_CREDENTIALS / INVALID_TOKEN, 403 ACCOUNT_INACTIVE |
| Role gates per matrix (B-4.1) | DONE | reports, categories, audit, dashboard, assignments, swagger |
| Ownership/org scoping (B-4.2) | DONE | report read/edit, org audit/assignments/dashboard, notification owner scoping + markAllRead fix |
| Audit writes (B-4.3) | DONE | AuditService.record; category create wired; role/org/settings audit with Phase 9 endpoints |
| Latent defects fixed | DONE | immutable jjwt claims broke all token generation; LIE on report get/create; unpersisted assignments; Long notification ids; H2 audit_logs DDL |
| Tests | DONE | 41/41 H2 (matrix 15, auth flow 16, token 5) + 3/3 PostgreSQL; OpenAPI regenerated |
| Live boot smoke on PG | DONE | register/login/refresh/me + 401/403 checks on port 8085 |
| Phase 4 report + approval gate | DONE | Approved by owner 2026-10-14 |

## Phase 5 — Core reporting workflow (COMPLETE — APPROVED 2026-10-14)

| Task | Status | Notes |
|------|--------|-------|
| Record Phase 4 approval | DONE | Commit `d1c5757` |
| Flyway V2 migration | DONE | 12-state status enum, drop `subject`, `category_id` NOT NULL + 12-category seed, `duplicate_of`, `reason_code`, `uploaded_by`, `report_code_counters` (`code_year` — H2 reserved-word fix); applied + validated on PostgreSQL |
| Domain model update | DONE | Report 12 states + `duplicateOf`, history `reasonCode`, media `uploadedBy`, ReportCodeCounter entity |
| Central state machine (B-5.1) | DONE | `report/lifecycle` — T1–T17 rules table; invalid transition → 409 `INVALID_STATUS_TRANSITION`; actor role gate 403 evaluated before state gate; history row + actor per transition; invariant covered by tests |
| Category-driven create (B-5.2) | DONE | `categoryId` required (400), unknown → 404, coordinates ±90/±180 → 400, SRID 4326 point write fix |
| Unique codes (B-5.3) | DONE | `MTF-<year>-<6-digit>` via pessimistic-locked per-year counter + window initializer; concurrency test 60/60 distinct; stable across get |
| Lifecycle endpoints (B-5.4) | DONE | GET `/reports` (status/categoryId/orgId + role scoping + paging), needs-info, resubmit, duplicate, start, complete (photo required), return, confirm, dispute, reopen, comments, media, DELETE `/media/{id}` |
| Coordinate validation (B-5.5) | DONE | Bean-validation on LocationDto (nested `@Valid`) + SRID set on write |
| Tests | DONE | H2 suite 49/49 green (lifecycle 10, code generator 1, matrix 15, auth 16, token 5, context 1, openapi 1); PG suite 3/3 green with V2 applied |
| Live boot smoke on PG | DONE | 16/16 checks on port 8085: register/login, create w/ seeded category + code format, list scoping + filters, history invariant, edit, comment, role-gate 403s, bad filter 400, media delete |
| OpenAPI regenerated | DONE | `docs/api/openapi.json` via OpenApiExportTest |
| Phase 5 report + approval gate | DONE | Approved by owner 2026-10-14; docs flipped to APPROVED |

## Phase 6 — Public website (COMPLETE — APPROVED 2026-10-15)

| Task | Status | Notes |
|------|--------|-------|
| Public APIs | DONE | `/reports/public`, `/reports/public/{code}`, `/categories/public`, `/stats/public`; 2dp coord rounding; CORS |
| Pages 1–4 + `/report` info + 404 | DONE | Landing, how-it-works, explore, detail; Router + Layout + LinkButton |
| Web tests | DONE | Vitest + Testing Library, 22/22; wired to `npm run test` + CI |
| Privacy review | DONE | No identity/media/comments; high-level timeline only |
| Live smoke | DONE | PG backend + Vite; **caught real `lower(bytea)`/untyped-Instant-null bug H2 missed** — fixed |
| Deferrals | NOTED | Map → Phase 8; public media → Phase 10/13; fuzzing → Phase 8 (owner decisions) |

## Phase 7 — Citizen web application (COMPLETE — APPROVED 2026-10-15)

| Task | Status | Notes |
|------|--------|-------|
| Record Phase 6 approval | DONE | Approved 2026-10-15 |
| Owner decisions (6) | DONE | localStorage+silent-refresh, local disk uploads, geolocation+manual coords, PUT /auth/me now, T8 cancel now, dashboard attention now |
| Backend: PUT /auth/me | DONE | fullName + mobile only; blank → 400 |
| Backend: T8 cancel | DONE | creator-only, SUBMITTED → REJECTED `CANCELLED_BY_CITIZEN`, history + notification + audit |
| Backend: multipart upload | DONE | `POST /reports/{id}/media/upload`, MIME allowlist + magic bytes + ≤20 MiB, local `./uploads` disk store |
| Backend: media content/stream | DONE | `GET /media/{id}/content` authenticated bytes; DELETE removes file too |
| Backend: dashboard attention | DONE | needsInfoCount / resolvedAwaitingConfirmCount / activeCount / attentionReports[] (cap 5) |
| Web auth layer | DONE | AuthProvider, RequireAuth, user menu, tokens localStorage + silent refresh-once-on-401 |
| Web pages 8–13 | DONE | dashboard, 4-step wizard (draft + geolocation + photo), my reports, tracking, notifications, profile; login/register/forgot-password info |
| Backend tests | DONE | `CitizenApiTest` 11; full suite 73 H2 + 3/3 PG green (`mvn verify`) |
| Web tests | DONE | Phase7 (6) + wizard (5); suite 33/33; lint/typecheck/build green |
| Live smoke on PG | DONE | port 8085: full citizen journey + 401/400/409/oversize paths |
| Bug: code counter collision | DONE | seeded codes beat counter → 500; bootstrapped from max `MTF-` serial at startup |
| Bug: oversize upload 500 | DONE | Tomcat 1MB default fired pre-check → Spring multipart limits + 413 handler |
| Docs sync (CHANGELOG/ROADMAP/BACKLOG/API_DESIGN) | DONE | 0.8.0 entry; Phase 7 sections; B-7.1–7.4 DONE with delivery notes |
| Phase 7 report + approval gate | DONE | Approved by owner 2026-10-15; docs flipped to APPROVED |

## Phase 8 — Geographic capabilities (COMPLETE — APPROVED 2026-10-15)

| Task | Status | Notes |
|------|--------|-------|
| Geo primitives | DONE | `common/geo`: LocationGeneralizer (HMAC fuzz 150–600 m, env `GEO_FUZZ_*`), GeoJsonPoint, GeoBounds, NearSearch |
| Null-island guard | DONE | (0,0) → 400 `BAD_REQUEST` on create/update (FR-11.2) |
| Spatial queries | DONE | `bbox` (ST_Intersects) + `near` (ST_DWithin ≤500 km) on `/reports/public`; mutually exclusive → 400; PG-only native SQL |
| GeoJSON (FR-11.5) | DONE | exact on `ReportDto`; fuzzed on public summary+detail; OpenAPI regenerated |
| Location privacy (FR-11.4) | DONE | deterministic HMAC fuzzing on **all** public locations (superset of "sensitive categories" — gate disclosure); replaces 2dp rounding |
| Web map interactions | DONE | `ReportMap`/`ReportMapView` (Leaflet lazy chunk, error boundary, sr-only list); explore toggle + "Search this area"; detail Location section; wizard tap-pick |
| Indexes (FR-11.3) | DONE | V3 GIST on `(location::geography)`; EXPLAIN: bbox + near both index-scan |
| Bug: sorted Pageable 500 | DONE | controller's `Sort.by(reportedAt)` → Hibernate appended invalid `order by reportedat` on native SQL; fixed via `PublicReportService.unsorted()`; IT now tests live path |
| Tests | DONE | backend 90/90 (H2 + PG 5/5 incl. sorted path); web 36/36; lint/typecheck/build green |
| Live smoke on PG | DONE | port 8085: fuzz determinism + differs-exact, spatial filter counts, 400s (null-island/bbox+near/malformed), exact geoJson authorized |
| Docs sync | DONE | CHANGELOG 0.9.0, ROADMAP/BACKLOG/BOARD Phase 8 sections, API_DESIGN public-feed row |
| Phase 8 report + gate | DONE | Report delivered 2026-10-15 — **APPROVED 2026-10-15** |

## Phase 9 — Administration and moderation (COMPLETE — APPROVED 2026-10-17)

| Task | Status | Notes |
|------|--------|-------|
| Membership model (D-P9.1) | DONE | Flyway V4 `organization_members` + backfill, drops `users.organisation_id`; `OrganisationMembershipService` (grant requires membership, role change away removes membership) |
| User admin (P19) | DONE | `/users` search + `/{id}/role` + `/{id}/active`; audited; self-change 400, last-active-admin 409 |
| Org management (P21) | DONE | `OrganisationController` CRUD + active + members (add grants org role, remove keeps role); inactive org assign → 409; audited |
| Category admin (P20) | DONE | `?includeInactive=true`, `PUT /{id}/active?value=`, `PUT /{id}/order`; public hides inactive; inactive category create → 400; audited |
| Staff report list (P15) | DONE | `GET /reports` gains `q` + `assignment` filters (role-scoped) |
| Admin dashboard (P14) | DONE | `GET /dashboard/admin`: byStatus, 14d activity, byCategory, moderation queue, workload, attention, recent |
| Audit filters (P22) | DONE | action/actor/entity/outcome/from/to + actorEmail enrichment; smoke found PG null-`Instant` 500 → `cast(:from as timestamp)` fix |
| Analytics (P23) | DONE | `GET /analytics/summary` PG-native (date/category/status/orgId/bbox); H2 covers validation only; PG IT executes it |
| Settings (P24, D-P9.2) | DONE | `GET /settings` ADMIN-only, read-only effective config; secrets masked (`fuzzSecretConfigured`/`fuzzSecretIsDefault`) |
| Admin web — 11 pages | DONE | `pages/admin/*` (P14–P24), role-guarded `/admin` routes, staff entry in account menu; `RequireAuth roles=` |
| Tests | DONE | backend 105/105 (H2+PG: +`AdministrationApiTest` 13, +2 PG IT); web 47/47; lint/typecheck/build green |
| Live smoke on PG | DONE | 68/68: authz boundaries, role-change audit, category/org/member flows, full lifecycle (claim→verify→assign, needs-info→resubmit, reject), inactive-org 409 with guard message |
| Error-hardening fixes | DONE | unknown path 500 → 404, malformed JSON 500 → 400 (new handlers, caught by smoke) |
| Matrix §2.3 revisit (D-P9.3) | DONE | MODERATOR assign stays **N** — reviewed at Phase 9, kept |
| Docs sync | DONE | CHANGELOG 0.10.0, ROADMAP/BACKLOG/BOARD Phase 9 sections, API_DESIGN rows, SRS markers, matrix note |
| Phase 9 report + approval gate | DONE | Approved by owner 2026-10-17; docs flipped to APPROVED |

## Phase 10 — Field-worker web application (COMPLETE — APPROVED 2026-10-17)

| Task | Status | Notes |
|------|--------|-------|
| Worker dashboard (P25) | DONE | `/worker` role-guarded routes, `WorkerDashboardPage` (New/In progress/Completed groups, counts, empty state), `GET /assignments/me` wiring, "My tasks" menu entry |
| Task details (P26) | DONE | `WorkerTaskDetailsPage`: instructions, location + map link, status-driven actions (start / completion claim / read-only at review), progress comments, status history |
| Completion evidence (FR-4.4, T11) | DONE | Evidence upload (multipart, 20 MiB client check) + media grid; backend 400 without photo surfaced on the form |
| Reviewer return (FR-4.5, T12) | DONE | Admin report details "Return to work" → `POST /reports/{id}/return` with required comment |
| Worker authorization tests | DONE | `ReportLifecycleTest` +1: unassigned worker 403 on GET/start/complete/comments, self-scoped `/assignments/me` |
| PublicApiTest flake fix | DONE | Precise-coordinate checks now assert ≥100 m haversine distance (HMAC fuzzing kept latitude digits on E/W bearings; ~0.2 %/run flake root-caused) |
| Tests | DONE | backend 106/106 (H2+PG), web 57/57 (+10 Phase 10); lint/typecheck/build green |
| Live smoke on PG | DONE | 50/50 worker journey on :8085: scoping, 403 boundaries, T11 400, multipart upload + media read, T12 return 400/200, re-claim, role gates, resolve→confirm→CLOSED |
| Finding: non-JSON POST → 500 | DEFERRED | `HttpMediaTypeNotSupportedException` unhandled (same gap family as 405); JSON empty body → 400 correctly; outside Phase 10 scope, flagged in report |
| Docs sync | DONE | CHANGELOG 0.11.0, ROADMAP/BACKLOG/BOARD Phase 10 sections, SRS FR-4.3/4.4/4.5 markers |
| Phase 10 report + approval gate | DONE | Approved by owner 2026-10-17; docs flipped to APPROVED |

## Phase 11 — Flutter application foundation (COMPLETE — APPROVED 2026-10-17)

| Task | Status | Notes |
|------|--------|-------|
| Project completeness | DONE | `flutter create --org tz.co.mtaafix`: Android Gradle (`.kts`) build + `MainActivity`, `ios/Runner` project, `analysis_options.yaml`, mobile `README.md` |
| Navigation + guards | DONE | `lib/app/router.dart` named-route table; protected → sign-in, signed-in away from auth surfaces, unknown → `NotFoundScreen`; splash restores the session before redirecting |
| Theme migration + shared components | DONE | Report list/form, notifications, login/register/profile off hardcoded `Colors.orange` onto the M3 theme; `LoadingView`/`FeedbackView`/`showAppSnack`; placeholder `auth_page.dart` removed |
| API integration + error handling | DONE | `ApiClient`: dart-define base URL, 15 s timeout, bearer auth, one refresh-and-retry with rotation, typed `ApiException` (0 = network), multipart upload helper |
| Authentication state | DONE | `AuthController`/`AuthScope`: restore → `GET /auth/me`, login, register→auto-login, logout; network failure keeps tokens, rejected credentials clear them |
| Android config | DONE | Dev cleartext HTTP; `configChanges` fixed (invalid `displaySize\|displayCutout` flags failed AAPT); NDK 28.2.13676358 installed (`sdkmanager` shim crashes → `android sdk install`) |
| iOS config | DONE | Display name, camera/photo/location usage descriptions, ATS dev exception; **build unexecutable on Windows** (needs macOS — B-X.4) |
| Tests | DONE | `flutter analyze` 0 issues; `flutter test` 29/29 (api client 8, auth controller 8, router 8, theme 2, token store 2, widget journey 1) |
| Debug build | DONE | `flutter build apk --debug` → `app-debug.apk` |
| Docs sync | DONE | CHANGELOG 0.12.0, ROADMAP/BACKLOG/BOARD Phase 11 sections, DESIGN_SYSTEM follow-up note |
| Phase 11 report + approval gate | DONE | Approved by owner 2026-10-17; docs flipped to APPROVED |

## Phase 12 — Flutter citizen features (COMPLETE — APPROVED 2026-10-18)

| Task | Status | Notes |
|------|--------|-------|
| Shared layer | DONE | `paged.dart`, `formatters.dart`, `status_theme.dart` (`StatusChip`), `report.dart`/`notification_item.dart`, `ReportsApi` (stats/CRUD/filters/lifecycle/comments/media/public bbox/categories), `NotificationsApi` |
| Onboarding + shell | DONE | 3-slide onboarding (`onboarding.seen`), splash flag routing, `MainShellScreen` IndexedStack + NavigationBar with `MainShellTab` activation refetch |
| Home dashboard | DONE | Welcome hero, flat `/dashboard/me` counters → status tiles, recent reports/notifications, CTA |
| Report creation | DONE | Category dropdown, validation, geolocator + permission denial, map-picker pin, 5-photo `image_picker` (MIME/20 MB), backend errors surfaced |
| List + detail | DONE | Filters/pagination/load-more; detail with timeline, comments, static map, status-gated cancel/edit/confirm/dispute/resubmit |
| Community map | DONE | flutter_map OSM, post-frame viewport bbox, debounced camera refetch, filters, my-location, marker sheet |
| Notifications | DONE | Unread styling, mark-read/mark-all, deep link to report detail |
| Profile | DONE | Session details, edit dialog (`PUT /auth/me`), privacy/about, sign-out confirmation |
| Fixes from device run | DONE | `uses-material-design` (tofu icons), bare-array `ApiClient` wrap (categories), map pre-camera bbox, dashboard stats flat shape, tab staleness |
| Gates | DONE | `flutter analyze` 0 issues; `flutter test` **111/111**; debug APK built (`MTAAFIX_API_BASE=…:8085`) |
| Emulator verification | DONE | Medium_Phone_API_36.1 + live backend: register → login → create `MTF-2026-000032` → detail → map marker → notifications → profile; screenshots `%TEMP%\phase12\` |
| Docs sync | DONE | CHANGELOG 0.13.0, ROADMAP/BACKLOG/BOARD Phase 12 sections, SRS page-table 27–36 |
| Phase 12 report + approval gate | DONE | Approved by owner 2026-10-18; docs flipped to APPROVED |

**Known gaps:** FR-2.4 date filter unsupported by backend; no change-password endpoint (Phase 13); iOS build macOS-only (B-X.4); password reset (B-X.1).

## Phase 13 — Notifications and Media (COMPLETE — APPROVED 2026-10-18)

| Task | Status | Notes |
|------|--------|-------|
| V5 migration + domain | DONE | `notification_preferences`, `notification_deliveries`, `password_reset_tokens` (SHA-256 hash, single-use, 30 min), repositories + DTOs |
| Preferences + unread API | DONE | `GET /notifications/unread-count`, `GET/PUT /notifications/preferences` (`emailEnabled`, `pushEnabled`, `pushAvailable=false`) |
| Password self-service (B-X.1) | DONE | `POST /auth/password/{forgot,reset,change}`; identical non-disclosing 202s; change preserves the authenticated session; JWTs not revoked after reset (flagged decision) |
| Delivery separation (B-X.2) | DONE | `NotificationDeliveryService` + `EmailRetryScheduler`: IN_APP DELIVERED; EMAIL PENDING→SENT/FAILED/SKIPPED retried ≤3; kill-switch `notifications.email.enabled=false` (no SMTP); no push rows (no provider) |
| Event wiring | DONE | `NotificationService.notifyUser` funnel → `dispatch()` for every lifecycle event (failure never fails the workflow); dispute→owner + `REPORT_CANCELLED` fixes; multipart limits for 413 |
| Media hardening | DONE | MIME allowlist + magic-byte check (pre-existing) plus config-driven `media.max-file-size-bytes` 20 MiB and `media.max-per-report` 10 (new), authorized `GET /media/{id}/content`; **public unauthenticated media NOT implemented** (owner decision) |
| Web | DONE | `NotificationBell` header badge; forgot/reset pages (non-disclosing confirmation, `?token=` prefilled); profile prefs card (auto-save, push explained) + change-password card; notifications pagination; authenticated blob media viewer (admin/worker); wizard 5-photo with per-file validation + partial-failure toast; tracking/worker input cap/type/size checks |
| Mobile | DONE | `UnreadCountScope`/`UnreadCountController` + Alerts badge; `NotificationPreferencesScreen`; forgot/reset/change-password screens + guarded routes; profile rows; report-detail evidence (picker upload with MIME/20 MB/10 checks, remove-with-confirm → `DELETE /media/{id}`, `canEditMedia` gate) |
| Backend tests | DONE | H2 **124/124**, PG IT **7/7** with V5 applied (`NotificationPhase13Test`, `PasswordResetTest`, `MediaLimitsTest`, lifecycle media-url) |
| Web tests | DONE | **68/68** (+11 `Phase13.test.tsx`); lint, `tsc --noEmit`, `vite build` all green |
| Mobile tests | DONE | `flutter analyze` 0 issues; `flutter test` **134/134** (+23) — tests caught a real defect: `ModalRoute.of` in `ResetPasswordScreen.initState` → moved to `didChangeDependencies` |
| Live verification | DONE | :8085 battery — forgot known/unknown identical 202 + token row; reset E2E (bogus 400 → planted 200 → reuse 400 → old pw 401 → new pw 200); change-password 200; unread-count/prefs GET+PUT persisted; media PNG 200 / .txt 400 / fake-JPEG magic 400 / 21 MB 400 / 11th attachment 400 / DELETE 204; submit+cancel notifications with unread increment; delivery rows IN_APP DELIVERED + EMAIL SKIPPED |
| Docs sync | DONE | CHANGELOG 0.14.0, ROADMAP/BACKLOG/BOARD Phase 13 sections, SRS FR-1.7/6.3/6.4 + page 7 + gap rows, OpenAPI regenerated |
| Phase 13 report + approval gate | DONE | Approved by owner 2026-10-18; docs flipped to APPROVED |

**Known gaps:** push provider (FCM/APNs) not configured — `pushAvailable=false`; email kill-switch off (no SMTP provider); public media endpoint intentionally absent (owner decision); backend attach/delete has no report-status guard (active-status gate is mobile-UI-side only); JWTs survive password reset; FR-2.4 date filter unsupported by backend; iOS build macOS-only (B-X.4).

## Phase 14 — Offline Capabilities (COMPLETE — APPROVED 2026-10-21)

| Task | Status | Notes |
|------|--------|-------|
| Scope assessment | DONE | Owner-approved scope: Option B backend idempotency; draft storage; pending queue + foreground retry; categories cache; sign-out clear. Offline browsing/map + background sync deferred; mobile-only beyond web's pre-existing draft key |
| Backend idempotency | DONE | Flyway **V6** `report_idem_keys` (key → report_id + payload_hash, 24 h TTL, hourly purge); `Idempotency-Key` `[A-Za-z0-9_-]{16,64}` on `POST /api/v1/reports` → 400 invalid / 201 first use / 201 stored replay (same id, no duplicate) / 409 `IDEMPOTENCY_CONFLICT` / 409 `IDEMPOTENCY_IN_PROGRESS` (stale intent 2 min); REQUIRES_NEW tx; private ObjectMapper + `hashOf`; conflict exception + handler; header optional |
| Mobile offline core | DONE | `features/offline/`: `OfflineController` (single-flight `flush()`, `_disposed` notify guard), `Zone.root`-wrapped atomic `OfflineStorage` (schema 1, tmp+rename, graceful degradation), `DraftStore`/`OutboxStore`/`CategoryCache`, `ConnectivityService`, `OfflineScope`, `PendingSubmissionsScreen` (Edit/Retry/Discard w/ confirm, offline hint) |
| Form integration | DONE | Debounced 400 ms draft autosave + Restore/Discard offer (empty `categoryId` drafts restorable — product fix), offline submit → queue, failed-entry Edit → prefill + re-queue replacing old entry (`keepPaths` keeps photo files), photos copied into offline storage at pick, one idempotency key per form session, category-cache fallback + stale hint |
| Shell/list/profile | DONE | Offline strip (flush on mount + app resume), pending banner + `consumeJustSubmitted()` reload, `clearAll()` on sign-out (action-wired, not auth-listener), guarded `/pending-submissions` route |
| API plumbing | DONE | `ApiClient.post(path, body, idempotencyKey)` → `Idempotency-Key` header incl. 401→refresh retries; `ReportsApi.create(..., idempotencyKey)`; deps `path_provider ^2.1.6`, `connectivity_plus ^7.3.2` |
| Backend tests | DONE | H2 **133/133** (incl. `IdempotencyTest` ×8), PG IT **8/8** with V6, jar packaged, `openapi.json` regenerated |
| Mobile tests | DONE | `flutter test` **167/167** (+33: offline controller ×12, storage ×6, pending screen ×5, form offline ×7, banner ×3, misc), `flutter analyze` 0 issues; test-driven fixes: post-dispose notify guard, empty-category draft rejection, SnackBar host Scaffold in tests |
| Live battery | DONE | :8085 (PG): new key → 201 `MTF-2026-000035`; same key + same body → 201 **same id**; same key + different body → 409 `IDEMPOTENCY_CONFLICT`; key `short` → 400 `BAD_REQUEST` |
| Design/docs | DONE | `docs/design/OFFLINE_CAPABILITIES.md` (new); CHANGELOG 0.15.0; ROADMAP Phase 14 + version history + delivery targets; API_DESIGN `POST /reports` row; SRS FR-2.11; BACKLOG deferrals |
| Phase 14 report + approval gate | DONE | Report delivered 2026-10-21; **approved by owner 2026-10-21** — Phase 15 may start |

**Known gaps/deferrals:** offline browsing of existing reports and offline map tiles (approved deferral); background sync — foreground-only retry by design; iOS build macOS-only (B-X.4); carried gaps unchanged (no push provider, email kill-switch off, no public media endpoint, no backend attach/delete status guard, JWTs survive password reset, FR-2.4 date filter).

## Phase 15 — Quality Assurance (COMPLETE — APPROVED 2026-10-21)

| Task | Status | Notes |
| ---- | ------ | ----- |
| Suites (clean runs) | DONE | backend `mvn test` **133/133** H2 (PG class auto-skipped without env); PG `mvn test -Dtest=PostgresIntegrationTest` **8/8** (`MTAAFIX_PG_IT=true` + local PostgreSQL, V6 applied); web `eslint .` 0 / `tsc --noEmit` 0 / `vitest run` **68/68** (10 files) / `vite build` green; mobile `flutter analyze` 0 / `flutter test` **170/170** (+3) |
| Gap-fill tests | DONE | `mobile/test/phase15_quality_test.dart`: citizen critical journey (sign in → create `MTF-2026-000042` → detail → back → refresh → report in My Reports), critical-screens no-overflow sweep at 320×640, accessible-semantics assertions; harness = stateful mock client + geolocator/connectivity/path-provider channel mocks (open connectivity stream kept alive to avoid resubscribe loop) |
| Responsive fixes | DONE | 5 RenderFlex overflows found at 320×640 and fixed: login auth actions `Row`→`Wrap`, home `_SectionHeader`→`Expanded`+ellipsis, category dropdown `isExpanded`, photo attach `Row`→`Wrap` |
| Map/test infra | DONE | `MapScreen` lazy `_mapBooted` tile mount (placeholder until first activation); new `core/map/app_tile_provider.dart` — `appTileProvider()` disables the disk-cache worker isolate **only under `FLUTTER_TEST`** (isolate stalled fake-async test completion); prod disk cache unchanged; wired into map screen + report detail + map picker |
| Live battery | DONE | :8085 **63/63** — two full lifecycle chains (claim/verify → assign w/ photo → start → complete → resolve → close → reopen; second pass ends citizen **confirm**); 403 matrix (citizen/mod/worker vs admin/moderation), 401 anonymous, FR-5.4 org-role-without-membership → 400, 409 state gate, 403 unassigned-ownership guard; defect probes reproduced: non-JSON POST → 500, GET-on-POST → 500 |
| A11y + responsive verification | DONE | mobile semantics: login field labels, NavigationBar destinations (tabLabel merge), form field labels, refresh tooltip (`byTooltip`); 320×640 sweep; web jsx-a11y clean + documented audit (single structural breakpoint `UserMenu`, wrapping header nav, no hamburger; jsdom cannot assert CSS-driven visibility — limitation recorded) |
| Defect review + mitigation plan | DONE | 5 responsive defects **resolved this phase**; carried defects + Phase 14 deferrals documented with severities/evidence/dispositions in BACKLOG "Phase 15 known defects + mitigation plan" (D1–D14) — exit criterion met |
| Docs sync | DONE | CHANGELOG 0.16.0; ROADMAP Phase 15 + version history + delivery table; BACKLOG B-15.1..6 + plan; BOARD Phase 15 table; `docs/api/openapi.json` tag-description sync |
| Phase 15 report + approval gate | DONE | Report delivered 2026-10-21; **approved by owner 2026-10-21** — Phase 16 may start |

## Phase 16 — Security Review and Hardening (COMPLETE — APPROVED 2026-10-22)

| Task | Status | Notes |
| ---- | ------ | ----- |
| Owner-approved scope | DONE | Fix Critical/High + two cheap Mediums; implement rate limiting; JWT revocation via `token_version` (reset **and** change); defer offline-storage encryption to Phase 20; pragmatic dependency review (`npm audit` + Maven inventory, no NVD scan); review record at `docs/standards/SECURITY_REVIEW.md` |
| Review record | DONE | `docs/standards/SECURITY_REVIEW.md` — 10 mandated areas, findings F1–F12 (severities, evidence, dispositions), deferral table, backup/restore runbook, live-verification evidence table |
| D1/D2 error contracts | DONE | 415 (`UNSUPPORTED_MEDIA_TYPE`) + 405 (`METHOD_NOT_ALLOWED`) handlers in `GlobalExceptionHandler` + `WebErrorHandlingTest` ×4; Phase 15 500-evidence was a **stale Oct-20 jar** — rebuilt artifact re-probed live (400/415/405 ApiError) |
| D3 JWT revocation | DONE | Flyway **V7** `users.token_version`; `tv` claim on access+refresh; filter + refresh reject stale; reset **and** change bump the version; `/auth/password/change` now returns rotated `AuthResponse` — web (`setAuthTokens`) + mobile (`setSession`) persist it; rotation tests both clients |
| D4 media status gate | DONE | `ReportService.assertMediaEditable` → 409 on RESOLVED/REJECTED/DUPLICATE/CLOSED for add/upload/delete media; `MediaLimitsTest` terminal-status test; live probe upload → citizen-cancel → upload 409 |
| Rate limiting | DONE | `RateLimitFilter` per-IP fixed windows: `POST /api/v1/auth/**` 30/min, `POST /api/v1/reports` 20/min; 429 + `Retry-After` + ApiError `RATE_LIMITED`; `RATE_LIMIT_*` config; disabled in test profile, re-enabled in `RateLimitTest` ×4; live: forgot-password attempt 31 → 429 + header |
| Secrets/deployment | DONE | `ProdSecretsGuard` (`prod` profile) refuses shipped defaults/blanks/short secrets; `.env.example` annotated with prod-required overrides + `RATE_LIMIT_*`; `management.health.mail.enabled` mirrors `NOTIFICATIONS_EMAIL_ENABLED` (health UP without SMTP) |
| Dependency review | DONE | Web prod 0 vulns; dev-only advisories recorded (vitest/vite/tinypool — majors deferred, B-16.2); Maven inventory: Spring Boot 4.1.1, jjwt 0.12.6, postgresql 42.7.7 current — no core updates |
| Logging/privacy audit | DONE | No credentials/tokens/PII in log statements (grep evidence in review record); audit trail keeps actor id + hashed reset tokens only |
| Suites | DONE | backend `mvn test` **144/144** H2 (+11 new tests); PG IT **8/8** (`MTAAFIX_PG_IT=true`, V7 applied); web `tsc` 0 / `vitest` **68/68**; Flutter `flutter test` **171/171** |
| Live battery | DONE | Rebuilt jar on :8085, `backend/target/battery-p16.ps1` **23/23** — regression, admin-only swagger, 401 anonymous, 415/405/400 contracts, D4 gate, D3 revocation E2E (old pair 401 → new pair 200), D6 429-at-31 + Retry-After, health UP |
| Docs sync | DONE | SECURITY.md checklist; CHANGELOG 0.17.0; ROADMAP Phase 16 + version history + delivery table; BACKLOG D1–D5 dispositions + B-16.1–5; BOARD this table; `.env.example` |
| Phase 16 report + approval gate | DONE | Report delivered 2026-10-22; **approved by owner 2026-10-22** — Phase 17 may start |

**Known deferrals:** offline drafts/outbox encryption → Phase 20 (B-16.1); web dev-toolchain majors (B-16.2); backup automation/PITR beyond documented runbook (B-16.3); prod-profile activation + forwarded-headers for the rate limiter → Phase 18 (B-16.4); full NVD scan (B-16.5, owner-skipped).

## Phase 17 — Performance and Reliability (COMPLETE — APPROVED 2026-10-22)

| Task | Status | Notes |
| ---- | ------ | ----- |
| Owner-approved scope | DONE | Q1 Caffeine caching; Q2a paginate user-growable lists (assignments, org members) + notifications cap, categories/organisations documented bounded exemptions; Q3 ~5,000-report direct-SQL seed; Q4 workstreams A–H |
| A — N+1 elimination | DONE | `ReportDto.listItem` lean factory (batch media, empty history/comments; detail `from()` unchanged); `findByScope` fetch-joins assignment (assignee/org); `findMediaByReportIds` batch; assignment service returns scalar DTOs via `PagedResponse` with explicit count queries |
| B — Index review | DONE | Flyway **V8**: composite btree `(status\|organisation\|category\|user, reported_at DESC)` + `pg_trgm` GIN on `lower(code/title/description)`; PG IT asserts 4 composite + ≥3 trigram indexes; live `EXPLAIN` shows composite used for selective statuses |
| C — Pagination | DONE | `GET /assignments/me` + `GET /organizations/{id}/members` → `PagedResponse` (page/size, size ≤ 100, members batch-load users); `GET /notifications` size clamped (Page shape kept); web `api.ts` unwraps `.content` (fetch size=100); tests updated (AdministrationApiTest, ReportLifecycleTest, Phase10 mocks) |
| D — Timing + measurement | DONE | `RequestTimingFilter` (DEBUG per request, WARN > `timing.warn-threshold-ms` 500, best-effort `X-Response-Time-Ms`) wired before `RateLimitFilter`; `scripts/seed-perf.sql` (5,000 reports, idempotent) + `scripts/measure-perf.ps1` (5 warmup + 30 iterations, p50/p95/max → `perf-results.json`) |
| E — Caching | DONE | `spring-boot-starter-cache` + Caffeine, `expireAfterWrite=60s`; `categories` evicted on create/update/activate/reorder; `publicStats` + `adminDashboard` TTL; user dashboard uncached (freshness); test profile `type: simple` (`none` broke `@EnableCaching` — no CacheManager bean); `CachingTest` ×3 via `@MockitoSpyBean` proves hit/evict |
| F — Map feeds | DONE | ExplorePage separate map fetch (`size=100`, own loading/error, marker-count note, privacy note always visible); mobile `publicList` default 50 → 100 |
| G — Activity bucketing | DONE | `buildActivity14d` → SQL `GROUP BY (reported_at AT TIME ZONE 'Africa/Dar_es_Salaam')::date` when `database-platform` = PostgreSQL (set in application.yml); Java fallback kept for H2 tests |
| H — Docs sync | DONE | `docs/standards/PERFORMANCE_REVIEW.md` (new); API_DESIGN (rate-limit row corrected → shipped Phase 16, pagination exemptions, list-row note, members/assignments paged rows); CHANGELOG 0.18.0; ROADMAP Phase 17 + version history; BACKLOG B-17.1–7; BOARD this table |
| Suites | DONE | backend `mvn test` **148/148** H2 (+CachingTest); PG IT **9/9** (`MTAAFIX_PG_IT=true`, V8 asserted); web `tsc` 0 / `vitest` **68/68**; Flutter `flutter test` **171/171** |
| NFR-7 measurement | DONE | Seeded 5,038-report dataset, live jar on :8085 — **11/11 endpoints p95 < 500 ms → MET**; worst p95 397.2 ms (public bbox size=100); server WARNs = 3 one-time startup spikes only; results in `scripts/perf-results.json` + review §4 |
| Phase 17 report + approval gate | DONE | Report delivered 2026-10-22; **approved by owner 2026-10-22** — Phase 18 may start |

**Known deferrals:** marker clustering (B-17.1); Micrometer/Prometheus (B-17.2 → Phase 19); Redis (B-17.3); user-dashboard caching rejected by design (B-17.4); reference-data pagination exemption (B-17.5); search/bbox caching (B-17.6); repeat larger-N measurement (B-17.2/B-17.7 → Phase 19).

## Phase 18 — Deployment and CI/CD (COMPLETE — APPROVED 2026-10-22)

| Task | Status | Notes |
| ---- | ------ | ----- |
| Owner-approved scope | DONE | Q1 deployment = readiness only (no remote host); Q2 Docker = skip + document rationale (no container runtime here); Q3 CI depth = all four (PG service + IT, flutter test, release workflow, gitleaks); Q4 commit Phases 16–18, owner pushes |
| A — Prod profile + guard tests | DONE | `application-prod.yml`: `server.forward-headers-strategy: framework` (B-16.4), explicit `ddl-auto: validate`, actuator health-only (`show-details: never`); `ForwardedIpRateLimitTest` (XFF rewrite at `HIGHEST_PRECEDENCE` before the security chain: client A exhausts 2/min → 429 + Retry-After + `RATE_LIMITED`, client B behind same socket passes); `ProdSecretsGuardTest` ×6 |
| B — Runbook | DONE | `docs/operations/DEPLOYMENT.md`: prerequisites, secrets/env, build, prod boot, nginx `X-Forwarded-*` example, CI/release, stop/rollback, scope notes (Docker skipped, backups Phase 19) |
| C — CI/CD | DONE | `ci.yml` 4 parallel jobs: backend (`postgis/postgis:18-3.6` service, `MTAAFIX_PG_IT=true`, `JWT_SECRET` ≥32 for the default-profile IT context), web (lint + vitest + tsc/vite build), mobile (`flutter analyze` + `flutter test`), gitleaks (full history); concurrency cancel + `workflow_dispatch`; `release.yml` on `v*` → jar (tests rerun) + `web-dist.zip` on the GitHub Release |
| D — Verification battery | DONE | Refusal: `prod` + shipped `JWT_SECRET` → exit 1 (`JWT secret must be at least 32 characters`); real JWT/DB + default `GEO_FUZZ_SECRET` → `ProdSecretsGuard: Refusing to start with the prod profile: geo.fuzz-secret still holds a shipped development default`. Boot: fresh scratch DB → Flyway **V1–V8**, validate passed, health **UP ~10 s**, public API 200, scratch dropped. Suites: backend **155/155** (146 H2 + 9 PG) / web lint + **68/68** + build / mobile analyze + **171/171** |
| E — Docs sync | DONE | CHANGELOG 0.19.0; ROADMAP Phase 18 section + version history + delivery row; BACKLOG D11 + B-16.4 closed, Phase 18 outcomes (B-18.1/2); BOARD this table; DEVELOPMENT §8 (new pipeline + local PG IT env); `.env.example` (`RATE_LIMIT_*_PER_MINUTE` names, `GEO_FUZZ_SECRET`, timing var); DEPLOYMENT §6 drill evidence |
| Phase 18 report + approval gate | DONE | Report delivered 2026-10-22; **approved by owner 2026-10-22** — Phase 19 may start |

**Known deferrals:** Docker image/compose (owner decision — B-18.1); remote host provisioning (readiness only — B-18.2); backup automation + PITR → Phase 19 (B-16.3).

## Phase 19 — Observability and Operations (IN PROGRESS — started 2026-10-23)

| Task | Status | Notes |
| ---- | ------ | ----- |
| Owner-approved scope (2026-10-23) | DONE | Q1 metrics exposure = same port + nginx allowlist (`/actuator/prometheus` permitted; runbook restricts it to the metrics network); Q2 depth = **full PITR + dump drill** on a scratch PG 18 cluster; Q3 alerts = **rules as code + docs** (no live Prometheus/Grafana here) |
| A — Micrometer/Prometheus (B-17.2) | DONE | `micrometer-registry-prometheus` dependency; management config: `health,prometheus` exposure (default + prod), `application` tag, percentiles [0.5/0.95/0.99] + percentile histograms for `http.server.requests`; `SecurityConfig` permits `/actuator/prometheus`; `PrometheusMetricsTest` ×2 (unauth scrape 200, `application="mtaafix"`, histogram buckets, 404 recorded under bounded uri tag, raw path never leaks, `/actuator/env` still 401) |
| B — Structured logging | DONE | prod JSON via `logging.structured.format.console: logstash`; `RequestTimingFilter` assigns `X-Request-Id` (inbound honoured when matching `[A-Za-z0-9._-]{1,64}`, else UUID), MDC + response echo + clear; dev console pattern carries `[requestId]`; `RequestTimingFilterTest` ×3 (generate, honour, reject-injection) |
| C — Alerts as code | DONE | `ops/prometheus/alerting-rules.yml` — 9 rules (target down, scrape missing, 5xx ratio >5%, p95 >1s, Hikari saturation/queueing, heap >90%, CPU >85%, disk <10%) with severities + runbook pointers; metric names verified against a live scrape; YAML validated |
| D — Backup automation (B-16.3) | DONE | `backend/scripts/backup.ps1` (pg_dump `-Fc` + uploads zip + SHA-256 manifest + retention pruning, default 14 days, PGPASSWORD-from-env only) + `backend/scripts/restore-database.ps1` (scratch target DB by default, refuses the live `mtaafix` without `-Force`) |
| D2 — Drills | IN PROGRESS | **dump/restore drill PASSED** (backup → restore into `mtaafix_restore`: 5,038 reports + PostGIS tables landed); PITR drill script `backend/scripts/pitr-drill.ps1` written (scratch cluster, WAL archiving, pg_basebackup, recover-to-time between markers, promote + assert) — first run stalled waiting on the scratch cluster; hardening (connect timeouts, job-wrapped basebackup, step log) underway |
| E — NFR-7 re-run (B-17.7) | TODO | Live app + seeded dataset, `scripts/measure-perf.ps1`, deltas recorded in `docs/standards/PERFORMANCE_REVIEW.md` |
| F — Troubleshooting doc | TODO | `docs/operations/TROUBLESHOOTING.md` (alert triage table, log correlation via `X-Request-Id`, DB/backup/restore, slow requests) |
| G — Docs sync | TODO | CHANGELOG 0.20.0; ROADMAP Phase 19 section + version history + delivery row; BACKLOG B-17.2/B-16.3/B-17.7 closed; BOARD this table; DEPLOYMENT (nginx allowlist, backup/PITR runbook, alerting); DEVELOPMENT (metrics/logging); `.env.example` |
| Phase 19 report + approval gate | TODO | After suites green — owner reviews and approves |

## Phase delivery summary (0–23, updated 2026-10-23)

| Phase | Status | Notes |
|-------|--------|-------|
| Phase 0 - Project audit & foundation | Complete | Contaminated home-dir repo extracted to a canonical tree; green backend/mobile baselines; root `.gitignore`/CI guards; pushed to the MtaaFix remote |
| Phase 1 - Requirements & system design | Complete (approved 2026-10-13) | SRS, role-permission matrix, 12-state lifecycle spec, domain model, API design, decisions D1–D4, backlog, ROADMAP restructured to Phases 0–23 |
| Phase 2 - Design system & UI foundation | Complete (approved 2026-10-13) | Tailwind v4 + tokens, ESLint 9 + jsx-a11y, component library, `/design` gallery, DESIGN_SYSTEM.md, Flutter theme groundwork |
| Phase 3 - Backend foundation | Complete (approved 2026-10-14) | PostGIS 3.6 on PostgreSQL 18, Flyway V1 repair, Boot 4 dependency fixes, H2+PG test matrix, OpenAPI export, DEVELOPMENT.md runbook |
| Phase 4 - AuthN/Z | Complete (approved 2026-10-14) | Matrix enforcement + scoping + audit + tests done |
| Phase 5 - Core reporting workflow | Complete (approved 2026-10-14) | 12-state machine, unique codes, comments/media, lifecycle endpoints; deferrals noted in report |
| Phase 6 - Public website | Complete (approved 2026-10-15) | 4 public pages + public APIs + privacy; real-PG smoke caught/fixed H2-masked query bug |
| Phase 7 - Citizen web | Complete (approved 2026-10-15) | Auth layer + 6 citizen pages; profile/T8-cancel/multipart/dashboard-attention backend; live smoke caught 2 real bugs (code counter, oversize 500) — both fixed |
| Phase 8 - Geographic capabilities | Complete (approved 2026-10-15) | PostGIS bbox/near queries, Leaflet map, HMAC coord fuzzing, V3 geography index; live smoke + IT hardening caught 1 real bug (sorted Pageable → 500) — fixed |
| Phase 9 - Administration and moderation | Complete (approved 2026-10-17) | Membership V4, user/org/category admin, filtered audit, admin dashboard + analytics + read-only settings, 11 admin pages; live smoke caught 3 real bugs (PG audit filter 500, unknown-path 500, malformed-body 500) — all fixed |
| Phase 10 - Field-worker web application | Complete (approved 2026-10-17) | Worker dashboard + task details pages, progress/evidence, admin return action, worker-scoping tests, PublicApiTest flake fix; live smoke 50/50 found 1 deferred gap (non-JSON content type → 500) |
| Phase 11 - Flutter application foundation | Complete (approved 2026-10-17) | Route guards, secure token storage, auth wiring, themed screens, Android config fixed (AAPT `configChanges` + NDK 28.2), iOS scaffolded (needs macOS); 29/29 Flutter tests + debug APK |
| Phase 12 - Flutter citizen features | Complete (approved 2026-10-18) | Onboarding → shell → create → detail → map → notifications → profile; live-backend emulator run; 111/111 tests; 5 verification defects fixed (tofu icons, bare-array decode, map bbox, dashboard stats shape, tab staleness) |
| Phase 13 - Notifications and media | Complete (approved 2026-10-18) | V5 prefs/deliveries/reset-tokens, unread-count + preferences API, forgot/reset/change password, delivery separation with kill-switch, media caps/MIME/magic hardening, web bell + reset pages + blob media, mobile badge/prefs/recovery/evidence; gates 124+7 PG / 68 web / 134 mobile + live battery |
| Phase 14 - Offline capabilities | Complete (approved 2026-10-21) | Backend idempotency (V6), mobile draft/outbox/pending-queue with foreground retry, categories cache, sign-out clear; 133+8 PG / 68 web / 167 mobile; live battery 67/67 |
| Phase 15 - Quality assurance | Complete (approved 2026-10-21) | Gap-fill tests (critical journey, 320×640 no-overflow, semantics), 5 responsive fixes, lazy map mount + test tile provider, role-boundary/lifecycle live battery 63/63 |
| Phase 16 - Security review and hardening | Complete (approved 2026-10-22) | 10-area review record; 415/405 contracts (D1/D2), JWT revocation via token_version incl. rotating password change (D3), media status gate (D4), per-IP rate limiting, prod secrets guard; 144+8 PG / 68 web / 171 mobile + live battery 23/23 |
| Phase 17 - Performance and reliability | Complete (approved 2026-10-22) | N+1 elimination, V8 composite + trigram indexes, assignments/members pagination + notifications clamp, Caffeine TTL caches, separate map feeds (100 markers), SQL activity bucketing, RequestTimingFilter; NFR-7 measured 11/11 p95 < 500 ms (5,038-report seed); 148+9 PG / 68 web / 171 mobile |
| Phase 18 - Deployment and CI/CD | Complete (approved 2026-10-22) | Prod profile (forwarded headers, health-only actuator) + guard/forwarded-IP tests; DEPLOYMENT.md runbook; CI 4 jobs (PG service IT, web, mobile, gitleaks) + release.yml; drills: refusal + fresh-DB boot health UP Flyway V1-V8; 155 (146+9 PG) / 68 web / 171 mobile |
| Phase 19 - Observability and operations | **In progress (started 2026-10-23)** | Micrometer/Prometheus per-endpoint metrics + histograms (B-17.2), backup automation + PITR with restore drill (B-16.3), repeat NFR-7 measurement (B-17.7), structured logging, alerts, troubleshooting docs |
| Phase 20 - Beta release | Not started | Critical-workflow verification, test data, known-limitations doc, structured feedback, release criteria; carries offline-storage encryption (B-16.1) |
| Phase 21 - Approved advanced features | Not started | Analytics, Swahili, multi-org improvements, duplicate detection, human-reviewed AI assistance, integrations — each with own acceptance criteria |
| Phase 22 - Production readiness / v1.0 | Not started | Defect closure, security/privacy verification, backup/restore verification, documentation finalization, release notes |
| Phase 23 - Post-release development | Not started | Feedback- and data-driven iteration under the same approval-gated process |

## Environment notes

- Java 25.0.4, Maven 3.9.16, Node 24.13.1, Flutter 3.47.7 (upgraded 2026-10-23), PostgreSQL 18 running
- PostGIS 3.6.2 installed for PG 18 (OSGeo bundle overlay, Phase 3)
- No Docker; no Testcontainers
- `gh` CLI token invalid; git push works via Windows Credential Manager
- Old home-dir repo untouched at `C:\Users\PC\.git` — never push from it
