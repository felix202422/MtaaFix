# Roadmap — MtaaFix

> Phase numbering follows the **master specification** (Phases 0–23) per decision D1
> (2026-10-13). Status reflects **verified repository state** (last audited 2026-10-14),
> not intent. **MVP = Phases 0–7** (approved). Every phase ends with an approval gate
> (master spec Part 16). Local tracking: [BOARD.md](BOARD.md) ·
> Work items: [BACKLOG.md](BACKLOG.md) · Requirements: [../requirements/SRS.md](../requirements/SRS.md)

## Version History

- **[0.19.0]** — Phase 18: Deployment and CI/CD (prod profile with
  forwarded headers + health-only actuator, ProdSecretsGuard drill,
  ForwardedIpRateLimitTest for proxy-keyed buckets, DEPLOYMENT.md
  runbook + verified local boot on a fresh database, CI: 4 jobs incl.
  PostgreSQL/PostGIS service + flutter test + gitleaks, release.yml tag
  pipeline; suites 155 (146 H2 + 9 PG) / 68 web / 171 mobile) —
  **APPROVED 2026-10-22**
- **[0.18.0]** — Phase 17: Performance and reliability (N+1 elimination,
  Flyway V8 composite + trigram indexes, pagination of assignments/members/
  notifications clamp, Caffeine TTL caches, separate map feeds, SQL activity
  bucketing, RequestTimingFilter; NFR-7 measured: 11/11 endpoints p95 < 500 ms
  on a seeded 5,038-report dataset — record in
  `docs/standards/PERFORMANCE_REVIEW.md`; suites 148 H2 + 9 PG / 68 web /
  171 mobile) — **APPROVED 2026-10-22**
- **[0.17.0]** — Phase 16: Security review and hardening (10-area review
  record in `docs/standards/SECURITY_REVIEW.md`; 415/405 error contracts,
  JWT revocation via `users.token_version` incl. rotating password-change
  contract, server-side media status gate, per-IP rate limiting on auth +
  report submission, prod-profile secret fail-fast, health/mail indicator
  fix; suites 144 H2 + 8 PG / 68 web / 171 mobile; live battery 23/23) —
  **APPROVED 2026-10-22**
- **[0.16.0]** — Phase 15: Quality assurance (full suites green — 133 H2 +
  8 PG / 68 web / 170 mobile; new citizen critical-journey, 320×640
  no-overflow and semantics tests; five responsive overflow fixes; lazy map
  mount + test-only tile-provider; live battery 63/63 with role-boundary
  and lifecycle chains; defects mitigation plan) — **APPROVED
  2026-10-21**
- **[0.15.0]** — Phase 14: Offline capabilities (Option B idempotency on
  `POST /reports` — stored-response replay, 409 conflict/in-progress, 400
  invalid key, 24 h TTL; mobile draft autosave + pending queue with
  foreground retry + categories cache + sign-out clear; honesty invariant,
  live battery, 133 H2 + 8 PG / 167 mobile tests) — **APPROVED
  2026-10-21**
- **[0.14.0]** — Phase 13: Notifications and media (unread badge + prefs
  API, delivery separation with email kill-switch, password forgot/reset/
  change, media caps/MIME/magic hardening, web bell + reset pages + blob
  viewer, mobile badge/prefs/recovery/evidence lifecycle; live battery,
  124+7 PG / 68 web / 134 mobile tests) — **APPROVED**
  2026-10-18
- **[0.13.0]** — Phase 12: Flutter citizen features (onboarding, home
  dashboard, report creation with photos/location/map-picker, list/detail
  with lifecycle actions, community map, notifications, profile; live-backend
  emulator verification, 111/111 Flutter tests) — **APPROVED**
  2026-10-18
- **[0.12.0]** — Phase 11: Flutter application foundation (navigation + route
  guards, secure token storage, auth-state wiring, platform configuration,
  themed screens, 29/29 Flutter tests, debug APK) — **APPROVED**
  2026-10-17
- **[0.11.0]** — Phase 10: field-worker web application (worker dashboard + task
  details pages, assignment progression, completion evidence, reviewer return,
  worker-scoping tests; 106/106 backend + 57/57 web) — **APPROVED**
  2026-10-17
- **[0.10.0]** — Phase 9: administration and moderation (membership model V4, user/
  org/category admin APIs, filtered audit, admin dashboard, analytics, read-only
  settings, 11 admin web pages; 105/105 backend + 47/47 web) — **APPROVED**
  2026-10-17
- **[0.9.0]** — Phase 8: geographic capabilities (HMAC location fuzzing, bbox/near
  spatial queries, GeoJSON, Leaflet map interactions, GIST indexes; 90/90 backend +
  36/36 web) — **APPROVED** 2026-10-15
- **[0.8.0]** — Phase 7: citizen web application (auth layer, 6 citizen pages, profile
  update, T8 cancel, multipart photo upload/streaming, dashboard attention; 73/73 + 3/3
  backend, 33/33 web) — **APPROVED** 2026-10-15
- **[0.7.0]** — Phase 6: public website (4 public pages, public APIs, privacy review; 22/22 web tests) — **APPROVED** 2026-10-15
- **[0.6.0]** — Phase 5: core reporting workflow (12-state lifecycle machine, category-driven
  create, unique MTF codes, comments/media, full lifecycle endpoint set; 49/49 + 3/3 tests) — **APPROVED** 2026-10-14
- **[0.5.0]** — Phase 4: authentication & authorization enforcement (matrix gates, DB role lookup, org/ownership scoping, audit wiring; 41/41 + 3/3 tests) — **APPROVED** 2026-10-14
- **[0.4.0]** — Phase 3: backend foundation completed (PostGIS 3.6.2 verified,
  Flyway V1 fixed + applied, H2/PostgreSQL test matrix, OpenAPI export,
  DEVELOPMENT.md) — **APPROVED** 2026-10-14
