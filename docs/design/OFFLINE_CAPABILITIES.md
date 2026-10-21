# MtaaFix — Offline Capabilities Specification

> **Status:** v1.0 — APPROVED (owner, 2026-10-21) · Phase 14 · 2026-10-21
> Scope as approved by the owner for Phase 14: **Option B** backend
> idempotency, local report-form draft storage, a pending-submission queue with
> foreground retry, categories cache for offline form loads, and local-state
> hygiene on sign-out. Offline browsing of the report list and offline map
> tiles are **deferred**. Web is unchanged beyond its pre-existing
> `mtaafix.reportDraft.v1` local draft key.

## 1. Honesty invariant (exit criterion)

> **Nothing is ever presented as successfully submitted until the server has
> accepted it.**

Concretely:

* A form saved while offline lands in the **pending queue** and is labelled
  *"Not yet submitted · Waiting"* — never in "My Reports" and never with an
  MTF code.
* A queued report that reaches the server but fails photo attachment is
  reported honestly: *"Report MTF-… was filed, but N photo(s) could not be
  attached."* The photos are not silently dropped.
* Offline submission feedback says *"saved to your pending queue"*, not
  *"submitted"*.
* The offline shell strip (*"You are offline"*) disables retry while there is
  no connection.

## 2. Requirements traceability (master spec Phase 14, lines 2047–2066)

| Master task | Delivered by |
| ----------- | ------------ |
| Assess offline requirements | Owner decision on Option B + scope list (this doc, §1, §11) |
| Local draft storage if approved | §7 draft store (mobile `DraftStore`, debounced autosave, restore offer) |
| Implement retry behavior | §5 queue flush triggers (connectivity, mount, resume, manual retry) |
| Implement synchronization | §5 outbox drain: create → photo attach stepwise |
| Define conflict handling | §6 idempotency semantics (replay / 409 conflict / 409 in-progress / 400) |
| Distinguish drafts from submitted reports | §1 honesty invariant; drafts excluded from My Reports, queue never carries MTF codes |
| Test connectivity transitions | §10 evidence (offline→online auto-flush, offline banner/hint tests) |
| Exit: no misrepresentation | §1; enforced in `PendingSubmission` states and UI copy |

## 3. Architecture

```
┌────────────────────────────  mobile (Flutter)  ────────────────────────────┐
│                                                                            │
│  ReportFormScreen ──enqueue──► OfflineController ──► OutboxStore           │
│        │  ▲                        │   │         └──► DraftStore           │
│        │  │ restore / edit         │   └──► CategoryCache (categories tab) │
│  debounced autosave (400 ms)       │                                       │
│                                    │  flush() (single-flight)              │
│  OfflineScope (InheritedNotifier)  │        │                              │
│        ▲                           ▼        ▼                              │
│  main_shell strip + banners   ReportsApi.submit / uploadPhoto              │
│  PendingSubmissionsScreen     ApiClient(+Idempotency-Key) ──── HTTP ───►   │
└────────────────────────────────────────────────────────────────────────────┘
┌────────────────────────────  backend (Spring)  ────────────────────────────┐
│  ReportController ─► IdempotencyService (REQUIRES_NEW tx) ─► report table  │
│        │                    │ report_idem_keys (24 h TTL, hourly purge)    │
│        └── Idempotency-Key header validated: [A-Za-z0-9_-]{16,64}          │
└────────────────────────────────────────────────────────────────────────────┘
```

Mobile pieces (all under `mobile/lib/features/offline/`):

| Piece | Role |
| ----- | ---- |
| `OfflineController` (`ChangeNotifier`) | Queue state, single-flight `flush()`, draft/category passthroughs, `consumeJustSubmitted()` |
| `OfflineStorage` | Atomic JSON files (tmp + rename), `schema: 1` on every file, all public operations wrapped in `Zone.root.run` so real I/O completes independently of the UI zone; graceful degradation (missing path_provider → system temp, read errors → null, write errors → false) |
| `DraftStore` / `OutboxStore` / `CategoryCache` | One draft, N queued submissions, cached category list |
| `ConnectivityService` | Wraps `connectivity_plus`; notifies on online transitions |
| `OfflineScope` | InheritedNotifier exposing the controller; `maybeOf` for optional UI use, `read` for imperative calls |
| `PendingSubmissionsScreen` | Queue UI: status chips, per-item Edit / Retry / Discard (with confirmation), offline hint, "Nothing pending" empty state |

## 4. Data model (mobile, persisted under the app-support directory)

**Draft** (`drafts/report_draft.json`, `schema: 1`):

```json
{ "schema": 1, "categoryId": "", "categoryName": null,
  "title": "…", "description": null,
  "latitude": 0.0, "longitude": 0.0,      // 0/0 = "no location yet"
  "photoPaths": ["…"], "updatedAt": "ISO-8601" }
```

An **empty `categoryId` is valid** (autosave can fire before a category is
picked); `tryFromJson` only rejects wrong types/missing fields — losing typed
text would be worse than a category-less draft.

**Pending submission** (`outbox/pending.json`, `schema: 1`):

| Field | Meaning |
| ----- | ------- |
| `localId` | Client UUID v4; queue identity |
| `idempotencyKey` | One per **form session** (see §6) |
| `categoryId`, `categoryName`, `title`, `description`, `latitude`, `longitude` | Create payload snapshot |
| `photoPaths` | Local files still to upload (shrinks as attachments succeed) |
| `uploadedPhotoCount` | Attachments already on the server (for `attachingPhotos` resumes) |
| `state` | `pending` \| `attachingPhotos` \| `failed` |
| `lastError` | Honest failure text shown in the UI |
| `serverId`, `serverCode` | Set once the server accepted the create |
| `createdAt`, `updatedAt` | Local timestamps |

