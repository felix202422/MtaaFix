# MtaaFix — API Design Outline & Conventions

> **Status:** v1.0 — DRAFT FOR APPROVAL · Phase 1 · 2026-10-13
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
| Roles | Enforced server-side per [ROLE_PERMISSION_MATRIX](../design/ROLE_PERMISSION_MATRIX.md); role claim in JWT, DB lookup preferred for revocation (matrix §3.6) |
| IDs | Internal UUIDs in authorized API surfaces; public references `MTF-YYYY-NNNNNN` for citizen/public display |
| Timestamps | ISO-8601 UTC instant strings |
| Pagination | Query `page` (0-based), `size` (default 20, max 100); response `PagedResponse { totalElements, totalPages, page, size, content }` |
| Sorting | `sort=field,dir` where supported (Phase 5+) |
| Error envelope | `ApiError { timestamp, status, code, message, path, details? }` — `code` machine-readable (e.g. `VALIDATION_ERROR`, `INVALID_STATUS_TRANSITION`, `RESOURCE_NOT_FOUND`) |
| Validation | Bean Validation on DTOs → 400 `VALIDATION_ERROR` with field details |
| State conflicts | Invalid lifecycle transition → 409 `INVALID_STATUS_TRANSITION` |
| Not found | 404 `RESOURCE_NOT_FOUND` |
| Unauthorized / forbidden | 401 (no/invalid token) / 403 (insufficient role or scope) |
| Rate limits | Not yet implemented; assessment Phase 17 (NFR-7) |
| CORS | Configured origins only (`CORS_ALLOWED_ORIGINS`) |

## 2. Implemented endpoints (verified in code, 2026-10-13)

### Auth — `AuthController`
| Method & path | Auth | Role | Notes |
| ------------- | ---- | ---- | ----- |
| POST `/auth/register` | Public | — | Creates CITIZEN (FR-1.1); other roles via admin only |
| POST `/auth/login` | Public | — | Returns accessToken/refreshToken/expiresIn |
| POST `/auth/refresh` | Public (refresh token body) | — | Rotates tokens |
| POST `/auth/logout` | Refresh token body | — | Invalidates refresh token |
| GET `/auth/me` | Bearer | Any | Current user profile |

### Reports — `ReportController`
| Method & path | Auth | Role | Notes |
| ------------- | ---- | ---- | ----- |
| POST `/reports` | Bearer | Any authenticated (citizen flow) | Creates SUBMITTED; returns code (FR-2.2) |
| GET `/reports/{id}` | Bearer | Scoped (matrix area 1) | |
| PUT `/reports/{id}` | Bearer | Owner, state-limited (FR-2.6) | Note: PUT not PATCH — deviation from legacy API.md, accepted |
| POST `/reports/{id}/submit` | Bearer | Moderator/admin (T2) | **Provisional:** currently no role check — Phase 4/5 |
| POST `/reports/{id}/verify` | Bearer | Moderator/admin (T5) | Provisional role check |
| POST `/reports/{id}/reject` | Bearer | Moderator/admin (T6) | Reason in body |
| POST `/reports/{id}/assign` | Bearer | Admin/org admin (T9) | `AssignReportRequest` |
| POST `/reports/{id}/resolve` | Bearer | Reviewer (T13) | Provisional |
| POST `/reports/{id}/close` | Bearer | System/admin (T14) | Provisional |

**Controller gaps vs service layer:** `ReportService` has comment/media methods
(`CreateCommentRequest`, `CreateMediaRequest`, addMedia) but the controller exposes
no `POST /reports/{id}/comments` or `POST /reports/{id}/media` routes — tracked Phase 5.

### Categories — `CategoryController`
| GET `/categories` | Bearer | Any | List |
| POST `/categories` | Bearer | Admin (A) | Create |

### Assignments — `AssignmentController`
| GET `/assignments/me` | Bearer | Field worker | Own assignments |
| GET `/assignments/organizations/{orgId}/reports` | Bearer | Org scope | Org reports |

