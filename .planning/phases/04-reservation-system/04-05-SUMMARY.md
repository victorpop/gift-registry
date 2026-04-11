---
phase: 04-reservation-system
plan: 05
subsystem: ui
tags: [kotlin, jetpack-compose, firestore, reservation, countdown]

# Dependency graph
requires:
  - phase: 04-reservation-system
    provides: createReservation Cloud Function writes expiresAt Timestamp to Firestore item doc; ReservationCountdown composable already implemented but orphaned
provides:
  - Item domain model with expiresAt: Long? field
  - ItemDto with expiresAt: Long? manually populated from Firestore Timestamp via doc.getTimestamp()
  - FirestoreDataSource.observeItems reads expiresAt Timestamp and converts to epoch millis
  - ReservationCountdown wired into ItemCard in RegistryDetailScreen for status==RESERVED items
  - Gap 1 from 04-VERIFICATION.md closed: reserved items now show live countdown text
affects: [04-reservation-system verification, phase-05, phase-06]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "doc.getTimestamp(field)?.toDate()?.time pattern for reading server-written Firestore Timestamps in Android"
    - "Manual .copy() enrichment after doc.toObject() for fields that cannot be POJO-mapped (Timestamp → Long)"

key-files:
  created: []
  modified:
    - app/src/main/java/com/giftregistry/domain/model/Item.kt
    - app/src/main/java/com/giftregistry/data/model/ItemDto.kt
    - app/src/main/java/com/giftregistry/data/registry/FirestoreDataSource.kt
    - app/src/main/java/com/giftregistry/data/registry/ItemRepositoryImpl.kt
    - app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailScreen.kt
    - app/src/test/java/com/giftregistry/domain/usecase/ReserveItemUseCaseTest.kt
    - app/src/test/java/com/giftregistry/ui/registry/detail/ReserveItemViewModelTest.kt

key-decisions:
  - "doc.getTimestamp('expiresAt') used (not POJO auto-mapping) because Firestore writes expiresAt as Timestamp server-side; Long POJO field would silently deserialize to null"
  - "expiresAt excluded from Item.toMap() and Item.toUpdateMap() — client must never write this field (D-08: no client-side timer authority)"
  - "item.expiresAt?.let{} pattern preserves backward compat: legacy reserved items without expiresAt show only the Reserved label, not a broken countdown"

patterns-established:
  - "Server-written Timestamp fields: always read via doc.getTimestamp() + .copy() enrichment, never via POJO auto-mapping"
  - "Display-only countdown: ReservationCountdown manages its own internal time state via LaunchedEffect; no ViewModel involvement needed"

requirements-completed: [RES-02, RES-06]

# Metrics
duration: 8min
completed: 2026-04-06
---

# Phase 4 Plan 5: Reservation Countdown Summary

**Firestore expiresAt Timestamp threaded through DTO → domain → UI to wire orphaned ReservationCountdown composable into reserved ItemCards**

## Performance

- **Duration:** 8 min
- **Started:** 2026-04-06T00:00:00Z
- **Completed:** 2026-04-06T00:08:00Z
- **Tasks:** 2
- **Files modified:** 7

## Accomplishments

- Added `val expiresAt: Long? = null` to both `Item` domain model and `ItemDto`
- `FirestoreDataSource.observeItems` now reads the server-authoritative Firestore Timestamp via `doc.getTimestamp("expiresAt")?.toDate()?.time` and attaches it via `.copy(expiresAt = expiresAtMs)` — bypasses POJO auto-mapping which would fail silently for Timestamp fields
- `ItemRepositoryImpl.toDomain` threads `expiresAt` through from DTO to domain
- `RegistryDetailScreen.ItemCard` renders `ReservationCountdown(expiresAtMs = item.expiresAt!!)` inside an `item.expiresAt?.let{}` block under the RESERVED status branch — composable is no longer orphaned
- Gap 1 from 04-VERIFICATION.md closed: Truth #7 "Reserved items show countdown text derived from expiresAt" is now verifiable

## Task Commits

1. **Task 1: Add expiresAt field to Item + ItemDto + populate from Firestore** - `7392e70` (feat)
2. **Task 2: Wire ReservationCountdown into ItemCard + fix pre-existing test bugs** - `cedaa64` (feat)

## Files Created/Modified

