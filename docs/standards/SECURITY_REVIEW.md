# Security Review - Phase 16 (2026-10-22)

Structured security review per master spec Phase 16 across the ten mandated
areas (authentication, authorization, input validation, uploads, secrets,
dependencies, logging/privacy, rate limiting, CORS/deployment, backups).
Findings are evidenced against this repository (tests, live battery, static
inspection); remediations for Critical/High plus two cheap Mediums were
implemented in this phase per owner-approved scope (2026-10-22):

1. Critical/High findings fixed; Medium/Low documented with dispositions
2. Rate limiting implemented (SECURITY_STANDARDS gap, was absent)
3. JWT revocation via `token_version` (option a: reset **and** change)
4. Offline-storage encryption (D5) deferred with documented disposition
5. Dependency review via `npm audit` + Maven version inventory (no NVD scan)
6. This document lives next to `SECURITY_STANDARDS.md`

## Summary of findings

| ID | Area | Severity | Finding | Disposition |
| -- | ---- | -------- | ------- | ----------- |
| F1 (was D1) | Input validation | High | Wrong content-type probes answered 500 on the stale Phase 15 jar (live evidence); handler gaps | **Fixed** - explicit 415 handler + `WebErrorHandlingTest` + live probe |
| F2 (was D2) | Input validation | High | Wrong HTTP method answered 500 (same fall-through class) | **Fixed** - explicit 405 handler + tests + live probe |
| F3 (was D3) | Authentication | High | JWTs survive password reset/change (stateless, no revocation) | **Fixed** - `users.token_version` (Flyway V7), `tv` claim on access+refresh, filter/refresh checks, reset & change bump version; change now rotates the pair (clients adopt it) |
| F4 (was D4) | Uploads / validation | Medium | Media attach/delete status gate existed only in the mobile UI | **Fixed** - server-side gate on RESOLVED/REJECTED/DUPLICATE/CLOSED (409) + tests + live probe |
| F5 (was D5) | Data protection | Medium | Mobile offline drafts/outbox stored unencrypted at rest | **Deferred** (owner decision) - tokens already protected by `flutter_secure_storage` (Keystore/KeyChain); drafts/outbox encryption is a Phase 20 item (BACKLOG) |
| F6 (was - ) | Rate limiting | High (standard gap) | No rate limiting although SECURITY_STANDARDS requires it on auth + report submission | **Fixed** - per-IP fixed one-minute windows on `POST /api/v1/auth/**` and `POST /api/v1/reports`; 429 + `Retry-After` + ApiError body; configurable; 429 proven live (attempt 31) |
| F7 | Secrets | High (latent) | Shipped dev defaults for JWT secret, DB password, geo-fuzz secret; no prod fail-fast | **Fixed (guard)** - `ProdSecretsGuard` (`prod` profile) refuses known-weak defaults/short secrets at startup; `.env.example` annotated; activating the profile is Phase 18's job |
| F8 | Deployment | Medium (ops wart) | `/actuator/health` reported DOWN (503) whenever SMTP was unconfigured even though the email channel is intentionally off | **Fixed** - `management.health.mail.enabled` mirrors `NOTIFICATIONS_EMAIL_ENABLED` (verified live: health UP) |
| F9 | Dependencies | Info | Web production audit clean; web dev toolchain advisories; backend stack current-gen | **Documented** - details below; toolchain majors on BACKLOG |
| F10 | Logging/privacy | Info | Log statements carry paths/IDs/messages only; reset tokens explicitly kept out of logs; no credentials/tokens/precise coordinates logged | **Accepted** (evidence: code audit below) |
| F11 | Backups/DR | Medium (process) | No backup/restore runbook or automation | **Documented** - runbook below; automation deferred to ops/deploy phase |
| F12 | CORS/deploy | Info | CORS allow-list env-driven, swagger admin-only, actuator exposes only health; rate-limiter sees socket IP (proxy caveat) | **Accepted** + Phase 18 handoff note (forwarded headers) |

## Review areas (1-10)

### 1. Authentication and session management
- BCrypt password hashing; stateless JWT (access 15 min, refresh 7 d).
- Refresh tokens carry the same `tv` claim as access tokens; `POST /auth/refresh`
  rejects stale versions (`UserService.refresh`).
- **F3 remediation (this phase):** `users.token_version` (V7), bumped by
  `PasswordResetService.reset` and `.change`. The JWT filter compares the `tv`
  claim to the stored version; tokens issued before the first password event
  have no claim and read as 0 (column default) so live sessions survive the
  upgrade. `/auth/password/change` now returns a rotated `AuthResponse`; web
  (`ProfilePage` + `setAuthTokens`) and mobile (`auth_controller.changePassword`
  + `setSession`) adopt it - covered by `PasswordResetTest`, web `Phase13.test`,
  mobile `auth_controller_test`.
