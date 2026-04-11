---
phase: 04-reservation-system
plan: 01
subsystem: testing
tags: [cloud-functions, cloud-tasks, firestore-rules, kotlin-tests, tdd, wave-0, nyquist]

# Dependency graph
requires:
  - phase: 03-registry-item-management
    provides: Firestore items subcollection with status field, existing security rules framework
provides:
  - "@google-cloud/tasks@6.2.1 dependency pre-wired in functions/package.json"
  - "createReservation Cloud Function stub (onCall, throws NOT_IMPLEMENTED)"
  - "releaseReservation Cloud Function stub (onTaskDispatched, throws NOT_IMPLEMENTED)"
  - "Phase 4 Firestore security rule tests (item status read + reservations hard-deny)"
  - "ReserveItemUseCaseTest Kotlin stub (RED state, throws NotImplementedError)"
  - "GuestPreferencesDataStoreTest Kotlin stub (RED state, throws NotImplementedError)"
  - "ReserveItemViewModelTest Kotlin stub (RED state, throws NotImplementedError)"
affects:
  - 04-02 (createReservation implementation)
  - 04-03 (ReserveItemUseCase implementation)
  - 04-04 (ReserveItemViewModel implementation)

# Tech tracking
tech-stack:
  added:
    - "@google-cloud/tasks@6.2.1 (Cloud Task enqueueing for reservation expiry)"
  patterns:
    - "Wave 0 Nyquist: pre-create stub files and failing tests before any implementation so all downstream verify commands have real targets"
    - "onCall stub pattern: export function that immediately throws HttpsError('unimplemented', ...)"
    - "onTaskDispatched stub pattern: export function that immediately throws Error('NOT_IMPLEMENTED...')"
    - "Kotlin RED-state stub: @Test fun that throws NotImplementedError with plan reference"

key-files:
  created:
    - "functions/src/reservation/createReservation.ts"
    - "functions/src/reservation/releaseReservation.ts"
    - "app/src/test/java/com/giftregistry/domain/usecase/ReserveItemUseCaseTest.kt"
    - "app/src/test/java/com/giftregistry/data/preferences/GuestPreferencesDataStoreTest.kt"
    - "app/src/test/java/com/giftregistry/ui/registry/detail/ReserveItemViewModelTest.kt"
  modified:
    - "functions/package.json (added @google-cloud/tasks)"
    - "functions/package-lock.json"
    - "functions/src/index.ts (added createReservation and releaseReservation exports)"
    - "tests/rules/firestore.rules.test.ts (added Phase 4 describe blocks)"

key-decisions:
  - "createReservation stub omits CloudTasksClient import (dependency present in package.json but import deferred to Plan 02 implementation to keep stub minimal)"
  - "releaseReservation stub throws Error (not HttpsError) since onTaskDispatched has no HttpsError equivalent"

patterns-established:
  - "Wave 0 Nyquist pattern: all stubs created first so verify commands in downstream plans always reference existing files"

requirements-completed: [RES-01, RES-02, RES-03, RES-05, RES-06, RES-07, RES-09]

# Metrics
duration: 2min
completed: 2026-04-11
---

# Phase 4 Plan 01: Wave 0 Nyquist Scaffolding Summary

**@google-cloud/tasks pre-wired, createReservation and releaseReservation stubs created, Phase 4 Firestore rule tests added, and three Kotlin stub tests in RED state for Waves 1-3**

## Performance

- **Duration:** 2 min
- **Started:** 2026-04-11T16:30:50Z
- **Completed:** 2026-04-11T16:32:50Z
- **Tasks:** 2
- **Files modified:** 9

## Accomplishments

- Installed `@google-cloud/tasks@6.2.1` in functions package so Plans 02-04 can import CloudTasksClient without mixing dependency installation with implementation
- Created `createReservation.ts` (onCall stub) and `releaseReservation.ts` (onTaskDispatched stub) — both export real functions that throw NOT_IMPLEMENTED; TypeScript build passes clean
- Extended `firestore.rules.test.ts` with two Phase 4 describe blocks; both tests pass immediately against existing rules (unauthenticated item read allowed, authenticated reservation write denied)
- Created three Kotlin test stubs in RED state — `ReserveItemUseCaseTest`, `GuestPreferencesDataStoreTest`, `ReserveItemViewModelTest` — all compile but throw `NotImplementedError` at runtime

## Task Commits

Each task was committed atomically:

1. **Task 1: Install @google-cloud/tasks and create Cloud Function stubs** - `dd8e693` (feat)
2. **Task 2: Add failing security rule tests + Kotlin unit test stubs** - `88f165e` (test)

**Plan metadata:** (see final commit below)

## Files Created/Modified

- `functions/package.json` - Added @google-cloud/tasks@6.2.1 dependency
- `functions/package-lock.json` - Lock file updated
- `functions/src/index.ts` - Added createReservation and releaseReservation exports
- `functions/src/reservation/createReservation.ts` - onCall stub throwing NOT_IMPLEMENTED
- `functions/src/reservation/releaseReservation.ts` - onTaskDispatched stub throwing NOT_IMPLEMENTED
- `tests/rules/firestore.rules.test.ts` - Added Phase 4 item status read and reservations hard-deny describe blocks
- `app/src/test/java/com/giftregistry/domain/usecase/ReserveItemUseCaseTest.kt` - RED stub
- `app/src/test/java/com/giftregistry/data/preferences/GuestPreferencesDataStoreTest.kt` - RED stub
- `app/src/test/java/com/giftregistry/ui/registry/detail/ReserveItemViewModelTest.kt` - RED stub

## Decisions Made

- createReservation stub omits CloudTasksClient import — dependency is present in package.json but the import is deferred to Plan 02 where it will be used, keeping the stub truly minimal
- releaseReservation throws `Error` (not `HttpsError`) since `onTaskDispatched` handlers do not use HttpsError

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- All Plan 02-04 verify command targets now exist on disk (Nyquist compliance)
- `@google-cloud/tasks` dependency is available — Plan 02 can immediately import `CloudTasksClient`
- Kotlin test stubs will turn GREEN after Plans 03-04 implement the corresponding classes
- Firestore rules tests confirm existing hard-deny on reservations collection is in place (RES-09 satisfied at rules level)

---
*Phase: 04-reservation-system*
*Completed: 2026-04-11*
