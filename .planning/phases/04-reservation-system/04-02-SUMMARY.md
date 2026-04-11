---
phase: 04-reservation-system
plan: 02
subsystem: api
tags: [cloud-functions, firestore, cloud-tasks, reservation, typescript]

# Dependency graph
requires:
  - phase: 04-01
    provides: Wave 0 stubs for createReservation and releaseReservation, @google-cloud/tasks in package.json

provides:
  - createReservation onCall: Firestore runTransaction (atomicity), CloudTasksClient.createTask enqueued post-transaction, cloudTaskName stored on reservation doc
  - releaseReservation onTaskDispatched: guard transaction (idempotent), item revert to available, stub expiry email with re-reserve deep link
  - firestore.rules Phase 4 comment documenting Admin SDK write-only restriction

affects: [04-03, 04-04, phase-05-purchase-confirmation, phase-06-email]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Cloud Task enqueued OUTSIDE runTransaction to prevent duplicate tasks on contention retry"
    - "CloudTasksClient.createTask (not Admin SDK taskQueue.enqueue) to expose cloudTaskName resource path"
    - "Guard transaction in releaseReservation: check status==active && now>expiresAt before any writes (idempotent)"
    - "Stub email via console.log [STUB] prefix — wired to real email in Phase 6"

key-files:
  created: []
  modified:
    - functions/src/reservation/createReservation.ts
    - functions/src/reservation/releaseReservation.ts
    - firestore.rules

key-decisions:
  - "createReservation wraps task enqueue in try/catch so emulator build succeeds even without Cloud Tasks running — releaseReservation is testable via direct HTTP POST to emulator endpoint"
  - "cloudTaskName stored as empty string in transaction, updated via separate post-transaction .update() — avoids side-effect inside transaction body"

patterns-established:
  - "Post-transaction side-effect pattern: runTransaction resolves, THEN enqueue, THEN update doc with task name"
  - "Guard transaction pattern for idempotent task handlers: read doc, check preconditions, all writes in single tx"

requirements-completed: [RES-01, RES-03, RES-05, RES-06, RES-07, RES-08, RES-09]

# Metrics
duration: 1min
completed: 2026-04-11
---

# Phase 4 Plan 02: Reservation System Summary

**Server-authoritative Firestore runTransaction + CloudTasksClient expiry pipeline: createReservation atomically reserves items and schedules 30-min Cloud Task; releaseReservation guard-transaction reverts item on expiry with stub email**

## Performance

- **Duration:** 1 min
- **Started:** 2026-04-11T16:34:14Z
- **Completed:** 2026-04-11T16:35:39Z
- **Tasks:** 2
- **Files modified:** 3

## Accomplishments
- `createReservation` onCall: Firestore `runTransaction` reads item, aborts on status != available (ITEM_UNAVAILABLE), atomically sets item to reserved and creates reservation doc; `CloudTasksClient.createTask` enqueued OUTSIDE transaction body (Pitfall 2 compliance), `cloudTaskName` stored on doc for future Phase 5 cancellation
- `releaseReservation` onTaskDispatched: guard transaction checks `status == active && now > expiresAt` before any writes, reverts item fields via `FieldValue.delete()`, sets reservation to expired; idempotent (multiple Cloud Tasks firing safely converge)
- `firestore.rules` comment documents Phase 4 RES-09/D-19 Admin SDK restriction; hard-deny `allow read, write: if false` preserved
- Emulator smoke test: POST to `/releaseReservation` with nonexistent reservationId returns 204 (no-op path verified)

## Task Commits

Each task was committed atomically:

1. **Task 1: Implement createReservation onCall (transaction + Cloud Tasks enqueue)** - `36919aa` (feat)
2. **Task 2: Implement releaseReservation onTaskDispatched + emulator smoke test** - `f2247a1` (feat)

**Plan metadata:** (docs commit — see below)

## Files Created/Modified
- `functions/src/reservation/createReservation.ts` - Full implementation replacing Wave 0 stub; runTransaction + CloudTasksClient.createTask
- `functions/src/reservation/releaseReservation.ts` - Full implementation replacing Wave 0 stub; onTaskDispatched guard transaction + stub email
- `firestore.rules` - Phase 4 comment added above reservations match block

## Decisions Made
- `createReservation` wraps `CloudTasksClient.createTask` in try/catch: emulator doesn't have Cloud Tasks, so the catch logs a warning and continues — `cloudTaskName` stored as empty string in that case. This keeps the emulator development loop working while production correctly stores the task name.
- `cloudTaskName` initialized as `""` inside the transaction, then written via a separate `.update()` after `createTask` resolves. This is the correct pattern per Pitfall 2 research — avoids any side effects inside the transaction callback.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

- Emulator port 8080 was already occupied by a running Firestore emulator instance. Used the already-running emulator (project `gift-registry-ro`) for smoke test — curl to port 5001 returned 204 confirming no-op path works.

## User Setup Required

None - no external service configuration required. Cloud Tasks queue `release-reservation` in `europe-west3` must be created before production deployment (pre-existing infrastructure requirement from Phase 1 planning).

## Next Phase Readiness
- Both Cloud Functions compile clean (`npm run build` exits 0)
- Phase 4 Firestore rules tests pass (2/2)
- `createReservation` → `releaseReservation` flow is complete and testable via direct POST to emulator
- Ready for Plan 03: Android reservation UI (ReservationViewModel, ReservationScreen)
- Phase 5 (purchase confirmation) can use `cloudTaskName` stored on reservation docs to cancel pending expiry tasks

---
*Phase: 04-reservation-system*
*Completed: 2026-04-11*