- `app/src/main/java/com/giftregistry/domain/model/Item.kt` - Added `val expiresAt: Long? = null`
- `app/src/main/java/com/giftregistry/data/model/ItemDto.kt` - Added `val expiresAt: Long? = null` (manually populated, not POJO-mapped)
- `app/src/main/java/com/giftregistry/data/registry/FirestoreDataSource.kt` - observeItems reads Firestore Timestamp via `doc.getTimestamp("expiresAt")`
- `app/src/main/java/com/giftregistry/data/registry/ItemRepositoryImpl.kt` - toDomain passes `expiresAt = expiresAt` through
- `app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailScreen.kt` - ItemCard wires `ReservationCountdown` under RESERVED branch
- `app/src/test/java/com/giftregistry/domain/usecase/ReserveItemUseCaseTest.kt` - Auto-fix: added missing `resolve()` override to FakeReservationRepository
- `app/src/test/java/com/giftregistry/ui/registry/detail/ReserveItemViewModelTest.kt` - Auto-fix: added missing `deepLinkBus` parameter to vm() factory

## Decisions Made

- `doc.getTimestamp("expiresAt")` with manual `.copy()` chosen over POJO mapping: Firestore writes `expiresAt` as `com.google.firebase.Timestamp` server-side; an `ItemDto` field of type `Long` would fail to deserialize it and silently return `null` — explicit read avoids this.
- `expiresAt` excluded from `Item.toMap()` and `Item.toUpdateMap()`: client must never write or overwrite the server-authoritative expiry (D-08 preservation).
- `item.expiresAt?.let{}` wrapping: allows legacy reserved items (created before this field was readable) to display only the static "Reserved" label without crashing.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] FakeReservationRepository missing resolve() override in ReserveItemUseCaseTest**
- **Found during:** Task 2 verification (./gradlew :app:testDebugUnitTest)
- **Issue:** `ReservationRepository` interface gained a `resolve()` method in plan 04-04 but `FakeReservationRepository` in the test was not updated; caused `compileDebugUnitTestKotlin` failure
- **Fix:** Added `override suspend fun resolve(...) = Result.failure(UnsupportedOperationException(...))` stub to the fake
- **Files modified:** `app/src/test/java/com/giftregistry/domain/usecase/ReserveItemUseCaseTest.kt`
- **Verification:** `./gradlew :app:testDebugUnitTest` passes
- **Committed in:** `cedaa64` (Task 2 commit)

**2. [Rule 1 - Bug] ReserveItemViewModelTest vm() factory missing deepLinkBus parameter**
- **Found during:** Task 2 verification (./gradlew :app:testDebugUnitTest)
- **Issue:** `RegistryDetailViewModel` constructor gained `deepLinkBus: ReservationDeepLinkBus` in plan 04-04; test helper `vm()` was not updated; caused `compileDebugUnitTestKotlin` failure
- **Fix:** Added `deepLinkBus = ReservationDeepLinkBus()` to the `RegistryDetailViewModel(...)` call in `vm()`
- **Files modified:** `app/src/test/java/com/giftregistry/ui/registry/detail/ReserveItemViewModelTest.kt`
- **Verification:** All `ReserveItemViewModelTest` tests pass
- **Committed in:** `cedaa64` (Task 2 commit)

---

**Total deviations:** 2 auto-fixed (2 × Rule 1 - Bug)
**Impact on plan:** Both fixes were necessary to achieve the acceptance criterion of passing unit tests. No scope creep — both bugs were directly caused by plan 04-04 changes not propagated to tests.

## Issues Encountered

- Stale KSP-generated Hilt factory (`RegistryDetailViewModel_Factory.java`) was cached with old constructor signature from before `deepLinkBus` was added in 04-04. Clean build resolved it; no code change needed.

## Known Stubs

None — `item.expiresAt` is populated from the live Firestore Timestamp written by the `createReservation` Cloud Function. No placeholder data paths.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Reservation countdown gap fully closed; 04-VERIFICATION.md Truth #7 is now satisfiable end-to-end
- No client writes `expiresAt` — server authority preserved for the 30-minute reservation window
- Phase 5 (Web Fallback / Sharing) can proceed without reservation system blockers

---
*Phase: 04-reservation-system*
*Completed: 2026-04-06*
