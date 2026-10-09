# Roadmap — MtaaFix

## Version History

- **[0.3.0]** — Backend MVP API: Auth, Report lifecycle, Categories, Organizations, Location, Media, Notifications, Dashboard, Categories, Assignments, Audit
- **[0.4.0]** — Flutter Mobile App: iOS + Android with full reporting features
- **[Unreleased]** — Phase 5 Web Application & Phase 3 Role Experience MVP

---

## Phase 0 — Project Foundation (COMPLETE)

- Repository scaffold: backend, web, mobile, docs, `.github`
- Architecture decision records
- API surface
- CI workflow (Java 25, Maven)
- Issue/PR templates
- Coding & security standards
- README, CHANGELOG, LICENSE, CODE_OF_CONDUCT, CONTRIBUTING
- `.freebuff/project-id`
- Backend dependency scaffold (`pom.xml`, `.gitignore`, `.env.example`)
- Web frontend scaffold (Vite + React + TypeScript)
- Flutter mobile app scaffold (Android + iOS)

---

## Phase 1 — Requirements & Design (COMPLETE)

- Detailed functional & non-functional requirements
- Domain-driven design and problem/event storming
- Database schema design (entities, relations, constraints, indexes)
- API contract finalization (OpenAPI/Swagger + JSON schemas)
- UI/UX wireframes for all five roles
- Coding standards document
- Security standards document

---

## Phase 2 — Backend Foundation & MVP API (COMPLETE)

- Java 25, Spring Boot, Maven
- PostgreSQL with PostGIS (Flyway migrations)
- Auth: register/login/refresh/logout with JWT (BCrypt)
- Report lifecycle: create, submit, verify, reject, assign, resolve, close
- Categories and organizations
- Location capture with PostGIS geometry
- Media upload/download with validation
- Notification endpoints (in-app)
- Dashboard endpoints (user stats, org stats, global stats)
- Category endpoints (list, create)
- Assignment endpoints (my assignments, org reports)
- Audit endpoints (org audit log, global audit log)
- Global exception handling
- Bean Validation
- OpenAPI/Swagger documentation

---

## Phase 3 — Role Experience MVP (In Progress)

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

## Phase 4 — Observability & Hardening

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

## Phase 5 — Web Application MVP (IMPLEMENTING NOW)

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
- [ ] Map view (Report markers, clustering, filtering)
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

## Phase 6 — Map & Geospatial Features

- [ ] PostGIS optimized queries for map display
- [ ] Marker clustering (Leaflet.markercluster)
- [ ] Filter by category, status, location
- [ ] Search location (Geocoding)
- [ ] Capture location (GPS + map click)
- [ ] Mobile location detection
- [ ] Public map without sensitive details
- [ ] Map layers (categories, status)

---

## Phase 7 — Administration & Assignment

- [ ] Full user management (CRUD, roles)
- [ ] Category tree management
- [ ] Assignment assignment + reassignment
- [ ] Assignment completion with evidence
- [ ] Organization management
- [ ] Audit log with filters (date, action, actor, entity)

---

## Phase 8 — Flutter Mobile App MVP

- [ ] Authentication flow (login, register, refresh token)
- [ ] Citizen dashboard
- [ ] Report creation (camera, gallery, GPS, map)
- [ ] Report tracking (status, history)
- [ ] Notifications (in-app, push)
- [ ] Profile
- [ ] Offline draft (save to local storage when offline)
- [ ] Sync pending reports when online
- [ ] Push notifications (FCM)
- [ ] Location-based recommendations

---

## Phase 9 — Notifications

- [ ] In-app notifications
- [ ] Email notifications (via SendGrid/Brevo)
- [ ] Push notifications (FCM/APNs)
- [ ] Notification preferences
- [ ] In-app notification badges

---

## Phase 10 — Testing & Quality

- Backend:
  - [ ] Unit tests (services, mappers, validation)
  - [ ] Integration tests (repository, controller)
  - [ ] Security tests
  - [ ] API tests (Postman/Requests)
  - [ ] Testcontainers (PostgreSQL, PostGIS)
- Frontend (Web):
  - [ ] Unit tests (React components)
  - [ ] Integration tests (React Query, forms)
  - [ ] E2E tests (Cypress)
- Mobile (Flutter):
  - [ ] Unit tests (services, models)
  - [ ] Widget tests
  - [ ] Integration tests
- E2E (critical flow):
  - Register → Login → Create report → Upload image → Capture location → Submit → Admin reviews → Assign → Worker updates → Resolve → Citizen confirms → Close

---

## Phase 11 — Deployment

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

## Phase 12 — Beta Release

- [ ] Deploy to staging environment
- [ ] Recruit pilot users (10-20)
- [ ] Collect feedback
- [ ] Fix critical bugs
- [ ] Prioritize feature requests
- [ ] Beta release notes

---

## Phase 13 — Advanced Features

- [ ] AI-assisted image classification (report category prediction)
- [ ] Duplicate report detection
- [ ] Automatic categorization
- [ ] Smart prioritization (severity + location + category)
- [ ] Offline mobile reporting (fully functional)
- [ ] Advanced analytics (power BI connector)
- [ ] Public transparency dashboards
- [ ] Multi-organization support (already designed, implement orgs)
- [ ] Swahili language support
- [ ] Advanced notifications (channels, preferences)
- [ ] Data export (CSV, PDF, Excel)

---

## Phase 14 — Production Hardening

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

| Phase | Target |
|-------|--------|
| 0 | Complete |
| 1 | Complete |
| 2 | Complete |
| 3 | In Progress |
| 4 | TBD |
| 5 | TBD |
| 6 | TBD |
| 7 | TBD |
| 8 | Complete |
| 9 | TBD |
| 10 | TBD |
| 11 | TBD |
| 12 | TBD |
| 13 | TBD |
| 14 | TBD |
