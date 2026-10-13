# Changelog

All notable changes to this project will be documented in this file.

Statuses below reflect what was **verified in this repository** (compile, tests,
analyzer, or file presence) — not intentions.

## [0.4.0] — 2026-10-14
### Phase 3 — Backend foundation completion
Awaiting owner approval. Verified the backend against real PostgreSQL 18.6 +
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
