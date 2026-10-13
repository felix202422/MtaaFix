# MtaaFix — Initial Domain Model

> **Status:** v1.0 — APPROVED (owner, 2026-10-13) · Phase 1 · 2026-10-13
> Reconciled against the merged backend entities and Flyway `V1__create_base_tables.sql`
> (2026-10-13). "Present" = exists in code today. "Planned" = required by the SRS but
> not yet modeled. Coordinate system: **EPSG:4326** (WGS84 lon/lat); PostGIS geometry
> types. Decisions D3 (lifecycle) and D4 (category-driven) are reflected below.

## 1. Entity overview

```
User ──┬──< Report >──┬── ReportCategory
       │              ├──< ReportStatusHistory
       │              ├──< ReportComment
       │              ├──< ReportMedia
       │              ├──< Assignment >── User (assignee)
       │              └── Organisation
       └──< Notification
       └──< AuditLog (actor)
```

## 2. Entities (code-reconciled)

### 2.1 User — Present (`users`)
| Field | Type | Constraints |
| ----- | ---- | ----------- |
| id | UUID | PK (BaseEntity) |
| email | String(255) | NOT NULL, unique, indexed |
| passwordHash | String(255) | NOT NULL (BCrypt) |
| fullName | String(255) | NOT NULL |
| mobile | String(50) | nullable |
| role | enum String(30) | NOT NULL: CITIZEN, MODERATOR, ADMINISTRATOR, FIELD_WORKER, ORGANIZATION_ADMIN (D2) |
| active | boolean | account status |
| createdAt / updatedAt / createdBy / updatedBy | | BaseEntity audit cols |

Notes: no organization-membership table yet — org↔user links currently live only on
`Assignment.assignee` / `Assignment.organisation` and `Report.organisation`. Planned:
`organization_members` (user, org, org_role) for ORG_ADMIN scoping (Phase 9).

### 2.2 Report — Present (`reports`)
| Field | Type | Constraints |
| ----- | ---- | ----------- |
| id | UUID | PK |
| code | String(50) | NOT NULL, unique — `MTF-<YYYY>-<NNNNNN>` (Part 4.4) |
| subject | enum String(50) | NOT NULL — **DEPRECATED (D4)**; removal Phase 5 migration |
| title | String(255) | NOT NULL |
| description | text | nullable |
| location | geometry(Point,4326) | nullable (required by validation at create) |
| locationArea | geometry(Polygon,4326) | nullable — area-type issues |
| reportedAt | timestamptz | NOT NULL, default now |
| status | enum String(30) | NOT NULL, default SUBMITTED — **expanded to 12 values per D3, Phase 5 migration** |
| score | integer | NOT NULL, default 0 — priority/quality score (future use) |
| user_id | FK users | NOT NULL — reporter |
| category_id | FK report_categories | nullable today → NOT NULL after D4 migration |
| organisation_id | FK organisations | nullable — set at assignment |
| assignment_id | FK assignments | nullable — current assignment |

Indexes: status, category_id, organisation_id, subject (drop after D4), user_id.
Planned (Phase 5/8): GIST index on `location` / `locationArea`; unique constraint on
`code`; CHECK on status enum values.

### 2.3 ReportCategory — Present (`report_categories`)
| Field | Type | Constraints |
| ----- | ---- | ----------- |
| id | UUID | PK |
| name | String(100) | NOT NULL |
| slug | String(100) | NOT NULL |
| type | enum: PRIMARY, SECONDARY | NOT NULL |
| parentId | String(100) | **Weakness:** should be FK to report_categories.id — fix Phase 5 |
| active / sortOrder | boolean / integer | **Planned** (FR-5.1 activate/deactivate/ordering) — Phase 9 |

### 2.4 Organisation — Present (`organisations`)
| Field | Type | Constraints |
| ----- | ---- | ----------- |
| id | UUID | PK |
| name | String(255) | NOT NULL |
| slug | String(100) | NOT NULL, unique, indexed |
| email / phone / address / description / logoUrl | | nullable contact fields |

Planned: `serviceArea` geometry(Polygon) for geographic routing (Phase 8/9);
`active` flag; membership via `organization_members` (Phase 9).

### 2.5 Assignment — Present (`assignments`)
| Field | Type | Constraints |
| ----- | ---- | ----------- |
| id | UUID | PK |
| report_id | FK reports | NOT NULL |
| assignee_id | FK users | nullable — FIELD_WORKER (or unassigned-to-org-only) |
| organisation_id | FK organisations | nullable |
| status | enum: PENDING, IN_PROGRESS, COMPLETED, CANCELLED | NOT NULL, default PENDING |
| assignedAt / completedAt | timestamptz | nullable |

