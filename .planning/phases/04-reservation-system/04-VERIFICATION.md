---
phase: 04-reservation-system
verified: 2026-04-11T17:15:00Z
status: human_needed
score: 9/9 must-haves verified
re_verification:
  previous_status: gaps_found
  previous_score: 7/9
  gaps_closed:
    - "Reserved items display countdown derived from expiresAt (D-08, D-18) — ReservationCountdown now wired into ItemCard via item.expiresAt?.let{}"
    - "Re-reserve deep link resolves reservationId and re-opens reserve flow (RES-08) — TODO stub replaced with ReReserveResolver composable + resolveReservation Cloud Function"
  gaps_remaining: []
  regressions: []
human_verification:
  - test: "Reserve flow end-to-end on device/emulator"
    expected: "Tap Reserve on available item -> GuestIdentitySheet appears (if first time) -> submit -> Intent fires to affiliateUrl in browser -> item shows Reserved status in real time for other users"
    why_human: "Intent launch, ModalBottomSheet rendering, and real-time Firestore status update require a running device"
  - test: "Race condition prevention (RES-09) under concurrent access"
    expected: "Two simultaneous reservations on the same item: exactly one succeeds with 200 + reservationId, the other returns ITEM_UNAVAILABLE"
    why_human: "Cannot simulate concurrent Firestore transactions without a running emulator and two parallel HTTP clients"
  - test: "30-minute timer auto-release (RES-05)"
    expected: "After 30 minutes, Cloud Task fires releaseReservation, item reverts to available status, stub email log appears"
    why_human: "Requires running Firebase emulator; emulator ignores scheduleTime so direct POST to /releaseReservation needed"
  - test: "Guest identity persistence across process death (D-16)"
    expected: "Save guest identity, kill app, relaunch — GuestIdentitySheet does not reappear; reservation proceeds with saved identity"
    why_human: "DataStore persistence requires a real device or Robolectric; current test uses in-memory fake"
  - test: "Re-reserve deep link end-to-end"
    expected: "Tap re-reserve link in email (https://giftregistry.app/reservation/{id}/re-reserve) -> app opens, spinner briefly shown, navigates to correct registry -> reserve flow auto-triggers for the specific item. If item no longer available, reservation_error_unavailable snackbar appears."
    why_human: "Requires running device + Firebase Functions emulator; deep link Intent and Navigation3 back-stack replacement cannot be verified statically"
  - test: "Reservation countdown display"
    expected: "Reserved item card shows live countdown text (e.g. '29:42 remaining') that decrements every second. Items with no expiresAt (legacy or available) show only the 'Reserved' label, not a broken countdown."
    why_human: "Requires a running device with a live Firestore reservation document containing an expiresAt Timestamp"
---

# Phase 4: Reservation System Verification Report

