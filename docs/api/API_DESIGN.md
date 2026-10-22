# MtaaFix — API Design Outline & Conventions

> **Status:** v1.0 — APPROVED (owner, 2026-10-13) · Phase 1 · 2026-10-13
> Reconciled against the merged backend controllers (2026-10-13). This document
> **supersedes** `docs/api/API.md` for conventions and target design; `API.md` is
> retained temporarily as a legacy reference and will be rewritten in Phase 3 to
> document only implemented endpoints (Part 8 rule: never document planned endpoints
> as implemented).
>
> Convention: tables below mark each endpoint **Implemented** (verified in code today)
> or **Planned** (SRS-tracked; not yet in code).

## 1. Conventions

| Topic | Rule |
| ----- | ---- |
| Base path | `/api/v1` — major version in path; breaking changes bump version |
| Content type | `application/json; charset=UTF-8` (multipart only for media upload) |
| Auth | `Authorization: Bearer <accessToken>`; stateless JWT; refresh via `/auth/refresh` |
| Roles | Enforced server-side per [ROLE_PERMISSION_MATRIX](../design/ROLE_PERMISSION_MATRIX.md); Phase 4: role resolved from DB per request (revocation/role change effective immediately), JWT role claim informational only |
| IDs | Internal UUIDs in authorized API surfaces; public references `MTF-YYYY-NNNNNN` for citizen/public display |
| Timestamps | ISO-8601 UTC instant strings |
| Pagination | Query `page` (0-based), `size` (default 20, max 100); response `PagedResponse { totalElements, totalPages, page, size, content }` — applies to every user-growable list (reports, assignments, org members, users, audit, notifications); bounded reference data (`/categories*`, `/organizations` list) may return a top-level array (Phase 17 exemption, NFR-7) |
| Sorting | `sort=field,dir` where supported (Phase 5+) |
| Error envelope | `ApiError { timestamp, status, code, message, path, details? }` — `code` machine-readable (e.g. `VALIDATION_ERROR`, `INVALID_STATUS_TRANSITION`, `RESOURCE_NOT_FOUND`) |
| Validation | Bean Validation on DTOs → 400 `VALIDATION_ERROR` with field details |
| State conflicts | Invalid lifecycle transition → 409 `INVALID_STATUS_TRANSITION` |
| Not found | 404 `RESOURCE_NOT_FOUND` |
| Payload too large | 413 `PAYLOAD_TOO_LARGE` (Spring multipart limit, Phase 7); files ≤ container limit but > 20 MiB → 400 from service check |
| Unauthorized / forbidden | 401 (no/invalid token; codes `INVALID_TOKEN`, `INVALID_CREDENTIALS`, `UNAUTHORIZED`) / 403 (insufficient role or scope; codes `FORBIDDEN`, `ACCOUNT_INACTIVE`) |
| Rate limits | Per-IP fixed one-minute windows on `POST /auth/**` (30/min) and `POST /reports` (20/min); 429 + `Retry-After` + `RATE_LIMITED` (Phase 16, shipped); tuning/telemetry Phase 19 |
| CORS | Configured origins only (`CORS_ALLOWED_ORIGINS`) |

## 2. Implemented endpoints (verified in code, 2026-10-17)

### Auth — `AuthController`
| Method & path | Auth | Role | Notes |
| ------------- | ---- | ---- | ----- |
| POST `/auth/register` | Public | — | Creates CITIZEN (FR-1.1); other roles via admin only |
| POST `/auth/login` | Public | — | Returns accessToken/refreshToken/expiresIn |
| POST `/auth/refresh` | Public (refresh token body) | — | Rotates tokens |
| POST `/auth/logout` | Refresh token body | — | Invalidates refresh token |
| GET `/auth/me` | Bearer | Any | Current user profile |
| PUT `/auth/me` | Bearer | Any (own account) | Update `fullName`/`mobile` only (Phase 7); blank name → 400; email/password not editable here |
| POST `/auth/password/forgot` | Public | — | Reset request (FR-1.7, Phase 13); **always 202** with an identical body for known/unknown email (non-disclosing); stores SHA-256 token, 30 min, single-use |
| POST `/auth/password/reset` | Public | — | Consumes the token (Phase 13); invalid/expired/already-used → generic 400 `Invalid or expired reset token.`; token min size 10 (validation) |
| POST `/auth/password/change` | Bearer | Any (own account) | `currentPassword`+`newPassword` (Phase 13); success and failure both preserve the authenticated session |