- **[0.3.0]** — Phase 2: design system & UI foundation (tokens, components, gallery,
  Flutter theme groundwork) — **APPROVED** 2026-10-13
- **[0.2.0]** — Phase 1: requirements & system design baseline (SRS, role matrix,
  lifecycle, domain model, API design, ADRs 0002–0004) — **APPROVED** 2026-10-13
- **[0.1.0]** — Phase 0: repository remediation & verified baseline — 2026-10-13

---

## MVP summary (Phases 0–7)

| Phase | Name | Status (2026-10-14) |
| ----- | ---- | ------------------- |
| 0 | Project audit and foundation | ✅ Complete |
| 1 | Requirements and system design | ✅ Complete (approved 2026-10-13) |
| 2 | Design system and UI foundation | ✅ Complete (approved 2026-10-13) |
| 3 | Backend foundation | ✅ Complete (**APPROVED** 2026-10-14) — PostGIS verified; Flyway V1 green; H2+PG test matrix |
| 4 | Authentication and authorization | ✅ Complete (**APPROVED** 2026-10-14) — matrix gates, DB role lookup, scoping; tests 41/41 + 3/3 PG |
| 5 | Core reporting workflow | ✅ Complete (**APPROVED** 2026-10-14) — 12-state machine, unique codes, comments/media; 49/49 + 3/3 tests |
| 6 | Public website | ✅ Complete (**APPROVED** 2026-10-15) — 4 public pages, public APIs, privacy review; 22/22 web tests |
| 7 | Citizen web application | ✅ Complete (**APPROVED** 2026-10-15) — auth layer, 6 citizen pages; 73/73 + 3/3 backend, 33/33 web |

Post-MVP: Phases 8–23 below.

---

## Phase 0 — Project Audit and Foundation (COMPLETE)

Verified: contaminated-history cleanup; canonical monorepo; merged backend
(compiles, context-load test green); mobile analyze-clean; guarded CI; root
.gitignore/.env.example; truth-ified docs. See CHANGELOG 0.1.0 and Phase 0 report.

## Phase 1 — Requirements and System Design (COMPLETE — approved 2026-10-13)

Deliverables produced 2026-10-13:
- [x] Requirements specification — [../requirements/SRS.md](../requirements/SRS.md)
- [x] Role-permission matrix — [../design/ROLE_PERMISSION_MATRIX.md](../design/ROLE_PERMISSION_MATRIX.md)
- [x] Report lifecycle specification — [../design/REPORT_LIFECYCLE.md](../design/REPORT_LIFECYCLE.md)
- [x] Initial domain model — [../design/DOMAIN_MODEL.md](../design/DOMAIN_MODEL.md)
- [x] API design outline + conventions — [../api/API_DESIGN.md](../api/API_DESIGN.md)
- [x] Implementation backlog with acceptance criteria — [BACKLOG.md](BACKLOG.md)
- [x] Architecture decisions — ADR-0002 (lifecycle), ADR-0003 (categories), ADR-0004 (roles)
- [x] Decisions D1–D4 recorded in SRS §1.5

## Phase 2 — Design System and UI Foundation (COMPLETE — approved 2026-10-13)

Delivered 2026-10-13:
- [x] Design tokens (Tanzanian palette, spec §9.1) — `web/src/styles/tokens.css`
- [x] Global styles + Inter typography — `web/src/styles/global.css`, `web/index.html`
- [x] Tailwind v4 + ESLint 9 flat config with jsx-a11y — `web/vite.config.ts`, `web/eslint.config.js`
- [x] Reusable React component library — `web/src/design-system/` (Button, Field,
  TextAreaField, Card, Badge, Modal, Toast + ToastProvider, EmptyState,
  LoadingState, ErrorState)
- [x] In-repo design gallery at `/design` with layout previews (landing, auth,
  dashboard, report wizard) — `web/src/gallery/` (design validation only)
- [x] Design system documentation — [../design/DESIGN_SYSTEM.md](../design/DESIGN_SYSTEM.md)
  (tokens, status mapping, responsive breakpoints, manual a11y checklist)
- [x] Minimal brand assets — wordmark + `web/public/favicon.svg`
- [x] Shared Flutter theme groundwork — `mobile/lib/app/theme.dart` wired into
  `mobile/lib/app.dart` (screens migrated off hardcoded colors in Phase 11)
- [x] CI: web step now runs lint + build; `npm run lint` added
- Verified: `npm run lint`, `npm run build` (tsc + vite), `flutter analyze` all green

## Phase 3 — Backend Foundation (COMPLETE — approved 2026-10-14)

Delivered 2026-10-14:
- [x] PostGIS 3.6.2 installed and verified against PostgreSQL 18.6
- [x] V1 migration fixed for real PostGIS (extension, public schema, geometry+GIST, FK cycle, updated_at, from_status nullable, varchar(36) ids)
- [x] Boot 4 deps: hibernate-spatial, spring-boot-flyway (auto-config was absent), spring-boot-webmvc-test; Testcontainers removed
- [x] Entity fixes: BaseEntity PrePersist + assigned ids; AuditLog jsonb
- [x] App boots on PG+PostGIS; Flyway V1 applies; ddl validate passes; health UP
- [x] Test matrix: H2 always; PostgresIntegrationTest gated by MTAAFIX_PG_IT - 5/5 green with PG
- [x] Committed OpenAPI spec - docs/api/openapi.json
- [x] Runbook - DEVELOPMENT.md (prerequisites, PostGIS install, config matrix, troubleshooting)
- Exit: backend builds and tests pass against PostgreSQL+PostGIS

## Phase 4 — Authentication and Authorization (COMPLETE — approved 2026-10-14)

