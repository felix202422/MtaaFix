# MtaaFix — Report Lifecycle Specification

> **Status:** v1.0 — DRAFT FOR APPROVAL · Phase 1 · 2026-10-13
> Decision D3 (approved): adopt the master spec state set plus CLOSED as the
> terminal state. CANCELLED as a distinct state is **deferred**; citizen withdrawal
> before review is represented as REJECTED with reason code `CANCELLED_BY_CITIZEN`.
> This document finalizes the state model required by master spec §4.1 before the
> complete workflow is implemented (Phase 5).

## 1. State model (12 states)

| State | Kind | Definition (Part 4.2 + extensions) |
| ----- | ---- | ---------------------------------- |
| SUBMITTED | Active | Accepted by the system; awaits review. Initial state on creation (FR-2.3). |
| UNDER_REVIEW | Active | An authorized reviewer is examining the report. |
| NEEDS_INFORMATION | Active (paused) | More information required before review continues. Citizen may edit and resubmit. |
| VERIFIED | Active | Report passed verification; eligible for assignment. |
| ASSIGNED | Active | Responsibility assigned to an organization/eligible worker. |
| IN_PROGRESS | Active | Authorized personnel actively working the issue. |
| RESOLUTION_REVIEW | Active | Completion claim submitted with evidence; awaits independent review. |
| RESOLVED | Pending-closure | Resolution met approval requirements; awaiting citizen confirmation window. |
| REJECTED | Terminal | Rejected under authorized policy with recorded reason (includes citizen-cancel reason code). |
| DUPLICATE | Terminal | Identified as duplicate of a canonical report; relationship recorded. |
| REOPENED | Active | Previously resolved issue returned to active workflow under authorized rule. |
| CLOSED | Terminal | Report complete: resolution confirmed (or confirmation window elapsed). No further transitions. |

```
SUBMITTED ──claim──► UNDER_REVIEW ──verify──► VERIFIED ──assign──► ASSIGNED ──start──► IN_PROGRESS
    │                     │                                                     │
    │                     ├──request info──► NEEDS_INFORMATION ──resubmit──► UNDER_REVIEW
    │                     │                                                     │
    │                     ├──reject──► REJECTED (terminal)                     │
    │                     └──duplicate──► DUPLICATE (terminal)                 │
    │                                                                          │
    └──cancel (citizen)──► REJECTED(reason=CANCELLED_BY_CITIZEN)               │
                                                                               ▼
RESOLVED ◄──approve──────── RESOLUTION_REVIEW ◄──submit completion─────────────┘
   │                              ▲
   │confirm / window elapsed      └──return to work── IN_PROGRESS
   ▼
 CLOSED ──(authorized reopen, policy window)──► REOPENED ──► UNDER_REVIEW
```

## 2. Transition table

Columns per master spec §4.3. "Notify" = in-app notification to affected parties
(FR-6.1). "Audit" = privileged-event audit record where applicable (FR-10.1).
"Public" = whether the transition appears in the public timeline of a public report.

