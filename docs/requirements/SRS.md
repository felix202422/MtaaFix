# MtaaFix — Software Requirements Specification (SRS)

> **Status:** **v1.0 — DRAFT FOR APPROVAL** · Phase 1 — Requirements & System Design · 2026-10-13
> **Approval:** Pending owner review of this Phase 1 deliverable set.
> All open decisions (D1–D4) are resolved below. Implementation of later phases
> may proceed on these requirements once approved.

**MtaaFix — See It. Report It. Fix It Together.**

Related: [roadmap](../project-management/ROADMAP.md) · [backlog](../project-management/BACKLOG.md) ·
[role-permission matrix](../design/ROLE_PERMISSION_MATRIX.md) ·
[report lifecycle](../design/REPORT_LIFECYCLE.md) · [domain model](../design/DOMAIN_MODEL.md) ·
[API design](../api/API_DESIGN.md) · [architecture](../architecture/ADR_0001_PROJECT_STRUCTURE.md) ·
[security standards](../standards/SECURITY_STANDARDS.md)

---

## 1. Introduction

### 1.1 Purpose
This document specifies the functional and non-functional requirements for **MtaaFix**,
a civic technology platform for reporting, reviewing, assigning, and resolving community
issues in Tanzania. It is the controlling requirements baseline for all subsequent
implementation phases (master spec Phase 2 onward). It reconciles the 36-page master
specification ("the inventory") with the code that already exists in this repository.

### 1.2 Product scope
MtaaFix lets citizens report community problems (potholes, broken streetlights, waste,
drainage, flooding, water supply, damaged infrastructure, unsafe spaces, and other
admin-configurable categories) with a description, a location, and photographic evidence.
Authorized staff verify reports, assign them to responsible organizations and field
workers, track progress through a controlled lifecycle, and close them with an auditable
record. The public can explore appropriately visible reports. Citizens track their own
reports and receive notifications.

MtaaFix is not merely a complaint form: it is an issue-management system with controlled
workflow, geographic information, evidence handling, assignment management, status
history, notifications, analytics, and public transparency — implemented incrementally
per the phased roadmap.

### 1.3 Definitions
| Term | Meaning |
| ---- | ------- |
| **Report** | A citizen-submitted community issue record with location and evidence |
| **Reference (code)** | Unique public report identifier, format `MTF-<year>-<serial>` |
| **Category** | Admin-configurable report classification (DB-driven; see D4) |
| **Assignment** | Task record routing a verified report to an organization/worker |
| **Organization** | Responsible entity (e.g. municipal department, utility) |
| **Field worker** | User who performs assigned field work and submits completion evidence |
| **Moderator** | User who reviews, verifies, or rejects submitted reports |
| **Lifecycle** | The controlled status state machine every report follows |
| **The inventory** | The 36-page master specification this SRS is derived from |

### 1.4 Out of scope (v1 / MVP)
Per master spec Part 2.4 and the approved MVP (§1.6): AI-assisted categorization,
automatic duplicate detection, push notifications (FCM/APNs), email notifications,
offline mobile workflows, Swahili UI (English at launch; strings structured for later
i18n), advanced analytics/BI, and multi-region expansion. These are tracked post-MVP
(master spec Phases 13, 21).

### 1.5 Open decisions (owner input required → resolved)
| # | Decision | Options | Impact | Status |
| - | -------- | ------- | ------ | ------ |
| D1 | Phase numbering | Old 14-phase repo roadmap vs master spec Phases 0–23 | ROADMAP, BOARD, backlog, all phase references | ✅ **Resolved:** adopt master spec numbering (Phases 0–23); MVP = Phases 0–7 |
| D2 | System administrator role | 6th enum role vs scoped ADMINISTRATOR | Role matrix, future migrations | ✅ **Resolved:** keep 5 roles; system-admin operations are a scoped subset of ADMINISTRATOR permissions |
| D3 | Report state model | Code's 8 states vs spec's expanded set | Lifecycle spec, Phase 5 backend work, DB migration | ✅ **Resolved:** adopt spec states + CLOSED terminal (12 states; CANCELLED deferred) — see [REPORT_LIFECYCLE.md](../design/REPORT_LIFECYCLE.md) |
| D4 | Subject enum vs Category | Hardcoded enum vs DB-driven categories | Report entity, migration, admin UX | ✅ **Resolved:** category-driven; `Report.Subject` enum deprecated, removal tracked for Phase 5 — see [ADR_0003](../architecture/ADR_0003_CATEGORY_DRIVEN_SUBJECTS.md) |