Present: register/login/refresh/logout/me, JWT filter, BCrypt, SecurityConfig
(coarse: authenticated-vs-permitAll). Completed this phase:
- [x] Enforce [role matrix](../design/ROLE_PERMISSION_MATRIX.md) at service/controller layer
  (`@EnableMethodSecurity`; `@PreAuthorize` gates per API_DESIGN access column; 401 invalid
  token / 403 insufficient role or scope / 409 `INVALID_STATUS_TRANSITION` semantics)
- [x] Org/ownership scoping (report read/edit/comment/media, org paths on audit,
  assignments, dashboard; role resolved from DB per request so revocation and role
  changes apply immediately; report list query carries role-scoped predicates)
- [x] Authorization tests (positive+negative: `AuthorizationMatrixTest`, `AuthFlowTest`,
  `JwtTokenProviderTest`; 41/41 green on H2, 3/3 green on PostgreSQL 18 + Flyway)
- [x] Audited A-scope operations (`AuditService.record`; category create writes
  `CATEGORY_CREATED` rows, asserted by test)
- Deferrals: T1-T17 actor rules + centralized 409 state machine (Phase 5); matrix rows
  8/9/14 worker/org/settings endpoints (Phase 9); CORS config (Phase 6/16); refresh
  token invalidation on logout (Phase 16)
- Exit: authorized/unauthorized access behaves per matrix; tests green

## Phase 5 — Core Reporting Workflow (COMPLETE - approved 2026-10-14)

Delivered (see BACKLOG B-5.1–B-5.5 for acceptance criteria):
- [x] Flyway V2: status enum → 12 states; drop subject; category_id NOT NULL +
  12-category seed; duplicate_of; reason_code; uploaded_by; report_code_counters
  (applied + validated on PostgreSQL)
- [x] Central state-machine guard (T1–T17); 409 `INVALID_STATUS_TRANSITION`;
  actor-role gate (403) evaluated before state gate
- [x] Exposed comment/media endpoints + needs-info/resubmit/duplicate/start/
  complete/return/confirm/dispute/reopen; GET `/reports` list with
  status/categoryId/orgId filters + role scoping
- [x] Unique code generation `MTF-YYYY-NNNNNN` (pessimistic-lock per-year counter,
  concurrency-tested; no DB unique constraint — counter guarantees uniqueness)
- [x] Category seeding from legacy subjects; coordinate validation (±90/±180, 400);
  SRID 4326 write fix
- [x] Lifecycle tests: full journey T1→T17, forbidden transitions (409), role
  negatives (403), required fields (400), history invariant, audit assertions
- Verification: H2 49/49, PG 3/3 (V2 applied), live smoke 16/16 on port 8085
- Deferrals (documented in phase report): T8/T2r/T7r/T10r endpoints have no
  §2/§3 contract rows (not implemented); byte-level media content checks (storage
  is Phase 13); reopen-window enforcement needs `closed_at`; org-wide T14
  notifications
- Exit: ✅ core report workflow works; business rules tested
- **Gate: APPROVED by owner 2026-10-14 — Phase 6 may start**

## Phase 6 — Public Website (COMPLETE — APPROVED 2026-10-15)

Pages 1–4 (landing, how-it-works, explore, public details) per SRS §3.8 plus
`/report` info route. Public read APIs implemented (`/reports/public`,
`/reports/public/{code}`, `/categories/public`, `/stats/public`); privacy
filtering (FR-8.5): no identity/media/comments, coords rounded to 2dp pending
Phase 8 fuzzing (FR-11.4) *(fuzzing shipped in Phase 8, replaced 2dp)*. Map view
deferred to Phase 8 (owner decision) *(shipped in Phase 8)*. Media in public
payloads deferred to Phase 10/13 (owner decision). Web tests:
Vitest + Testing Library, wired to `npm run test` + CI. Real-PG smoke caught
and fixed a `lower(bytea)`/untyped-Instant-null query bug H2 could not see.

- Exit: ✅ public users can navigate the site and view only authorized public information
- **Gate: APPROVED by owner 2026-10-15 — Phase 7 may start**

## Phase 7 — Citizen Web Application (COMPLETE — APPROVED 2026-10-15)

Pages 8–13 (dashboard, 4-step report wizard, my reports, tracking, notifications,
profile) per SRS §3.2/3.6/3.7, plus login/register. Real API integration only.

Delivered 2026-10-15:
- [x] Backend citizen gaps closed: `PUT /auth/me` profile update (fullName/mobile
  only), `POST /reports/{id}/cancel` (T8: creator + SUBMITTED → REJECTED,
  `CANCELLED_BY_CITIZEN`), multipart `POST /reports/{id}/media/upload` with
  MIME allowlist + magic-byte + ≤20 MiB checks, `GET /media/{id}/content`
  authenticated streaming, `DELETE /media/{id}` (removes file too)
- [x] Real photo storage on local disk (`./uploads`, UUID names) — documented
  Phase 13 placeholder owner decision
- [x] Dashboard attention data (FR-7.4): `needsInfoCount`,
  `resolvedAwaitingConfirmCount`, `activeCount`, `attentionReports[]` (cap 5)
- [x] Web auth layer: `AuthProvider` + `useAuth()`, token pair in localStorage
  with silent refresh-once-on-401 (B-7.1 AC), `<RequireAuth>` guard, header
  user menu, `/login`, `/register`, `/forgot-password` (info-only, Phase 13)