| # | From | To | Actor role | Required fields | Evidence | Reason | Notify | History | Public | Reversible |
| - | ---- | -- | ---------- | --------------- | -------- | ------ | ------ | ------- | ------ | ---------- |
| T1 | (new) | SUBMITTED | CITIZEN (creator) | category, title, description, location | Optional photo | No | Creator confirm | Yes | Yes | No (cancel instead) |
| T2 | SUBMITTED | UNDER_REVIEW | MODERATOR, ADMINISTRATOR | — | — | No | Creator | Yes | Yes | Yes: T2r below |
| T2r | UNDER_REVIEW | SUBMITTED | MODERATOR, ADMINISTRATOR | — | — | No | Creator | Yes | Yes | Unclaim review |
| T3 | UNDER_REVIEW | NEEDS_INFORMATION | MODERATOR, ADMINISTRATOR | message (mandatory) | — | Msg serves as reason | Creator | Yes | Yes | Via T4 |
| T4 | NEEDS_INFORMATION | UNDER_REVIEW | CITIZEN (owner) resubmit | Edits to report | Optional new photo | No | Moderators queue | Yes | Yes | Via T3 |
| T5 | UNDER_REVIEW | VERIFIED | MODERATOR, ADMINISTRATOR | — | — | No | Creator | Yes | Yes | No (use reopen after resolve only) |
| T6 | UNDER_REVIEW | REJECTED | MODERATOR, ADMINISTRATOR | reasonCode + comment (mandatory) | — | Mandatory | Creator | Yes | Yes | Policy: citizen may file new report; no direct undo |
| T7 | UNDER_REVIEW | DUPLICATE | MODERATOR, ADMINISTRATOR | canonicalReportRef (mandatory link) | — | Mandatory | Creator | Yes | Yes | Admin may restore to UNDER_REVIEW (T7r) |
| T7r | DUPLICATE | UNDER_REVIEW | ADMINISTRATOR | — | — | Mandatory comment | Creator | Yes | Yes | Restoration |
| T8 | SUBMITTED | REJECTED | CITIZEN (owner) | reasonCode=CANCELLED_BY_CITIZEN | — | Mandatory (fixed code) | — (self action) | Yes | No | No |
| T9 | VERIFIED | ASSIGNED | ADMINISTRATOR, ORG_ADMIN | organizationId; optional assigneeId (must be FIELD_WORKER/eligible) | — | No | Creator + assignee | Yes | Yes | Yes: reassign (T9r) |
| T9r | ASSIGNED | ASSIGNED | ADMINISTRATOR, ORG_ADMIN (own org) | new assignee/org | — | Mandatory comment | Old + new assignee | Yes | No | Reassignment; assignment history recorded |
| T10 | ASSIGNED | IN_PROGRESS | FIELD_WORKER (assignee), ORG_ADMIN | — | — | No | Creator | Yes | Yes | Yes: back to ASSIGNED (T10r, admin/org only) |
| T11 | IN_PROGRESS | RESOLUTION_REVIEW | FIELD_WORKER (assignee), ORG_ADMIN | completionNote | **Mandatory** completion evidence (≥1 photo) | Note optional | Reviewers + creator | Yes | Yes | Reviewer returns via T12 |
| T12 | RESOLUTION_REVIEW | IN_PROGRESS | MODERATOR, ADMINISTRATOR, ORG_ADMIN | comment (mandatory) | — | Mandatory | Assignee + creator | Yes | Yes | Return to work |
| T13 | RESOLUTION_REVIEW | RESOLVED | MODERATOR, ADMINISTRATOR | — | Evidence retained | No | Creator + assignee | Yes | Yes | Via T15 reopen or T14 never (resolved is pre-closure) |
| T14 | RESOLVED | CLOSED | System (confirmation window, default 7 days) or CITIZEN confirm, or ADMINISTRATOR | optional citizenFeedback | — | Optional feedback | Org + admin stats | Yes | Yes | Via T16 reopen |
| T15 | RESOLVED | IN_PROGRESS | MODERATOR, ADMINISTRATOR (citizen dispute) | disputeReason (mandatory) | — | Mandatory | Assignee | Yes | Yes | Returns to active work |
| T16 | CLOSED | REOPENED | ADMINISTRATOR (policy: within reopen window, default 30 days, or justified override) | justification (mandatory) | — | Mandatory | Creator + org | Yes | Yes | Then T17 |
| T17 | REOPENED | UNDER_REVIEW | MODERATOR, ADMINISTRATOR | — | — | No | Creator | Yes | Yes | Normal review resumes |

Any transition not listed is **forbidden** (FR-3.8). Attempts return HTTP 409 with
error code `INVALID_STATUS_TRANSITION`.

## 3. Invariants

1. Every transition appends exactly one `ReportStatusHistory` row with from/to,
   actor, timestamp, and reason/comment when applicable (FR-3.7). History is
   append-only; ordinary users cannot rewrite it (Part 4.5).
2. A report always has exactly one current status; the latest history row's
   `to_status` must equal `Report.status` (invariant verified by tests, Phase 5).
3. Only one open assignment per report at a time; reassignment supersedes the prior
   assignment record (status CANCELLED on prior assignment).
4. FIELD_WORKER can never produce RESOLVED directly — resolution requires T13
   independent review (Part 4.3).
5. Citizen confirmation is **not** the only resolution path: ADMINISTRATOR may close
   after the confirmation window (Part 4.3 final paragraph).
6. REOPENED requires authorization and justification; it never erases prior history.
7. DUPLICATE always stores the canonical report reference; the canonical report's
   history notes the duplicate link.

## 4. Unique references (Part 4.4)

- Format: `MTF-<YYYY>-<6-digit serial>` e.g. `MTF-2026-000042`.
- Generated by backend on creation (T1), unique constraint in DB, stable for life.
- Displayed to citizens; internal UUID primary key never exposed in public contexts
  (API may expose UUID to authorized owners/moderators for routing — see API_DESIGN).

## 5. Resolution verification policy (Part 4.7)

1. Field worker submits completion claim with mandatory evidence (T11).
2. Authorized reviewer checks claim (moderator/admin/org admin per matrix).
3. Approved → RESOLVED (T13); rejected → back to IN_PROGRESS with reason (T12).
4. Citizen notified at RESOLVED; may confirm, dispute (T15), or do nothing.
5. Closed automatically after confirmation window or on confirm (T14).
6. All steps auditable via status history (+ audit log for privileged acts).

## 6. Duplicate policy (Part 4.6)

- v1: administrative review with explicit linking (T7). No automated detection.
- Automated detection (post-MVP, Phase 21) must never silently discard a report.

## 7. Implementation gap (code vs this spec)

Current `Report.Status` enum has 8 values; missing: NEEDS_INFORMATION,
RESOLUTION_REVIEW, DUPLICATE, REOPENED. Current `ReportService` implements a subset
of transitions with per-method checks and no centralized state machine. Phase 5 work:
add missing enum values via Flyway migration, centralize the transition table as a
guard (enum-based state machine), implement T3/T4/T7/T11–T17, and add tests for every
row of §2 including forbidden-transition negatives. Until then, the API surface
implemented in Phase 2 code is considered provisional (report in Phase 5 report).