### 1.6 Initial MVP definition (approved)
MVP = master spec **Phases 0–7**, delivering:
1. Requirements and system design (this document set) — Phase 1
2. Design system and UI foundation — Phase 2
3. Backend foundation with PostgreSQL + PostGIS, Flyway, tests — Phase 3
4. Authentication and authorization (JWT, role enforcement) — Phase 4
5. Core reporting workflow (submission through lifecycle enforcement) — Phase 5
6. Public website (landing, how-it-works, explore reports, public details) — Phase 6
7. Citizen web application (dashboard, report wizard, my reports, tracking,
   notifications, profile) — Phase 7

Explicitly post-MVP: admin/moderation/field-worker web UIs (Phases 9–10), Flutter
mobile features (Phases 11–12), notifications/media hardening (Phase 13), offline
(Phase 14). Note: backend endpoints for moderation, assignment, dashboard, audit, and
notifications already exist in code; MVP web work focuses on public + citizen surfaces
while lifecycle-enforcement backend work lands in Phase 5.

---

## 2. Overall description

### 2.1 Product perspective
Three clients share one backend API and one PostgreSQL + PostGIS database:
- **Web app** (React + TypeScript + Vite): public site, auth, citizen app;
  later admin, moderation, and field-worker apps.
- **Mobile app** (Flutter): post-MVP (Phases 11–12).
- **Backend API** (Java 25 + Spring Boot, modular monolith): REST under `/api/v1`.

### 2.2 User classes
| Class | Description | Access |
| ----- | ----------- | ------ |
| Public visitor | Browses public pages and public reports | No account |
| Citizen | Submits and tracks own reports; confirms/disputes resolution | Registered account, role `CITIZEN` |
| Moderator | Reviews, verifies, rejects, flags duplicates | Role `MODERATOR` |
| Administrator | Manages categories, users, orgs, assignments; views audit/analytics; includes system-admin scope per D2 | Role `ADMINISTRATOR` |
| Organization manager | Manages own org's members, assignments, workload | Role `ORGANIZATION_ADMIN`, org-scoped |
| Field worker | Executes assigned tasks; uploads evidence; submits completion | Role `FIELD_WORKER`, assignment-scoped |

### 2.3 Operating environment
Modern evergreen browsers (desktop/laptop/tablet/mobile) for web. Android and iOS via
Flutter (post-MVP). Backend on Linux with JDK 25, Maven, PostgreSQL 16+ with PostGIS.

### 2.4 Constraints
- Primary market: Tanzania; launch language English (i18n-ready structure).
- Stack fixed by owner: Java 25 / Spring Boot, React/TS/Vite, Flutter, PostgreSQL + PostGIS.
- Monorepo (backend, web, mobile, docs) — preserved per ADR-0001.
- No Docker/Testcontainers in this environment; local PostgreSQL + PostGIS instead.
- GitHub Projects integration unavailable (`gh` auth invalid); local BOARD.md used until fixed.

---

## 3. Functional requirements

IDs are stable. "MVP" marks requirements exercised within Phases 0–7.
Traceability column maps to master spec inventory parts.

### 3.1 Accounts and authentication
| ID | Requirement | MVP | Trace |
| -- | ----------- | --- | ----- |
| FR-1.1 | A visitor can register with email, password, full name, optional mobile; role at registration is CITIZEN only (staff roles assigned by admins) | Yes | Part 3.2, P6 |
| FR-1.2 | A user can log in with email + password and receive JWT access + refresh tokens | Yes | Part 12.1, P5 |
| FR-1.3 | Access tokens expire (default 15 min); refresh tokens expire (default 7 days); refresh rotates tokens | Yes | Part 12.1 |
| FR-1.4 | A user can log out (refresh token invalidated server-side) | Yes | P5 |
| FR-1.5 | A user can retrieve their own profile (`GET /auth/me`) | Yes | P13 |
| FR-1.6 | Passwords are stored with a strong adaptive hash (BCrypt); never logged or returned | Yes | Part 12.1 |
| FR-1.7 | Password reset flow (forgot/reset with expiring token, non-disclosing responses) | No (needs email provider) | P7 — post-MVP, Phase 13 |
| FR-1.8 | Duplicate registration with an existing email is rejected with a clear error | Yes | P6 |

