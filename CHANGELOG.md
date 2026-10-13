# Changelog

All notable changes to this project will be documented in this file.

Statuses below reflect what was **verified in this repository** (compile, tests,
analyzer, or file presence) — not intentions.

## [0.2.0] — 2026-10-13
### Phase 1 — Requirements & system design (docs only; no code changes)
Established the requirements baseline before further implementation, per master
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
