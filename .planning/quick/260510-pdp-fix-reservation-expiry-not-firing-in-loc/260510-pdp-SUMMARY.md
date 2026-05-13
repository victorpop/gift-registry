---
quick_id: 260510-pdp
description: Fix reservation expiry not firing in local Firebase emulator
date: 2026-05-10
status: complete
verification_status: Verified
human_verify: approved
human_verify_date: 2026-05-13
commits:
  - 8672900
  - 2df463f
  - 889a27f
tasks_planned: 3
tasks_completed: 3
---

# Quick Task 260510-pdp — Summary

## Problem

Reservations created against the local Firebase emulator never auto-expired. Items stayed `status: "reserved"` forever past the 30-minute mark because the Firebase Emulator Suite ships no Cloud Tasks emulator — `createReservation` caught the enqueue failure but had no fallback, so `releaseReservation` (an `onTaskDispatched` handler) was never invoked locally.

Production is unaffected: deployed Functions auto-provision a real Cloud Tasks queue, so enqueue succeeds and the 30-min release fires durably.

## What Shipped

| Commit | Task | Change |
|--------|------|--------|
| `8672900` | 1 — refactor + RED | Extract `releaseReservationCore({ reservationId, db })` from the `releaseReservation` `onTaskDispatched` body; wrapper signature unchanged. Add `createReservation.test.ts` with 3 tests proving fallback semantics (Test 1 RED until Task 2). |
| `2df463f` | 2 — GREEN | Wire emulator-only `setTimeout` fallback in `createReservation.ts` catch block. Gated on `process.env.FUNCTIONS_EMULATOR === "true"`. `timer.unref?.()` so the emulator process can exit cleanly. |
| `889a27f` | (out-of-scope) | Repair functions build entry point: `package.json` `main` from `lib/index.js` → `lib/src/index.js`. Required because `functions/tsconfig.json` produces `lib/src/*.js` not `lib/*.js`. Discovered while verifying the local emulator picked up the new build; unblocked the loader. Logged separately because it sits outside the original plan scope. |

## Key Design Properties

- **Production untouched.** No code path runs in production that does not exist today. `FUNCTIONS_EMULATOR` is only set by `firebase emulators:start`, never in deployed Functions.
- **Idempotent.** `releaseReservationCore` short-circuits on `status !== "active"` and on `now < expiresAtSeconds`. A late-firing timer after the giver confirms purchase is a safe no-op.
- **No new wiring.** Production deployment path (Cloud Tasks enqueue → `onTaskDispatched` → `releaseReservationCore`) is line-for-line equivalent to the pre-refactor handler body — the wrapper just delegates.

## Known Limitation (documented, not fixed)

If the Functions emulator restarts while a reservation is pending, the in-process `setTimeout` is lost. The reservation will stay `reserved` forever in that emulator session. Workaround: manually advance status in the Firestore emulator UI, or restart with a clean emulator data dir. Production is durable via Cloud Tasks.

## Verification

- `cd functions && npm test` → **78/78 tests pass** (11 suites). Includes the 3 new `createReservation.test.ts` cases (emulator fallback fires; production path does not; happy enqueue does not).
- `cd functions && npx tsc --noEmit` → clean.
- Manual emulator end-to-end walkthrough: user confirmed item flips back to `available` at the expiry mark with the `[createReservation] Emulator fallback: scheduling release …` log line firing per new reservation.

## Files Modified

- `functions/src/reservation/releaseReservation.ts` (refactor — exposed `releaseReservationCore`)
- `functions/src/reservation/createReservation.ts` (added emulator-only setTimeout fallback)
- `functions/src/__tests__/createReservation.test.ts` (new — 3 tests pinning fallback contract)
- `functions/package.json` (`main` field repair, out-of-scope but blocking)

## What This Unblocks

Local end-to-end testing of:
- Reservation expiry → item auto-release → reservation status flip to `expired`
- Owner-side reservation_expired notifications (Phase 6 trigger fires inside `releaseReservationCore`)
- Giver-side re-reserve email flow (sent inside `releaseReservationCore`)
- Phase 14 (web fallback live deploy + guest UAT) regression dry-runs against the emulator before hitting production

## Self-Check: PASSED
