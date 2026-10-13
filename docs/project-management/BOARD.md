# MtaaFix — Local Task Board

Local tracking board (GitHub `gh` CLI unavailable; revisit if auth is fixed).
Status values: `TODO` / `IN PROGRESS` / `DONE` / `BLOCKED`.
Last updated: 2026-10-13.

## Phase 0 — Repository remediation (MVP milestone)

| Task | Status | Notes |
|------|--------|-------|
| Audit contaminated home-dir repo | DONE | History rooted at `C:\Users\PC`; unrelated projects mixed in |
| Extract MtaaFix files from old HEAD to staging | DONE | `git archive` of old repo HEAD |
| Assemble canonical tree at `Desktop\MtaaFix` | DONE | Dropped junk (`.freebuff`, mojibake, duplicate root `src/`+`pom.xml`) |
| Merge three backend copies into one module | DONE | Root backend base + Desktop overlay + unique `ReportController`/ymls |
| Fix `ReportController` compile errors | DONE | `update(id,…)`; new `AssignReportRequest` DTO |
| Backend compile green | DONE | `mvn compile` BUILD SUCCESS (65 sources) |
| Backend tests green | DONE | `mvn test` 1/1 context-load on H2; fixed H2/PG dialect mismatch in test profile |
| Remove duplicate test class | DONE | Dropped `com.mtaafix.test.MtaaFixApplicationTests` |
| Fix mobile pubspec | DONE | Removed nonexistent `boat_dirt_track`; `intl` → `^0.20.3` |
| `flutter pub get` green | DONE | |
| `flutter analyze` green | DONE | Fixed 15 issues (missing `User` import, bad `localizations` param, undefined icon, unused imports, deprecated `withOpacity`) |
| Root `.gitignore` + `.env.example` | DONE | |
| Guard CI web step | DONE | Skips until `web/src` exists |
| Truth-ify CHANGELOG + ROADMAP | DONE | Removed unverified "COMPLETE" claims |
| This board | DONE | |
| `git init` + initial commit + push to MtaaFix remote | TODO | Push only to `github.com/felix202422/MtaaFix` |
| Phase 0 report + approval gate | TODO | Stop; do not start Phase 1 without approval |

## Next phases (blocked on Phase 0 approval)

| Phase | Status | Notes |
|-------|--------|-------|
| Phase 1 — Requirements & design reconciliation | TODO | Reconcile `docs/api/API.md` with merged backend; wireframes |
| Phase 3 — PostGIS install + runtime verification | BLOCKED | Needs Phase 0 approval; PostGIS install approved |
| Phase 5 — Web app MVP | TODO | `web/` is manifest-only today |

## Environment notes

- Java 25.0.4, Maven 3.9.16, Node 24.13.1, Flutter 3.47.1, PostgreSQL 18 running
- PostGIS not installed (approved for Phase 3)
- No Docker; no Testcontainers
- `gh` CLI token invalid; git push credentials work via Windows Credential Manager
- Old home-dir repo left untouched at `C:\Users\PC\.git` — never push from it
