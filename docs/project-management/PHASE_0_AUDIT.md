# MtaaFix Phase 0 — Project Audit Report

**Phase:** 0 — Project Audit and Foundation
**Status:** Audit complete — exit criteria NOT met without foundation stabilization
**Date:** 2026-10-09
**Repository:** https://github.com/felix202422/MtaaFix (public, `master`, local checkout synced with `origin/master` at time of audit, 14 commits)

---

## 1. Objectives

Understand the current state of MtaaFix before changing anything: repositories, structure,
documentation, dependencies, backend/web/mobile implementations, database, security, API,
tests, CI, Git state, UI coverage, and gaps — then stop for approval before Phase 1.

## 2. Repository responsibility map

**Key finding: the backend is fragmented into three divergent copies.**

| Location | Contents | Tracked files | Built by CI? |
|---|---|---|---|
| Root `pom.xml` + `src/` | 46 Java files — auth, report, notification, user, security, config. Empty dirs: `assignment/`, `audit/`, `category/`, `dashboard/`, `media/`, `db/migration/` | 48 | **No** |
| `backend/` | Own pom (adds H2), auth + report + notification, the **only** Flyway migration, 2 tests, `.env.example`, `.gitignore` | 51 | **Yes** (only CI target) |
| `Desktop/MtaaFix/` | Partial backend (assignment/audit/category/dashboard/notification controllers), **the actual Flutter app** (16 files), CHANGELOG/ROADMAP claiming Phases 0–14 | 42 | No |

`diff -rq` confirms the three copies diverge file-by-file (`ReportService`, `Report`,
`SecurityConfig`, `NotificationService`, and others all differ). Consolidation into one
canonical backend location is a prerequisite for all feature work.

Additional structural issues:

- **No root `.gitignore`** — `target/` and `web/node_modules/` are untracked but not ignored (accidental-commit risk).
- Junk tracked files: a garbled filename (`òàYEcÑ{…`) and `.README.md` (duplicate/hidden README).
- **`web/`** contains only `package.json` + three tsconfig files — `web/src/` and `web/public/`
  exist but are completely empty; no `index.html`, no React source, no design system.
- **`mobile/`** contains only `.gitignore` + `pubspec.yaml`; the real Flutter code lives under
  `Desktop/MtaaFix/mobile/`.

## 3. Technology and version inventory (installed and verified)

| Component | Version / state |
|---|---|
| Java | **25.0.4.1** installed ✔ — but `JAVA_HOME` points to a deleted JDK 21 path; `mvn` fails until corrected |
| Maven | 3.9.16 |
| Spring Boot parent | **4.1.1** (root and backend poms) |
| Backend libs | jjwt 0.12.6, springdoc 2.8.9, Flyway (+postgresql module), JTS 1.20.0 (PostGIS), PostgreSQL driver 42.7.7, Testcontainers 1.21.3 (declared, unused), H2 (backend pom only) |
| Node | v24.13.1 |
| Web deps | React 18.3, Vite 5.4, TypeScript 5.6 — **no router, state, HTTP, map, chart, or form libraries** |
| Flutter | **3.47.1** stable ✔ |
| gh CLI | Not installed → GitHub Projects board not inspectable (limitation) |

## 4. Baseline build and test results (actual commands, this audit)

| Check | Command | Result |
|---|---|---|
| Root backend compile | `mvn -B -DskipTests compile` | ❌ **FAIL** — ≥8 compile errors: `JwtTokenProvider` (`Key` vs `SecretKey` in `verifyWith`), `IdUtils` (missing `ZoneId` import), `SecurityConfig` (XSS header type mismatch), `ReportController` (Update→Create DTO mismatch), `ReportService` (missing `User` symbol), `ReportDto.getCode()` missing |
| CI backend tests | `mvn -B -f backend/pom.xml test` | ✅ **PASS** — 2 tests, 0 failures (~33s). Caveats: both are `contextLoads` duplicates (`com.mtaafix` + `com.mtaafix.test`), running on **H2**, so the PostGIS/Flyway migration is never executed |
| Web typecheck | `npx tsc --noEmit` | ✅ PASS — vacuous (zero source files) |
| Web build | `npm run build` | ❌ **FAIL** — `Could not resolve entry module "index.html"` |
| Flutter | `flutter pub get` / `flutter analyze` | ❌ **FAIL** — `boat_dirt_track` does not exist on pub.dev; analyze never ran |
| CI workflow | `.github/workflows/ci.yml` | Builds only `backend/pom.xml`; web step runs `npm ci` with **no package-lock.json** (will fail); Flutter skipped on runner. CI is effectively red/partial |

## 5. Database findings

- Single migration `backend/src/main/resources/db/migration/V1__create_base_tables.sql`:
  11 tables, UUID PKs, FKs, indexes, `GEOGRAPHY(POINT, 4326)` — sound on paper.
- ❌ **Broken SQL**: `mtaafix.reports` declares `FK … REFERENCES mtaafix.assignments(id)`
  **before** `assignments` is created → fails on real PostgreSQL. Undetected because tests use
  H2 without Flyway.
- Root `src/` copy has an **empty `db/migration/`** — no migrations for the code CI does not build.
- No seed data; Testcontainers declared but unwired.

## 6. Security review (baseline)

