# Roadmap — MtaaFix

> Phase numbering follows the **master specification** (Phases 0–23) per decision D1
> (2026-10-13). Status reflects **verified repository state** (last audited 2026-10-14),
> not intent. **MVP = Phases 0–7** (approved). Every phase ends with an approval gate
> (master spec Part 16). Local tracking: [BOARD.md](BOARD.md) ·
> Work items: [BACKLOG.md](BACKLOG.md) · Requirements: [../requirements/SRS.md](../requirements/SRS.md)

## Version History

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

## Phase 12 — Flutter Citizen Features (NOT STARTED)

Pages 27–36 (onboarding, home, creation with camera/GPS, map, notifications,
profile). Permission-denial handling; Android/iOS verification.

## Phase 13 — Notifications and Media (PARTIAL)

Present: in-app notifications on events; list/read/read-all.
Remaining: delivery-state separation, email channel, push (FCM/APNs), preferences,
media validation hardening, upload limits, password-reset flow (FR-1.7).

## Phase 14 — Offline Capabilities (NOT STARTED)

Drafts, retry, sync, conflict handling; never misrepresent unsent data as submitted.

## Phase 15 — Quality Assurance (NOT STARTED)

Full backend/web/mobile/integration/E2E suite per Part 13; role-boundary and
lifecycle-transition test passes; accessibility + responsive audits.

## Phase 16 — Security Hardening (NOT STARTED)

Structured review per Part 16 (authn/z, validation, uploads, secrets, dependencies,
logging/privacy, rate limiting, CORS, backups).

## Phase 17 — Performance and Reliability (NOT STARTED)

Query/index review, pagination, API measurement, map rendering, caching where
justified (NFR-7 targets).

## Phase 18 — Deployment and CI/CD (NOT STARTED)

Docker (owner environment permitting), production config, migrations in deploy,
health checks, rollback docs, secret protection.

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

## Delivery Targets (updated 2026-10-17)

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
| 12 | Not started |
| 13 | Partial (in-app only) |
| 14–23 | Not started |
