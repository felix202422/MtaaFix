# Changelog

All notable changes to this project will be documented in this file.

Statuses below reflect what was **verified in this repository** (compile, tests,
analyzer, or file presence) — not intentions.

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
