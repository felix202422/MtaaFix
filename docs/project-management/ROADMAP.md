# Roadmap — MtaaFix

> Phase numbering follows the **master specification** (Phases 0–23) per decision D1
> (2026-10-13). Status reflects **verified repository state** (last audited 2026-10-13),
> not intent. **MVP = Phases 0–7** (approved). Every phase ends with an approval gate
> (master spec Part 16). Local tracking: [BOARD.md](BOARD.md) ·
> Work items: [BACKLOG.md](BACKLOG.md) · Requirements: [../requirements/SRS.md](../requirements/SRS.md)

## Version History

- **[0.2.0]** — Phase 1: requirements & system design baseline (SRS, role matrix,
  lifecycle, domain model, API design, ADRs 0002–0004) — **APPROVED** 2026-10-13
- **[0.1.0]** — Phase 0: repository remediation & verified baseline — 2026-10-13

---

## MVP summary (Phases 0–7)

| Phase | Name | Status (2026-10-13) |
| ----- | ---- | ------------------- |
| 0 | Project audit and foundation | ✅ Complete |
| 1 | Requirements and system design | ✅ Complete (approved 2026-10-13) |
| 2 | Design system and UI foundation | Not started |
| 3 | Backend foundation | Partial — builds/tests green on H2; PostGIS runtime unverified |
| 4 | Authentication and authorization | Partial — JWT flows present; role enforcement not matrix-complete |
| 5 | Core reporting workflow | Partial — provisional endpoints; state machine gaps |
| 6 | Public website | Not started (`web/` is manifest-only) |
| 7 | Citizen web application | Not started |

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

## Phase 2 — Design System and UI Foundation (NOT STARTED)

- Design tokens (Tanzanian flag palette, Part 9.1)
- Typography (Inter), spacing, card/form/status patterns
- Reusable component library (React) + shared Flutter theme groundwork
- Responsive layout rules; accessibility checklist (WCAG 2.1 AA)
- Brand assets review (logo/favicon)
- Exit: reusable foundation suitable for Phases 6–7 interfaces

## Phase 3 — Backend Foundation (PARTIAL)

Present: Maven module, Java 25, Spring Boot app, Flyway V1, application profiles,
context-load test on H2, Springdoc annotations, health endpoint permitted.
Remaining:
- [ ] Install/configure PostGIS locally; verify Flyway V1 + entity DDL against real PostgreSQL+PostGIS
- [ ] Test configuration matrix (H2 unit vs Postgres integration profile)
- [ ] Commit exported OpenAPI spec (admin runtime UI exists)
- [ ] `.env.example` → DEVELOPMENT.md runbook
- Exit: backend builds and tests pass against PostgreSQL+PostGIS

## Phase 4 — Authentication and Authorization (PARTIAL)

Present: register/login/refresh/logout/me, JWT filter, BCrypt, SecurityConfig
(coarse: authenticated-vs-permitAll).
Remaining:
- [ ] Enforce [role matrix](../design/ROLE_PERMISSION_MATRIX.md) at service/controller layer
- [ ] Org/ownership scoping in queries
- [ ] Authorization tests (positive+negative per matrix row)
- [ ] Audited A-scope operations
- Exit: authorized/unauthorized access behaves per matrix; tests green

## Phase 5 — Core Reporting Workflow (PARTIAL)

Present: create/list/get/update, submit/verify/reject/assign/resolve/close
(provisional guards), status history, comments/media service methods.
Remaining (see BACKLOG):
- [ ] Flyway migration: status enum → 12 states; drop subject; category_id NOT NULL; duplicate_of; history nullability fix
- [ ] Central state-machine guard (T1–T17); 409 on invalid transitions
- [ ] Expose comment/media endpoints; needs-info/resubmit/duplicate/complete/return/confirm/dispute/reopen
- [ ] Unique code generation `MTF-YYYY-NNNNNN` with DB constraint
- [ ] Category seeding from legacy subjects; coordinate validation
- [ ] Lifecycle tests for every transition row + negatives
- Exit: core report workflow works; business rules tested

## Phase 6 — Public Website (NOT STARTED)

Pages 1–4 (landing, how-it-works, explore, public details) per SRS §3.8.
Public read APIs (Planned table, API_DESIGN §3); privacy filtering (FR-8.5,
FR-11.4). Vite/React scaffold from scratch under `web/`.

## Phase 7 — Citizen Web Application (NOT STARTED)

Pages 8–13 (dashboard, 4-step report wizard, my reports, tracking, notifications,
profile) per SRS §3.2/3.6/3.7. Real API integration only; ownership tests.

## Phase 8 — Geographic Capabilities (NOT STARTED)

PostGIS spatial queries, GIST indexes, map interactions (Leaflet), public
generalization, coordinate fuzzing, geographic filters. Depends on Phase 6 maps.

## Phase 9 — Administration and Moderation (NOT STARTED)

Pages 14–24. Backend endpoints for users/orgs/categories/audit largely exist;
needs management APIs + UIs + `organization_members` + category activate/order.

## Phase 10 — Field-Worker Web Application (NOT STARTED)

Pages 25–26. Assignment progression UIs; completion evidence; authorization tests.

## Phase 11 — Flutter Application Foundation (PARTIAL)

Present: analyze-clean scaffold (splash/login/register/report screens, ApiClient).
Remaining: navigation/theme hardening, secure token storage, Android/iOS project
completeness (iOS folder missing), auth-state wiring, initial tests.

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

## Delivery Targets (2026-10-13)

| Phase | Status |
| ----- | ------ |
| 0 | Complete |
| 1 | Complete (approved) |
| 2 | Not started |
| 3 | Partial |
| 4 | Partial |
| 5 | Partial |
| 6 | Not started |
| 7 | Not started |
| 8–10 | Not started |
| 11 | Partial (scaffold) |
| 12 | Not started |
| 13 | Partial (in-app only) |
| 14–23 | Not started |