### 3.2 Report submission (citizen)
| ID | Requirement | MVP | Trace |
| -- | ----------- | --- | ----- |
| FR-2.1 | An authenticated citizen can create a report: category (from configured list), title, description, optional photo evidence, and a location (map pick or GPS) | Yes | Part 4, P9 |
| FR-2.2 | Every accepted report receives a unique, stable public reference `MTF-<year>-<serial>`, generated by backend logic, not exposing DB IDs | Yes | Part 4.4 |
| FR-2.3 | A report is created in status SUBMITTED and its creation is written to status history | Yes | Part 4.1, 4.5 |
| FR-2.4 | A citizen can view and filter their own reports (status, category, date; paginated) | Yes | P10 |
| FR-2.5 | A citizen can view a full timeline (status history), evidence, and comments on their own report | Yes | P11 |
| FR-2.6 | A citizen can edit their own report (title/description/category) only while SUBMITTED or NEEDS_INFORMATION | Yes | Part 4.3 |
| FR-2.7 | A citizen can cancel their own report only while SUBMITTED (policy: pre-review withdrawal) — implemented as REJECTED-with-reason=CANCELLED_BY_CITIZEN in v1; dedicated CANCELLED state deferred per D3 | Yes | Part 4.1 |
| FR-2.8 | A citizen can add comment/update to their own report while it is active | Yes | P11 |
| FR-2.9 | A citizen can confirm resolution or dispute it when a report reaches RESOLVED | Yes | Part 4.7 |
| FR-2.10 | Citizens cannot see or modify other citizens' private reports | Yes | Part 3.2 |

### 3.3 Review and moderation
| ID | Requirement | MVP | Trace |
| -- | ----------- | --- | ----- |
| FR-3.1 | A moderator can list reports in SUBMITTED / UNDER_REVIEW / NEEDS_INFORMATION (review queue) | Backend only in MVP | P17 |
| FR-3.2 | A moderator can claim a report for review (SUBMITTED → UNDER_REVIEW) | Backend only | Part 4.3 |
| FR-3.3 | A moderator can verify a report (UNDER_REVIEW → VERIFIED) | Backend only | Part 4.3 |
| FR-3.4 | A moderator can reject a report with a mandatory reason (UNDER_REVIEW → REJECTED) | Backend only | Part 4.3 |
| FR-3.5 | A moderator can request more information (UNDER_REVIEW → NEEDS_INFORMATION) with a mandatory message; citizen may then edit and resubmit | Backend only | Part 4.2 |
| FR-3.6 | A moderator can mark a report DUPLICATE with a mandatory link to the canonical report | Backend only | Part 4.6 |
| FR-3.7 | Every moderation decision is recorded in status history with actor, timestamp, and reason; a notification is sent to the citizen | Yes (backend) | Part 4.5 |
| FR-3.8 | No arbitrary status changes: only transitions in [REPORT_LIFECYCLE.md](../design/REPORT_LIFECYCLE.md) are permitted | Yes | Part 4.3 |

### 3.4 Assignment and field work
| ID | Requirement | MVP | Trace |
| -- | ----------- | --- | ----- |
| FR-4.1 | An admin or org manager can assign a VERIFIED report to an organization and optionally a specific field worker (VERIFIED → ASSIGNED) | Backend only | Part 4.3, P18 |
| FR-4.2 | An assignment tracks assignee, organization, status (PENDING/IN_PROGRESS/COMPLETED/CANCELLED), timestamps | Yes | Part 6.3 |
| FR-4.3 | A field worker can view only assignments assigned to them; can start work (ASSIGNED → IN_PROGRESS) | Backend only | Part 3.6, P26 |
| FR-4.4 | A field worker can submit completion with required evidence (IN_PROGRESS → RESOLUTION_REVIEW) | Backend only | Part 4.7 |
| FR-4.5 | An authorized reviewer (moderator/admin) approves (RESOLUTION_REVIEW → RESOLVED) or returns to IN_PROGRESS | Backend only | Part 4.7 |
| FR-4.6 | Reassignment of an ASSIGNED/IN_PROGRESS report to a different eligible party is recorded in history | Backend only | P18 |
| FR-4.7 | A field worker cannot self-assign unrelated tasks, cannot skip RESOLUTION_REVIEW, and cannot modify system settings | Yes | Part 3.6 |

