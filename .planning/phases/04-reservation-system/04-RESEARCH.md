# Phase 04: Reservation System - Research

**Researched:** 2026-04-11
**Domain:** Firebase Cloud Tasks (v2), Firestore transactions, Android DataStore, Jetpack Compose countdown UI
**Confidence:** HIGH (core patterns verified against official docs and codebase; Cloud Tasks emulator caveats MEDIUM)

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

- **D-01:** `createReservation` callable Cloud Function (firebase-functions v2, `europe-west3`, Node 22) using Firestore `runTransaction` — aborts with `HttpsError("failed-precondition", "ITEM_UNAVAILABLE")` if status != `available`
- **D-02:** Within the same transaction: update item status to `reserved` + set `reservedBy`/`reservedAt`/`expiresAt` AND create `reservations/{reservationId}` doc
- **D-03:** Clients never write reservations directly — `firestore.rules` hard-deny on reservations collection preserved
- **D-04:** Reservation doc schema: `{ itemId, registryId, giverId, giverName, giverEmail, status, createdAt, expiresAt, cloudTaskName, affiliateUrl }`
- **D-05:** `createReservation` enqueues a Cloud Task via `@google-cloud/tasks` targeting `releaseReservation` at `now + 30 minutes`
- **D-06:** Store returned `cloudTaskName` on reservation document for later cancellation
- **D-07:** `releaseReservation` re-runs transaction reverting item to `available` only if `status == active && now > expiresAt`
- **D-08:** No client-side timer authority — Android countdown is display-only
- **D-09:** Use `firebase-functions` v2 `onTaskDispatched` for the queue handler
- **D-10:** Stub expiration email (console.log) — full SendGrid wiring in Phase 6
- **D-11:** Re-reserve calls same `createReservation` callable — no shortcut authorization
- **D-12:** Email deep link format: `https://giftregistry.app/reservation/{reservationId}/re-reserve`
- **D-13:** `ReservationRepository` + `ReserveItemUseCase` in domain/data layers following `ItemRepository`/`RegistryRepository` pattern
- **D-14:** `RegistryDetailScreen` hosts "Reserve" button — visible only when `status == AVAILABLE`
- **D-15:** Launch Intent to `item.affiliateUrl` AFTER callable returns success — never before
- **D-16:** Guest giver identity persisted via new `GuestPreferencesDataStore` mirroring `LanguagePreferencesDataStore`
- **D-17:** Guest identity sent in callable request payload and stored on reservation doc
- **D-18:** Reserved item card displays countdown from `expiresAt` field (display-only, re-computed on recomposition)
- **D-19:** Add tests to `tests/rules/firestore.rules.test.ts` for: client cannot write reservations directly, items `status` readable by anyone, client cannot directly write `status` field

### Claude's Discretion

- Exact Compose layout for reservation countdown + button states
- Error UX (snackbar vs dialog) for conflict/network errors
- Cloud Task queue configuration (min/max instances, dispatch deadline)
- Whether to expose a "my active reservations" screen in Phase 4 or defer

### Deferred Ideas (OUT OF SCOPE)

- Full SendGrid email delivery — Phase 6
- "My active reservations" dashboard for givers — future polish
- Owner notification on reservation — Phase 6
- Web fallback reservation flow — Phase 5

</user_constraints>

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| RES-01 | Gift giver can reserve an available item | D-01/D-02: `createReservation` callable + Firestore transaction pattern; verified against Admin SDK docs |
| RES-02 | Reserved items show as unavailable to other givers in real time | Existing `FirestoreDataSource.observeItems` callbackFlow propagates status changes; no new listener needed |
| RES-03 | 30-minute reservation timer starts on reserve action | D-05: Cloud Task enqueued at `createdAt + 30m`; `expiresAt` field drives Android countdown display |
| RES-04 | Giver redirected to retailer immediately upon reservation | D-15: Android `Intent(ACTION_VIEW, affiliateUrl.toUri())` after callable success |
| RES-05 | Reservation auto-releases after 30 minutes if not confirmed | D-07: `releaseReservation` task handler with guard transaction |
| RES-06 | Auto-released items return to available status in real time | Same `observeItems` listener propagates; Firestore real-time covers this automatically |
| RES-07 | Giver receives expiration email when reservation lapses | D-10: Stubbed as `console.log` in Phase 4 — full email in Phase 6 |
| RES-08 | Expiration email includes option to re-reserve immediately | D-12: Deep link URL written to stub log; actual email deferred to Phase 6 |
| RES-09 | Two givers cannot simultaneously reserve the same item | D-01: `runTransaction` with conditional abort — serializable isolation guarantee |

</phase_requirements>

---

## Summary

Phase 4 implements the reservation-to-purchase flow: the product's core trust feature. The domain involves three interlocking subsystems: (1) a server-authoritative Firestore transaction in a Cloud Function, (2) a Cloud Tasks expiry timer per reservation, and (3) an Android UI that drives the giver through guest identity collection, reservation, and retailer redirect. All three must be correct together — the Firestore rules, transaction logic, Cloud Tasks enqueueing, and Android callable client are a single integrated feature, not a sequence of independent tasks.

The most important research finding is that Firebase Admin SDK's `taskQueue.enqueue()` does NOT return the task name (GitHub issue #1753, confirmed unresolved). The `@google-cloud/tasks` `CloudTasksClient.createTask()` must be used directly to get back the task resource name (`projects/{P}/locations/{L}/queues/{Q}/tasks/{T}`) needed for D-06 (`cloudTaskName` field). This means `@google-cloud/tasks` is a required new dependency in `functions/package.json`. The task handler (`releaseReservation`) uses `onTaskDispatched` from `firebase-functions/v2/tasks`, which is a separate import path from the `onCall` functions.

