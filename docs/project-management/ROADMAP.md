# Roadmap — MtaaFix

> Phase numbering follows the **master specification** (Phases 0–23) per decision D1
> (2026-10-13). Status reflects **verified repository state** (last audited 2026-10-13),
> not intent. **MVP = Phases 0–7** (approved). Every phase ends with an approval gate
> (master spec Part 16). Local tracking: [BOARD.md](BOARD.md) ·
> Work items: [BACKLOG.md](BACKLOG.md) · Requirements: [../requirements/SRS.md](../requirements/SRS.md)

## Version History

- **[0.5.0]** — Phase 4: authentication & authorization enforcement (matrix gates, DB role lookup, org/ownership scoping, audit wiring; 41/41 + 3/3 tests) — **APPROVED** 2026-10-14
- **[0.4.0]** — Phase 3: backend foundation completed (PostGIS 3.6.2 verified,
  Flyway V1 fixed + applied, H2/PostgreSQL test matrix, OpenAPI export,
  DEVELOPMENT.md) — **APPROVED** 2026-10-14
- **[0.3.0]** — Phase 2: design system & UI foundation (tokens, components, gallery,
  Flutter theme groundwork) — **APPROVED** 2026-10-13
- **[0.2.0]** — Phase 1: requirements & system design baseline (SRS, role matrix,
  lifecycle, domain model, API design, ADRs 0002–0004) — **APPROVED** 2026-10-13
- **[0.1.0]** — Phase 0: repository remediation & verified baseline — 2026-10-13

---

## MVP summary (Phases 0–7)

| Phase | Name | Status (2026-10-13) |
| ----- | ---- | ------------------- |
| 0 | Project audit and foundation | ✅ Complete |
| 1 | Requirements and system design | ✅ Complete (approved 2026-10-13) |
| 2 | Design system and UI foundation | ✅ Complete (approved 2026-10-13) |
| 3 | Backend foundation | ✅ Complete (**APPROVED** 2026-10-14) — PostGIS verified; Flyway V1 green; H2+PG test matrix |
| 4 | Authentication and authorization | ✅ Complete (**APPROVED** 2026-10-14) — matrix gates, DB role lookup, scoping; tests 41/41 + 3/3 PG |
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

## Phase 2 — Design System and UI Foundation (COMPLETE — approved 2026-10-13)

Delivered 2026-10-13:
- [x] Design tokens (Tanzanian palette, spec §9.1) — `web/src/styles/tokens.css`
- [x] Global styles + Inter typography — `web/src/styles/global.css`, `web/index.html`
- [x] Tailwind v4 + ESLint 9 flat config with jsx-a11y — `web/vite.config.ts`, `web/eslint.config.js`
- [x] Reusable React component library — `web/src/design-system/` (Button, Field,
  TextAreaField, Card, Badge, Modal, Toast + ToastProvider, EmptyState,
  LoadingState, ErrorState)
- [x] In-repo design gallery at `/design` with layout previews (landing, auth,
  dashboard, report wizard) — `web/src/gallery/` (design validation only)
- [x] Design system documentation — [../design/DESIGN_SYSTEM.md](../design/DESIGN_SYSTEM.md)
  (tokens, status mapping, responsive breakpoints, manual a11y checklist)
- [x] Minimal brand assets — wordmark + `web/public/favicon.svg`
- [x] Shared Flutter theme groundwork — `mobile/lib/app/theme.dart` wired into
  `mobile/lib/app.dart` (screens still use hardcoded colors; Phase 11)
- [x] CI: web step now runs lint + build; `npm run lint` added
- Verified: `npm run lint`, `npm run build` (tsc + vite), `flutter analyze` all green

## Phase 3 — Backend Foundation (COMPLETE — approved 2026-10-14)

Delivered 2026-10-14:
- [x] PostGIS 3.6.2 installed and verified against PostgreSQL 18.6
- [x] V1 migration fixed for real PostGIS (extension, public schema, geometry+GIST, FK cycle, updated_at, from_status nullable, varchar(36) ids)
- [x] Boot 4 deps: hibernate-spatial, spring-boot-flyway (auto-config was absent), spring-boot-webmvc-test; Testcontainers removed
- [x] Entity fixes: BaseEntity PrePersist + assigned ids; AuditLog jsonb
- [x] App boots on PG+PostGIS; Flyway V1 applies; ddl validate passes; health UP
- [x] Test matrix: H2 always; PostgresIntegrationTest gated by MTAAFIX_PG_IT - 5/5 green with PG
- [x] Committed OpenAPI spec - docs/api/openapi.json
- [x] Runbook - DEVELOPMENT.md (prerequisites, PostGIS install, config matrix, troubleshooting)
- Exit: backend builds and tests pass against PostgreSQL+PostGIS

## Phase 4 — Authentication and Authorization (COMPLETE — approved 2026-10-14)

Present: register/login/refresh/logout/me, JWT filter, BCrypt, SecurityConfig
(coarse: authenticated-vs-permitAll). Completed this phase:
- [x] Enforce [role matrix](../design/ROLE_PERMISSION_MATRIX.md) at service/controller layer
  (`@EnableMethodSecurity`; `@PreAuthorize` gates per API_DESIGN access column; 401 invalid
  token / 403 insufficient role or scope / 409 `INVALID_STATUS_TRANSITION` semantics)
- [x] Org/ownership scoping (report read/edit/comment/media, org paths on audit,
  assignments, dashboard; role resolved from DB per request so revocation and role
  changes apply immediately; report list query carries role-scoped predicates)
- [x] Authorization tests (positive+negative: `AuthorizationMatrixTest`, `AuthFlowTest`,
  `JwtTokenProviderTest`; 41/41 green on H2, 3/3 green on PostgreSQL 18 + Flyway)
- [x] Audited A-scope operations (`AuditService.record`; category create writes
  `CATEGORY_CREATED` rows, asserted by test)
- Deferrals: T1-T17 actor rules + centralized 409 state machine (Phase 5); matrix rows
  8/9/14 worker/org/settings endpoints (Phase 9); CORS config (Phase 6/16); refresh
  token invalidation on logout (Phase 16)
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

Present: analyze-clean scaffold (splash/login/register/report screens, ApiClient),
shared theme groundwork (`mobile/lib/app/theme.dart`, Phase 2).
Remaining: navigation hardening, screen migration off hardcoded colors, secure
token storage, Android/iOS project completeness (iOS folder missing), auth-state
wiring, initial tests.

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
| 2 | Complete (approved) |
| 3 | Complete (approved) |
| 4 | Complete (approved) |
| 5 | Partial |
| 6 | Not started |
| 7 | Not started |
| 8–10 | Not started |
| 11 | Partial (scaffold) |
| 12 | Not started |
| 13 | Partial (in-app only) |
| 14–23 | Not started |
