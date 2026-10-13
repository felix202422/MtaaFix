# ADR-0004: Single-Role User Model (No Separate System-Admin Role)

- **Status:** Accepted — approved by owner with Phase 1 set (2026-10-13)
- **Context:** Master spec Part 3 describes seven role *types* including a distinct
  System Administrator (§3.7). Code stores a single `User.Role` enum with five values.
  Roles may also be org-scoped in future (Part 6.1).
- **Decision:** Keep five roles: CITIZEN, MODERATOR, ADMINISTRATOR, FIELD_WORKER,
  ORGANIZATION_ADMIN. "System administrator" is a **permission scope within
  ADMINISTRATOR** (user role grants, org/category management, system settings, global
  audit — marked **A** in the permission matrix), always audited. Public visitor is
  unauthenticated, not a role. Org-scoped membership via a future
  `organization_members` table (Phase 9) — not a new global role (D2).
- **Consequences:**
  - No enum migration needed for roles; matrix `docs/design/ROLE_PERMISSION_MATRIX.md`
    is the enforcement contract for Phase 4 tests.
  - High-risk A-scope operations must emit audit events (FR-10.1).
  - If separation of duties later demands a literal SYSTEM_ADMIN, add it via
    migration and split the A-scope checks — matrix rows already isolate them.
- **Alternatives considered:** Add SYSTEM_ADMIN as 6th enum now (rejected: no current
  operational need; speculative schema change); full permission/ACL tables (rejected:
  premature for v1, Part 6.1 "model that complexity only if required").