Photo files picked on the form are **copied into the store's `photos/` group**
at pick time so they survive external cache cleanup.

## 5. Queue lifecycle

```
                 (offline submit / stranded photos)
 form ────────────────────────────► pending
   │                                   │  flush() — connectivity online,
   │ online submit accepted            │  controller mount, app resume,
   ▼                                   ▼  manual Retry, FAB retry
attachingPhotos ◄── create ok, photos remain ──► create(PENDING)
   │  every photo attached                 │ network error → stays pending
   ▼                                       │ 4xx (permanent) → failed
 removed from queue                        ▼
 (server accepted, local photo        Edit → prefill form → re-queue
  files released)                  Discard → confirm → delete entry + files
```

Flush rules:

* **Single-flight**: concurrent `flush()` calls no-op while one is running.
* **Foreground only** — no background isolates (approved scope); flush runs on
  connectivity-online events, on shell mount (post-frame), on app `resumed`,
  and from Retry actions.
* Items in `failed` state are skipped by automatic flush (user must Edit or
  Discard).
* Steps are durable: the create is replayed with the **same idempotency key**
  until it succeeds, then each photo attaches and shrinks `photoPaths`; a
  photo upload failure does not repeat the create.
* Photo files are deleted only when they no longer belong to any queue entry
  (`discard(keepPaths:)`, `releasePhotos`).
* `flush()` never notifies after `dispose()` (`_disposed` guard).

## 6. Conflict handling — backend idempotency (Option B)

Header `Idempotency-Key` on `POST /api/v1/reports`:

| Situation | Result |
| --------- | ------ |
| Key absent | Normal create (legacy clients unaffected) |
| Key invalid (`!^[A-Za-z0-9_-]{16,64}$`) | `400 BAD_REQUEST` with explicit message |
| First use | `201`, report created, key row stored (`report_id`, `payload_hash`, TTL 24 h) |
| Same key, byte-identical payload | `201`, **stored response replayed** — same report id, no duplicate row |
| Same key, different payload | `409 IDEMPOTENCY_CONFLICT` |
| First use in flight (stale intent) | `409 IDEMPOTENCY_IN_PROGRESS`; retry after `STALE_INTENT_MINUTES=2` |
| Purge | Hourly delete of rows older than 24 h |

Mechanics: `IdempotencyService` runs inserts/removes in a **REQUIRES_NEW**
transaction; `hashOf` (SHA-256 of the canonical payload) is computed by a
private `ObjectMapper` with `JavaTimeModule` (no Spring bean dependency).
Repos: `report_idem_keys` (V6 migration).

Mobile side: **one key per form session** (`_idempotencyKey ??= generateUuidV4()`),
generated at first submit and reused for every queued retry — if a timed-out
create actually reached the server, the replay returns the same report instead
of duplicating it. The key travels as `Idempotency-Key` via
`ApiClient.post(path, body, idempotencyKey)`; the 401→refresh retry path
carries it too.

## 7. Draft behavior (form)

* Debounced autosave (400 ms) of title/description/photos/category/location
  while editing a **new** report (never for edit-mode or pending-prefill).
* On open (new report only): if a draft with content exists, offer
  *Restore / Discard* — it never silently overwrites what the user is typing.
* Guards: an offline submit or successful online submit only clears the draft
  if it belongs to *this* form (prefill submissions never wipe an unrelated
  unfinished draft).
* Sign-out (`ProfileScreen._signOut`) calls `clearAll()` — queue, draft, and
  their photo files are removed (shared-device hygiene). Wired to the sign-out
  action itself, not an auth listener, so an offline session restore does not
  wipe drafts.

## 8. Categories cache

* On every successful load the form refreshes `CategoryCache`.
* On failure/offline the form falls back to the cache and shows
  *"Offline — showing saved categories, which may be outdated."*
* Offline browsing of the report list/map is deferred (approved scope).

## 9. Testing evidence

| Layer | Evidence |
| ----- | -------- |
| Backend unit (H2) | `mvn test` → **133/133** incl. `IdempotencyTest` (8 tests: format, first-use, replay, conflict, in-progress, purge, …) |
| Backend PG integration | `PostgresIntegrationTest` → **8/8** against local PostgreSQL |
| Backend jar | `mvn -DskipTests package` green; `docs/api/openapi.json` regenerated |
| Live battery (localhost:8085, PG) | POST new key → `201` MTF-2026-000035 · same key + same body → `201` **same id** (no duplicate) · same key + different body → `409 IDEMPOTENCY_CONFLICT` · key `short` → `400 BAD_REQUEST` |
| Mobile unit | `offline_controller_test` (12): persistence, offline→online auto-flush, network-fail keeps pending, 400→failed, 409 in-progress/conflict, photo attach/resume/honest failure, offline no-op flush, clearAll, discard keepPaths; `offline_storage_test` (6): roundtrips, corrupt degrade, copyInto, atomic writes |
| Mobile widget | pending screen (5), form offline (7: restore/discard/autosave/cache-fallback/offline-queue/online-prefill/unrelated-draft), offline banner (3) |
| Mobile total | **167/167 green**, `flutter analyze` → 0 issues |

Connectivity-transition coverage: offline→online auto-flush (unit), offline
hint disables retry (widget), banner appears/disappears with `isOnline`
(widget), flush no-op while offline (unit).

## 10. Known gaps / deferred

* Offline browsing of existing reports and offline map tiles (out of approved
  scope).
* Background sync (foreground-only retry was the approved behavior; OS-level
  background execution would need isolates + platform review).
* iOS untestable in this environment (B-X.4).
* Carried gaps unrelated to Phase 14: JWTs not revoked after password reset,
  public media endpoint absent, push provider absent, FR-2.4 date filter
  unsupported (see BACKLOG).
