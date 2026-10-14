# MtaaFix — Role-Permission Matrix

> **Status:** v1.0 — APPROVED (owner, 2026-10-13) · Phase 1 · 2026-10-13
> Decision D2: five roles; "system administrator" is a scoped subset of
> ADMINISTRATOR, not a separate role. Public visitor is unauthenticated, not a DB role.
> Backend must enforce every rule below (master spec §3.8). Hiding a button is not a
> security control.

Legend: **Y** = allowed · **N** = denied · **S** = self/own-scope only ·
**O** = own organization scope · **C** = conditional (state/ownership rules apply) ·
**A** = ADMINISTRATOR system-admin scope (audited, high-risk)

## 1. Matrix (14 areas from master spec §3.8)

| # | Permission area | Public | CITIZEN | MODERATOR | ADMINISTRATOR | ORG_ADMIN | FIELD_WORKER |
| - | --------------- | ------ | ------- | --------- | ------------- | --------- | ------------ |
| 1 | Read reports | Public fields only (FR-8.5) | S (own) | Y (all) | Y (all) | O (org) | S (assigned) |
| 2 | Create reports | N | Y | Y (as citizen) | Y (as citizen) | Y (as citizen) | Y (as citizen) |
| 3 | Edit reports | N | C: own, only SUBMITTED / NEEDS_INFORMATION (FR-2.6) | N (use moderation actions) | N (use moderation actions) | N | N |
| 4 | View private evidence | N | S (own reports) | Y | Y | O | S (assigned only) |
| 5 | Review reports | N | N | Y | Y | N | N |
| 6 | Change report status | N | C: own confirm/dispute at RESOLVED; cancel while SUBMITTED (FR-2.7, FR-2.9) | C: review states only (§REPORT_LIFECYCLE transitions) | C: all review/assignment states | C: assignment states for org reports | C: own assignment progression only (start, complete-submit) |
| 7 | Assign reports | N | N | N | Y | O (org's verified reports) | N |
| 8 | Manage workers | N | N | N | Y (A) | O (org members) | N |
| 9 | Manage organizations | N | N | N | Y (A) | N (view own only) | N |
| 10 | Manage categories | N | N | N | Y (A) | N | N |
| 11 | View analytics | N | S (own stats) | Y (operational) | Y (global) | O (org) | S (own workload) |
| 12 | Manage users | N | S (own profile only) | N | Y (A: role grants, activate/deactivate) | O (org members, limited) | N |
| 13 | View audit logs | N | N | N | Y (global) | O (org events) | N |
| 14 | Change system settings | N | N | N | Y (A) | N | N |

## 2. Notes and rules

### 2.1 Public visitor
- Access: landing, how-it-works, explore public reports, public report details, help/policy pages.
- Must never receive: private reports, private attachments, internal notes, reporter
  identity, precise sensitive locations, admin data (SRS §5, FR-8.5).

### 2.2 CITIZEN
- Report editing restricted by state (FR-2.6). Status powers limited to:
  cancel-while-SUBMITTED (recorded as REJECTED with reason CANCELLED_BY_CITIZEN per D3),
  and confirm/dispute at RESOLVED (FR-2.9).
- Cannot rewrite status history (Part 4.5).

### 2.3 MODERATOR
- Review powers only: claim review, verify, reject (reason required), request info,
  mark duplicate (link required). No user/category/org/settings management — explicitly
  "should not automatically receive every system-administration permission" (Part 3.3).
- Cannot assign reports (area 7 = N): assignment belongs to ADMINISTRATOR / ORG_ADMIN.
  Rationale: separation of review and execution duties; owner may revisit in Phase 9.

### 2.4 ADMINISTRATOR (incl. system-admin scope A)
- Scope **A** operations (audited per FR-10.1): user role grants, org CRUD, category
  CRUD, system settings, global audit access, Swagger/OpenAPI access.
- Regular admin powers: review reports, assign, view all analytics.

### 2.5 ORGANIZATION_ADMIN
- Strictly org-scoped: cannot see other orgs' private data (Part 3.5).
- Assignment power limited to reports already VERIFIED and within their organization's
  remit; reassignment recorded in history (FR-4.6).

### 2.6 FIELD_WORKER
- Assignment-scoped only: view assigned tasks, start work, submit completion with
  evidence. Cannot self-assign, cannot verify/reject, cannot mark fully resolved
  (must pass RESOLUTION_REVIEW — Part 4.3, FR-4.7).

## 3. Enforcement requirements (Phase 4 acceptance basis)

1. Every non-public endpoint requires authentication (already true: `anyRequest().authenticated()`).
2. Role checks enforced at service/controller layer with tests per row of §1
   (positive + negative cases).
3. Org/ownership scoping enforced in queries (not just UI filters).
4. State-transition guards enforce [REPORT_LIFECYCLE.md](REPORT_LIFECYCLE.md); invalid
   transitions return 409 Conflict with a machine-readable error code.
5. Privileged A-scope operations emit audit events (FR-10.1).
6. **Phase 4 decision (implemented):** role is resolved from the DB on every request
   (via `JwtAuthenticationFilter` -> `UserRepository.findById`), so role changes and
   account revocation take effect immediately without re-authentication. The JWT role
   claim is informational only; authorities are built from the loaded `User` entity.