Notes: assignment statuses are a **sub-state machine** of the report lifecycle
(report ASSIGNED↔IN_PROGRESS↔RESOLUTION_REVIEW drive assignment PENDING↔IN_PROGRESS↔COMPLETED).
One open assignment per report (invariant, REPORT_LIFECYCLE §3.3). Planned:
`WorkProgressUpdate` and `ResolutionEvidence` entities (Part 6.3) — evidence can ride
on `ReportMedia` scoped to assignment in Phase 10; progress notes via `ReportComment`
with internal visibility flag (Phase 10).

### 2.6 ReportStatusHistory — Present (`report_status_history`)
| Field | Type | Constraints |
| ----- | ---- | ----------- |
| id | UUID | PK |
| report_id | FK reports | NOT NULL, indexed |
| from_status | enum String(30) | NOT NULL (nullable in constructor for first row → schema says NOT NULL; first row uses SUBMITTED→SUBMITTED or null-from bug — **fix Phase 5**: allow NULL for creation row) |
| to_status | enum String(30) | NOT NULL |
| user_id | FK users | nullable (system transitions) |
| comment | text | nullable — reason/feedback |

Append-only (no update/delete paths in service). Must always mirror current
`Report.status` as latest row (invariant).

### 2.7 ReportComment — Present (`report_comments`)
Fields: id, report_id FK, user_id FK, body text, createdAt. Planned: `internal`
boolean (moderator-only notes, never public — SRS §5) — Phase 9/10.

### 2.8 ReportMedia — Present (`report_media`)
Fields: id, report_id FK, url/storage key, mediaType enum (IMAGE/DOCUMENT/…),
sizeBytes, uploadedBy. Planned: MIME allowlist enforcement + content sniffing
(FR-9.2), assignment_id link for completion evidence (FR-9.4), `visibility`
(public/private) flag — Phases 5/10/13.

### 2.9 Notification — Present (`notifications`)
Fields: id, user_id FK, type enum (REPORT_SUBMITTED, REPORT_VERIFIED, REPORT_REJECTED, …),
report_id FK, title, body, isRead boolean, createdAt. Planned: `deliveredAt` /
delivery-channel separation (Part 6.5 — "should not be marked delivered just because
the DB record exists") → Phase 13; `NotificationPreference` → Phase 13.

### 2.10 AuditLog — Present (`audit_logs`)
Fields: id, actor_id FK, action, entity_type, entity_id, outcome, createdAt.
Indexes: actor, (entity_type, entity_id), created_at. Invariant: no secrets/PII
beyond necessity (Part 6.6). Protection: no update/delete endpoints exist (good).

## 3. Conceptual entities NOT yet modeled (deferred, per Part 6 "assess, don't blindly create")

| Entity | Justification | Decision |
| ------ | ------------- | -------- |
| Role / Permission tables | Single enum role suffices (D2, ADR-0004) | Not needed v1 |
| UserProfile | User table carries profile fields | Merge into User until needed |
| Team | Org + assignment covers routing | Defer |
| FieldWorkerProfile | Role + assignments sufficient | Defer |
| AssignmentHistory | ReportStatusHistory + assignment status changes cover audit | Defer; revisit Phase 10 |
| DuplicateReportRelationship | T7 stores canonical ref in history + optional `duplicate_of` column | Add `duplicate_of` FK on reports in Phase 5 migration |
| ReportFeedback | Citizen feedback rides on T14 `citizenFeedback` field + comment | Defer dedicated table |
| NotificationDeliveryAttempt | Part of Phase 13 delivery hardening | Defer |
| OrganizationMembership | Needed for ORG_ADMIN scoping | **Phase 9** |

## 4. Planned schema changes (owned by later phases)

| Change | Phase | Migration notes |
| ------ | ----- | --------------- |
| Expand `reports.status` to 12 values (D3) | 5 | Check constraint update; backfill none needed (old 8 are subset) |
| Drop `reports.subject`; make `category_id` NOT NULL (D4) | 5 | Data migration: map legacy subject enum → seeded categories |
| Add `reports.duplicate_of` FK | 5 | For T7 |
| Fix `report_status_history.from_status` nullability | 5 | Allow NULL for creation row |
| `report_categories.parent_id` → UUID FK; add `active`, `sort_order` | 5/9 | |
| GIST spatial indexes on reports geometry | 5/8 | Requires PostGIS extension in DB |
| `organization_members` table | 9 | user_id, org_id, org_role, unique(user,org) |
| Media `visibility` + assignment link | 10/13 | |
| Notification delivery columns | 13 | |

## 5. Database rules (Part 6.7) adopted

1. Every new/changed entity ships with a Flyway migration, entity/repo updates,
   business logic, tests, and doc update (this file + API_DESIGN).
2. DB constraints (NOT NULL, unique, FK, CHECK) mirror app validation.
3. No production `ddl-auto`; only Flyway (`validate` in prod, `create-drop` in tests).
4. No tables without consuming functionality (Part 6.7 final rule) — conceptual
   entities above stay uncreated until their phase.