The second critical finding is that the Cloud Tasks emulator ignores `scheduleTime` / `scheduleDelaySeconds` — tasks fire immediately (issue #8254, open). This is a feature, not a bug, for testing: test the expiry path by directly POST-ing to the emulated function endpoint rather than waiting 30 minutes. The emulator exposes `onTaskDispatched` handlers as plain HTTPS endpoints at `http://localhost:5001/{projectId}/{region}/{functionName}`.

**Primary recommendation:** Use `@google-cloud/tasks` `CloudTasksClient` directly inside `createReservation` to enqueue the task and capture `response[0].name` as `cloudTaskName`. Use `onTaskDispatched` from `firebase-functions/v2/tasks` for `releaseReservation`. All other patterns are already established in the codebase and should be followed exactly.

---

## Standard Stack

### Core (Functions — new additions to functions/package.json)

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `@google-cloud/tasks` | 6.2.1 | Enqueue Cloud Tasks and get task name for cancellation | Firebase Admin `taskQueue.enqueue()` does not return task name — direct SDK is required for D-06 |
| `firebase-functions/v2/tasks` | bundled in `firebase-functions` 7.2.3 | `onTaskDispatched` handler | v2 task queue handler; separate import from `firebase-functions/v2/https` |

### Core (Android — no new libraries needed)

All Android libraries required for this phase are already in `gradle/libs.versions.toml`. No new Android dependencies required.

| Library | Version | Purpose | Already Present |
|---------|---------|---------|-----------------|
| `firebase-functions` (Android) | via BoM 34.11.0 | `FirebaseFunctions.getHttpsCallable().call()` for `createReservation` | Yes — `firebase-functions` in `libs.versions.toml` |
| `datastore-preferences` | 1.2.1 | `GuestPreferencesDataStore` for guest name/email persistence | Yes — `datastore-preferences` in `libs.versions.toml` |
| `kotlinx-coroutines` | via Kotlin | `LaunchedEffect` + countdown loop | Yes — bundled |

### Functions Installation

```bash
cd functions
npm install @google-cloud/tasks
```

**Version verification:**
- `@google-cloud/tasks` 6.2.1 — verified against npm registry 2026-04-11
- `firebase-functions` 7.2.3 — confirmed in `functions/package.json`
- `firebase-admin` 13.7.0 — confirmed in `functions/package.json`

---

## Architecture Patterns

### Recommended Project Structure (new files only)

```
functions/src/
├── reservation/
│   ├── createReservation.ts     # onCall: transaction + Cloud Tasks enqueue
│   └── releaseReservation.ts    # onTaskDispatched: expiry transaction + stub email

app/src/main/java/com/giftregistry/
├── domain/
│   ├── model/
│   │   └── Reservation.kt       # Domain model: id, itemId, registryId, expiresAt, status
│   ├── reservation/
│   │   └── ReservationRepository.kt  # Interface: reserve(), observeActiveReservation()
│   └── usecase/
│       └── ReserveItemUseCase.kt
├── data/
│   ├── model/
│   │   └── ReservationDto.kt    # Firestore DTO (unused directly — Functions write only)
│   ├── preferences/
│   │   └── GuestPreferencesDataStore.kt  # Mirrors LanguagePreferencesDataStore
│   └── reservation/
│       └── ReservationRepositoryImpl.kt  # Functions callable invocation
├── di/
│   └── DataModule.kt            # Add ReservationRepository + GuestPreferencesRepository bindings
└── ui/
    └── registry/
        └── detail/
            ├── RegistryDetailScreen.kt   # Add Reserve button + countdown per item card
            └── RegistryDetailViewModel.kt  # Add reservation UI state
```

### Pattern 1: Cloud Tasks Enqueue via CloudTasksClient (CRITICAL — not Admin SDK taskQueue)

**What:** Use `@google-cloud/tasks` `CloudTasksClient` directly inside `createReservation` to get the task name back for storage in Firestore.

**Why:** `getFunctions().taskQueue().enqueue()` does not return the task name. The `cloudTaskName` is needed for D-06 (cancellation when owner marks purchased in later phases).

**Example:**
```typescript
// Source: @google-cloud/tasks createTask API; cloud.google.com/tasks/docs/reference/rest/v2
import { CloudTasksClient } from "@google-cloud/tasks";

const tasksClient = new CloudTasksClient();

const projectId = process.env.GCLOUD_PROJECT!;
const location = "europe-west3";
const queueName = "release-reservation";
const queuePath = tasksClient.queuePath(projectId, location, queueName);

const expiresAtSeconds = Math.floor((Date.now() + 30 * 60 * 1000) / 1000);

const [taskResponse] = await tasksClient.createTask({
  parent: queuePath,
  task: {
    httpRequest: {
      httpMethod: "POST" as const,
      url: `https://${location}-${projectId}.cloudfunctions.net/releaseReservation`,
      body: Buffer.from(JSON.stringify({ reservationId })).toString("base64"),
      headers: { "Content-Type": "application/json" },
    },
    scheduleTime: { seconds: expiresAtSeconds },
  },
});