### Dashboard — `DashboardController`
| GET `/dashboard/me` | Bearer | Any | Citizen/org aggregation |
| GET `/dashboard/organizations/{id}/stats` | Bearer | Org/admin | |
| GET `/dashboard/stats` | Bearer | Admin | Global |

### Notifications — `NotificationController`
| GET `/notifications` | Bearer | Any | Own, paginated |
| POST `/notifications/{id}/read` | Bearer | Owner | |
| POST `/notifications/read-all` | Bearer | Any | |

### Audit — `AuditController`
| GET `/audit/organizations/{id}` | Bearer | Org/admin | Org events |
| GET `/audit` | Bearer | Admin | Global |

### Misc
| GET `/actuator/health` | Public | — | Health check |
| GET `/swagger-ui/**`, `/v3/api-docs/**` | Bearer | ADMINISTRATOR | OpenAPI |

## 3. Planned endpoints (SRS-tracked; NOT implemented)

| Method & path | Purpose | SRS | Phase |
| ------------- | ------- | --- | ----- |
| POST `/reports/{id}/comments` | Citizen/staff comments (FR-2.8) | FR-2.8 | 5 |
| POST `/reports/{id}/media` | Attach evidence to report (FR-9.1) | FR-9.1 | 5 |
| DELETE `/media/{id}` | Remove own unattached/mistaken media | FR-9.2 | 5 |
| POST `/reports/{id}/needs-info` | T3 request info | FR-3.5 | 5 |
| POST `/reports/{id}/resubmit` | T4 citizen resubmit | FR-3.5 | 5 |
| POST `/reports/{id}/duplicate` | T7 mark duplicate | FR-3.6 | 5 |
| POST `/reports/{id}/start` | T10 start work | FR-4.3 | 5 |
| POST `/reports/{id}/complete` | T11 completion claim + evidence | FR-4.4 | 5 |
| POST `/reports/{id}/return` | T12 return to work | FR-4.5 | 5 |
| POST `/reports/{id}/reopen` | T16 reopen | lifecycle | 5 |
| POST `/reports/{id}/confirm` | T14 citizen confirm | FR-2.9 | 5 |
| POST `/reports/{id}/dispute` | T15 citizen dispute | FR-2.9 | 5 |
| GET `/reports/public` | Public explore feed (FR-8.3) | FR-8.3 | 6 |
| GET `/reports/public/{code}` | Public detail by reference (FR-8.4) | FR-8.4 | 6 |
| GET `/categories/public` | Public filter list | FR-8.3 | 6 |
| PUT `/categories/{id}`, POST `/categories/{id}/activate|deactivate` | Category management (FR-5.1) | FR-5.1 | 9 |
| GET/PUT `/users`, POST `/users/{id}/activate` etc. | User management (FR-12.1) | FR-12.1 | 9 |
| GET/POST `/organizations`, membership APIs | Org management (FR-5.3) | FR-5.3 | 9 |
| POST `/auth/forgot-password`, `/auth/reset-password` | Password reset (FR-1.7) | FR-1.7 | 13 |

## 4. Corrections to legacy `docs/api/API.md`

| Legacy claim | Reality / decision |
| ------------ | ------------------ |
| `PATCH /reports/{id}` | Implemented as **PUT** — keep PUT |
| `POST /media` standalone | Not implemented; media attaches to reports (Planned Phase 5) |
| `POST /reports/{id}/comments` | Service exists; controller route missing → Phase 5 |
| `POST /reports/{id}/media` | Service exists; controller route missing → Phase 5 |
| Base URL `https://api.mtaafix.example.com` | Local dev: `http://localhost:8080/api/v1` |
| `assigneeId`+`organizationId` on assign | Matches `AssignReportRequest` (verify exact fields in Phase 5 doc pass) |

## 5. OpenAPI

Springdoc annotations exist across controllers (OpenApiConfig, `@Operation`, `@Schema`).
Static spec export committed to repo is a Phase 3 task; runtime UI remains
admin-only per SecurityConfig.