### Reports — `ReportController`
| Method & path | Auth | Role | Notes |
| ------------- | ---- | ---- | ----- |
| POST `/reports` | Bearer | Any authenticated (citizen flow) | Creates SUBMITTED; returns code (FR-2.2); `categoryId` required (Phase 5); `location` validated ±90/±180 and implausible (0,0) → 400 (FR-11.2); optional `Idempotency-Key` `[A-Za-z0-9_-]{16,64}` (Phase 14): first use → 201 + stored response; same key + identical payload → replayed 201 (same report id, no duplicate); same key + different payload → 409 `IDEMPOTENCY_CONFLICT`; concurrent first use → 409 `IDEMPOTENCY_IN_PROGRESS` (stale intent 2 min); invalid key → 400; keys purged after 24 h |
| GET `/reports` | Bearer | Scoped by role (citizen=own, worker=assigned, org admin=org, mod/admin=all) | Filters: `status`, `categoryId`, `orgId`; staff (mod/admin/org/worker) also `q` (code/title/description text search) + `assignment`=`assigned|unassigned` (P15, Phase 9); `page`/`size` (0-based, size ≤ 100) (Phase 5); list rows are lean (Phase 17 NFR-7): `statusHistory`/`comments` empty (detail endpoint carries them), `media` batch-fetched |
| GET `/reports/{id}` | Bearer | Scoped (matrix area 1) | Includes statusHistory, comments, media; response carries **exact** `location` + `geoJson` (authorized surfaces only, FR-11.5) |
| PUT `/reports/{id}` | Bearer | Owner, state-limited (FR-2.6) | Only SUBMITTED/NEEDS_INFORMATION (Phase 5); note: PUT not PATCH — deviation from legacy API.md, accepted |
| POST `/reports/{id}/submit` | Bearer | Moderator/admin (T2/T17) | Enforced (Phase 4): MODERATOR or ADMINISTRATOR; also claims REOPENED→UNDER_REVIEW |
| POST `/reports/{id}/needs-info` | Bearer | Moderator/admin (T3) | `NeedsInfoRequest.message` required (Phase 5) |
| POST `/reports/{id}/resubmit` | Bearer | Citizen creator (T4) | Only from NEEDS_INFORMATION (Phase 5) |
| POST `/reports/{id}/cancel` | Bearer | Citizen creator (T8) | Only SUBMITTED; → REJECTED with `CANCELLED_BY_CITIZEN`, history + notification + audit (Phase 7) |
| POST `/reports/{id}/verify` | Bearer | Moderator/admin (T5) | Enforced (Phase 4): MODERATOR or ADMINISTRATOR |
| POST `/reports/{id}/reject` | Bearer | Moderator/admin (T6) | `RejectReportRequest.reasonCode` required; comment optional (Phase 5) |
| POST `/reports/{id}/duplicate` | Bearer | Moderator/admin (T7) | `canonicalReportRef` = report id or code; notes link on canonical (Phase 5) |
| POST `/reports/{id}/assign` | Bearer | Admin/org admin (T9/T9r) | AssignReportRequest; org scope enforced (Phase 4); reassignment cancels prior (Phase 5) |
| POST `/reports/{id}/start` | Bearer | Field worker (assignee) / org admin (T10) | Assigned→IN_PROGRESS (Phase 5) |
| POST `/reports/{id}/complete` | Bearer | Assignee / org admin (T11) | Requires ≥1 PHOTO attached (FR-4.4); assignment→COMPLETED (Phase 5) |
| POST `/reports/{id}/return` | Bearer | Mod/admin/org admin (T12) | `ReturnReportRequest.comment` required (Phase 5) |
| POST `/reports/{id}/resolve` | Bearer | Reviewer (T13) | Enforced (Phase 4): MODERATOR or ADMINISTRATOR |
| POST `/reports/{id}/confirm` | Bearer | Citizen creator / admin (T14) | Optional feedback; RESOLVED→CLOSED (Phase 5) |
| POST `/reports/{id}/dispute` | Bearer | Citizen creator / mod / admin (T15) | `disputeReason` required; RESOLVED→IN_PROGRESS (Phase 5) |
| POST `/reports/{id}/close` | Bearer | System/admin (T14) | Enforced (Phase 4): ADMINISTRATOR |
| POST `/reports/{id}/reopen` | Bearer | Admin (T16) | `justification` required; CLOSED→REOPENED (Phase 5) |
| POST `/reports/{id}/comments` | Bearer | Report readers (creator/staff), FR-2.8 | `CreateCommentRequest.content` required; notifies creator (Phase 5) |
| POST `/reports/{id}/media` | Bearer | Report readers, FR-9.1 | Metadata only; MIME allowlist + ≤20 MiB, PHOTO for completion (FR-9.2) (Phase 5) |
| POST `/reports/{id}/media/upload` | Bearer | Report readers, FR-9.1 | Multipart (`file`) photo upload → real disk storage under `media.storage-dir`; MIME allowlist + magic bytes + ≤20 MiB (Phase 7) |
| DELETE `/media/{id}` | Bearer | Uploader (or mod/admin) | 204; deletes stored file too (Phase 7; was metadata-only in Phase 5) |