- Logout remains client-side (stateless design); 15-minute access TTL bounds
  residual risk (documented design decision, unchanged).

### 2. Authorization and privilege controls
- Method security (`@PreAuthorize`) + service-layer guards; role and active
  flag read from the database on every request, so privilege changes and
  deactivations apply immediately (SECURITY_STANDARDS "rotate on privilege
  changes" is satisfied by design here - no token rotation needed).
- Report scoping: citizens see own reports; moderators/admins all; org-admins
  own org; field workers own assignments (`assertCanRead`, matrix-tested).
- Media deletion is uploader/privileged-only; attach requires read access.
- Evidence: `AuthorizationMatrixTest` 15/15, live Phase 15 battery 63/63.

### 3. Input validation and error handling
- Bean validation on DTOs; service-level plausibility checks (locations,
  report codes, idempotency keys).
- **F1/F2 remediation:** explicit `HttpMediaTypeNotSupportedException` (415)
  and `HttpRequestMethodNotSupportedException` (405) handlers now emit the
  standard ApiError body; malformed JSON already mapped to 400 on disk - the
  Phase 15 500-evidence came from a stale Oct-20 jar, which this phase replaced
  (fresh `mvn package`) before re-probing. Live battery: text/plain POST ->
  415 `UNSUPPORTED_MEDIA_TYPE`; GET on POST-only -> 405 `METHOD_NOT_ALLOWED`;
  `{not-json` -> 400 `MALFORMED_REQUEST`; unknown path under permitAll -> 404.

### 4. File upload security
- MIME allowlist (jpeg/png/webp) + magic-byte check + size cap
  (`MEDIA_MAX_FILE_SIZE_BYTES`, default 20 MiB) + per-report cap; stored under
  `./uploads` (local-disk placeholder - Phase 18 hardening candidate).
- **F4 remediation:** `ReportService.assertMediaEditable` rejects attach
  (JSON + multipart) and delete once the report is RESOLVED/REJECTED/DUPLICATE/
  CLOSED with 409 `INVALID_STATUS_TRANSITION`, mirroring the mobile
  `canEditMedia` rule. Evidence: `MediaLimitsTest.terminalStatusBlocksAttachmentChanges`
  (all four statuses x three operations) + live probe (upload -> cancel -> upload 409).

### 5. Secrets management
- Secrets via environment (`JWT_SECRET`, `SPRING_DATASOURCE_PASSWORD`,
  `GEO_FUZZ_SECRET`, mail vars); none committed (`.env` ignored;
  `.env.example` holds placeholders only).
- `JwtTokenProvider` rejects secrets < 32 chars at startup.
- **F7 remediation:** `ProdSecretsGuard` (`@Profile("prod")`) fails startup when
  `jwt.secret`, `spring.datasource.password`, or `geo.fuzz-secret` still holds a
  shipped default (`min-32-char-secret*`, `change-me`, `mtaafix-local-dev-geo-fuzz-secret`),
  is blank, or (jwt/geo) is shorter than 32 chars. Inert outside the profile so
  local/test runs are unaffected. Phase 18 must activate `prod` + real values.
- Swagger UI/API docs: admin-role-only (`SecurityConfig`); `/actuator` exposes
  health only; health is authenticated-free by design and now reports UP while
  the email channel is off (**F8**: `management.health.mail.enabled` mirrors the
  feature flag).

### 6. Dependency review
- Web production (`npm audit --omit=dev`): **0 vulnerabilities**.
- Web dev toolchain (`npm audit`): 5 advisories - `@vitest/mocker`/`vitest`
  (moderate, 2), `esbuild`/`vite` (moderate - dev-server exposure only),
  `tinypool` (critical prototype-pollution gadgets in test workers). All are
  development/test-only; none ship. Fixes require breaking majors (vitest 5,
  vite 8) - BACKLOG item, not phase-blocking.
- Backend (`mvn versions:display-dependency-updates`): no updates flagged for
  Spring Boot 4.1.1 line, jjwt 0.12.6, postgresql 42.7.7, springdoc 2.8.9;
  only transitive/buildtool notes (cassandra driver query-builder patch,
  groovy major for build tools, pulsar buildtools). Stack is current-generation;
  no known-CVE exposure identified by inspection. Full NVD scan intentionally
  skipped per owner decision (network/API friction); revisit in ops phase if a
  scanner is provisioned.
- Mobile: small pubspec surface (http, flutter_secure_storage, flutter_map,
  geolocator, image_picker, connectivity_plus, shared_preferences, etc.);
  no offline audit tool available - rely on platform updates (documented).

### 7. Logging and privacy
- Audited all `log.*` calls: messages carry request paths, IDs (delivery id,
  user subject id at DEBUG), or static text. Password-reset flow logs
  "token not logged" explicitly; raw reset tokens leave the system only via
  the reset email (never persisted - SHA-256 hashed at rest).
- `GlobalExceptionHandler` logs paths, not bodies. No passwords, tokens,
  emails, phone numbers, or coordinates are logged (grep evidence in review
  session). `audit_log` stores action metadata (admin-only surface) - PII
  there is by design and access-controlled.
- Public API responses strip identity/comments/media/precise location
  (fuzzed geometry; `PublicApiTest`).

### 8. Rate limiting (F6 - implemented this phase)
- `RateLimitFilter` (security chain, before the JWT filter): per-IP fixed
  one-minute windows; buckets keyed by client IP + path so hammering one
  endpoint cannot starve others. Limited: every `POST /api/v1/auth/**`
  (30/min default) and `POST /api/v1/reports` (20/min default). Everything
  else (GETs, other POSTs) is exempt - proven by tests.
- Response: 429 + `Retry-After` + ApiError `RATE_LIMITED` body.
- Config: `RATE_LIMIT_ENABLED`, `RATE_LIMIT_AUTH_PER_MINUTE`,
  `RATE_LIMIT_REPORT_PER_MINUTE` (application.yml). Disabled under the test
  profile so the suite is unaffected; `RateLimitTest` re-enables it per-context.
- Evidence: `RateLimitTest` (4 tests) + live battery (429 at attempt 31,
  Retry-After present).
- Deployment caveat (Phase 18 handoff): the filter counts the socket IP.
  Behind a reverse proxy, enable forwarded-header handling
  (`server.forward-headers-strategy=framework` + trusted proxy config) or all
  clients will share the proxy's bucket.

### 9. CORS and deployment configuration
- CORS allow-list from `CORS_ALLOWED_ORIGINS` (default localhost:5173); methods
  and headers restricted; `/api/**` only.
- Stateless sessions; CSRF disabled (bearer-token API, no cookies); security
  headers on (Spring defaults incl. frame options deny).
- Swagger/OpenAPI admin-only; actuator exposes health only; health no longer
  false-DOWN without SMTP (**F8**).
- Prod profile guard (**F7**) + rate-limit proxy caveat (**F8 handoff**) are
  the Phase 18 integration points.

### 10. Backups and data protection
- Sensitive stores: users (emails, phone, bcrypt hashes), reports (description,
  fuzzed coordinates - true coordinates never persisted), audit_log,
  media files on local disk, notification deliveries.
- **F11 runbook** (documentation-level remediation; automation deferred):
  - Backup: `pg_dump -Fc -f mtaafix-$(date +%F).dump mtaafix` (run as a
    scheduled task; retain per policy) plus the `uploads/` directory
    (`uploads` volume is stateful).
  - Restore: stop the app, `pg_restore -d mtaafix <dump>`, restore `uploads/`,
    start the app (Flyway re-validates schema).
  - Secrets (`JWT_SECRET`, `GEO_FUZZ_SECRET`) must be backed up in the
    secret store, not beside the dump: rotating `GEO_FUZZ_SECRET` changes
    public-location fuzzing determinism; rotating `JWT_SECRET` invalidates
    all outstanding tokens (acceptable during an incident).
  - Point-in-time recovery is not configured (single-node Postgres) - noted
    as an ops-phase gap.

## Verification (this phase)

| Suite | Result |
| ----- | ------ |
| Backend H2 (`mvn test`) | **144/144** (was 133; +11 new: rate limit 4, error handling 4, token provider 2, media gate 1) |
| Backend PG (`MTAAFIX_PG_IT=true`) | **8/8** - Flyway V7 applied on live PostgreSQL + asserted |
| Web (`tsc --noEmit`, `vitest run`) | 0 errors, **68/68** |
| Mobile (`flutter test`) | **171/171** (+1 rotation test) |
| Live battery (`backend/target/battery-p16.ps1`, rebuilt jar, :8085) | **23/23** - health UP, register/login/me, report create, public list, 401/403 probes, swagger closed, 415/405/400 ApiError, D4 gate (upload -> cancel -> 409), D3 revocation E2E (old pair dead, new pair live), D6 rate limit (429 at 31st attempt + Retry-After) |

## Deferrals and handoffs

| Item | Owner decision | Target |
| ---- | -------------- | ------ |
| Offline drafts/outbox encryption (F5) | Deferred | Phase 20 (keystore-backed key; device testing) |
| Prod profile activation + real secrets (F7 wiring) | Out of phase scope | Phase 18 deployment |
| Forwarded-header/proxy config for rate limiter IPs (F6 caveat) | Out of phase scope | Phase 18 deployment |
| Backup automation / PITR (F11) | Documentation now, automation later | Ops/deploy phase |
| Web dev-toolchain majors (vitest 5 / vite 8, F9) | Breaking upgrades scheduled later | BACKLOG |
| Full NVD dependency scan | Skipped per owner (network friction) | Ops phase if scanner provisioned |
| Local-disk media storage hardening (F4 adjacent) | Documented placeholder | Phase 18 |