### 3.5 Categories and organizations
| ID | Requirement | MVP | Trace |
| -- | ----------- | --- | ----- |
| FR-5.1 | Report categories are stored in the database and admin-managed (create, edit, activate/deactivate, ordering); no code changes to add a category | Yes | Part 2.2, P20 |
| FR-5.2 | Categories support a primary/secondary hierarchy | Yes | Part 2.2 |
| FR-5.3 | An administrator can manage organizations (name, contact, description) | Backend only | P21 |
| FR-5.4 | New registrations are CITIZEN only; staff roles (MODERATOR, ADMINISTRATOR, FIELD_WORKER, ORGANIZATION_ADMIN) are granted by administrators and audited | Yes | Part 3.4, P19 |

### 3.6 Notifications
| ID | Requirement | MVP | Trace |
| -- | ----------- | --- | ----- |
| FR-6.1 | The backend creates in-app notifications for defined events (submitted, under review, verified, rejected, needs info, assigned, resolved, closed) | Yes | Part 6.5, P12 |
| FR-6.2 | A user can list their notifications, mark one read, mark all read (paginated) | Yes | P12 |
| FR-6.3 | Notification delivery state is separate from creation; email/push channels are post-MVP (Phase 13) | Creation only in MVP | Part 6.5 |
| FR-6.4 | Notification preferences are post-MVP | No | P13, Phase 13 |

### 3.7 Dashboards and analytics
| ID | Requirement | MVP | Trace |
| -- | ----------- | --- | ----- |
| FR-7.1 | A citizen dashboard shows own report summary, recent reports, and attention-required items | Yes (web Phase 7) | P8 |
| FR-7.2 | `GET /dashboard/me` returns the citizen's aggregated stats | Yes (backend exists) | P8 |
| FR-7.3 | Organization stats and global stats endpoints exist for authorized roles | Backend only | P14, P23 |
| FR-7.4 | All displayed metrics derive from real data; fabricated statistics are prohibited | Yes | Part 10 |

### 3.8 Public website
| ID | Requirement | MVP | Trace |
| -- | ----------- | --- | ----- |
| FR-8.1 | Public landing page: hero ("See a Problem? Let's Fix It Together."), how-it-works summary, real verified statistics only, recent public reports, footer | Yes (Phase 6) | P1 |
| FR-8.2 | How It Works page explaining the lifecycle and role responsibilities | Yes | P2 |
| FR-8.3 | Explore Community Reports: search, category/status/date filters, list + map view, pagination — public fields only | Yes | P3 |
| FR-8.4 | Public Report Details: reference, title, description, category, generalized public location, submission date, status, public evidence, timeline, resolution info | Yes | P4 |
| FR-8.5 | Public views never expose reporter identity, contact details, precise private locations, internal notes, or private attachments | Yes | Part 3.1, 12.5 |

### 3.9 Media and evidence
| ID | Requirement | MVP | Trace |
| -- | ----------- | --- | ----- |
| FR-9.1 | Citizens can attach photo evidence at creation and later while the report is active | Yes | Part 12.4, P9 |
| FR-9.2 | Uploads are validated: MIME type allowlist (JPEG/PNG/WEBP), size limit (default 20 MiB), content checks | Yes | Part 12.4 |
| FR-9.3 | Media access is authorized: private reports' media only for owner + authorized roles; public reports' media may be public | Yes | Part 12.4 |
| FR-9.4 | Field workers upload completion evidence to assignments | Backend only | P26 |
| FR-9.5 | No unrestricted public links to sensitive attachments | Yes | Part 12.4 |