### Media — `MediaController` (Phase 7)
| Method & path | Auth | Role | Notes |
| ------------- | ---- | ---- | ----- |
| GET `/media/{id}/content` | Bearer | Report readers (ownership/moderation scope) | Raw bytes (`image/jpeg|png|webp`), inline; authenticated — used by web via blob fetch (FR-9.3) |

### Categories — `CategoryController`
| GET `/categories` | Bearer | Any | List (active-only for non-admin) |
| GET `/categories?includeInactive=true` | Bearer | ADMINISTRATOR | Admin list incl. inactive (P20, Phase 9) |
| POST `/categories` | Bearer | Admin (A) | Create |
| PUT `/categories/{id}` | Bearer | Admin (A) | Update name/description (P20, Phase 9) |
| PUT `/categories/{id}/active?value=true|false` | Bearer | Admin (A) | Activate/deactivate; hidden from `/categories/public` when inactive; audited (Phase 9) |
| PUT `/categories/{id}/order` | Bearer | Admin (A) | Set `displayOrder` (P20, Phase 9) |

### Users (administration) — `UserAdminController` (Phase 9, P19)
| Method & path | Auth | Role | Notes |
| ------------- | ---- | ---- | ----- |
| GET `/users?q=&role=&active=&page=&size=` | Bearer | ADMINISTRATOR | Paged search (FR-12.1) |
| PUT `/users/{id}/role` | Bearer | ADMINISTRATOR | `{role}`; audited `USER_ROLE_CHANGED`; self → 400; last-active-admin → 409 |
| PUT `/users/{id}/active` | Bearer | ADMINISTRATOR | `{active}`; audited `USER_ACTIVATED`/`USER_DEACTIVATED`; last-active-admin off → 409 |

### Organizations (administration) — `OrganisationController` (Phase 9, P21)
| Method & path | Auth | Role | Notes |
| ------------- | ---- | ---- | ----- |
| GET `/organizations` | Bearer | Admin / org admin | Top-level array (org admin: own only) |
| POST `/organizations` | Bearer | ADMINISTRATOR | Create; audited (FR-5.3) |
| GET `/organizations/{id}` | Bearer | Admin / org admin | Own-scope enforced |
| PUT `/organizations/{id}` | Bearer | ADMINISTRATOR | Update; audited |
| PUT `/organizations/{id}/active` | Bearer | ADMINISTRATOR | `{active}`; audited; inactive org → 409 on assignment |
| GET `/organizations/{id}/members` | Bearer | Admin / org admin | `PagedResponse<MemberDto>` (`page`/`size`, size ≤ 100; Phase 17) |
| POST `/organizations/{id}/members` | Bearer | Admin / org admin | `{userId, role=ORGANIZATION_ADMIN|FIELD_WORKER}` — creates membership **and** grants the org role (201, audited) |
| DELETE `/organizations/{id}/members/{userId}` | Bearer | Admin / org admin | 204; removes affiliation only — role kept (documented) |

### Assignments — `AssignmentController`
| GET `/assignments/me` | Bearer | Field worker | Own assignments; `PagedResponse<AssignmentDto>` (`page`/`size`, size ≤ 100; Phase 17) |
| GET `/assignments/organizations/{orgId}/reports` | Bearer | Org scope | Org reports |

### Dashboard — `DashboardController`
| GET `/dashboard/me` | Bearer | Any | Citizen/org aggregation; Phase 7 adds `needsInfoCount`, `resolvedAwaitingConfirmCount`, `activeCount`, `attentionReports[]` (cap 5) — FR-7.4 |
| GET `/dashboard/organizations/{id}/stats` | Bearer | Org/admin | |
| GET `/dashboard/stats` | Bearer | Admin/moderator (matrix row 11) | Global |
| GET `/dashboard/admin` | Bearer | Moderator/admin | P14 admin overview: totals/byStatus, 14-day activity, byCategory, moderation queue, unassigned/assigned workload, attention, recent reports (Phase 9) |

### Analytics — `AnalyticsController` (Phase 9, P23)
| Method & path | Auth | Role | Notes |
| ------------- | ---- | ---- | ----- |
| GET `/analytics/summary?from=&to=&categoryId=&status=&orgId=&bbox=` | Bearer | Mod/admin/org admin | Date/category/status/org/geographic filters; **PostgreSQL-only** native spatial SQL (H2 covers validation 400s); ORG_ADMIN non-member → 403, foreign `orgId` forced to own org |

