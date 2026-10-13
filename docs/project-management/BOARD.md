# MtaaFix — Local Task Board

Local tracking board (GitHub `gh` CLI unavailable; revisit if auth is fixed).
Status values: `TODO` / `IN PROGRESS` / `DONE` / `BLOCKED`.
Phase numbering: master spec Phases 0–23 (decision D1).
Last updated: 2026-10-13.

## Current phase: Phase 1 APPROVED (2026-10-13) — next: Phase 2 (+ parallel Phase 3), awaiting start authorization

## Phase 0 — Project audit & foundation (COMPLETE)

| Task | Status | Notes |
|------|--------|-------|
| Audit contaminated home-dir repo | DONE | Rooted at `C:\Users\PC`; unrelated projects mixed in |
| Extract MtaaFix files from old HEAD | DONE | `git archive` staging |
| Assemble canonical tree | DONE | Junk dropped; three backend copies merged |
| Backend compile + tests green | DONE | `mvn test` 1/1 on H2 |
| Mobile pub get + analyze clean | DONE | 15 analyzer issues fixed; bogus dep removed |
| Root `.gitignore` / `.env.example` / CI guards | DONE | |
| Truth-ify docs; this board | DONE | |
| `git init` + push to MtaaFix remote | DONE | Commits `a18288d`, `69056ea` |

## Phase 1 — Requirements & system design (COMPLETE — APPROVED 2026-10-13)

| Deliverable | Status | File |
|-------------|--------|------|
| Requirements specification | DONE | `docs/requirements/SRS.md` |
| Role-permission matrix | DONE | `docs/design/ROLE_PERMISSION_MATRIX.md` |
| Report lifecycle specification (12 states, T1–T17) | DONE | `docs/design/REPORT_LIFECYCLE.md` |
| Initial domain model (code-reconciled) | DONE | `docs/design/DOMAIN_MODEL.md` |
| API design outline + conventions | DONE | `docs/api/API_DESIGN.md` |
| Decisions D1–D4 | DONE | SRS §1.5 + ADRs 0002–0004 |
| Backlog with acceptance criteria | DONE | `docs/project-management/BACKLOG.md` |
| ROADMAP restructured to Phases 0–23 | DONE | `docs/project-management/ROADMAP.md` |
| Commit + push | DONE | Commits `e0cad27` + approval bookkeeping pushed to `origin/master` |
| Phase 1 report + approval gate | DONE | Approved by owner 2026-10-13; docs flipped to APPROVED |

## Next phases (Phase 1 approved; start authorization pending)

| Phase | Status | Notes |
|-------|--------|-------|
| Phase 2 — Design system & UI foundation | READY | Tokens, components, responsive rules (web) |
| Phase 3 — Backend foundation completion | READY | PostGIS runtime verification (PostGIS install approved); OpenAPI export; DEVELOPMENT.md; may run parallel to Phase 2 |
| Phase 4 — AuthN/Z | BLOCKED | Depends on Phase 3; role-matrix enforcement + tests |
| Phase 5 — Core reporting workflow | BLOCKED | State machine migration + endpoints (BACKLOG B-5.x) |
| Phase 6–7 — Public site + citizen web | BLOCKED | Depend on Phases 2, 4, 5 |

## Environment notes

- Java 25.0.4, Maven 3.9.16, Node 24.13.1, Flutter 3.47.1, PostgreSQL 18 running
- PostGIS not installed (approved for Phase 3)
- No Docker; no Testcontainers
- `gh` CLI token invalid; git push works via Windows Credential Manager
- Old home-dir repo untouched at `C:\Users\PC\.git` — never push from it