### 3.10 Audit
| ID | Requirement | MVP | Trace |
| -- | ----------- | --- | ----- |
| FR-10.1 | Significant security, administrative, and workflow events are recorded (actor, action, entity, timestamp, outcome) | Yes | Part 12.6, P22 |
| FR-10.2 | Org-scoped audit visible to org admins; global audit to administrators | Backend only | Part 3.4 |
| FR-10.3 | Audit records contain no passwords, tokens, or unnecessary sensitive personal data; they are protected from modification | Yes | Part 6.6, 12.6 |

### 3.11 Geography
| ID | Requirement | MVP | Trace |
| -- | ----------- | --- | ----- |
| FR-11.1 | Report location stored as PostGIS Point (EPSG:4326); optional area Polygon for area issues | Yes | Part 6.4 |
| FR-11.2 | Coordinate validation: latitude ∈ [-90,90], longitude ∈ [-180,180]; implausible values rejected | Yes | Part 6.4 |
| FR-11.3 | Spatial index (GIST) on report geometry for map queries | Phase 5/8 | Part 6.4 |
| FR-11.4 | Public map displays generalized locations for sensitive categories (e.g. distance fuzzing); exact location only to authorized roles | Phase 8 | Part 12.5 |
| FR-11.5 | API exposes GeoJSON-compatible representation alongside lat/long | Yes | Part 6.4 |

### 3.12 Administration
| ID | Requirement | MVP | Trace |
| -- | ----------- | --- | ----- |
| FR-12.1 | Admins manage users: search, activate/deactivate, role changes (audited) | Backend only | P19 |
| FR-12.2 | Admins manage categories (FR-5.1) and organizations (FR-5.3) | Backend only | P20–21 |
| FR-12.3 | System settings (reporting rules, upload limits, notification toggles) — minimal config in MVP via env; full settings UI post-MVP | Config-only in MVP | P24 |
| FR-12.4 | High-risk operations (role grants, org changes, settings) require ADMINISTRATOR and are audited | Yes | D2, Part 3.7 |

---

## 4. Non-functional requirements

| ID | Category | Requirement |
| -- | -------- | ----------- |
| NFR-1 | Security | All endpoints enforce authentication and role authorization server-side; frontend guards are not security controls (Part 3.8) |
| NFR-2 | Security | JWT secrets ≥ 32 chars from environment; no secrets in logs or client bundles (Part 12.7) |
| NFR-3 | Security | CSRF disabled for stateless JWT API; security headers enabled; CORS restricted to configured origins |
| NFR-4 | Security | Server-side validation on all inputs (Bean Validation + service checks); client validation is UX-only (Part 12.3) |
| NFR-5 | Privacy | Collect only necessary PII; never publicly expose reporter identity, contact info, or precise sensitive locations (Part 12.5) |
| NFR-6 | Privacy | Documented retention/deletion policy for reports, media, and PII (draft: see §5; final policy owner-approved before production) |
| NFR-7 | Performance | Report list/map queries use DB indexes; pagination mandatory on list endpoints; target p95 < 500 ms for typical list queries on modest hardware (measured in Phase 17) |
| NFR-8 | Reliability | All state transitions transactional; status history append-only; referential integrity via FKs |
| NFR-9 | Accessibility | WCAG 2.1 AA: semantic structure, keyboard nav, visible focus, labeled inputs, status never by color alone (Part 9.7) |
| NFR-10 | Usability | Responsive layouts for the viewport ranges defined in Phase 2 design system; Tanzanian flag-inspired design tokens (Part 9.1) |
| NFR-11 | Compatibility | Evergreen browsers; Android 10+ / iOS 14+ for post-MVP mobile |
| NFR-12 | Maintainability | Modular monolith with module boundaries per ADR-0001; OpenAPI documented; coding standards enforced (docs/standards) |
| NFR-13 | Auditability | Every lifecycle transition and privileged action produces a history/audit record (Part 4.5, 12.6) |
| NFR-14 | i18n-readiness | UI strings externalized; English at launch; Swahili post-MVP (Part 2.4) |

---

## 5. Privacy boundaries

| Data | Public | Citizen (own) | Moderator/Admin | Field worker |
| ---- | ------ | ------------- | --------------- | ------------ |
| Reporter name/contact | Never | Yes | Yes | No |
| Report title/description | If report public | Yes | Yes | Assigned only |
| Precise coordinates | Generalized per FR-11.4 | Yes | Yes | Assigned only |
| Photo evidence | Only if classified public | Yes | Yes | Assigned only |
| Internal comments/notes | Never | No | Yes | No |
| Status timeline | High-level if public | Full | Full | Assigned only |
| Audit logs | Never | Never | Org: own org; Admin: global | Never |
| Assignment details | Never | High-level status | Yes | Own assignments |

