# MtaaFix — Implementation Backlog

> **Status:** v1.0 — APPROVED (owner, 2026-10-13) · Phase 1 · 2026-10-13
> Prioritized work items with acceptance criteria (master spec Part 14.2).
> Statuses: `BACKLOG` (ready), `BLOCKED` (waiting on dependency/gate).
> GitHub Projects unavailable; this file + [BOARD.md](BOARD.md) are the local
> source of truth until `gh` auth is fixed. Labels: `phase:N`, `be`/`web`/`mobile`,
> `docs`, `test`, `sec`.

Dependencies flow: Phase 2 ∥ Phase 3 → Phase 4 → Phase 5 → Phase 6 → Phase 7.
(Phases 2 and 3 can proceed in parallel; 4 and 5 are sequential on the backend;
6 and 7 need 4–5 for real integrations and 2 for the UI foundation.)

---

## Phase 2 — Design system (web + shared)

### B-2.1 Design tokens and theme
- **As** a team, **we** encode the approved palette, typography, spacing, radius,
  shadow tokens so all later UI is consistent.
- **AC:** Tokens file(s) in `web/` (CSS variables or Tailwind config) matching Part 9.1
  hex values; Storybook-or-docs page (or markdown visual spec) shows primary/semantic
  usage; contrast checks pass WCAG AA for text pairs; `npm run build` green.
- **Phase:** 2 · **Labels:** web, docs · **Tests:** none (visual review)

### B-2.2 Component primitives
- Button, input, card, badge/status pill, modal, toast, empty/loading/error states.
- **AC:** Each component keyboard-accessible with visible focus; status badges pair
  color with text/icon (NFR-9); components reused by a demo page; typecheck green.
- **Phase:** 2 · **Depends:** B-2.1

### B-2.3 Layout & responsive rules
- **AC:** Documented breakpoints; demo layouts verified at 360/768/1280/1920 px.
- **Phase:** 2 · **Depends:** B-2.1

---

## Phase 3 — Backend foundation completion

### B-3.1 PostgreSQL + PostGIS runtime verification
- Install PostGIS extension; create `mtaafix` DB; run Flyway V1; boot app; smoke
  health endpoint.
- **AC:** `flyway migrate` (or app boot with flyway enabled) succeeds on
  PostgreSQL+PostGIS; `GET /actuator/health` = UP; entity `validate` passes;
  commands + results recorded in phase report.
- **Phase:** 3 · **Labels:** be, test · **Blocked on:** PostGIS install (approved)

### B-3.2 Integration test profile
- **AC:** Maven profile (e.g. `-Pit`) runs context/repository tests against real
  Postgres; CI keeps H2 unit tier green; documented in DEVELOPMENT.md.
- **Phase:** 3 · **Depends:** B-3.1

### B-3.3 OpenAPI export
- **AC:** `openapi.yaml` (or json) generated from springdoc and committed;
  regeneration command documented.
- **Phase:** 3

### B-3.4 DEVELOPMENT.md runbook
- **AC:** Prereqs (JDK 25, Maven, PG+PostGIS, Node, Flutter), env vars from
  `.env.example`, run/test commands, troubleshooting; verified by following it on a
  clean shell.
- **Phase:** 3

---

## Phase 4 — AuthN/Z

### B-4.1 Method-level role enforcement
- Apply `@Precheck`/service guards per [role matrix](../design/ROLE_PERMISSION_MATRIX.md).
- **AC:** Every row of matrix §1 has at least one automated positive and one negative
  test; 401/403 semantics correct; no endpoint relies on `authenticated()` alone for
  role separation.
- **Phase:** 4 · **Labels:** be, sec, test

### B-4.2 Ownership & org scoping
- **AC:** Citizen cannot read/update another user's report (404/403 per convention);
  org admin cannot read other orgs' private data; field worker sees only own
  assignments — each with tests.
- **Phase:** 4 · **Depends:** B-4.1

### B-4.3 Audited A-scope operations
- **AC:** Role grants, org/category/settings changes write AuditLog rows containing
  actor, action, entity, outcome; test asserts row written; no secrets in payload logs.
- **Phase:** 4
- **Partial (Phase 4 done):** category create writes `CATEGORY_CREATED` audit rows;
  role grants / org / settings audit arrives with their Phase 9 endpoints (rows 8/9/14).

---

## Phase 5 — Core reporting workflow