### Notifications — `NotificationController`
| GET `/notifications` | Bearer | Any | Own, paginated |
| POST `/notifications/{id}/read` | Bearer | Owner | |
| POST `/notifications/read-all` | Bearer | Any | |
| GET `/notifications/unread-count` | Bearer | Any | `{"count": n}` for the unread badge (FR-6.1, Phase 13) |
| GET `/notifications/preferences` | Bearer | Any | `{emailEnabled, pushEnabled, pushAvailable}` (FR-6.4, Phase 13); defaults created lazily; `pushAvailable=false` until a provider exists |
| PUT `/notifications/preferences` | Bearer | Any | Partial update of `emailEnabled`/`pushEnabled` (FR-6.4, Phase 13); returns the stored preference |

### Audit — `AuditController`
| GET `/audit/organizations/{id}` | Bearer | Org/admin | Org events (own-scope enforced), filters below |
| GET `/audit` | Bearer | Admin | Global, filters below |

Both audit endpoints accept filters (Phase 9): `action` (exact), `actorId`,
`entityType`, `entityId`, `outcome` (`SUCCESS`/`DENIED`), `from`/`to` (ISO-8601,
invalid → 400), `page`/`size`; entries include resolved `actorEmail`.

### Settings — `SettingsController` (Phase 9, P24)
| Method & path | Auth | Role | Notes |
| ------------- | ---- | ---- | ----- |
| GET `/settings` | Bearer | ADMINISTRATOR | Read-only effective configuration (FR-12.3): general, reporting rules, security TTLs/CORS, privacy + geographic fuzzing, integrations; **secrets masked** (`fuzzSecretConfigured`/`fuzzSecretIsDefault` booleans only); no mutation endpoints in MVP (D-P9.2) |

### Misc
| GET `/actuator/health` | Public | — | Health check |
| GET `/swagger-ui/**`, `/v3/api-docs/**` | Bearer | ADMINISTRATOR | OpenAPI |

### Public (unauthenticated, CORS-enabled) — Phase 6/8
| GET `/reports/public` | Public | — | Explore feed: q/status/categoryId/dateFrom/dateTo + **geo filters** `bbox=minLon,minLat,maxLon,maxLat` (ST_Intersects) or `near=lat,lon,radiusKm` (ST_DWithin, ≤500 km; cannot be combined, invalid → 400; **PostgreSQL-only** native queries) — locations **fuzzed per FR-11.4** (deterministic HMAC, uniform 150–600 m; replaces Phase 6 2dp rounding) + `geoJson`; no identity/media/comments (FR-8.3, FR-8.5) |
| GET `/reports/public/{code}` | Public | — | Detail by `MTF-YYYY-NNNNNN` code: high-level timeline (status+timestamp only), `duplicateOfCode`, `resolvedAt`/`closedAt`, fuzzed location + `geoJson` (FR-8.4) |
| GET `/categories/public` | Public | — | Category filter list |
| GET `/stats/public` | Public | — | Aggregate counts only: total/submitted/resolved/categories (FR-8.1) |

CORS: `cors.allowed-origins` (env `CORS_ALLOWED_ORIGINS`, default `http://localhost:5173`) applied to `/api/**` — GET/POST/PUT/DELETE/OPTIONS with Authorization/Content-Type/Accept headers.

## 3. Planned endpoints (SRS-tracked; NOT implemented)

| Method & path | Purpose | SRS | Phase |
| ------------- | ------- | --- | ----- |
| GET `/media/{id}/content` (unauthenticated) | Public media read | FR-8.5 | Owner decision — **deliberately not implemented** (Phase 13 flagged); content serving is authenticated only |
| Push registration/delivery endpoints (FCM/APNs) | Push channel | FR-6.3 | No provider configured — `pushAvailable=false`; no push delivery rows |

(Prior row `POST /auth/password/forgot\|reset` shipped in Phase 13 — see §2 Auth.)

## 4. Corrections to legacy `docs/api/API.md`

| Legacy claim | Reality / decision |
| ------------ | ------------------ |
| `PATCH /reports/{id}` | Implemented as **PUT** — keep PUT |
| `POST /media` standalone | Not implemented; media attaches to reports (`POST /reports/{id}/media`, Phase 5) |
| `POST /reports/{id}/comments` | Implemented in Phase 5 |
| `POST /reports/{id}/media` | Implemented in Phase 5 |
| Base URL `https://api.mtaafix.example.com` | Local dev: `http://localhost:8080/api/v1` |
| `assigneeId`+`organizationId` on assign | Matches `AssignReportRequest` (`assigneeId`, `organisationId`, optional `dueDate`) |

## 5. OpenAPI

Springdoc annotations exist across controllers (OpenApiConfig, `@Operation`, `@Schema`).
Static spec export committed to repo is a Phase 3 task; runtime UI remains
admin-only per SecurityConfig.
