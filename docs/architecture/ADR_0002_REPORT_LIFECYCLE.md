# ADR-0002: Report Lifecycle State Model

- **Status:** Accepted (Phase 1, 2026-10-13) — pending owner approval of Phase 1 set
- **Context:** Master spec §4.1 requires the exact state model to be finalized in
  Phase 1 before implementing the full workflow. Code implements 8 states; the spec
  proposes a richer set including review-pause, resolution-review, duplicate, and
  reopen.
- **Decision:** Adopt 12 states: SUBMITTED, UNDER_REVIEW, NEEDS_INFORMATION, VERIFIED,
  ASSIGNED, IN_PROGRESS, RESOLUTION_REVIEW, RESOLVED, REJECTED, DUPLICATE, REOPENED,
  CLOSED. CLOSED is the terminal state after citizen confirmation or confirmation
  window. A distinct CANCELLED state is deferred; citizen withdrawal before review is
  REJECTED with reason code `CANCELLED_BY_CITIZEN` (D3).
- **Consequences:**
  - Phase 5 adds 4 enum values + `duplicate_of` column via Flyway; centralizes
    transitions in a state-machine guard; forbidden transitions → 409.
  - FIELD_WORKER can never directly produce RESOLVED (independent review enforced).
  - Full transition table, actors, and invariants: `docs/design/REPORT_LIFECYCLE.md`.
- **Alternatives considered:** Keep code's 8 states (rejected: loses resolution-review
  independence and duplicate tracking required by spec §4.2/4.6); add CANCELLED now
  (rejected: citizen-cancel-as-rejected-reason covers MVP need; revisit with policy).
