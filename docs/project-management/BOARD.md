# MtaaFix — Local Task Board

Local tracking board (GitHub `gh` CLI unavailable; revisit if auth is fixed).
Status values: `TODO` / `IN PROGRESS` / `DONE` / `BLOCKED`.
Phase numbering: master spec Phases 0–23 (decision D1).
Last updated: 2026-10-17.

## Current phase: Phase 9 APPROVED (2026-10-17) — next: Phase 10 (not started)

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

## Phase 6 — Public website (COMPLETE — APPROVED 2026-10-15)

| Task | Status | Notes |
|------|--------|-------|
| Public APIs | DONE | `/reports/public`, `/reports/public/{code}`, `/categories/public`, `/stats/public`; 2dp coord rounding; CORS |
| Pages 1–4 + `/report` info + 404 | DONE | Landing, how-it-works, explore, detail; Router + Layout + LinkButton |
| Web tests | DONE | Vitest + Testing Library, 22/22; wired to `npm run test` + CI |
| Privacy review | DONE | No identity/media/comments; high-level timeline only |
| Live smoke | DONE | PG backend + Vite; **caught real `lower(bytea)`/untyped-Instant-null bug H2 missed** — fixed |
| Deferrals | NOTED | Map → Phase 8; public media → Phase 10/13; fuzzing → Phase 8 (owner decisions) |

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
| Flutter theme groundwork | DONE | `mobile/lib/app/theme.dart` wired in; `flutter analyze` green; Phase 11 migrates screens |
| CI lint step | DONE | `npm run lint` in web CI step |
| Verify lint + build + flutter | DONE | `eslint .` 0 issues; `tsc` + `vite build` green; analyze clean |
| Phase 2 report + approval gate | DONE | Approved by owner 2026-10-13 |

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

## Next phases

| Phase | Status | Notes |
|-------|--------|-------|
| Phase 4 - AuthN/Z | Complete (approved 2026-10-14) | Matrix enforcement + scoping + audit + tests done |
| Phase 5 - Core reporting workflow | Complete (approved 2026-10-14) | 12-state machine, unique codes, comments/media, lifecycle endpoints; deferrals noted in report |
| Phase 6 - Public website | Complete (approved 2026-10-15) | 4 public pages + public APIs + privacy; real-PG smoke caught/fixed H2-masked query bug |
| Phase 7 - Citizen web | Complete (approved 2026-10-15) | Auth layer + 6 citizen pages; profile/T8-cancel/multipart/dashboard-attention backend; live smoke caught 2 real bugs (code counter, oversize 500) — both fixed |
| Phase 8 - Geographic capabilities | Complete (approved 2026-10-15) | PostGIS bbox/near queries, Leaflet map, HMAC coord fuzzing, V3 geography index; live smoke + IT hardening caught 1 real bug (sorted Pageable → 500) — fixed |
| Phase 9 - Administration and moderation | Complete (approved 2026-10-17) | Membership V4, user/org/category admin, filtered audit, admin dashboard + analytics + read-only settings, 11 admin pages; live smoke caught 3 real bugs (PG audit filter 500, unknown-path 500, malformed-body 500) — all fixed |

## Environment notes

- Java 25.0.4, Maven 3.9.16, Node 24.13.1, Flutter 3.47.1, PostgreSQL 18 running
- PostGIS 3.6.2 installed for PG 18 (OSGeo bundle overlay, Phase 3)
- No Docker; no Testcontainers
- `gh` CLI token invalid; git push works via Windows Credential Manager
- Old home-dir repo untouched at `C:\Users\PC\.git` — never push from it
