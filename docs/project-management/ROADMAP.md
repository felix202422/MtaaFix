# Roadmap — MtaaFix

Status is based on **verified repository state** (last audited 2026-10-13),
not intent. MVP scope for this effort: **Phases 0–7**.

## Version History

- **[0.1.0]** — Phase 0 remediation: clean repo baseline, merged backend,
  lint-clean mobile, guarded CI (2026-10-13)

---

## Phase 0 — Project Foundation (COMPLETE)

Verified present:
- Repository scaffold: `backend/`, `web/`, `mobile/`, `docs/`, `.github/`
- CI workflow (backend compile + test; guarded web/mobile steps)
- Issue/PR templates, README, CHANGELOG, LICENSE, CODE_OF_CONDUCT,
  CONTRIBUTING, SECURITY
- Coding standards, security standards, one ADR, API doc
- Backend build (Maven, Java 25) compiles; tests green (1 context-load test)
- Flutter app analyzes clean
- Root `.gitignore`, `.env.example`

---

## Phase 1 — Requirements & Design (PARTIAL)

- [x] API contract doc (`docs/api/API.md`) — exists, needs reconciliation
  with the merged backend (Phase 1 follow-up)
- [x] Coding standards (`docs/standards/CODING_STANDARDS.md`)
- [x] Security standards (`docs/standards/SECURITY_STANDARDS.md`)
- [x] One ADR (`docs/architecture/ADR_0001_PROJECT_STRUCTURE.md`)
- [ ] Full functional/non-functional requirements doc
- [ ] UI/UX wireframes for all five roles
- [ ] OpenAPI spec exported/committed (Springdoc annotations exist in code)

---

## Phase 2 — Backend Foundation & MVP API (PARTIAL)

Verified:
- [x] Java 25, Spring Boot, Maven module builds
- [x] Auth: register/login/refresh/logout with JWT (code present)
- [x] Report lifecycle endpoints (code present)
- [x] Categories, Assignments, Dashboard, Audit, Notifications (code present)
- [x] Media upload/download endpoints (code present)
- [x] Flyway V1 migration with PostGIS geometry columns
- [x] Context-load test green on H2

Not yet verified / not done:
- [ ] Real PostgreSQL + PostGIS runtime test (extension not installed locally)
- [ ] Service/repository/controller test coverage
- [ ] End-to-end auth and report lifecycle tested against a live DB

---

## Phase 3 — Role Experience & Runtime Setup (NOT STARTED)

Blocked prerequisite: install PostGIS on local PostgreSQL (approved).

### Citizen
- [ ] Dashboard with report stats
- [ ] Report history with status timeline
- [ ] Feedback on resolved reports
- [ ] Notifications with online/offline sync

### Moderator
- [ ] Review queue (reports awaiting verification)
- [ ] Analytics: reports by status, category, location
- [ ] User management (basic)

### Administrator
- [ ] User management (CRUD)
- [ ] Category management (CRUD)
- [ ] Assignment management (CRUD)
- [ ] Organisation management (CRUD)
- [ ] Global audit log

### Field Worker
- [ ] Assignment list (my assignments)
- [ ] Work evidence upload
- [ ] Assignment completion

### Organization Admin
- [ ] Organization dashboard + stats
- [ ] User management (org-level)
- [ ] Assignment routing

---

## Phase 4 — Observability & Hardening (NOT STARTED)

- Structured logging (SLF4J, Logback, JSON format)
- Correlation/request IDs
- Health endpoints (Actuator)
- Metrics (Micrometer)
- Rate limiting (Semaphore)
- Retry and circuit-breaker policies (Resilience4j)
- Security scanning (OWASP Dependency-Check, SonarQube)
- Dependency updates (Dependabot)
- Secret scanning in CI
- Deployment to cloud (env vars, HTTPS, managed DB/PostGIS)
- Backup strategy and disaster recovery
- Monitoring (Prometheus, Grafana)

---

## Phase 5 — Web Application MVP (NOT STARTED)

Current state: `web/` has `package.json`, `tsconfig.json`, `tsconfig.node.json`
only — no application source. CI skips web build until `web/src` exists.

### User Stories

**CITIZEN**
- As a citizen, I can view the landing page to understand the platform
- As a citizen, I can register for an account
- As a citizen, I can log in to my account
- As a citizen, I can create a report (category, description, location, evidence)
- As a citizen, I can view my submitted reports
- As a citizen, I can view a report's status and history
- As a citizen, I can receive notifications

**MODERATOR**
- As a moderator, I can view reports that need review
- As a moderator, I can verify a report
- As a moderator, I can reject a report with a comment

**ADMINISTRATOR**
- As an admin, I can manage users (CRUD)
- As an admin, I can manage categories (CRUD)
- As an admin, I can manage assignments
- As an admin, I can view the global audit log
- As an admin, I can view system statistics

**FIELD_WORKER**
- As a field worker, I can view my assigned reports
- As a field worker, I can update assignment status
- As a field worker, I can upload completion evidence

**ORGANIZATION_ADMIN**
- As an org admin, I can view my organization's reports
- As an org admin, I can assign assignments to my workers

### Features

- [ ] Landing page (branding, stats, map, how it works, contact)
- [ ] Authentication pages (register, login, logout)
- [ ] Citizen dashboard (report stats, my reports, create report)
- [ ] Report creation form (category, description, location, evidence)
- [ ] Report list (paginated, filterable, sortable)
- [ ] Report detail (status, history, comments, media)
- [ ] Moderator dashboard (review queue, stats)
- [ ] Report moderation flow (verify, reject, comment)
- [ ] Admin dashboard (users, categories, assignments, audit)
- [ ] Map view (report markers, clustering, filtering)
- [ ] Notifications center
- [ ] Profile page
- [ ] Report confirmation (MTF-2026-XXXXXX)

