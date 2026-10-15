# MtaaFix — Local Task Board

Local tracking board (GitHub `gh` CLI unavailable; revisit if auth is fixed).
Status values: `TODO` / `IN PROGRESS` / `DONE` / `BLOCKED`.
Phase numbering: master spec Phases 0–23 (decision D1).
Last updated: 2026-10-15.

## Current phase: Phase 6 COMPLETE (pending approval 2026-10-15) — stop here for owner gate

## Phase 6 — Public website (COMPLETE — pending approval)

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
| Phase 6 - Public website | Complete (pending approval 2026-10-15) | 4 public pages + public APIs + privacy; real-PG smoke caught/fixed H2-masked query bug |
| Phase 7 - Citizen web | READY | Blocked only by Phase 6 gate |

## Environment notes

- Java 25.0.4, Maven 3.9.16, Node 24.13.1, Flutter 3.47.1, PostgreSQL 18 running
- PostGIS 3.6.2 installed for PG 18 (OSGeo bundle overlay, Phase 3)
- No Docker; no Testcontainers
- `gh` CLI token invalid; git push works via Windows Credential Manager
- Old home-dir repo untouched at `C:\Users\PC\.git` — never push from it
