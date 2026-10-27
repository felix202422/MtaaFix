# Troubleshooting — MtaaFix Operations

**Status:** Complete · **Phase:** 19 (Observability and Operations)
**Companions:** [DEPLOYMENT.md](DEPLOYMENT.md) (boot, proxy, backups) ·
[`ops/prometheus/alerting-rules.yml`](../../ops/prometheus/alerting-rules.yml)
(alerts as code) · [PERFORMANCE_REVIEW.md](../standards/PERFORMANCE_REVIEW.md) (NFR-7 evidence)

Every alert rule's `runbook` annotation points at
`docs/operations/TROUBLESHOOTING.md#alert-triage` — the section below.

---

## Alert triage

Alerts are defined as code in `ops/prometheus/alerting-rules.yml`
(owner decision 2026-10-23: rules + docs, no live Prometheus/Grafana in this
repo — the file loads into any Prometheus via `rule_files`).

**First move for any alert:**

1. Note the firing labels (`uri`, `method`, `status`, `job`).
2. Grab a recent `requestId` from the app log (`grep requestId`) — every WARN
   line carries it (see [Log correlation](#log-correlation-with-x-request-id)).
3. Cross-check live numbers: `curl http://localhost:8080/actuator/prometheus`.

| Alert | Sev | Fires when | Triage |
|---|---|---|---|
| `MtaaFixTargetDown` | critical | `up{job="mtaafix"} == 0` for 1m | Is the process alive? `curl localhost:8080/actuator/health`. If dead, read the service log tail (secret guard aborts prod boot with exit 1 — see DEPLOYMENT §4/§6). If healthy but unscraped: port/firewall between Prometheus and the app. |
| `MtaaFixScrapeMissing` | warning | `application="mtaafix"` series absent for 5m | Scrape reachable but no request metrics → app not serving traffic or scrape path broken. Confirm `GET /actuator/prometheus` returns 200 and contains `http_server_requests_seconds_count{application="mtaafix"}`. |
| `MtaaFixHighErrorRate` | critical | 5xx ratio > 5% of API responses (5m window, actuator excluded) for 5m | Find the failing route: `http_server_requests_seconds_count{status=~"5.."}` by `uri`. Then grep the log for that route + `requestId` → exception stack. 500s from Flyway/DB → check DB reachable (below); 401/403 storms are *not* this alert (4xx excluded). |
| `MtaaFixSlowRequests` | warning | p95 > **1 s** on a route for 10m | NFR-7 budget for list queries is 500 ms; the alert sits at 2× (1 s) to absorb spikes. Reproduce with `scripts/measure-perf.ps1` (p95 numbers in `scripts/perf-results.json`). Persistent slowness on list endpoints → pool saturation or missing indexes (both alerted separately); slowness everywhere → `MtaaFixHighCpu`/`MtaaFixHeapPressure` below. First-touch WARNs (~1–2 s at boot) are expected and not a regression — see PERFORMANCE_REVIEW §8. |
| `MtaaFixDbPoolSaturated` | warning | `hikaricp_connections_active / hikaricp_connections_max > 0.9` for 5m | Watch `hikaricp_connections_pending`: rising → real exhaustion, requests are queueing (`MtaaFixDbPoolQueueing` will follow). Causes: slow queries holding connections (correlate with `MtaaFixSlowRequests`), traffic spike, or pool too small (`spring.datasource.hikari.maximum-pool-size`). Verify DB side: `SELECT count(*), state FROM pg_stat_activity GROUP BY state;`. |
| `MtaaFixDbPoolQueueing` | critical | `hikaricp_connections_pending > 10` for 2m | Users are blocked on the DB *now*. Check `pg_stat_activity` for `idle in transaction` sessions and long `query` durations; kill the offender or scale pool/CPU. This is the "requests will time out" stage — treat as user-facing outage. |
| `MtaaFixHeapPressure` | warning | heap used/max > 90% for 10m | Grab a thread/heap dump before restart if reproducible. Common causes: oversized unpaginated queries (list endpoints are paged and NFR-7-tested), media/byte arrays buffered per request, leak in a cache. Sustained-but-stable 90%+ with good latency → consider raising `-Xmx`. |
| `MtaaFixHighCpu` | warning | `process_cpu_usage > 0.85` for 10m | Correlate with request rate and `MtaaFixSlowRequests`. CPU spikes at low traffic → check the GC (`jvm_gc_*` in the scrape), or PostGIS-heavy geo queries. CPU high + pool queueing → DB is likely the bottleneck, not the app. |
| `MtaaFixDiskAlmostFull` | warning | disk free < 10% for 10m | Two consumers need space: `uploads/` (media) and the WAL archive / `backend/scripts/backups/` (backup script fails when full). Trim old backups (`backup.ps1 -RetentionDays`), rotate logs, grow the volume. Never delete WAL files an active archive is still writing. |

### Loading the rules

```yaml
# prometheus.yml
scrape_configs:
  - job_name: mtaafix
    metrics_path: /actuator/prometheus
    static_configs: [{ targets: ['app-host:8080'] }]
rule_files:
  - ops/prometheus/alerting-rules.yml
```

Metric names were verified against a live scrape (sample kept by
`PrometheusMetricsTest` → `target/prometheus-sample.txt`).

---

## Log correlation with X-Request-Id

`RequestTimingFilter` (Phase 19) assigns a correlation id to **every** request:

- An inbound `X-Request-Id` header is honoured when it matches
  `[A-Za-z0-9._-]{1,64}` (gateway/trace ids pass through); otherwise a UUID
  is generated. Either way the id is **echoed in the response header**.
- The id is put in the SLF4J **MDC as `requestId`**, so every log line the
  request produces — including WARN/ERROR from any logger — starts with
  `[<requestId>]`.
- Requests slower than `timing.warn-threshold-ms` (500 ms) log
  `Slow request: <METHOD> <uri> took N ms`.

Workflow when a user reports an error:

1. Get the id: response header `X-Request-Id` (browser devtools / client
   log), or ask the user for it if the client shows it.
2. Grep the service log for that id — you get the full trail for that one
   request across all loggers:
   ```
   grep '\[<requestId>\]' service.log
   ```
3. No id at hand? Grep by endpoint + time window instead:
   ```
   grep 'Slow request: POST /api/v1/auth/login' service.log
   ```
4. For incidents without a request context (health, boot), grep by logger
   (`Flyway`, `prodSecretsGuard`, `HikariPool`).

`X-Response-Time-Ms` is best-effort (set after the chain; empty once the
response is committed) — measure latency client-side or from Micrometer
percentiles, not this header.

---

## Metrics and scrape security

- Exposed on the **app port**: `/actuator/health` and `/actuator/prometheus`
  only (`management.endpoints.web.exposure.include: health,prometheus` in both
  default and `prod`). Everything else (`/actuator/env`, `/actuator/beans`,
  …) returns 401/404 — verified live and by `PrometheusMetricsTest`.
- The scrape endpoint has **no authentication on the app port** by owner
  decision 2026-10-23 — it is therefore **not routed publicly**. The jar
  binds to loopback and nginx only allowlists the metrics path for the
  metrics network:

```nginx
# app port stays loopback-only; /api and health are proxied (DEPLOYMENT §5)
location = /actuator/prometheus {
    allow 10.0.0.0/8;      # metrics/Prometheus network only
    deny  all;
    proxy_pass http://127.0.0.1:8080;
}
```

- Sanity check after deploy: `curl localhost:8080/actuator/prometheus`
  returns 200 with `application="mtaafix"` on every series;
  `curl -I localhost:8080/actuator/env` must NOT be 200.

---

## Slow requests

Two different numbers, two different purposes:

| | Budget | Where |
|---|---|---|
| NFR-7 gate (list queries, p95) | **< 500 ms** | `measure-perf.ps1` → `scripts/perf-results.json`; current: 11/11 pass, worst p95 152.2 ms (PERFORMANCE_REVIEW §8) |
| Alert `MtaaFixSlowRequests` (p95, any route) | **> 1 s for 10 m** | `alerting-rules.yml` — 2× the gate so transient spikes don't page |
| Single request WARN | **> 500 ms** | `RequestTimingFilter` log line with `requestId` |

Triage order: reproduce with `measure-perf.ps1` (deterministic, seeded
5,000-report dataset via `seed-perf.sql`) → if only one route regressed,
compare with the p95 table in PERFORMANCE_REVIEW §8 → if everything regressed,
check pool/CPU/heap alerts first.

---

## Database: backups, restore, PITR

All scripts live in `backend/scripts/`. `PGPASSWORD` must be supplied via the
environment for the shell session — never stored in a script or the repo.

### Backup (B-16.3)

```powershell
$env:PGPASSWORD = '<password>'
powershell -ExecutionPolicy Bypass -File backend\scripts\backup.ps1
powershell -ExecutionPolicy Bypass -File backend\scripts\backup.ps1 -RetentionDays 30
```

`pg_dump` custom format + `uploads/` copy → `backend/scripts/backups/` with
SHA-256 hashes and age-based retention. Failure = disk full (see
`MtaaFixDiskAlmostFull`) or `PGPASSWORD` unset (script throws instead of
hanging on a prompt).

### Restore

```powershell
$env:PGPASSWORD = '<password>'
powershell -ExecutionPolicy Bypass -File backend\scripts\restore-database.ps1 `
  -DumpFile backend\scripts\backups\mtaafix_<stamp>.dump
# restores into mtaafix_restore (scratch) by default
# -TargetDb mtaafix -Force   # overwrites the LIVE database (destructive)
```

The script refuses to touch the live database unless you name it **and** pass
`-Force`. Verify after restore: row counts (`reports`, `categories`) and
PostGIS (`SELECT postgis_version();`) — the Phase 19 restore drill landed
5,038 reports + PostGIS into `mtaafix_restore`.

### PITR drill (proof of point-in-time recovery)

```powershell
powershell -ExecutionPolicy Bypass -File backend\scripts\pitr-drill.ps1
# -KeepWorkDir   # keep pgdata/archive/basebackup for inspection
```

Runs on a **scratch cluster** (ports 55432/55433, `%TEMP%\mtaafix_pitr_drill`)
— never the service cluster on 5432. ~90 s. It proves, end to end: WAL
archiving on → `pg_basebackup -X stream` → markers A (before), B (after
backup), C (after target time) → recovery with `restore_command` +
`recovery_target_time` (between B and C) + `recovery_target_action=promote` →
**A and B recovered, C absent**.

Artifacts:

- `backend/scripts/pitr-drill-evidence.txt` — full evidence log incl.
  `SHOW archive_command` round-trip, per-segment `archive-command.log` and
  `restore-command.log` (each archived/replayed WAL segment with timestamp
  and post-copy size).
- `%TEMP%\mtaafix_pitr_drill_steps.log` — timestamped step trace (survives a
  lost console).

If the drill fails, read the evidence file first — `Wait-Archive` timeouts
dump the archive dir listing, PG's `archive_status`, the wrapper log and the
server log tail. Known gotchas baked into the script: hand-written
`postgresql.conf` values treat backslash as a GUC escape (hence forward-slash
paths), and hot standby accepts connections *before* replay finishes (hence
promotion is polled via `pg_is_in_recovery()` before verification).

---

## Quick reference

```bash
# health + scrape
curl -s localhost:8080/actuator/health
curl -s localhost:8080/actuator/prometheus | grep -E 'application=|http_server_requests_seconds_count|hikaricp'

# recent errors with correlation ids
grep -E 'ERROR|Slow request' service.log | tail -50

# DB activity
psql -h localhost -U postgres -d mtaafix -c "SELECT pid, state, now()-query_start AS dur, left(query,80) FROM pg_stat_activity WHERE datname='mtaafix' ORDER BY dur DESC;"

# NFR-7 re-measure (server on :8085, seeded dataset)
powershell -ExecutionPolicy Bypass -File backend\scripts\measure-perf.ps1
```