**Phase Goal:** Gift givers can reliably reserve an available item with a server-authoritative 30-minute timer, no duplicate reservations, and automatic release on expiry
**Verified:** 2026-04-11
**Status:** human_needed (all automated checks pass; 2 previously-failed truths now verified; 4 items need device/emulator confirmation)
**Re-verification:** Yes — after gap closure plans 04-05 and 04-06

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Gift giver can reserve an available item (RES-01) | ✓ VERIFIED | `createReservation.ts` runs `db.runTransaction` that checks `status == "available"`, creates reservation doc, updates item status. Android `ReservationRepositoryImpl` calls `getHttpsCallable("createReservation")`. `RegistryDetailScreen` shows Reserve button on `AVAILABLE` items. |
| 2 | Reserved items show as unavailable in real time (RES-02) | ✓ VERIFIED | `FirestoreDataSource.observeItems` uses `addSnapshotListener` (callbackFlow) so status changes from Cloud Function propagate instantly to UI. `ItemCard` renders status via `ItemStatusChip`. |
| 3 | 30-minute timer starts on reserve action (RES-03) | ✓ VERIFIED | `createReservation.ts`: `expiresAtMs = Date.now() + 30*60*1000`, Cloud Task enqueued at `scheduleTime: seconds = Math.floor(expiresAtMs / 1000)` AFTER transaction commits. |
| 4 | Giver is redirected to retailer immediately upon reservation (RES-04) | ✓ VERIFIED | `RegistryDetailScreen` collects `ReservationEvent.OpenRetailer` in `LaunchedEffect(Unit)` and fires `Intent(Intent.ACTION_VIEW, affiliateUrl.toUri())`. Intent fires only after callable returns success. |
| 5 | Reservation auto-releases after 30 minutes (RES-05) | ✓ VERIFIED | `releaseReservation.ts` guards: `status != "active"` → no-op; `nowSeconds < expiresAtSeconds` → no-op; otherwise sets item `status = "available"` and reservation `status = "expired"`. Cloud Task schedules invocation at expiry time. |
| 6 | Auto-released items return to available in real time (RES-06) | ✓ VERIFIED | `releaseReservation.ts` sets `item.status = "available"` via transaction. `observeItems` snapshot listener propagates change to UI. |
| 7 | Countdown shown on reserved items (D-08, D-18) | ✓ VERIFIED | `Item.expiresAt: Long?` added to domain model and `ItemDto`. `FirestoreDataSource.observeItems` reads Firestore Timestamp via `doc.getTimestamp("expiresAt")?.toDate()?.time` and sets via `.copy(expiresAt = expiresAtMs)`. `ItemRepositoryImpl.toDomain` passes `expiresAt = expiresAt` through. `RegistryDetailScreen.ItemCard` renders `ReservationCountdown(expiresAtMs = expiresAtMs)` inside `item.expiresAt?.let{}` under RESERVED branch — composable is no longer orphaned. `expiresAt` excluded from `Item.toMap()` and `Item.toUpdateMap()` (D-08 client-write prohibition preserved). |
| 8 | Re-reserve deep link route resolves reservationId to the correct registry item and re-opens the reserve flow (RES-08) | ✓ VERIFIED | `resolveReservation.ts` onCall reads `reservations/{id}` via Admin SDK and returns `{registryId, itemId, status}`. `AppNavigation.entry<ReReserveDeepLink>` TODO stub replaced with `ReReserveResolver` composable backed by `ReReserveResolverViewModel`, which calls `resolveReservationUseCase(reservationId)`, emits to `ReservationDeepLinkBus(replay=1)`, then navigates to `RegistryDetailKey(registryId)`. `RegistryDetailViewModel.init` collects bus requests and calls `onReserveClicked(req.itemId)` when `req.registryId == registryId`. Error path falls back to HomeKey. No TODO stubs remain in AppNavigation.kt. |
| 9 | Two givers cannot simultaneously reserve the same item (RES-09) | ✓ VERIFIED | `createReservation.ts` uses `db.runTransaction` (Firestore serializable isolation). If `item.status != "available"` inside the transaction, throws `HttpsError("failed-precondition", "ITEM_UNAVAILABLE")`. `createTask` call is outside `runTransaction` body. |

**Score:** 9/9 truths verified

---

## Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `functions/src/reservation/createReservation.ts` | onCall with runTransaction + CloudTasksClient enqueue | ✓ VERIFIED | 112 lines; `runTransaction`, `createTask`, `ITEM_UNAVAILABLE`, `ITEM_NOT_FOUND`, `MISSING_REQUIRED_FIELDS`, `minInstances:1` all present |
| `functions/src/reservation/releaseReservation.ts` | onTaskDispatched with guard transaction + stub email | ✓ VERIFIED | `onTaskDispatched`, `FieldValue.delete()`, `status !== "active"` guard, `nowSeconds < expiresAtSeconds` guard, `[STUB] Email`, re-reserve URL present |
| `functions/src/reservation/resolveReservation.ts` | onCall Admin SDK reads reservation, returns registryId/itemId/status | ✓ VERIFIED | 43 lines; `onCall`, `db.collection("reservations").doc(reservationId).get()`, returns `{registryId, itemId, status}`; handles missing/malformed cases with HttpsError |
| `functions/src/index.ts` | exports createReservation + releaseReservation + resolveReservation | ✓ VERIFIED | All three exports on lines 16-18 |
| `app/…/domain/model/Item.kt` | data class Item with expiresAt: Long? | ✓ VERIFIED | `val expiresAt: Long? = null` present at line 15 |
| `app/…/data/model/ItemDto.kt` | ItemDto with expiresAt: Long? | ✓ VERIFIED | `val expiresAt: Long? = null` present at line 14; manually populated via `.copy()`, not POJO auto-mapped |
| `app/…/data/registry/FirestoreDataSource.kt` | observeItems reads expiresAt as Firestore Timestamp | ✓ VERIFIED | `doc.getTimestamp("expiresAt")?.toDate()?.time` at line 64; `.copy(expiresAt = expiresAtMs)` at line 67 |
| `app/…/data/registry/ItemRepositoryImpl.kt` | toDomain passes expiresAt through; toMap/toUpdateMap do NOT write it | ✓ VERIFIED | `expiresAt = expiresAt` in toDomain (line 64); `"expiresAt"` absent from toMap and toUpdateMap |
| `app/…/domain/model/Reservation.kt` | data class ReservationResult | ✓ VERIFIED | `data class ReservationResult(reservationId, affiliateUrl, expiresAtMs)` |
| `app/…/domain/model/ReservationLookup.kt` | data class ReservationLookup(registryId, itemId, status) | ✓ VERIFIED | 7-line file with correct fields |
| `app/…/domain/reservation/ReservationRepository.kt` | suspend fun reserve + suspend fun resolve interfaces | ✓ VERIFIED | Both methods present including `suspend fun resolve(reservationId: String): Result<ReservationLookup>` at line 15 |
| `app/…/domain/usecase/ResolveReservationUseCase.kt` | @Inject + suspend operator invoke delegating to repository | ✓ VERIFIED | `class ResolveReservationUseCase @Inject constructor`, `suspend operator fun invoke` delegates to `repository.resolve(reservationId)` |
| `app/…/ui/registry/detail/ReservationDeepLinkBus.kt` | @Singleton SharedFlow(replay=1) bus | ✓ VERIFIED | `@Singleton`, `MutableSharedFlow<AutoReserve>(replay = 1, extraBufferCapacity = 1)`, `suspend fun request(registryId, itemId)` |
| `app/…/ui/registry/detail/ReservationCountdown.kt` | Countdown composable from expiresAtMs | ✓ VERIFIED | Composable exists; call site in RegistryDetailScreen line 488 — no longer orphaned |
| `app/…/ui/navigation/AppNavigation.kt` | ReReserveDeepLink entry with ReReserveResolver + ReReserveResolverViewModel | ✓ VERIFIED | Entry at line 152; `ReReserveResolver` composable (line 171); `ReReserveResolverViewModel` (line 202) with `resolveReservationUseCase` + `deepLinkBus`; no TODO stubs |
| `app/…/ui/registry/detail/RegistryDetailViewModel.kt` | deepLinkBus injected; init block collects bus and calls onReserveClicked | ✓ VERIFIED | `deepLinkBus: ReservationDeepLinkBus` in constructor (line 35); `init` block at line 41-48 collects requests and calls `onReserveClicked(req.itemId)` gated by `req.registryId == registryId` |
| `app/…/ui/registry/detail/RegistryDetailScreen.kt` | ItemCard renders ReservationCountdown for RESERVED items with expiresAt | ✓ VERIFIED | Lines 487-491: `item.expiresAt?.let { expiresAtMs -> ReservationCountdown(expiresAtMs = expiresAtMs) }` inside RESERVED branch |

---

## Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `createReservation.ts` | Firestore reservations collection | `tx.set(reservationRef, {...})` | ✓ WIRED | `db.collection("reservations").doc()` inside transaction |
| `createReservation.ts` | Cloud Tasks queue | `tasksClient.createTask` | ✓ WIRED | `createTask` call OUTSIDE runTransaction (Pitfall 2 compliant) |
| `resolveReservation.ts` | Firestore reservations collection | `db.collection("reservations").doc(id).get()` | ✓ WIRED | Admin SDK read at line 26 — bypasses client hard-deny rule |
| `releaseReservation.ts` | stub email log | `[STUB] Email` console.log | ✓ WIRED | `console.log("[STUB] Email would be sent to...")` with re-reserve URL |
| `ReservationRepositoryImpl` | Firebase callable 'createReservation' | `functions.getHttpsCallable("createReservation").call(...)` | ✓ WIRED | `.await()` handles async |
| `ReservationRepositoryImpl` | Firebase callable 'resolveReservation' | `getHttpsCallable("resolveReservation")` | ✓ WIRED | Line 46 of ReservationRepositoryImpl |
| `DataModule` | ReservationRepositoryImpl | `@Binds bindReservationRepository` | ✓ WIRED | Present in `DataModule.kt` |
| `RegistryDetailViewModel` | ReservationDeepLinkBus | `@Inject constructor` + `init` block | ✓ WIRED | Collects `deepLinkBus.requests`, routes to `onReserveClicked(req.itemId)` |
| `ReReserveResolverViewModel` | ReservationDeepLinkBus | `deepLinkBus.request(registryId, itemId)` before State.Resolved | ✓ WIRED | Bus emit at line 224 precedes state update — replay=1 ensures RegistryDetailViewModel receives it |
| `AppNavigation.entry<ReReserveDeepLink>` | RegistryDetailKey | `onResolved` navigates `backStack.add(RegistryDetailKey(registryId))` | ✓ WIRED | Lines 155-158; clears backstack and opens correct registry screen |
| `FirestoreDataSource.observeItems` | ItemDto.expiresAt | `doc.getTimestamp("expiresAt")?.toDate()?.time` via `.copy()` | ✓ WIRED | Lines 64+67 |
| `ItemRepositoryImpl.toDomain` | Item.expiresAt | `expiresAt = expiresAt` field-by-field copy | ✓ WIRED | Line 64 |
| `RegistryDetailScreen.ItemCard` | ReservationCountdown composable | `item.expiresAt?.let { expiresAtMs -> ReservationCountdown(expiresAtMs = expiresAtMs) }` | ✓ WIRED | Lines 487-491; conditional on non-null expiresAt |
| `RegistryDetailScreen` | affiliateUrl Intent | `Intent(ACTION_VIEW, uri)` | ✓ WIRED | `Intent(Intent.ACTION_VIEW, event.affiliateUrl.toUri())` in LaunchedEffect |

---

## Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|---------------|--------|--------------------|--------|
| `RegistryDetailScreen` (ItemCard) | `items` (List<Item>) | `FirestoreDataSource.observeItems` via `addSnapshotListener` callbackFlow | Yes — real Firestore snapshot listener | ✓ FLOWING |
| `RegistryDetailViewModel` | `reservationEvents` Channel | `reserveItemUseCase` -> `ReservationRepositoryImpl` -> Firebase callable | Yes — callable returns real `{reservationId, affiliateUrl, expiresAtMs}` | ✓ FLOWING |
| `ReservationCountdown` | `expiresAtMs: Long` | `Item.expiresAt` ← `ItemDto.expiresAt` ← `doc.getTimestamp("expiresAt")` from Firestore item doc `expiresAt` Timestamp written by `createReservation` transaction | Yes — Firestore server-written Timestamp flows end-to-end | ✓ FLOWING |
| `RegistryDetailViewModel` (auto-reserve) | `deepLinkBus.requests` SharedFlow | `ReReserveResolverViewModel.resolve()` -> `resolveReservationUseCase` -> `getHttpsCallable("resolveReservation")` -> Firestore `reservations/{id}` | Yes — Admin SDK reads real document, emits real registryId/itemId | ✓ FLOWING |

---

## Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| `createReservation.ts` TypeScript compiles | File content check — 112-line implementation, no stub throws | Implementation present | ✓ PASS (inferred) |
| `runTransaction` before `createTask` (Pitfall 2) | grep line numbers: runTransaction opens line 41, closes ~79; createTask at line 88 | createTask AFTER `});` of runTransaction | ✓ PASS |
| `releaseReservation.ts` guard: status != active | `status !== "active"` guard present | Guard present | ✓ PASS |
| `releaseReservation.ts` guard: not yet expired | `nowSeconds < expiresAtSeconds` guard present | Guard present | ✓ PASS |
| Firestore rules: reservations hard-deny | `allow read, write: if false` preserved | Hard-deny preserved | ✓ PASS |
| `resolveReservation.ts` does NOT loosen hard-deny | Function uses Admin SDK (`admin.firestore()`) not client SDK | Admin SDK bypasses rules correctly | ✓ PASS |
| `expiresAt` absent from toMap/toUpdateMap | `grep '"expiresAt"' ItemRepositoryImpl.kt` returns 0 matches | Client never writes expiresAt | ✓ PASS |
| `ReservationCountdown` has call site | `grep 'ReservationCountdown(' RegistryDetailScreen.kt` returns match | Line 488 — no longer orphaned | ✓ PASS |
| No TODO stubs in AppNavigation.kt | `grep 'TODO Phase 6' AppNavigation.kt` returns nothing | No stubs | ✓ PASS |
| ReServeItemViewModelTest GREEN | Tests auto-fixed (deepLinkBus param, FakeRepo.resolve stub) in 04-05 | Passing per 04-05-SUMMARY.md | ✓ PASS (per summary) |

---

## Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| RES-01 | 04-01, 04-02, 04-03 | Gift giver can reserve an available item | ✓ SATISFIED | createReservation callable + Android ReservationRepositoryImpl + ReserveItemUseCase |
| RES-02 | 04-01, 04-03, 04-04, 04-05 | Reserved items show as unavailable in real time | ✓ SATISFIED | `addSnapshotListener` in FirestoreDataSource propagates `status: "reserved"` change; ItemStatusChip renders it |
| RES-03 | 04-01, 04-02 | 30-minute timer starts on reserve action | ✓ SATISFIED | `expiresAtMs = Date.now() + 30*60*1000`; Cloud Task scheduled at that epoch second |
| RES-04 | 04-04 | Giver is redirected to retailer immediately upon reservation | ✓ SATISFIED | `Intent(ACTION_VIEW)` fired from `OpenRetailer` event after callable success |
| RES-05 | 04-01, 04-02 | Reservation auto-releases after 30 minutes | ✓ SATISFIED | `releaseReservation` onTaskDispatched with expiry guard reverts item to available |
| RES-06 | 04-01, 04-02, 04-05 | Auto-released items return to available in real time | ✓ SATISFIED | releaseReservation sets status available; snapshot listener propagates |
| RES-07 | 04-02 | Giver receives expiration email when reservation lapses | ✓ SATISFIED (stub) | `[STUB] Email would be sent to {giverEmail}` logged. Full email integration deferred per plan design. |
| RES-08 | 04-04, 04-06 | Expiration email includes re-reserve option that re-opens the reserve flow | ✓ SATISFIED | Re-reserve URL in stub email log; `resolveReservation` Cloud Function resolves reservationId to registryId+itemId; `ReReserveResolver` composable navigates to correct registry and auto-triggers `onReserveClicked` via `ReservationDeepLinkBus(replay=1)`. TODO stub removed. |
| RES-09 | 04-01, 04-02 | Two givers cannot simultaneously reserve the same item | ✓ SATISFIED | Firestore `runTransaction` serializable isolation; `status != "available"` check inside transaction throws `ITEM_UNAVAILABLE` |

---

## Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `RegistryDetailScreen.kt` | 175 | `/* Share — Phase 5 */` | ℹ️ Info | Share button is a planned future feature; unrelated to Phase 4 goal |
| `releaseReservation.ts` | — | `[STUB] Email` console.log | ℹ️ Info | Full email integration deferred by design; RES-07 satisfied at stub level per plan |

Previously-reported blockers resolved:
- `RegistryDetailScreen.kt` line 480 stale `countdown deferred` comment — removed
- `AppNavigation.kt` line 152 `TODO Phase 6: resolve re-reserve` log — removed, replaced with working implementation

---

## Human Verification Required

### 1. Reserve Flow End-to-End