### Tech Stack
- React 18 + TypeScript
- Vite
- Tailwind CSS
- React Router
- TanStack Query (server state)
- React Hook Form + Zod (form validation)
- Axios (HTTP client)
- Leaflet + React Leaflet (mapping)
- Recharts (analytics)

---

## Phase 6 — Map & Geospatial Features (NOT STARTED)

- [ ] PostGIS optimized queries for map display
- [ ] Marker clustering (Leaflet.markercluster)
- [ ] Filter by category, status, location
- [ ] Search location (Geocoding)
- [ ] Capture location (GPS + map click)
- [ ] Mobile location detection
- [ ] Public map without sensitive details
- [ ] Map layers (categories, status)

---

## Phase 7 — Administration & Assignment (NOT STARTED)

- [ ] Full user management (CRUD, roles)
- [ ] Category tree management
- [ ] Assignment creation + reassignment
- [ ] Assignment completion with evidence
- [ ] Organization management
- [ ] Audit log with filters (date, action, actor, entity)

---

## Phase 8 — Flutter Mobile App (PARTIAL — post-MVP)

Current state: app analyzes clean. Screens exist for splash, login, register,
report list, report form, notifications, profile; `ApiClient` points at
`http://localhost:8080/api/v1`. Gaps: no iOS project folder, no token
persistence, no offline draft/sync, no push notifications, no tests.

- [ ] Authentication flow wired to backend with token persistence
- [ ] Citizen dashboard
- [ ] Report creation (camera, gallery, GPS, map)
- [ ] Report tracking (status, history)
- [ ] Notifications (in-app, push)
- [ ] Profile
- [ ] Offline draft (save to local storage when offline)
- [ ] Sync pending reports when online
- [ ] Push notifications (FCM)
- [ ] iOS project scaffold + build
- [ ] Unit/widget/integration tests

---

## Phase 9 — Notifications (NOT STARTED)

- [ ] In-app notifications
- [ ] Email notifications (via SendGrid/Brevo)
- [ ] Push notifications (FCM/APNs)
- [ ] Notification preferences
- [ ] In-app notification badges

---

## Phase 10 — Testing & Quality (NOT STARTED)

- Backend:
  - [ ] Unit tests (services, mappers, validation)
  - [ ] Integration tests (repository, controller) against PostgreSQL + PostGIS
  - [ ] Security tests
  - [ ] API tests
- Frontend (Web):
  - [ ] Unit tests (React components)
  - [ ] Integration tests (React Query, forms)
  - [ ] E2E tests (Cypress)
- Mobile (Flutter):
  - [ ] Unit tests (services, models)
  - [ ] Widget tests
  - [ ] Integration tests
- E2E (critical flow):
  - Register → Login → Create report → Upload image → Capture location →
    Submit → Admin reviews → Assign → Worker updates → Resolve → Citizen
    confirms → Close

---

## Phase 11 — Deployment (NOT STARTED)

- [ ] Dockerfiles for backend and web
- [ ] docker-compose.yml (PostgreSQL, PostGIS, backend, web)
- [ ] Production configuration (profiles, envs)
- [ ] Reverse proxy (Nginx) configuration
- [ ] HTTPS (cert-manager, Let's Encrypt)
- [ ] Database deployment (managed PostgreSQL + PostGIS)
- [ ] Object storage (S3/MinIO for media)
- [ ] CI/CD pipelines (GitHub Actions)
- [ ] Monitoring (Prometheus, Grafana)
- [ ] Logging (ELK or similar)
- [ ] Backups (scheduled, verified)

---

## Phase 12 — Beta Release (NOT STARTED)

- [ ] Deploy to staging environment
- [ ] Recruit pilot users (10-20)
- [ ] Collect feedback
- [ ] Fix critical bugs
- [ ] Prioritize feature requests
- [ ] Beta release notes

---

## Phase 13 — Advanced Features (NOT STARTED)

- [ ] AI-assisted image classification (report category prediction)
- [ ] Duplicate report detection
- [ ] Automatic categorization
- [ ] Smart prioritization (severity + location + category)
- [ ] Offline mobile reporting (fully functional)
- [ ] Advanced analytics (Power BI connector)
- [ ] Public transparency dashboards
- [ ] Multi-organization support (designed; implement orgs)
- [ ] Swahili language support
- [ ] Advanced notifications (channels, preferences)
- [ ] Data export (CSV, PDF, Excel)

---

## Phase 14 — Production Hardening (NOT STARTED)

- [ ] Security audit (third-party)
- [ ] Dependency audit (OWASP, Snyk)
- [ ] Performance testing (JMeter, k6)
- [ ] Database optimization (indexes, queries)
- [ ] Backup verification (restore test)
- [ ] Disaster recovery testing
- [ ] Logging verification
- [ ] Monitoring dashboards
- [ ] Privacy review (GDPR style)
- [ ] Accessibility review (WCAG 2.1 AA)
- [ ] Mobile release testing (App Store, Play Store)
- [ ] API security review
- [ ] Documentation review

---

## Delivery Targets

| Phase | Status (2026-10-13) |
|-------|---------------------|
| 0 | Complete |
| 1 | Partial |
| 2 | Partial (compiles; runtime on PostGIS unverified) |
| 3 | Not started |
| 4 | Not started |
| 5 | Not started (web scaffold only) |
| 6 | Not started |
| 7 | Not started |
| 8 | Partial (post-MVP; analyze-clean, not wired) |
| 9 | Not started |
| 10 | Not started |
| 11 | Not started |
| 12 | Not started |
| 13 | Not started |
| 14 | Not started |
