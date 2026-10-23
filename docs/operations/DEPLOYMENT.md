# Deployment Runbook - MtaaFix

Phase 18 (owner-approved scope 2026-10-22): **deployment readiness only** -
production profile, this runbook, and a verified local production-profile
drill. No remote server is provisioned; once a host exists, execute this
document end-to-end.

## 1. Prerequisites

- JDK 25 + Maven 3.9+
- PostgreSQL 18 + PostGIS 3.6 (target database `mtaafix`; Flyway applies
  V1-V8 on boot, creating required extensions automatically)
- Node 22+ (web build); Flutter stable (mobile builds, not a server dep)
- Optional reverse proxy (nginx/traefik) terminating TLS

## 2. Configure the environment

```bash
cp .env.example .env   # then edit - never commit .env
```

Production secrets (ProdSecretsGuard refuses boot while any is blank or a
shipped default; `JWT_SECRET` and `GEO_FUZZ_SECRET` must be >= 32 chars):

- `SPRING_DATASOURCE_PASSWORD`
- `JWT_SECRET`
- `GEO_FUZZ_SECRET`

Also set `SPRING_DATASOURCE_URL` / `SPRING_DATASOURCE_USERNAME`,
`CORS_ALLOWED_ORIGINS` (the public web origin(s), comma separated), and the
`NOTIFICATIONS_*` / `SPRING_MAIL_*` block once an SMTP provider exists.

## 3. Build

```bash
# Backend fat jar (runs the full H2 test suite)
mvn -B -f backend/pom.xml package
# -> backend/target/mtaafix-0.1.0-SNAPSHOT.jar

# Web static bundle
(cd web && npm ci && npm run build)
# -> web/dist/
```

## 4. Run (production profile)

```bash
set -a; source .env; set +a            # or systemd EnvironmentFile=.env
SPRING_PROFILES_ACTIVE=prod java -jar backend/target/mtaafix-0.1.0-SNAPSHOT.jar
```

- Flyway migrates on boot (V1-V8); JPA validates the schema
  (`ddl-auto: validate`) and fails fast on drift.
- Health: `curl http://localhost:8080/actuator/health` -> `{"status":"UP"}`.
  The prod profile exposes **health only** (no env/beans/metrics endpoints).
- Missing/weak secrets abort startup with:
  `Refusing to start with the prod profile: <property> ...`

Serve `web/dist/` from any static host/proxy and proxy `/api` (and
`/actuator/health`) to the jar.

## 5. Reverse proxy - rate limiter sees real client IPs (B-16.4)

The prod profile sets `server.forward-headers-strategy: framework`, so the
app honours `X-Forwarded-*` sent by the proxy: redirects, secure cookies and
the per-IP rate limiter (Phase 16) key on the true client address
(covered by `ForwardedIpRateLimitTest`).

```nginx
location /api/ {
    proxy_pass http://127.0.0.1:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

Bind the jar to loopback (or a private interface) so `X-Forwarded-For`
cannot be spoofed by clients hitting the app directly.

## 6. Local production-profile drill (Phase 18 evidence, executed 2026-10-22)

1. **Refusal (shipped defaults):** `SPRING_PROFILES_ACTIVE=prod java -jar
   mtaafix-0.1.0-SNAPSHOT.jar` with the shipped `JWT_SECRET` default exits
   1: `JWT secret must be at least 32 characters.` With a real JWT/DB but
   the default `GEO_FUZZ_SECRET`, the guard itself aborts startup:
   `prodSecretsGuard: Refusing to start with the prod profile:
   geo.fuzz-secret still holds a shipped development default` (all other
   default/blank/short combinations covered by `ProdSecretsGuardTest` ×6).
2. **Boot (real secrets, fresh database):** `prod` profile against a
   freshly created scratch database -> Flyway applied **V1-V8** (8/8
   success), `ddl-auto: validate` passed, `/actuator/health` returned
   `{"status":"UP"}` within ~10 s, `GET /api/v1/reports/public?size=5`
   responded 200; process stopped and the scratch database dropped
   afterwards.
3. **Suites:** backend **155/155** (146 H2 + 9 PostgreSQL/PostGIS IT),
   web lint + **68/68** + production build, mobile analyze + **171/171**.

## 7. CI and releases

- `.github/workflows/ci.yml` runs on push/PR to `master` (parallel jobs):
  - **backend** - `mvn test`: full H2 suite plus PostgreSQL+PostGIS
    integration tests (`MTAAFIX_PG_IT=true`) against a
    `postgis/postgis:18-3.6` service container
  - **web** - `npm run lint && npm run test && npm run build`
  - **mobile** - `flutter analyze && flutter test`
  - **secrets** - gitleaks secret scan over history
- Pushing a tag `v*` runs `.github/workflows/release.yml`: rebuilds and
  re-tests the jar, builds `web/dist`, and attaches the executable jar and
  a `web-dist.zip` to the GitHub Release.

## 8. Stop / rollback

Stop the process (`systemctl stop mtaafix` or SIGTERM); keep the previous
jar alongside for instant rollback. Flyway has no automatic undo - database
rollback means restoring a backup. Backup/restore automation ships in
Phase 19 (see ROADMAP/BACKLOG).

## 9. Out of scope (owner decision 2026-10-22)

- Remote host provisioning - readiness only (this runbook + local drill).
- Docker image/compose - skipped; this environment has no container runtime
  and the deployment target is a plain systemd/JAR host. Revisit if a
  container platform appears.
- Backups automation - Phase 19.
