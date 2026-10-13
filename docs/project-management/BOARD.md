# MtaaFix — Local Task Board

Local tracking board (GitHub `gh` CLI unavailable; revisit if auth is fixed).
Status values: `TODO` / `IN PROGRESS` / `DONE` / `BLOCKED`.
Phase numbering: master spec Phases 0–23 (decision D1).
Last updated: 2026-10-13.

## Current phase: Phase 2 APPROVED (2026-10-13) — Phase 3 IN PROGRESS (started 2026-10-13)

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

## Current phase: Phase 3 APPROVED (2026-10-14) — next: Phase 4 (AuthN/Z), awaiting start authorization

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
| H2 vs PG test matrix | DONE | 5/5 green with MTAAXFIX_PG_IT; PG suite skips cleanly without |
| Export OpenAPI spec | DONE | `docs/api/openapi.json` via OpenApiExportTest |
| DEVELOPMENT.md runbook | DONE | |
| Phase 3 report + approval gate | DONE | Approved by owner 2026-10-14; docs flipped to APPROVED |

## Next phases

| Phase | Status | Notes |
|-------|--------|-------|
| Phase 4 — AuthN/Z | READY | Role-matrix enforcement + tests (BACKLOG B-4.x) |
| Phase 5 — Core reporting workflow | BLOCKED | State machine migration + endpoints (BACKLOG B-5.x) |
| Phase 6–7 — Public site + citizen web | BLOCKED | Depend on Phases 2, 4, 5 |

## Environment notes

- Java 25.0.4, Maven 3.9.16, Node 24.13.1, Flutter 3.47.1, PostgreSQL 18 running
- PostGIS not installed (approved for Phase 3)
- No Docker; no Testcontainers
- `gh` CLI token invalid; git push works via Windows Credential Manager
- Old home-dir repo untouched at `C:\Users\PC\.git` — never push from it