### B-5.1 Lifecycle migration + state machine
- Flyway: add 4 statuses, `duplicate_of`, history nullability fix; central transition
  guard implementing T1–T17.
- **AC:** Invalid transition → 409 `INVALID_STATUS_TRANSITION`; every legal
  transition covered by test; history row appended per transition with actor;
  invariant test (latest history == current status).
- **Phase:** 5 · **Depends:** B-3.1, B-4.1 · **Labels:** be, test

### B-5.2 Category-driven reports (ADR-0003)
- **AC:** Legacy subjects seeded into `report_categories`; `subject` column dropped;
  `category_id` NOT NULL; create-report requires categoryId; adding a category needs
  no code change (verified by create-via-API test).
- **Phase:** 5 · **Depends:** B-5.1

### B-5.3 Report code generation
- **AC:** `MTF-<year>-<6-digit>` generated at creation; DB unique constraint;
  collision-safe under concurrent creates (test); stable across transitions.
- **Phase:** 5

### B-5.4 Missing endpoints (comments, media, lifecycle actions)
- Expose Planned routes in [API_DESIGN §3](../api/API_DESIGN.md) needed for MVP
  lifecycle: comments, media attach, needs-info, resubmit, duplicate, start,
  complete+evidence, return, confirm, dispute, reopen.
- **AC:** Each route matches API_DESIGN contract; role/state guards tested;
  media validates MIME/size (FR-9.2).
- **Phase:** 5 · **Depends:** B-5.1

### B-5.5 Coordinate validation
- **AC:** Out-of-range lat/long rejected 400; valid point round-trips EPSG:4326;
  test with boundary values.
- **Phase:** 5

---

## Phase 6 — Public website

### B-6.1 Web app scaffold
- Vite + React + TS + Tailwind + Router wired; env config; API client with error/
  auth handling (Part 11.1).
- **AC:** `npm ci && npm run build` green; CI web step activates (web/src exists);
  404 route works.
- **Phase:** 6 · **Depends:** B-2.1

### B-6.2 Public APIs
- **AC:** `/reports/public`, `/reports/public/{code}`, `/categories/public` return
  only public fields (SRS §5); reporter identity/phone/precise-sensitive-location
  never serialized (test with full-data report); pagination + filters per FR-8.3.
- **Phase:** 6 · **Depends:** B-5.2, B-5.3

### B-6.3 Pages 1–4
- **AC:** Each page functional against real APIs (no dead buttons/fake charts,
  Part 10); responsive at B-2.3 breakpoints; accessibility spot-check; privacy review
  vs SRS §5.
- **Phase:** 6 · **Depends:** B-6.1, B-6.2

---

## Phase 7 — Citizen web

### B-7.1 Auth pages (login/register)
- **AC:** Real `/auth/*` integration; token storage strategy documented; duplicate
  email error surfaced; logout clears session.
- **Phase:** 7 · **Depends:** B-6.1, B-4.1

### B-7.2 Report wizard (4 steps)
- **AC:** Category from API; location via map or pin; photo upload with client+server
  validation; review step; success shows MTF reference; draft protection against
  double-submit.
- **Phase:** 7 · **Depends:** B-5.4, B-2.2

### B-7.3 My Reports / Tracking / Notifications / Profile
- **AC:** List paginated+filterable; timeline shows full history; confirm/dispute
  actions appear only in eligible states; notifications mark-read works; profile edit
  own-only.
- **Phase:** 7 · **Depends:** B-7.1, B-5.1

### B-7.4 Citizen dashboard
- **AC:** Real stats from `/dashboard/me`; attention-required panel driven by
  NEEDS_INFORMATION/dispute states; no fabricated numbers (FR-7.4).
- **Phase:** 7 · **Depends:** B-7.1

---

## Cross-cutting (scheduled, not MVP-blocking)

| ID | Item | Phase | AC summary |
| -- | ---- | ----- | ---------- |
| B-X.1 | Password reset flow | 13 | Token expiring, non-disclosing responses, email provider integration |
| B-X.2 | Notification delivery separation | 13 | deliveredAt ≠ createdAt; failure handling |
| B-X.3 | `organization_members` + org scoping UI | 9 | Membership CRUD; matrix O-scopes enforced |
| B-X.4 | iOS project scaffold | 11 | `flutter build ios` succeeds on macOS CI-or-local; until then documented limitation |
| B-X.5 | Mobile token persistence + wiring | 11–12 | Login→session→report flow against real backend |