- [x] 6 citizen pages under `/app`: dashboard (attention + stat cards), 4-step
  wizard (draft in localStorage, MTF ref on success, geolocation "use my
  location" + manual lat/lng, photo upload with 20 MiB guard), my reports
  (pagination), tracking (timeline, comments, evidence blobs, edit/cancel/
  confirm/dispute/resubmit eligibility), notifications (read / read-all),
  profile (name + mobile)
- [x] Tests: `CitizenApiTest` (11) covering profile/T8/upload authz + validation/
  dashboard attention; web `Phase7.test.tsx` (6) + `ReportWizardPage.test.tsx`
  (5) covering auth flow, wizard validation/submit, mark-read, profile,
  ownership errors
- Bug found by live smoke: report-code counter collided with pre-existing
  seeded codes → 500 on create. Fixed by bootstrapping the counter from the
  max existing `MTF-` serial at startup.
- Bug found by live smoke: oversize upload returned 500 (Tomcat default 1 MB
  limit fired before the 20 MiB check). Fixed with Spring multipart limits
  (21 MB file / 25 MB request) + `MaxUploadSizeExceededException` → 413 handler.
- Deferrals (owner decisions): password change/reset → Phase 13;
  notification prefs/privacy options → Phase 13; map view → Phase 8;
  org features → Phase 9.
- Verification: backend `mvn verify` green (73 H2 + 3/3 PG); web
  lint/typecheck/build/test green (33/33); live smoke on PG (8085) covering
  register → login → create → photo upload → tracking → profile update →
  cancel → refresh, plus 401/403/400/409/oversize paths.
- **Gate: APPROVED by owner 2026-10-15 — Phase 8 may start**

## Phase 8 — Geographic Capabilities (COMPLETE — APPROVED 2026-10-15)

Delivered 2026-10-15 (see CHANGELOG 0.9.0; BACKLOG B-8.1–B-8.4):
- [x] Geographic persistence (FR-11.1): SRID 4326 Point (existing) + GeoJSON
  representation (FR-11.5) on authorized and public DTOs
- [x] Coordinate validation (FR-11.2): ±90/±180 + null-island (0,0) → 400 on
  create/update
- [x] Spatial queries: `bbox` (ST_Intersects/ST_MakeEnvelope) + `near`
  (ST_DWithin geography, ≤500 km) on `GET /reports/public`; mutually exclusive →
  400; PostgreSQL-only native SQL (H2 covers validation)
- [x] Map interactions (Leaflet): explore list/map toggle + "Search this area" bbox
  filter; detail Location section; wizard tap-to-pick; jsdom-safe lazy loading +
  sr-only marker list
- [x] Location privacy (FR-11.4): deterministic HMAC-SHA256 fuzzing, uniform
  150–600 m offset, applied to **all** public locations (superset of "sensitive
  categories" — disclosed at gate); replaces Phase 6's 2dp rounding
- [x] Indexes (FR-11.3): GIST verified for geometry; new V3 GIST on
  `(location::geography)` for radius search — `EXPLAIN` confirms both use index scans
- [x] Tests: LocationGeneralizerTest (11), PG IT bbox/near + sorted-Pageable path,
  public/citizen API fuzz assertions; web map tests — 90/90 backend + 36/36 web;
  live smoke green (fuzz determinism, spatial filters, 400 paths)
- Bugs found & fixed: native-query sorted-`Pageable` → invalid `order by reportedat`
  (500); radius search missing geography index (seq scan)
- **Exit: ✅ geographic features behave correctly and respect privacy boundaries**
- **Gate: APPROVED by owner 2026-10-15 — Phase 9 may start**

## Phase 9 — Administration and Moderation (COMPLETE — APPROVED 2026-10-17)

Pages 14–24. Delivered 2026-10-17 (see CHANGELOG 0.10.0; BACKLOG B-9.1–B-9.5):
- [x] Membership model (D-P9.1): `organization_members` (Flyway V4) backfills and
  replaces `users.organisation_id`; single org per user; org-role grant requires
  membership; role change away from an org role removes the membership
- [x] Administration APIs (FR-12.1/12.2): user search/role/active (audited, self-change
  400, last-active-admin 409), organisation CRUD + members (audited; inactive org →
  409 on assignment), category activate/order (public hides inactive; audited)
- [x] Moderation/ops surfaces: staff report list `q`/`assignment` filters (P15),
  `GET /dashboard/admin` panels (P14), filtered audit with `actorEmail`/`outcome` +
  date filters (P22), PostgreSQL-native `GET /analytics/summary` with
  date/category/status/orgId/bbox filters (P23), read-only `GET /settings` (P24, D-P9.2)
- [x] Admin web section: 11 role-guarded pages under `/admin` (P14–P24) + staff entry
  in the account menu
- [x] Tests: backend 105/105 (H2 + live PG), web 47/47; live smoke PG :8085 68/68 —
  smoke caught 3 real bugs (PG null-`Instant` audit filter 500 → `cast(...)` fix;
  unknown path 500 → 404; malformed body 500 → 400), all fixed and re-verified
- [x] Matrix §2.3 revisited: `MODERATOR` assign stays **N** (D-P9.3, kept)
- **Exit: ✅ administration and moderation features behave correctly and respect role
  boundaries**
- **Gate: APPROVED by owner 2026-10-17 — Phase 10 may start**

## Phase 10 — Field-Worker Web Application (COMPLETE — APPROVED 2026-10-17)

Pages 25–26. Delivered 2026-10-17 (see CHANGELOG 0.11.0; BACKLOG B-10.1–B-10.4):
- [x] Assignment visibility (FR-4.2/4.3): worker dashboard `/worker` wired to
  `GET /assignments/me` with New / In progress / Completed groups, counts, empty
  state, "My tasks" menu entry, `FIELD_WORKER` route guards
- [x] Task details (P26): instructions, location + map link, status-driven actions
  (start ASSIGNED→IN_PROGRESS, completion claim IN_PROGRESS→RESOLUTION_REVIEW with
  T11 photo requirement surfaced from the 400), evidence upload + media grid,
  progress comments, status history, read-only at RESOLUTION_REVIEW
- [x] Reviewer return (FR-4.5, T12): admin report details "Return to work" action
  → `POST /reports/{id}/return` (comment required → 400 when blank)
- [x] Tests: backend 106/106 (new worker-scoping test: unassigned worker 403 on
  GET/start/complete/comments, self-scoped `/assignments/me`); `PublicApiTest`
  flaky-coordinate assertions switched to ≥100 m haversine distance (root-caused
  HMAC-fuzz flake); web 57/57 (+10 Phase 10); lint/typecheck/build green
- [x] Live smoke PG :8085 50/50 — full worker journey incl. multipart evidence
  upload (`FF D8 FF`), assignee media read, T11/T12 negative paths, role gates,
  resolve → confirm → CLOSED; 1 finding: non-JSON content type → 500
  (`HttpMediaTypeNotSupportedException`, same documented-gap family as 405 —
  deferred outside Phase 10)
- **Exit: ✅ field workers can manage their assigned tasks from the web without
  bypassing the lifecycle**
- **Gate: APPROVED by owner 2026-10-17 — Phase 11 may start**

## Phase 11 — Flutter Application Foundation (COMPLETE — APPROVED 2026-10-17)

Delivered 2026-10-17 (see CHANGELOG 0.12.0; BACKLOG B-11.1–B-11.5):
- [x] Project inspection/creation — `flutter create --org tz.co.mtaafix`
  completed project scaffolding: Android Gradle (`.kts`) build + manifest,
  iOS `Runner/` project (build requires macOS), `analysis_options.yaml`,
  mobile `README.md`
- [x] Navigation — central named-route table with guards (protected → sign-in,
  signed-in redirected away from auth surfaces, unknown → `NotFoundScreen`);
  splash runs the real session restore before redirecting
- [x] Mobile theme + shared components — all screens migrated off hardcoded
  `Colors.orange`; `LoadingView`, `FeedbackView`, `showAppSnack`
- [x] API integration — `ApiClient` (dart-define base URL, 15 s timeout,
  bearer auth, single refresh-and-retry with rotation, typed
  `ApiException`, multipart upload)
- [x] Authentication state — `AuthController`/`AuthScope` restore →
  `GET /auth/me`, login, register→auto-login, logout; network failure during
  restore keeps tokens, rejected credentials clear them
- [x] Error handling — network/401/403 typed errors, empty-body tolerance,
  user-facing messages; splash failure paths
- [x] Android/iOS requirements — dev cleartext HTTP, valid `configChanges`
  (removed invalid `displaySize|displayCutout` flags that failed AAPT), NDK
  28.2.13676358 installed; iOS usage descriptions + ATS dev exception
- [x] Initial tests — `flutter analyze` 0 issues; `flutter test` 29/29;
  `flutter build apk --debug` → `app-debug.apk`
- Gaps: report form/media/map submission are themed placeholders (Phase 12);
  forgot-password → Phase 13; iOS build unexecutable on Windows (B-X.4)
- **Exit: ✅ the application builds and provides a reliable foundation for mobile features**
- **Gate: APPROVED by owner 2026-10-17 — Phase 12 may start**

## Phase 12 — Flutter Citizen Features (COMPLETE - APPROVED 2026-10-18)

Pages 27–36 (onboarding, home, creation with camera/GPS, map, notifications,
profile). Permission-denial handling; Android/iOS verification.

- Shared layer: `paged.dart`, `formatters.dart`, `status_theme.dart`
  (`StatusChip`), `report.dart`/`notification_item.dart` domain models,
  `ReportsApi` (stats, CRUD, filters, lifecycle actions, comments, multipart
  media, public bbox, categories) and `NotificationsApi`
- Screens: onboarding (persisted flag), home dashboard with flat
  `/dashboard/me` stat mapping, report list with filters + pagination,
  creation/edit (category dropdown, geolocator + permission denial,
  `image_picker` evidence with MIME/20 MB checks, map-picker pin), report
  detail (timeline, comments, status-gated cancel/edit/confirm/dispute/
  resubmit), community map (flutter_map, viewport bbox, markers, filters),
  notifications feed, profile with edit dialog and sign-out
- Shell: `MainShellScreen` with IndexedStack + NavigationBar; tabs refetch
  on activation via a `MainShellTab` inherited notifier (stale empty states
  after submission were caught in emulator verification)
- Defects found & fixed during verification: missing
  `uses-material-design` (tofu icons on device), bare-JSON-array response
  wrapping in `ApiClient`, pre-camera map bbox fetch, dashboard stats
  nested-vs-flat shape, first-mount-only tab data
- Gate: `flutter analyze` 0 issues, `flutter test` **111/111**,
  `flutter build apk --debug` green, live-backend emulator run (register →
  login → create report `MTF-2026-000032` → map marker → notifications →
  profile); iOS build still macOS-only (B-X.4)
- Gaps: FR-2.4 report date filter unsupported by backend (status/category/q
  only), no change-password endpoint (Phase 13), password reset (Phase 13)
- **Gate: APPROVED by owner 2026-10-18 — Phase 13 may start**

## Phase 13 — Notifications and Media (COMPLETE — APPROVED 2026-10-18)

Per master spec Phase 13 (notifications, preferences, delivery separation,
media hardening, B-X.1 password reset, B-X.2 delivery separation):

**Backend**
- V5 migration: `notification_preferences`, `notification_deliveries`,
  `password_reset_tokens` (SHA-256 hash, single-use, 30 min)
- `NotificationController`: `GET /notifications/unread-count`,
  `GET/PUT /notifications/preferences` (`emailEnabled`, `pushEnabled`,
  `pushAvailable`)
- `AuthController`: `POST /auth/password/{forgot,reset,change}` — identical
  non-disclosing 202s; password change preserves the session
- `NotificationDeliveryService` + `EmailRetryScheduler`: IN_APP always
  DELIVERED; EMAIL PENDING→SENT/FAILED/SKIPPED, retried ≤
  `notifications.email.max-attempts` (3); kill-switch
  `notifications.email.enabled` default **false** (no SMTP provider)
- `common/email` Mail sender behind the kill-switch; push **not configured**
  (`pushAvailable=false`, no FCM/APNs) — owner decision flagged
- Media hardening: MIME allowlist + magic-byte content check (pre-existing)
  now paired with config-driven caps — 20 MiB
  (`media.max-file-size-bytes`), new 10 attachments/report
  (`media.max-per-report`) — enforced on metadata and multipart paths;
  authorized `GET /media/{id}/content`; **public unauthenticated media NOT
  implemented** (owner decision — do not open unilaterally)
- Delivery wired at the single `NotificationService.notifyUser` funnel →
  `dispatch()` for all lifecycle events (failure never fails the workflow);
  dispute now also notifies the report owner; cancel emits
  `REPORT_CANCELLED`; multipart limits for 413

**Web** — NotificationBell unread badge in the header; forgot/reset
password pages (non-disclosing confirmation, token from `?token=`); profile
notification-preferences card (auto-save, push disabled with explanation) +
change-password card; notifications pagination nav; admin/worker media opened
as authenticated blobs (no long-lived URLs); wizard multi-photo (5) with
per-file validation + partial-failure toast; client-side cap/type/size checks
on tracking/worker upload inputs.

**Mobile** — `UnreadCountScope` + Alerts-tab badge (refresh on shell mount,
feed mark/open); `NotificationPreferencesScreen`; forgot/reset/change-password
screens (routes guarded; reset reads `token` route args); profile rows wired;
report-detail evidence section (add photo via picker with MIME/20 MB/10-photo
checks, remove with confirm → `DELETE /media/{id}`, `canEditMedia` gate:
not resolved/rejected/duplicate/closed).

**Gates** — backend H2 124/124 + PG IT 7/7 (V5 applied); web **68/68**
(+11 Phase13), lint/tsc/build green; mobile `flutter analyze` 0 issues,
`flutter test` **134/134** (+23); live backend on :8085 exercised:
forgot (known/unknown identical 202, token row), reset E2E (bogus→400,
planted token→200, reuse→400 "Invalid or expired", old password→401),
change-password→200, unread-count/prefs GET+PUT, media PNG 200 / .txt 400 /
magic-byte 400 / 21 MB 400 / 11th attachment 400 / DELETE 204, notifications
on submit+cancel with unread increment, delivery rows IN_APP DELIVERED +
EMAIL SKIPPED ("Email channel disabled").

**Gaps / decisions to flag:** email mirrors in-app content behind kill-switch
(enabled=false by default); push unavailable (no provider); JWTs not revoked
after reset (documented decision); public media endpoint absent (owner
decision); backend attach/delete has no report-status guard (active-status
gate enforced in mobile UI only); FR-2.4 date filter still backend-unsupported;
iOS build macOS-only (B-X.4).

**Gate: APPROVED by owner 2026-10-18 — Phase 14 may start**

## Phase 14 — Offline Capabilities (COMPLETE — APPROVED 2026-10-21)

Master spec Phase 14 (lines 2047–2066) under the owner-approved scope:
Option B backend idempotency; report-form draft storage; pending queue with
foreground retry; categories cache; sign-out local-state cleanup. Offline
browsing/map and background sync deferred (approved); mobile-only beyond the
web's pre-existing `mtaafix.reportDraft.v1` key.

- **Assessment**: approved scope recorded — exit criterion = unsent data is
  never presented as submitted
- **Backend**: Flyway **V6** `report_idem_keys` (24 h TTL, hourly purge);
  `Idempotency-Key` on `POST /api/v1/reports` → 400 invalid format, 201
  first use, 201 **stored replay** (same id, no duplicate), 409
  `IDEMPOTENCY_CONFLICT`, 409 `IDEMPOTENCY_IN_PROGRESS` (stale intent 2 min);
  REQUIRES_NEW transactions; private `ObjectMapper` + `hashOf`
- **Mobile**: `features/offline/` — `OfflineController` (single-flight
  flush), `Zone.root`-wrapped atomic `OfflineStorage` (schema 1, graceful
  degradation), `DraftStore`/`OutboxStore`/`CategoryCache`,
  `ConnectivityService`, `OfflineScope`, `PendingSubmissionsScreen`; form
  debounced autosave + restore offer, offline enqueue + failed-entry
  prefill re-queue, photo copy-in at pick, one idempotency key per form
  session; shell offline strip, pending banner with just-submitted reload,
  `clearAll()` on sign-out, guarded `/pending-submissions` route
- **Connectivity transitions tested**: offline→online auto-flush, offline
  no-op flush, offline hint disables retry, banner appear/disappear
- **Tests/evidence**: backend H2 **133/133** (`IdempotencyTest` 8), PG
  **8/8**, jar + OpenAPI; mobile **167/167** (+33), `flutter analyze` 0;
  live battery :8085 → replay same id / 409 conflict / 400 invalid key;
  two product fixes surfaced by tests (post-dispose notify guard,
  empty-`categoryId` draft rejection)
- **Docs**: `docs/design/OFFLINE_CAPABILITIES.md`; CHANGELOG 0.15.0;
  API_DESIGN `POST /reports` idempotency row; SRS FR-2.11; BACKLOG deferrals
- Deferrals (approved): offline report browsing, offline map tiles,
  background sync (foreground-only), iOS build unexecutable on Windows
  (B-X.4)

**Gate: APPROVED by owner 2026-10-21 — Phase 15 may start**

## Phase 15 — Quality Assurance (COMPLETE — APPROVED 2026-10-21)

Master spec Phase 15 (lines 2069–2089): validate the integrated system.

- **Suites (clean runs for the record)**: backend `mvn test` **133/133** H2
  (PG class auto-skips without env); PG integration `mvn test
  -Dtest=PostgresIntegrationTest` **8/8** with `MTAAFIX_PG_IT=true` +
  local PostgreSQL (Flyway V6, PostGIS, spatial queries, idempotency);
  web `eslint .` 0 / `tsc --noEmit` 0 / `vitest run` **68/68** (10 files)
  / `vite build` green; Flutter `flutter analyze` 0 / `flutter test`
  **170/170** (+3)
- **Gap-fill tests** (new `mobile/test/phase15_quality_test.dart`):
  citizen critical journey (sign in → create → detail → back → refresh →
  report appears in My Reports), critical-screens no-overflow sweep at
  320×640, accessible semantics labels (login fields, NavigationBar
  destinations, form field labels, refresh tooltip)
- **Responsive fixes found by the new test**: login auth actions Row →
  Wrap; home `_SectionHeader` title → Expanded + ellipsis; report-form
  category dropdown `isExpanded: true`; photo attach buttons Row → Wrap
- **Map/test infrastructure**: `MapScreen` lazy `_mapBooted` tile mount on
  first activation; `core/map/app_tile_provider.dart` — `appTileProvider()`
  disables the disk-cache worker isolate under `FLUTTER_TEST` only
  (the isolate stalled the fake-async runner); production disk cache
  unchanged; wired into map screen, report detail, map picker
- **Role boundaries + lifecycle, live**: battery **63/63** on :8085 — 403
  matrix (citizen/mod/worker vs admin/moderation endpoints), 401
  anonymous, FR-5.4 org-role-without-membership → 400, 409 state gate
  (mod /resolve while SUBMITTED), 403 ownership guard (worker /start
  unassigned), two full lifecycle chains (verify → assign → start →
  complete → resolve → close → reopen, then citizen confirm)
- **Accessibility/responsive verification**: mobile semantics assertions +
  320×640 sweep; web jsx-a11y clean + documented audit (single structural
  breakpoint, wrapping header nav; jsdom cannot assert CSS-driven
  visibility — limitation recorded)
- **Defect review**: five responsive overflow defects fixed this phase;
  carried defects + Phase 14 deferrals documented with mitigation plan in
  BACKLOG ("Phase 15 known defects + mitigation plan")

**Gate: APPROVED by owner 2026-10-21 — Phase 16 may start**

## Phase 16 — Security Review and Hardening (COMPLETE - APPROVED 2026-10-22)

Master spec Phase 16: structured security review across the ten mandated
areas with remediation of flagged findings. Owner-approved scope
(2026-10-22): fix Critical/High + two cheap Mediums; implement rate
limiting; JWT revocation via `token_version` (reset and change); defer
offline-storage encryption to Phase 20; pragmatic dependency review; review
record at `docs/standards/SECURITY_REVIEW.md`.

- **Review record**: `docs/standards/SECURITY_REVIEW.md` — findings F1-F12
  with severities, evidence, and dispositions across all ten areas; deferral
  table; backup/restore runbook (documentation-level remediation)
- **D1/D2 error contracts**: explicit 415 (`UNSUPPORTED_MEDIA_TYPE`) and 405
  (`METHOD_NOT_ALLOWED`) handlers; the Phase 15 500-evidence came from a
  stale Oct-20 jar — rebuilt artifact re-probed live (415/405/400 ApiError)
- **D3 JWT revocation**: Flyway **V7** `users.token_version`; `tv` claim on
  both token types; filter + refresh-flow checks; reset and change bump the
  version; `/auth/password/change` returns a rotated pair that web and
  mobile persist (contract updated in both clients + tests)
- **D4 media gate**: `assertMediaEditable` rejects attach/delete with 409 on
  RESOLVED/REJECTED/DUPLICATE/CLOSED (server-side mirror of the mobile rule)
- **Rate limiting** (SECURITY_STANDARDS gap): `RateLimitFilter` per-IP fixed
  windows on `POST /api/v1/auth/**` (30/min) and `POST /reports` (20/min);
  429 + Retry-After + ApiError; config via `RATE_LIMIT_*`; proxy-IP caveat
  documented for Phase 18
- **Secrets/deployment**: `ProdSecretsGuard` refuses shipped defaults under
  the `prod` profile; `.env.example` annotated; health no longer false-DOWN
  without SMTP (`management.health.mail.enabled` mirrors the feature flag)
- **Suites**: backend `mvn test` **144/144** H2 (+11 tests); PG IT **8/8**
  (`MTAAFIX_PG_IT=true`, V7 applied); web `tsc` 0 / `vitest` **68/68**;
  Flutter `flutter test` **171/171**
- **Live battery**: `backend/target/battery-p16.ps1` **23/23** on :8085
  (rebuilt jar) — regression + D1/D2 contracts + D3 revocation E2E + D4
  gate + D6 429-at-31 with Retry-After + health UP

**Gate: APPROVED by owner 2026-10-22 — Phase 17 may start**

## Phase 17 — Performance and Reliability (COMPLETE - APPROVED 2026-10-22)

Query/index review, pagination, API measurement, map rendering, caching where
justified (NFR-7 targets). Owner-approved scope Q1–Q4 (2026-10-22): Caffeine
caching; paginate user-growable lists (assignments, org members) +
notifications cap; ~5,000-report local seed for measurement; workstreams A–H.

- **N+1 elimination**: `ReportDto.listItem` lean list factory (empty
  statusHistory/comments, batch-fetched media); `findByScope` fetch-joins
  assignment; media batch query; assignment service → scalar DTOs
- **Indexes (Flyway V8)**: composite btree on `reports (status|organisation|
  category|user, reported_at DESC)` + `pg_trgm` GIN on
  `lower(code/title/description)` for the staff `q` filter; PG IT assertions
- **Pagination**: `GET /assignments/me` and `GET /organizations/{id}/members`
  → `PagedResponse` (size ≤ 100); `GET /notifications` size clamped; web
  clients unwrap `.content`; reference data (categories/org list) documented
  bounded exemptions
- **Caching**: Caffeine `expireAfterWrite=60s` — `categories` (evicted on
  admin writes), `publicStats`, `adminDashboard`; user dashboard uncached by
  design (freshness); `CachingTest` proves hit/evict via `@MockitoSpyBean`
- **Map rendering**: web ExplorePage separate map feed (size=100, own
  loading/error states); mobile `publicList` default 50 → 100; marker
  clustering deferred
- **Timing**: `RequestTimingFilter` (DEBUG per request, WARN > 500 ms,
  `timing.warn-threshold-ms`)
- **Activity bucketing**: dashboard 14-day series now `GROUP BY … AT TIME
  ZONE 'Africa/Dar_es_Salaam'::date` in SQL (Java fallback kept for H2)
- **Measurement**: `scripts/seed-perf.sql` (5,000 reports) +
  `scripts/measure-perf.ps1` (5 warmup + 30 iterations, p50/p95/max) —
  **11/11 endpoints p95 < 500 ms → NFR-7 MET**; worst p95 397.2 ms; review at
  `docs/standards/PERFORMANCE_REVIEW.md`
- **Suites**: backend `mvn test` **148/148** H2 (+CachingTest); PG IT **9/9**
  (V8 assertions); web `tsc` 0 / `vitest` **68/68**; Flutter **171/171**
- **Deferred (owner-endorsed)**: marker clustering, Micrometer/Prometheus
  (Phase 19), Redis, user-dashboard caching, reference-data pagination,
  search/bbox caching

**Gate: APPROVED by owner 2026-10-22 — Phase 18 may start**

## Phase 18 — Deployment and CI/CD (COMPLETE - APPROVED 2026-10-22)

Master spec Phase 18: production configuration, deploy/rollback
documentation, CI/CD pipeline, and secret protection. Owner-approved
scope (2026-10-22): Q1 deployment = readiness only (no remote host);
Q2 Docker = skip + document rationale (no container runtime here);
Q3 CI depth = all four (PG service + IT, flutter test, release workflow,
gitleaks); Q4 git = commit Phases 16-18, owner pushes.

- **Prod profile** (`application-prod.yml`):
  `server.forward-headers-strategy: framework` (true client IP for the
  Phase 16 rate limiter - B-16.4), explicit `ddl-auto: validate`,
  actuator health-only (`show-details: never`); secrets stay in the
  environment, guarded by `ProdSecretsGuard`
- **Tests**: `ForwardedIpRateLimitTest` (XFF bucket isolation,
  ForwardedHeaderFilter at HIGHEST_PRECEDENCE) + `ProdSecretsGuardTest`
  x6 (defaults/blanks/length/profile scope)
- **Runbook**: `docs/operations/DEPLOYMENT.md` - env/secrets, build,
  prod boot, nginx X-Forwarded-* example, CI/release, rollback, scope
  decisions (Docker skipped, backups Phase 19)
- **CI** (`ci.yml`): backend job with `postgis/postgis:18-3.6` service
  (`MTAAFIX_PG_IT=true` + `JWT_SECRET` >=32), web lint/test/build,
  mobile analyze + test, gitleaks; concurrency cancel + dispatch
- **Release** (`release.yml`): tag `v*` -> jar (tests rerun) +
  `web-dist.zip` attached to the GitHub Release
- **`.env.example`**: rate-limit var names corrected to
  `RATE_LIMIT_*_PER_MINUTE`, `GEO_FUZZ_SECRET` + timing var added
- **Live drill**: refusal verified (JWT default + guard message, exit
  1); fresh scratch DB boot: Flyway V1-V8, validate, health UP ~10 s,
  public API 200; suites 155/155 backend (146 H2 + 9 PG) / 68 web /
  171 mobile

**Gate: APPROVED by owner 2026-10-22 — Phase 19 may start**

## Phase 19 — Observability and Operations (NOT STARTED)

Structured logging, metrics, error reporting, alerts, backup/restore procedures,
troubleshooting docs.

## Phase 20 — Beta Release (NOT STARTED)

Critical-workflow verification, test data, known-limitations doc, structured
feedback, release criteria.

## Phase 21 — Approved Advanced Features (NOT STARTED)

Analytics, Swahili, multi-org improvements, duplicate detection, AI assistance
(never silently replacing human review), integrations — each with own ACs.

## Phase 22 — Production Readiness and Version 1.0 (NOT STARTED)

Defect closure, security/privacy verification, backup/restore verification,
documentation finalization, release notes.

## Phase 23 — Post-release Development (NOT STARTED)

Feedback- and data-driven iteration under the same approval-gated process.

---

## Delivery Targets (updated 2026-10-22)

| Phase | Status |
| ----- | ------ |
| 0 | Complete |
| 1 | Complete (approved) |
| 2 | Complete (approved) |
| 3 | Complete (approved) |
| 4 | Complete (approved) |
| 5 | Complete (approved) |
| 6 | Complete (approved) |
| 7 | Complete (approved) |
| 8 | Complete (approved 2026-10-15) |
| 9 | Complete (approved 2026-10-17) |
| 10 | Complete (approved 2026-10-17) |
| 11 | Complete (approved 2026-10-17) |
| 12 | Complete (approved 2026-10-18) |
| 13 | Complete (approved 2026-10-18) |
| 14 | Complete (approved 2026-10-21) |
| 15 | Complete (approved 2026-10-21) |
| 16 | Complete (approved 2026-10-22) |
| 17 | Complete (approved 2026-10-22) |
| 18 | Complete (approved 2026-10-22) |
| 19–23 | Not started |