const cloudTaskName = taskResponse.name!; // "projects/.../locations/.../queues/.../tasks/..."
// Store cloudTaskName on the reservation document for later cancellation (D-06)
```

**Cancellation (for later phases):**
```typescript
// Source: CloudTasksClient deleteTask
await tasksClient.deleteTask({ name: cloudTaskName });
```

### Pattern 2: Firestore runTransaction for Reservation Race Prevention (RES-09)

**What:** All reservation writes go through a single `runTransaction` that reads item status and atomically writes both the item update and reservation document. Transaction aborts if item is not available.

**When to use:** Both `createReservation` and `releaseReservation` must use transactions.

**Example (createReservation transaction body):**
```typescript
// Source: firebase.google.com/docs/firestore/manage-data/transactions
const db = admin.firestore();

await db.runTransaction(async (tx) => {
  const itemRef = db
    .collection("registries").doc(registryId)
    .collection("items").doc(itemId);

  const itemSnap = await tx.get(itemRef);
  if (!itemSnap.exists) {
    throw new HttpsError("not-found", "ITEM_NOT_FOUND");
  }
  if (itemSnap.data()!.status !== "available") {
    throw new HttpsError("failed-precondition", "ITEM_UNAVAILABLE");
  }

  const reservationRef = db.collection("reservations").doc();
  const now = admin.firestore.FieldValue.serverTimestamp();
  const expiresAt = admin.firestore.Timestamp.fromMillis(Date.now() + 30 * 60 * 1000);

  tx.update(itemRef, {
    status: "reserved",
    reservedBy: giverId,
    reservedAt: now,
    expiresAt,
  });

  tx.set(reservationRef, {
    itemId,
    registryId,
    giverId,       // null for guests
    giverName,
    giverEmail,
    affiliateUrl,
    status: "active",
    createdAt: now,
    expiresAt,
    cloudTaskName: "",  // filled after task enqueue below
  });

  return reservationRef.id;
});
// Note: transaction returns reservationId; enqueue Cloud Task after transaction
// Then update reservation with cloudTaskName via admin.firestore().doc(...).update(...)
```

**releaseReservation guard transaction:**
```typescript
// Source: PITFALLS.md Pitfall 2; ARCHITECTURE.md lines 186-196
await db.runTransaction(async (tx) => {
  const reservationSnap = await tx.get(reservationRef);
  if (!reservationSnap.exists) return;

  const data = reservationSnap.data()!;
  if (data.status !== "active") return; // Already expired or cancelled
  if (admin.firestore.Timestamp.now().seconds < data.expiresAt.seconds) return; // Not yet expired

  const itemRef = db
    .collection("registries").doc(data.registryId)
    .collection("items").doc(data.itemId);

  tx.update(itemRef, {
    status: "available",
    reservedBy: admin.firestore.FieldValue.delete(),
    reservedAt: admin.firestore.FieldValue.delete(),
    expiresAt: admin.firestore.FieldValue.delete(),
  });
  tx.update(reservationRef, { status: "expired" });
});

// STUB: console.log("[STUB] Email would be sent to {giverEmail} for reservation {reservationId}");
```

### Pattern 3: onTaskDispatched Handler (v2)

**What:** `releaseReservation` uses `onTaskDispatched` from `firebase-functions/v2/tasks`.

**Import path (CONFIRMED):** `firebase-functions/v2/tasks` — used for v2 task queue functions.

**Example:**
```typescript
// Source: firebase.google.com/docs/functions/task-functions
import { onTaskDispatched } from "firebase-functions/v2/tasks";
import * as admin from "firebase-admin";

interface ReleasePayload { reservationId: string; }

export const releaseReservation = onTaskDispatched<ReleasePayload>(
  {
    region: "europe-west3",
    retryConfig: {
      maxAttempts: 3,
      minBackoffSeconds: 10,
    },
    rateLimits: {
      maxConcurrentDispatches: 20,
    },
    minInstances: 0,           // Claude's discretion: 0 = cold start acceptable for expiry
    timeoutSeconds: 60,
  },
  async (req) => {
    const { reservationId } = req.data;
    // ... guard transaction + stub email
  }
);
```

**Queue name:** The `onTaskDispatched` function name becomes the Cloud Tasks queue name. Name the function `releaseReservation` — the queue is auto-created on first deploy as `release-reservation` (camelCase to kebab-case).

**CRITICAL NOTE on minInstances for createReservation:** Per PITFALLS.md Performance Traps — "Cold starts are unacceptable for a 30-second time-sensitive flow." Set `minInstances: 1` on `createReservation` (the onCall function). `releaseReservation` can be `minInstances: 0` — cold start is acceptable for a background expiry task.

### Pattern 4: GuestPreferencesDataStore (mirrors LanguagePreferencesDataStore exactly)

**What:** Store guest first name, last name, and email in a separate DataStore file.

**Key rule:** Each `preferencesDataStore(name = "...")` must use a unique name and be defined at the top level of exactly one Kotlin file.

**Example:**
```kotlin
// Source: existing LanguagePreferencesDataStore.kt — mirror this pattern exactly
// app/src/main/java/com/giftregistry/data/preferences/GuestPreferencesDataStore.kt

private val Context.guestDataStore: DataStore<Preferences> by preferencesDataStore(name = "guest_prefs")

@Singleton
class GuestPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) : GuestPreferencesRepository {

    private val firstNameKey = stringPreferencesKey("guest_first_name")
    private val lastNameKey  = stringPreferencesKey("guest_last_name")
    private val emailKey     = stringPreferencesKey("guest_email")

    override suspend fun saveGuestIdentity(firstName: String, lastName: String, email: String) {
        context.guestDataStore.edit { prefs ->
            prefs[firstNameKey] = firstName
            prefs[lastNameKey]  = lastName
            prefs[emailKey]     = email
        }
    }

