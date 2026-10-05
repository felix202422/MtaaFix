# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-10-02

### Added

#### Backend
- **Stack upgrade:** Spring Boot 4.1.1 on Java 25 with Spring Security 7,
  Spring Framework 7, Hibernate 7 and Jackson 3 (modular Boot 4 starters).
- Assignment module: REST API for coursework CRUD, student submissions and
  per-submission grading (`/api/assignments`).
- Timetable module: REST API for period scheduling with class- and teacher-day
  views (`/api/timetable`).
- Subject module: REST API for curriculum subjects (`/api/subjects`).
- Report/export module: CSV (OpenCSV), Excel (Apache POI) and PDF (iText)
  exports for students, attendance, payments and exam results, plus per-student
  report-card PDFs and CSV student import (`/api/reports`).
- Role dashboards: dedicated aggregate endpoints for teacher, student and
  parent portals (`/api/dashboard/{role}`).
- Admin analytics charts: gender distribution, attendance distribution and
  trend, monthly revenue, class sizes and grade distribution
  (`/api/dashboard/admin` → `charts`).
- Remember-me authentication: extendable refresh-token lifetime (30 days).
- Hibernate lazy-proxy serialization fixed via `jackson-datatype-hibernate7`.
- All secrets (database password, JWT secret, mail credentials, CORS origins)
  moved to environment variables with safe local defaults.

#### Frontend
- Stack: React 19.3, Vite 8, React Router 7, Axios 1.x, Recharts 3,
  Material UI 9, Lucide icons.
- Recharts admin dashboard: area, pie, donut, bar and line charts wired to the
  live `/api/dashboard/admin` charts payload.
- Role-based portals: Teacher, Student and Parent dashboard views with
  role-specific statistics and child cards.
- Role-based access control: route guards (`RoleRoute`) and a role-filtered
  sidebar driven by a central permissions map.
- New pages: Assignments (CRUD + status), Timetable (class/day filters),
  Subjects (CRUD).
- "Remember me" checkbox on the login screen.
- API base URL configurable via `VITE_API_URL`.

#### Project
- Community files: LICENSE (MIT), CONTRIBUTING.md, CODE_OF_CONDUCT.md,
  SECURITY.md, CHANGELOG.md.
- CI workflow: backend build + frontend build on push/PR.
- `.env.example` documenting every environment variable.
- Docker Compose reads all credentials from environment.
- docs/: architecture, API overview and environment setup guides.

### Changed
- `spring-boot-starter-web` → `spring-boot-starter-webmvc` and
  `spring-boot-starter-aop` → `spring-boot-starter-aspectj` (Boot 4 renames).
- SpringDoc OpenAPI upgraded to the 3.x line for Spring Boot 4.
- Jackson 2 `ObjectMapper` usage replaced with Jackson 3 `JsonMapper`
  (`tools.jackson`).

### Fixed
- PostgreSQL `GROUP BY` errors in class-size and daily-attendance aggregates.
- Native-query `::` casts replaced with `CAST(... AS TEXT)` for Hibernate 7.
- iText dependency resolution (explicit kernel/layout/io artifacts).