Retention draft (NFR-6, owner to approve before production): inactive citizen accounts
deletable on request with cascade policy for their reports (or anonymization where
reports serve public record); media retained with report; audit logs retained ≥ 1 year.

---

## 6. Page inventory mapping (36 pages)

| Pages | Surface | MVP? | Master phase |
| ----- | ------- | ---- | ------------ |
| 1–4 Landing, How It Works, Explore, Public Details | Public website | Yes | Phase 6 |
| 5–6 Login, Registration | Auth | Yes | Phase 4 |
| 7 Forgot/Reset password | Auth | No (email dep.) | Phase 13 |
| 8–13 Citizen Dashboard, Report Wizard, My Reports, Tracking, Notifications, Profile | Citizen web | Yes | Phase 7 |
| 14–24 Admin/moderation/assignment/user/category/org/audit/analytics/settings | Admin web | No | Phase 9 |
| 25–26 Field-Worker Dashboard, Task Details | Field web | No | Phase 10 |
| 27–36 Mobile (splash→profile) | Flutter | No | Phases 11–12 |

---

## 7. Traceability to the master inventory

| SRS section | Master spec part |
| ----------- | ---------------- |
| §3.1 auth | Part 3, 12.1; P5–7 |
| §3.2 report submission | Part 4; P9–11 |
| §3.3 moderation | Part 3.3, 4.2–4.3; P17 |
| §3.4 assignment | Part 3.5–3.6, 4.7; P18, P25–26 |
| §3.5 categories/orgs | Part 2.2; P20–21 |
| §3.6 notifications | Part 6.5; P12 |
| §3.7 dashboards | Part 2.3; P8, P14, P23 |
| §3.8 public site | Part 3.1; P1–4 |
| §3.9 media | Part 12.4; P9, P26 |
| §3.10 audit | Part 6.6, 12.6; P22 |
| §3.11 geography | Part 6.4; P34 |
| §3.12 admin | Part 3.4, 3.7; P19, P24 |
| §4 NFRs | Parts 9, 12, 13 |
| §5 privacy | Parts 3, 12.5 |
| Decisions D1–D4 | Part 15 phases; Parts 3–4; Part 2.2 |

---

## 8. Requirements vs current code (gap summary)

Verified against the merged backend (2026-10-13):

| Area | Code state | Gap → phase |
| ---- | ---------- | ----------- |
| Auth endpoints (register/login/refresh/logout/me) | Present | Role checks coarse → Phase 4 |
| Report CRUD + submit/verify/reject/assign/resolve/close | Present | No NEEDS_INFORMATION / RESOLUTION_REVIEW / DUPLICATE / REOPENED states; no state-machine guard beyond per-method checks → Phase 5 |
| Report.Subject enum | Present, hardcoded | Deprecated (D4); category-driven → Phase 5 migration |
| Categories CRUD | List + create only | Activate/deactivate/ordering → Phase 9 |
| Assignments | List endpoints; entity present | Progress/evidence/completion endpoints → Phase 5/10 |
| Notifications | List/read/read-all; send on events | Delivery-state separation, prefs → Phase 13 |
| Dashboard/audit | Endpoints present | UI → Phase 9; enforcement tests → Phase 4/15 |
| Media | DTOs/entity + addMedia path | Standalone upload endpoint, access-control tests → Phase 5/13 |
| Authorization | `anyRequest().authenticated()` only | Full role matrix enforcement → Phase 4 |
| Public API | None | Public read endpoints + privacy filtering → Phase 6 |
| Password reset | Absent | Phase 13 (email dependency) |
| Tests | 1 context-load test | Meaningful coverage → Phases 3–5, 15 |

---

## 9. Acceptance approach
Every backlog item in [BACKLOG.md](../project-management/BACKLOG.md) carries
acceptance criteria. A requirement is considered met only when its ACs pass relevant
automated or manually verified checks — per master spec Part 16 Definition of Done.