    override suspend fun getGuestIdentity(): GuestUser? {
        val prefs = context.guestDataStore.data.first()
        val email = prefs[emailKey] ?: return null
        return GuestUser(
            firstName = prefs[firstNameKey] ?: "",
            lastName  = prefs[lastNameKey]  ?: "",
            email     = email
        )
    }

    override suspend fun clearGuestIdentity() {
        context.guestDataStore.edit { it.clear() }
    }
}
```

**DataStore name** must be `"guest_prefs"` — different from the existing `"user_prefs"` used by `LanguagePreferencesDataStore`. Using the same name would share state between them.

### Pattern 5: Android callable invocation (follows RegistryRepositoryImpl)

```kotlin
// Source: existing RegistryRepositoryImpl.kt — inviteUser() pattern
override suspend fun reserve(
    registryId: String,
    itemId: String,
    giver: GuestUser
): Result<String> = runCatching {
    val result = functions.getHttpsCallable("createReservation")
        .call(mapOf(
            "registryId" to registryId,
            "itemId"     to itemId,
            "giverName"  to "${giver.firstName} ${giver.lastName}",
            "giverEmail" to giver.email,
            "giverId"    to null,  // null for guests; FirebaseAuth UID for registered users
        ))
        .await()
    // Return affiliateUrl from response to drive Android Intent (RES-04)
    @Suppress("UNCHECKED_CAST")
    (result.data as Map<String, Any?>)["affiliateUrl"] as String
}
```

### Pattern 6: Compose countdown timer (display-only, D-18)

**What:** Re-compute remaining minutes from `expiresAt` timestamp on each tick. State does not write back to Firestore.

```kotlin
// Source: Android side-effects docs; timer-with-launchedeffect pattern
@Composable
fun ReservationCountdown(expiresAtMs: Long) {
    var remainingMs by remember { mutableLongStateOf(expiresAtMs - System.currentTimeMillis()) }

    LaunchedEffect(expiresAtMs) {
        while (remainingMs > 0) {
            delay(1_000L)
            remainingMs = (expiresAtMs - System.currentTimeMillis()).coerceAtLeast(0L)
        }
    }

    val minutes = (remainingMs / 60_000).toInt()
    val seconds = ((remainingMs % 60_000) / 1_000).toInt()
    Text(stringResource(R.string.reservation_countdown, minutes, seconds))
}
```

**D-08 enforcement:** This composable only reads `expiresAtMs`, never writes to Firestore.

### Pattern 7: Firestore Security Rule update for items status field

Current `firestore.rules` allows any `create, update, delete` on items to the registry owner. Phase 4 requires that the `status` field on items is readable by anyone (already handled by the existing read rules), and that **client-side writes to `status` are blocked even for the owner** (Cloud Function Admin SDK bypasses rules).

**Updated items rule:**
```javascript
match /items/{itemId} {
  allow read: if canReadRegistry(
    get(/databases/$(database)/documents/registries/$(registryId)).data
  );
  // Owner can create/update/delete items BUT cannot write status field directly
  allow create: if isOwner(
    get(/databases/$(database)/documents/registries/$(registryId)).data
  ) && !("status" in request.resource.data);
  allow update: if isOwner(
    get(/databases/$(database)/documents/registries/$(registryId)).data
  ) && !("status" in request.resource.data.diff(resource.data).affectedKeys());
  allow delete: if isOwner(
    get(/databases/$(database)/documents/registries/$(registryId)).data
  );
}
```

**IMPORTANT CAVEAT:** Blocking `status` field writes for the owner may break existing Phase 3 `addItem` flows if they send a `status` field. The current `ItemRepositoryImpl.addItem()` call should be audited — if it sends `status: "available"` on create, the create rule must permit this. Simplest safe rule: allow owner to write any field (existing behavior), but the D-19 test simply verifies that a *non-owner* non-admin client cannot write `status`. The D-19 test requirement is about client-side direct writes to reservations (which is fully denied already) and that items `status` is readable — it does not require blocking owner writes to status. The planner should audit `ItemRepositoryImpl.addItem` before tightening this rule.

### Anti-Patterns to Avoid

- **Using `getFunctions().taskQueue().enqueue()` expecting a task name return:** It returns `void`. Use `CloudTasksClient.createTask()` for D-06.
- **Cloud Task target URL hardcoded:** Use `process.env.GCLOUD_PROJECT` and the function region — hardcoded project IDs break emulator testing.
- **Writing reservation status from Android:** Android never writes to Firestore items `status` or `reservations/*`. All writes go through Cloud Functions only.
- **Single transaction for task enqueue:** Cloud Tasks enqueue happens **outside** the Firestore transaction (after it succeeds). Enqueuing inside a transaction cannot be rolled back if the task is already dispatched — always enqueue after transaction commit, then update the reservation doc with the `cloudTaskName`.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Atomic reservation write | Sequential get+set Firestore calls | `admin.firestore().runTransaction()` | Check-then-act is a race condition; Firestore serializable isolation is the only correct solution |
| Task name for cancellation | Synthetic ID from reservation doc | `CloudTasksClient.createTask()` response `.name` | Firebase Admin `taskQueue.enqueue()` returns void — no task name without direct SDK |
| Task cancellation | Mark a flag in Firestore and poll | `CloudTasksClient.deleteTask({ name })` | Cloud Tasks has a native delete API; polling adds complexity and latency |
| 30-minute expiry enforcement | Android `ScheduledExecutorService` / `AlarmManager` | Cloud Tasks `scheduleTime` | Client timers are unreliable across process death, backgrounding, reboot |
| Real-time item status | Polling loop in Android ViewModel | Existing `FirestoreDataSource.observeItems` callbackFlow | Already implemented in Phase 3; no new listener infrastructure needed |
| Guest identity persistence | In-memory ViewModel state only | `GuestPreferencesDataStore` (DataStore Preferences) | Process death orphans reservations (PITFALLS.md Pitfall 7) |
| Countdown display | Server round-trip per tick | `LaunchedEffect` + `coerceAtLeast(0L)` local computation from `expiresAt` | Display-only countdown; `expiresAt` timestamp is already in item document |

**Key insight:** The reservation system's correctness guarantee comes entirely from Firestore's serializable transaction isolation and Cloud Tasks's reliable delivery. Neither can be replicated cheaply on the client side.

---

## Common Pitfalls

### Pitfall 1: Admin SDK taskQueue.enqueue() returns void — cloudTaskName is null
**What goes wrong:** Developer calls `getFunctions().taskQueue("releaseReservation").enqueue(payload)` and tries to use the return value as `cloudTaskName`. The method returns a `Promise<void>`. The `cloudTaskName` field on the reservation document stays null/empty, and future cancellation (Phase 5 purchase confirmation) fails silently.
**Why it happens:** Firebase documentation shows `enqueue()` for triggering tasks but does not expose the task resource name (GitHub issue #1753 closed without fix).
**How to avoid:** Use `@google-cloud/tasks` `CloudTasksClient` directly. `createTask()` returns `[Task, Request, Response]` — the `Task.name` field contains the full resource path. Install `@google-cloud/tasks` in `functions/package.json`.
**Warning signs:** `cloudTaskName` field is empty string or null on created reservation documents.

### Pitfall 2: Cloud Tasks enqueued inside the Firestore transaction
**What goes wrong:** Developer puts the `tasksClient.createTask()` call inside the `runTransaction` callback. Firestore may retry the transaction body multiple times on contention — each retry enqueues a new Cloud Task, creating multiple `releaseReservation` tasks for a single reservation.
**Why it happens:** Transactions are retried automatically by Firestore. Any side effects inside the transaction body run multiple times.
**How to avoid:** Enqueue the Cloud Task **after** `runTransaction` resolves. Then issue a separate `reservationRef.update({ cloudTaskName })` to store the name. The `releaseReservation` handler's guard transaction (check `status == active && now > expiresAt`) is idempotent, so even if two tasks fire, only one makes changes.
**Warning signs:** More `releaseReservation` invocations than reservations in logs; multiple expiry emails per reservation (observable in Phase 6).

### Pitfall 3: Cloud Tasks emulator fires immediately — test design confusion
**What goes wrong:** Developer runs `firebase emulators:start` and expects the 30-minute timer to actually wait. The emulator ignores `scheduleTime` and fires tasks immediately (open issue #8254). Tests that rely on a delay of 30 minutes will never work in the emulator.
**Why it happens:** Cloud Tasks emulator does not implement scheduling delays.
**How to avoid:** Two testing strategies: (a) For emulator integration tests, verify `releaseReservation` by POSTing directly to `http://localhost:5001/{projectId}/europe-west3/releaseReservation` with the reservation payload; (b) For `scheduleTime` correctness, deploy to a real Firebase project in staging and verify the task fires after 30 minutes.
**Warning signs:** `releaseReservation` fires within seconds of `createReservation` in emulator tests — this is expected emulator behavior, not a bug.

### Pitfall 4: Firestore rules block the owner's own item adds after D-19 security rule tightening
**What goes wrong:** If the updated items security rule blocks writes to the `status` field and `ItemRepositoryImpl.addItem()` sends `status: "available"` in the create payload, the owner's own item adds start failing with `PERMISSION_DENIED`.
**Why it happens:** Phase 3 `addItem` implementation sets `status: "available"` on item creation. Tightening the create rule to block `status` field breaks this.
**How to avoid:** Before tightening the security rule, check `ItemRepositoryImpl.addItem()` — if it sets `status`, either (a) remove `status` from the client-side add payload (let Cloud Function default it), or (b) scope the rule to block writes where `status` is being changed to a non-"available" value. Add a regression test in `firestore.rules.test.ts` for owner add.

### Pitfall 5: Region mismatch between createReservation queue path and releaseReservation function region
**What goes wrong:** `CloudTasksClient` is given `location = "us-central1"` but `releaseReservation` is deployed to `europe-west3`. The HTTP target URL doesn't match the queue path, and tasks are silently dropped or routed to the wrong region.
**Why it happens:** Cloud Tasks queues are regional — the queue location must match the function region.
**How to avoid:** Set both `location` and `onTaskDispatched` region to `"europe-west3"` consistently. Use `process.env.GCLOUD_PROJECT` for the project ID to avoid hardcoding.

### Pitfall 6: GuestPreferencesDataStore uses same DataStore file name as LanguagePreferencesDataStore
**What goes wrong:** `GuestPreferencesDataStore` defines `val Context.dataStore by preferencesDataStore(name = "user_prefs")` — the same name as in `LanguagePreferencesDataStore`. DataStore will throw an `IllegalStateException` at runtime about duplicate instances.
**Why it happens:** Kotlin property delegates with the same name string are backed by the same file.
**How to avoid:** Use `name = "guest_prefs"` in `GuestPreferencesDataStore`. Each DataStore must use a unique name.

### Pitfall 7: HTTP target URL for Cloud Task hardcoded with wrong project/region
**What goes wrong:** The `httpRequest.url` in `CloudTasksClient.createTask()` is constructed using a hardcoded project ID that differs between local emulator, staging, and production. Tasks enqueued in staging hit production functions.
**Why it happens:** Project IDs are environment-specific; hardcoding them in function code is a common mistake.
**How to avoid:** Derive the URL from `process.env.GCLOUD_PROJECT` and `process.env.FUNCTION_REGION` (or the known `europe-west3` constant). For emulator testing, override with `FUNCTIONS_EMULATOR_HOST` environment variable.

---

## Code Examples

### Full createReservation skeleton

```typescript
// functions/src/reservation/createReservation.ts
import { onCall, HttpsError } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";
import { CloudTasksClient } from "@google-cloud/tasks";

interface CreateReservationRequest {
  registryId: string;
  itemId: string;
  giverName: string;
  giverEmail: string;
  giverId: string | null;
}

interface CreateReservationResponse {
  reservationId: string;
  affiliateUrl: string;
  expiresAtMs: number;
}

const tasksClient = new CloudTasksClient();

export const createReservation = onCall<CreateReservationRequest>(
  { region: "europe-west3", minInstances: 1 },
  async (request): Promise<CreateReservationResponse> => {
    const { registryId, itemId, giverName, giverEmail, giverId } = request.data;

    // Validate inputs
    if (!registryId || !itemId || !giverName || !giverEmail) {
      throw new HttpsError("invalid-argument", "Missing required fields");
    }

    const db = admin.firestore();
    let reservationId = "";
    let affiliateUrl = "";
    const expiresAtMs = Date.now() + 30 * 60 * 1000;

    await db.runTransaction(async (tx) => {
      const itemRef = db.collection("registries").doc(registryId)
        .collection("items").doc(itemId);
      const itemSnap = await tx.get(itemRef);

      if (!itemSnap.exists) {
        throw new HttpsError("not-found", "ITEM_NOT_FOUND");
      }
      const itemData = itemSnap.data()!;
      if (itemData.status !== "available") {
        throw new HttpsError("failed-precondition", "ITEM_UNAVAILABLE");
      }

      affiliateUrl = itemData.affiliateUrl as string;
      const reservationRef = db.collection("reservations").doc();
      reservationId = reservationRef.id;
      const expiresAt = admin.firestore.Timestamp.fromMillis(expiresAtMs);

      tx.update(itemRef, {
        status: "reserved",
        reservedBy: giverEmail,
        reservedAt: admin.firestore.FieldValue.serverTimestamp(),
        expiresAt,
      });

      tx.set(reservationRef, {
        itemId, registryId,
        giverId: giverId ?? null,
        giverName, giverEmail,
        affiliateUrl,
        status: "active",
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
        expiresAt,
        cloudTaskName: "",  // updated after task enqueue
      });
    });

    // Enqueue Cloud Task AFTER transaction commits (never inside transaction)
    const projectId = process.env.GCLOUD_PROJECT!;
    const region = "europe-west3";
    const queuePath = tasksClient.queuePath(projectId, region, "release-reservation");

    const [taskResponse] = await tasksClient.createTask({
      parent: queuePath,
      task: {
        httpRequest: {
          httpMethod: "POST" as const,
          url: `https://${region}-${projectId}.cloudfunctions.net/releaseReservation`,
          body: Buffer.from(JSON.stringify({ reservationId })).toString("base64"),
          headers: { "Content-Type": "application/json" },
        },
        scheduleTime: { seconds: Math.floor(expiresAtMs / 1000) },
      },
    });

    // Store task name on reservation for future cancellation (D-06)
    await db.collection("reservations").doc(reservationId)
      .update({ cloudTaskName: taskResponse.name! });

    return { reservationId, affiliateUrl, expiresAtMs };
  }
);
```

### Firestore rules test additions (D-19)

```typescript
// Extends tests/rules/firestore.rules.test.ts

describe("Items status field — client write blocked", () => {
  beforeEach(async () => {
    await seedRegistry("status-test-reg", {
      ownerId: "owner-status",
      visibility: "public",
      invitedUsers: {},
    });
    await testEnv.withSecurityRulesDisabled(async (ctx) => {
      await setDoc(
        doc(ctx.firestore(), "registries", "status-test-reg", "items", "item-1"),
        { title: "Gift", status: "available", affiliateUrl: "https://example.com" }
      );
    });
  });

  it("allows unauthenticated read of item status (RES-02/RES-06)", async () => {
    const unauthDb = testEnv.unauthenticatedContext().firestore();
    await assertSucceeds(
      getDoc(doc(unauthDb, "registries", "status-test-reg", "items", "item-1"))
    );
  });
});

describe("Reservations hard-deny extended (D-19)", () => {
  it("denies authenticated write to reservations", async () => {
    const db = testEnv.authenticatedContext("any-uid").firestore();
    await assertFails(
      setDoc(doc(db, "reservations", "res-auth"), { status: "active" })
    );
  });
});
```

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `firebase-functions/v1` task queue | `firebase-functions/v2/tasks` `onTaskDispatched` | 2022 (v1 deprecated) | Import from `firebase-functions/v2/tasks` not `v1` |
| `getFunctions().taskQueue().enqueue()` for all task ops | Direct `@google-cloud/tasks` for task name retrieval | Always been the case; Admin SDK never returned names | Require `@google-cloud/tasks` as explicit dependency for D-06 |
| `callbackFlow` + `SharedPreferences` | `DataStore<Preferences>` | AndroidX DataStore 1.0 (2021) | DataStore is coroutine-native; SharedPreferences deprecated |
| XML layouts + Fragment navigation | Jetpack Compose + Navigation3 | Phase 2 established | All new UI follows Compose; no XML/Fragment |

**Deprecated/outdated:**
- `firebase-functions/v1` onCall/task patterns: never use in this codebase (established in Phase 1)
- `SharedPreferences`: never use — DataStore is the standard (per CLAUDE.md)
- `LiveData`: never use — use `StateFlow` + `collectAsStateWithLifecycle()` (per CLAUDE.md)

---

## Open Questions

1. **Cloud Task queue name for `releaseReservation`**
   - What we know: `onTaskDispatched` auto-creates a queue named after the function (`releaseReservation` → queue `release-reservation`). However, the `CloudTasksClient.createTask()` requires the queue to already exist (or be created separately).
   - What's unclear: Does deploying `onTaskDispatched` to Firebase automatically create the queue, or must `firebase.json` declare the queue explicitly?
   - Recommendation: Planner should include a Wave 0 step: `firebase deploy --only functions:releaseReservation` before deploying `createReservation`, to ensure the queue is created. Alternatively, add a Cloud Tasks queue resource to `firebase.json` if the project supports it.

2. **Items security rule for status field (D-19 scope)**
   - What we know: `ItemRepositoryImpl.addItem()` likely sends `status: "available"` in the create payload (verified in `ItemDto` default value). Blocking `status` writes in Firestore rules will break Phase 3 flows.
   - What's unclear: D-19 specifies "Items collection prevents client-side direct write to `status` field" but the existing owner add writes status.
   - Recommendation: Planner should narrow D-19 to "non-owner cannot write `status`" (already true via existing `isOwner` rule) rather than blocking the owner. The actual protection is that clients never call `createReservation` writes directly — Firestore rules + Cloud Functions Admin SDK bypass covers this.

3. **OIDC authentication for Cloud Task HTTP target**
   - What we know: Cloud Tasks calling an HTTPS Cloud Function endpoint requires either a service account OIDC token header or the function to allow unauthenticated calls.
   - What's unclear: Does Firebase-deployed `onTaskDispatched` automatically configure this, or must we add `oidcToken` to the `httpRequest` in `createTask()`?
   - Recommendation: Research this in a Wave 0 spike. The official Firebase docs for `onTaskDispatched` suggest the Admin SDK's `taskQueue.enqueue()` handles auth automatically, but when using direct `CloudTasksClient`, OIDC headers may need to be added manually.

---

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Node.js 22 | Cloud Functions runtime | Yes | v22.14.0 (local) | — |
| Firebase Emulator Suite | Local testing | Yes (Phase 1 configured) | — | — |
| `@google-cloud/tasks` | D-05/D-06: task enqueue + cloudTaskName | Not yet installed | 6.2.1 (latest) | None — required |
| `firebase-admin` 13.7.0 | Firestore runTransaction | Yes (in functions/package.json) | 13.7.0 | — |
| `firebase-functions` 7.2.3 | onCall + onTaskDispatched | Yes (in functions/package.json) | 7.2.3 | — |
| `@firebase/rules-unit-testing` | D-19 security rule tests | Yes (in tests/rules/package.json) | 5.0.0 | — |
| Android `firebase-functions` (client) | Callable invocation | Yes (in libs.versions.toml) | via BoM 34.11.0 | — |
| Android `datastore-preferences` | GuestPreferencesDataStore | Yes (in libs.versions.toml) | 1.2.1 | — |

**Missing dependencies with no fallback:**
- `@google-cloud/tasks` — must be added to `functions/package.json` before implementing `createReservation`. Install: `cd functions && npm install @google-cloud/tasks`

**Missing dependencies with fallback:**
- None.

---

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework (rules) | Jest 29 + `@firebase/rules-unit-testing` 5.0 (TypeScript) |
| Framework (Android) | JUnit 4 + MockK 1.13.17 + Turbine 1.2.0 + kotlinx-coroutines-test 1.9.0 |
| Config file (rules) | `tests/rules/jest.config.js` (exists) |
| Config file (Android) | Gradle test configuration (exists) |
| Quick run (rules) | `cd tests/rules && npm test -- --testPathPattern=firestore.rules.test` |
| Full suite (rules) | `firebase emulators:exec --only firestore,auth "cd tests/rules && npm test"` |
| Quick run (functions) | Direct HTTP POST to emulator endpoint (see below) |
| Full suite (functions) | `firebase emulators:exec --only functions,firestore "curl -X POST ..."` |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| RES-09 | Client cannot write `reservations/` directly (authenticated) | Security Rule | `cd tests/rules && npm test -- -t "denies authenticated write to reservations"` | Partial — add test in existing file |
| RES-02/RES-06 | Items `status` readable by unauthenticated client | Security Rule | `cd tests/rules && npm test -- -t "allows unauthenticated read of item status"` | No — Wave 0 |
| D-19 | Reservation collection: read+write deny for all clients | Security Rule | `cd tests/rules && npm test -- -t "Reservation collection"` | Yes (lines 105-120) |
| RES-01/RES-09 | `createReservation` aborts if status != available | Integration (emulator) | `curl -X POST http://localhost:5001/{projectId}/europe-west3/createReservation -H "Content-Type: application/json" -d '{"data":{...}}'` | No — Wave 0 |
| RES-05 | `releaseReservation` reverts item status and reservation | Integration (emulator) | `curl -X POST http://localhost:5001/{projectId}/europe-west3/releaseReservation -H "Content-Type: application/json" -d '{"data":{"reservationId":"test-id"}}'` | No — Wave 0 |
| RES-07 stub | Expiry stub logs correct message | Unit (Node) | `ts-node --eval "..."` or emulator test | No — Wave 0 |
| D-16 | Guest identity persists across process death | Android instrumented | Manual: Developer Options → "Don't keep activities" + reserve + reopen | Manual only |
| D-18 | Countdown display-only, never writes to Firestore | Android unit | ViewModel test with MockK: verify no Firestore write called after countdown | No — Wave 0 |

### Sampling Rate

- **Per task commit:** `cd tests/rules && npm test -- --forceExit` (security rules only, fast)
- **Per wave merge:** `firebase emulators:exec --only firestore,auth,functions "cd tests/rules && npm test"`
- **Phase gate:** All security rule tests green + manual emulator smoke test of `createReservation` → `releaseReservation` flow

### Wave 0 Gaps

- [ ] `tests/rules/firestore.rules.test.ts` — Add: "allows unauthenticated read of item status" test (RES-02/RES-06)
- [ ] `tests/rules/firestore.rules.test.ts` — Add: "denies authenticated write to reservations collection" (already partially present at line 116 — extend for authenticated write denial)
- [ ] `functions/src/reservation/createReservation.ts` — New file
- [ ] `functions/src/reservation/releaseReservation.ts` — New file
- [ ] `functions/package.json` — Add `@google-cloud/tasks`
- [ ] `functions/src/index.ts` — Export `createReservation`, `releaseReservation`

---

## Project Constraints (from CLAUDE.md)

| Directive | Applies To | Constraint |
|-----------|------------|------------|
| Firebase only — no other persistence layer | All data | No SQLite, no Room — reservation state in Firestore only |
| Kotlin for Android | Android code | No Java files; all new code in Kotlin |
| No KTX modules | Firebase Android imports | Use `firebase-functions`, `firebase-firestore` (not `-ktx` variants) |
| No Firebase Realtime Database | Data layer | Use Cloud Firestore only |
| No XML layouts | Android UI | All new UI in Jetpack Compose |
| No hardcoded strings | UI | All labels in `strings.xml` (`reservation_` prefix per Phase 1 convention) |
| Navigation3 1.0.1 | Android navigation | If a re-reserve deep link landing screen is added, use `AppNavKeys.kt` + Navigation3 back stack |
| StateFlow + collectAsStateWithLifecycle | Android reactive UI | No LiveData; ViewModel exposes `StateFlow`, composables collect with lifecycle awareness |
| DataStore Preferences | Local preferences | Guest identity uses DataStore, not SharedPreferences |
| Cloud Functions 2nd gen | Backend | Import from `firebase-functions/v2/*` only |
| minSdk 21 | Android | No API-level-gating for DataStore (DataStore supports API 16+) |
| AGP 9.x + KSP (not KAPT) | Build | KSP for Hilt annotation processing |
| Feature prefix in string keys | strings.xml | Use `reservation_` prefix for all new string resources |

---

## Sources

### Primary (HIGH confidence)
- `firebase.google.com/docs/functions/task-functions` — onTaskDispatched v2 signature, queue config options, emulator behavior
- `firebase.google.com/docs/firestore/manage-data/transactions` — `runTransaction` Admin SDK pattern, abort behavior
- `cloud.google.com/tasks/docs/reference/rest/v2/projects.locations.queues.tasks/create` — Task.name field format
- `docs.cloud.google.com/tasks/docs/reference/rest/v2/projects.locations.queues.tasks/create` — Confirmed Task.name format: `projects/{P}/locations/{L}/queues/{Q}/tasks/{T}`
- Existing codebase: `LanguagePreferencesDataStore.kt`, `RegistryRepositoryImpl.kt`, `FirestoreDataSource.kt`, `inviteToRegistry.ts` — established patterns confirmed by reading source

### Secondary (MEDIUM confidence)
- `github.com/firebase/firebase-admin-node/issues/1753` — Confirmed: `taskQueue.enqueue()` returns void, task name not exposed
- `github.com/firebase/firebase-admin-node/issues/2039` — Confirmed: explicit task name via Admin SDK is unimplemented
- `github.com/firebase/firebase-tools/issues/8254` — Confirmed: Cloud Tasks emulator ignores scheduleTime (open issue)
- `medium.com/@ak187429/...` — `CloudTasksClient.createTask()` returning `response[0].name` pattern (verified against Cloud Tasks REST API docs)
- `app.unpkg.com/firebase-functions@7.0.3/.../tasks.d.ts` — TaskQueueOptions interface with minInstances, retryConfig, rateLimits

### Tertiary (LOW confidence)
- Various Medium articles on Cloud Tasks patterns — cross-verified with official docs where critical
- Community confirmation of `firebase-functions/v2/tasks` import path — requires confirming with actual `functions/node_modules` inspection before implementing

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — verified against package.json, npm registry, official Firebase docs
- Architecture: HIGH — patterns derived from existing codebase + official transaction docs
- Cloud Tasks cancellation: HIGH — confirmed via two GitHub issues + Cloud Tasks REST API docs that Admin SDK `enqueue()` returns void
- Cloud Tasks emulator behavior: MEDIUM — confirmed via open GitHub issue; actual fix status uncertain
- onTaskDispatched import path: MEDIUM — `firebase-functions/v2/tasks` confirmed in docs examples; `firebase-functions/tasks` also used — both should work in firebase-functions 7.x
- Pitfalls: HIGH — derived from `.planning/research/PITFALLS.md` (pre-researched) + codebase analysis
- Security rules: HIGH — current `firestore.rules` verified; D-19 scope analysis based on existing rule structure

**Research date:** 2026-04-11
**Valid until:** 2026-05-11 (firebase-functions and @google-cloud/tasks are actively developed; re-verify versions before implementing)
