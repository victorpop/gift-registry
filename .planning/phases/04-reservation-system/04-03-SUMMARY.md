---
phase: 04-reservation-system
plan: 03
subsystem: api
tags: [kotlin, firebase, datastore, hilt, clean-architecture, domain-layer, reservation]

# Dependency graph
requires:
  - phase: 04-reservation-system
    provides: "04-01 Firestore schema + Cloud Functions createReservation/releaseReservation stubs"
  - phase: 04-reservation-system
    provides: "04-02 createReservation Cloud Function implementation with Cloud Tasks"
provides:
  - ReservationRepository interface (domain/reservation/ReservationRepository.kt)
  - ReserveItemUseCase with @Inject constructor (domain/usecase/ReserveItemUseCase.kt)
  - GuestPreferencesRepository interface (domain/preferences/GuestPreferencesRepository.kt)
  - ReservationResult data class (domain/model/Reservation.kt)
  - GuestPreferencesDataStore backed by DataStore name="guest_prefs" (data layer)
  - ReservationRepositoryImpl calling createReservation Firebase callable
  - Hilt bindings for both new repositories in DataModule
affects: [04-reservation-system, wave-3-ui]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "DataStore delegate on distinct Context extension property per feature (guest_prefs vs user_prefs) prevents runtime IllegalStateException"
    - "runCatching wraps Firebase suspend calls — converts FirebaseException to Result.failure at data layer boundary"
    - "FakeRepository in unit tests uses constructor-injected Result for deterministic test outcomes"
    - "InMemory fake implements interface contract to verify protocol without Android framework deps"

key-files:
  created:
    - app/src/main/java/com/giftregistry/domain/model/Reservation.kt
    - app/src/main/java/com/giftregistry/domain/reservation/ReservationRepository.kt
    - app/src/main/java/com/giftregistry/domain/preferences/GuestPreferencesRepository.kt
    - app/src/main/java/com/giftregistry/domain/usecase/ReserveItemUseCase.kt
    - app/src/main/java/com/giftregistry/data/preferences/GuestPreferencesDataStore.kt
    - app/src/main/java/com/giftregistry/data/reservation/ReservationRepositoryImpl.kt
  modified:
    - app/src/main/java/com/giftregistry/di/DataModule.kt
    - app/src/test/java/com/giftregistry/domain/usecase/ReserveItemUseCaseTest.kt
    - app/src/test/java/com/giftregistry/data/preferences/GuestPreferencesDataStoreTest.kt

key-decisions:
  - "GuestPreferencesDataStore uses DataStore name='guest_prefs' not 'user_prefs' — unique property name via Context extension prevents runtime IllegalStateException from duplicate DataStore initialization"
  - "GuestPreferencesDataStoreTest uses in-memory fake (not Robolectric) — verifies repository contract without Android framework; DataStore persistence behavior verified manually per 04-VALIDATION.md"
  - "ReservationRepositoryImpl payload includes giverId as nullable — supports both anonymous guests (null) and authenticated users"

patterns-established:
  - "Two-level DataStore isolation: each DataStore feature gets its own Context extension property name and val identifier"
  - "ReservationResult extracted from callable response as Map<String, Any?> with explicit casts — mirrors RegistryRepositoryImpl inviteToRegistry pattern"

requirements-completed: [RES-01, RES-02, RES-06]

# Metrics
duration: 2min
completed: 2026-04-11
---

# Phase 4 Plan 03: Reservation Domain + Data Layer Summary

**Clean-architecture seam for reservations: ReservationRepository interface, ReserveItemUseCase, GuestPreferencesDataStore (DataStore "guest_prefs"), and Hilt bindings — Wave 3 UI can inject ReserveItemUseCase directly**

## Performance

- **Duration:** 2 min
- **Started:** 2026-04-11T16:37:43Z
- **Completed:** 2026-04-11T16:39:43Z
- **Tasks:** 2
- **Files modified:** 9 (7 created, 2 replaced stubs)

## Accomplishments
- Established the RES-01 clean-architecture seam: domain interfaces with zero Firebase/Android imports
- Implemented GuestPreferencesDataStore with DataStore name="guest_prefs" — distinct from "user_prefs" (D-16 guest identity persists across process death)
- Replaced two Wave 0 stub tests (NotImplementedError) with GREEN test suites (3 tests each): ReserveItemUseCaseTest and GuestPreferencesDataStoreTest
- Wired Hilt bindings in DataModule: bindReservationRepository + bindGuestPreferences — assembleDebug passes

## Task Commits

Each task was committed atomically:

1. **Task 1: Domain model + interfaces + use case + GREEN ReserveItemUseCaseTest** - `a208f43` (feat)
2. **Task 2: GuestPreferencesDataStore + ReservationRepositoryImpl + DataModule wiring** - `2a33fde` (feat)

**Plan metadata:** *(docs commit follows)*

_Note: Both tasks included TDD — stub tests replaced with GREEN implementations_

## Files Created/Modified
- `app/src/main/java/com/giftregistry/domain/model/Reservation.kt` - ReservationResult data class (reservationId, affiliateUrl, expiresAtMs)
- `app/src/main/java/com/giftregistry/domain/reservation/ReservationRepository.kt` - Interface: suspend reserve(registryId, itemId, giver, giverId)
- `app/src/main/java/com/giftregistry/domain/preferences/GuestPreferencesRepository.kt` - Interface: observeGuestIdentity, getGuestIdentity, saveGuestIdentity, clearGuestIdentity
- `app/src/main/java/com/giftregistry/domain/usecase/ReserveItemUseCase.kt` - @Inject use case delegating to ReservationRepository
- `app/src/main/java/com/giftregistry/data/preferences/GuestPreferencesDataStore.kt` - DataStore "guest_prefs" implementation of GuestPreferencesRepository
- `app/src/main/java/com/giftregistry/data/reservation/ReservationRepositoryImpl.kt` - Firebase callable "createReservation" invocation with runCatching
- `app/src/main/java/com/giftregistry/di/DataModule.kt` - Added bindReservationRepository and bindGuestPreferences @Binds methods
- `app/src/test/java/com/giftregistry/domain/usecase/ReserveItemUseCaseTest.kt` - Replaced NotImplementedError stub with 3 GREEN tests using FakeReservationRepository
- `app/src/test/java/com/giftregistry/data/preferences/GuestPreferencesDataStoreTest.kt` - Replaced NotImplementedError stub with 3 GREEN tests using InMemoryGuestPrefs

## Decisions Made
- GuestPreferencesDataStore uses DataStore name="guest_prefs" not "user_prefs" — unique Context extension property name prevents runtime IllegalStateException from duplicate DataStore initialization (Research Pitfall 6)
- GuestPreferencesDataStoreTest uses in-memory fake (not Robolectric) — verifies repository contract without Android framework; DataStore persistence behavior to be verified manually per 04-VALIDATION.md
- ReservationRepositoryImpl payload includes giverId as nullable — supports both anonymous guests (null) and authenticated users passing Firebase UID

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Domain + data layer complete for reservations; Wave 3 UI (Plan 04) can inject ReserveItemUseCase and GuestPreferencesRepository
- GuestPreferencesDataStore real persistence (across process death) is a manual validation concern per 04-VALIDATION.md — not a code issue
- No blockers for Plan 04

---
*Phase: 04-reservation-system*
*Completed: 2026-04-11*

## Self-Check: PASSED

All 6 created files found on disk. Both task commits (a208f43, 2a33fde) verified in git log.