1. 🔴 **`@PreAuthorize` is a no-op everywhere** — annotations exist (auth controller; assignment/audit/category/dashboard in the Desktop copy), but **`@EnableMethodSecurity` is not declared anywhere**. No method authorization is enforced.
2. 🔴 **Lifecycle endpoints have zero role checks** — `verify`, `reject`, `assign`, `resolve`,
   `close` on `ReportController` are reachable by any authenticated user
   (`anyRequest().authenticated()` only). Violates spec §4.3 and §12.2.
3. 🔴 **No ownership checks** — `ReportService.get(id)` returns any report to any authenticated
   user; transitions never verify actor role or ownership.
4. 🟠 Default secrets committed: `JWT_SECRET` default `min-32-char-secret`, DB password
   `change-me` in `src/main/resources/application.yml`.
5. 🟠 `/api/v1/auth/**` fully `permitAll`; `cors.allowed-origins` property exists but no CORS
   configuration in `SecurityConfig`.
6. 🟠 Status transitions throw `IllegalStateException` → likely HTTP 500 instead of 409;
   history entries for `submit/assign/resolve/close` record **no actor**.
7. 🟠 Report reference generation (`IdUtils.generateReportCode`, `Mtf-…`) exists but is not
   verified to run on report creation; `docs/api/API.md` promises `Mtf-2026-<serial>` —
   doc/code drift.
8. No rate limiting, no account lockout; refresh/logout endpoints documented in API.md but not
   found implemented in any copy.

## 7. Page implementation matrix (36-page inventory)

| Area | Pages | Status |
|---|---|---|
| Public site (1–4) | 0/4 | **Missing** (web has zero source files) |
| Authentication (5–7) | 0/3 | Missing (backend register/login exist) |
| Citizen web (8–13) | 0/6 | Missing |
| Administration (14–24) | 0/11 | Missing (fragments of dashboard/category/audit controllers exist only in the Desktop copy) |
| Field worker (25–26) | 0/2 | Missing |
| Flutter mobile (27–36) | ~7 partial screens | **Non-buildable**: splash, login, register, report form, report list, notifications, profile exist under `Desktop/MtaaFix/mobile`; no map, dashboard, onboarding, or tests; `pub get` fails |

Design system: entirely unimplemented — no Tailwind, no tokens, no components; the
Tanzanian-flag palette/typography specification (spec §9) is not present anywhere.

## 8. Documentation status

Present: README, LICENSE, CODE_OF_CONDUCT, CONTRIBUTING, SECURITY, CHANGELOG,
`docs/api/API.md`, ADR-0001, ROADMAP, CODING_STANDARDS, SECURITY_STANDARDS,
issue + PR templates, CI workflow.

Missing: DEVELOPMENT.md, root `.env.example`, REPORT_LIFECYCLE.md, USER_ROLES.md
(role-permission matrix), DATABASE.md, TESTING_STRATEGY.md, UI_UX_SPECIFICATION.md,
DEPLOYMENT.md, ADRs beyond 0001.

**Integrity issue:** `Desktop/MtaaFix/CHANGELOG.md` claims Phases 0–14 complete while the root
CHANGELOG stops at 0.1.0 Phase 0 — and the actual state (empty web, broken mobile,
non-compiling root backend) contradicts those claims. `API.md` documents endpoints that exist
in no single copy consistently.

## 9. GitHub project status

- Repo reachable; `master` clean and synced. **0 issues, 0 PRs, single-branch workflow** (no branching strategy in use).
- GitHub Projects board: **could not be inspected** — no `gh` CLI and no API token available.
- No contributors were added; no branches or PRs were created.

## 10. Prioritized gap analysis / proposed backlog

**P0 — Foundation integrity (blocks everything):**

1. Consolidate to ONE backend location; relocate or remove the other two copies (including `Desktop/`).
2. Fix root backend compilation errors (or retire the non-building copy).
3. Enable method security; add role and ownership checks to all lifecycle endpoints.
4. Fix migration ordering bug; run tests against real PostgreSQL + PostGIS (Testcontainers).
5. Fix Flutter pubspec (remove `boat_dirt_track`), move app to canonical `mobile/`, make `flutter analyze` pass.
6. Create the real web scaffold (index.html, entry point) so `npm run build` passes; add package-lock.json.
7. Add root `.gitignore`; remove junk tracked files (`òàYEcÑ{…`, `.README.md`).
8. Point CI at the canonical backend + web + mobile and get it green.

**P1 — Phase 1 inputs:** role-permission matrix; lifecycle state model (current code implements
SUBMITTED → UNDER_REVIEW → VERIFIED → ASSIGNED → RESOLVED → CLOSED — missing
NEEDS_INFORMATION, IN_PROGRESS, RESOLUTION_REVIEW, REOPENED, DUPLICATE, REJECTED handling
policy review); privacy boundaries; API contract reconciliation; MVP definition.

**P2:** all remaining roadmap work.

## 11. Exit criteria assessment

The existing implementation **is** now understood well enough to plan changes without
unnecessary rewriting. However, the audit surfaced P0 defects that must be scheduled before
feature work. Recommended next step: **Phase 0.5 Foundation Stabilization** (the eight P0
items) or an approved Phase 1 kickoff — decision belongs to the project owner.