**Test:** Install app on device/emulator. Open a registry with an available item. Tap "Reserve". If first-time: fill GuestIdentitySheet (first name, last name, email) and tap Continue. Verify browser/intent opens to the item's affiliate URL.
**Expected:** Browser opens to retailer URL. Item status changes to "Reserved" immediately in the list. A second user viewing the same registry sees the item as Reserved in real time.
**Why human:** Intent launch and ModalBottomSheet rendering require a running device. Real-time status propagation requires two clients.

### 2. Race Condition Prevention (RES-09)

**Test:** Using Firebase emulator, send two simultaneous POST requests to `createReservation` callable for the same `registryId`/`itemId`.
**Expected:** Exactly one returns `{ reservationId, affiliateUrl, expiresAtMs }`. The other returns `HttpsError` with code `ITEM_UNAVAILABLE`.
**Why human:** Requires running Firebase Functions emulator and concurrent HTTP requests.

### 3. Auto-Release After 30 Minutes (RES-05)

**Test:** Using Firebase emulator, create a reservation. Directly POST to `releaseReservation` endpoint with the `reservationId`. Verify item `status` reverts to `"available"` in Firestore and reservation `status` becomes `"expired"`. Verify stub email log line appears in function output.
**Expected:** Item available, reservation expired, `[STUB] Email would be sent to...` in function logs.
**Why human:** Requires running Firebase emulator. Emulator ignores `scheduleTime` so direct HTTP POST needed.

### 4. Guest Identity Persistence (D-16)

**Test:** Reserve an item (which saves guest identity to DataStore "guest_prefs"). Force-kill the app. Relaunch and navigate to a registry. Tap Reserve on an available item.
**Expected:** GuestIdentitySheet does NOT appear. Reservation proceeds with previously saved identity.
**Why human:** DataStore persistence requires a real device or Robolectric integration test.

### 5. Re-Reserve Deep Link End-to-End

**Test:** With Firebase Functions emulator running, create a reservation to get a `reservationId`. Open `https://giftregistry.app/reservation/{reservationId}/re-reserve` via deep link Intent on the device.
**Expected:** App opens, brief CircularProgressIndicator shown, then navigates to the correct RegistryDetailScreen. The reserve flow auto-triggers for the specific item (GuestIdentitySheet appears or reserve proceeds directly if identity cached). If item is no longer available, `reservation_error_unavailable` snackbar is shown.
**Why human:** Deep link Intent, Navigation3 back-stack replacement, and async Firebase callable resolution require a running device and emulator.

### 6. Reservation Countdown Display

**Test:** With a live Firebase reservation (expiresAt Timestamp present on the item doc), open the registry on device. Observe the reserved ItemCard.
**Expected:** Live countdown text (e.g. "29:42 remaining") appears below the "Reserved" label and decrements every second. An item reserved before this update (no expiresAt field) shows only the "Reserved" label without errors.
**Why human:** Requires a running device with a live Firestore item document containing a server-written `expiresAt` Timestamp.

---

## Gaps Summary

No gaps remaining. All 9 truths are verified at the code level. Human verification items are non-blocking confirmations of device-level behavior that cannot be assessed by static code inspection.

**Gap closure summary:**
- **Gap 1 (Countdown — D-08, D-18):** Closed by plan 04-05. `expiresAt: Long?` threaded from Firestore Timestamp through `ItemDto` → `Item` domain model. `ReservationCountdown` composable wired into `ItemCard` via `item.expiresAt?.let{}` under RESERVED branch. Client write-path exclusion preserved.
- **Gap 2 (Re-reserve deep link — RES-08):** Closed by plan 04-06. New `resolveReservation` onCall Cloud Function reads `reservations/{id}` via Admin SDK (preserving client hard-deny). `ReReserveResolver` composable + `ReReserveResolverViewModel` replace the TODO stub in `AppNavigation`. `ReservationDeepLinkBus` (SharedFlow replay=1) bridges nav resolver to `RegistryDetailViewModel.init` which calls `onReserveClicked(itemId)`. Error path falls back to HomeKey gracefully.

---

_Verified: 2026-04-11_
_Verifier: Claude (gsd-verifier)_
