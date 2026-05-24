# Phase 16 — Deferred Items

Out-of-scope discoveries made while executing Phase 16 plans. These are NOT
fixed as part of Phase 16 because they are pre-existing failures in code
this phase does not own.

---

## Pre-existing failure: `functions/src/__tests__/createReservation.test.ts` — `getFunctions()` requires `initializeApp()`

**Discovered during:** Plan 16-01 (Wave 0 RED scaffolding) — full `npx jest`
suite run to verify scope of breakage.

**Symptom:** 3 of 3 tests in `createReservation.test.ts` fail with
`The default Firebase app does not exist. Make sure you call initializeApp()
before using any of the Firebase services.` thrown from
`src/reservation/createReservation.ts:85` where `getFunctions()` is invoked
to look up the `releaseReservation` task queue.

**Root cause:** The test file does not jest-mock `firebase-admin/functions`,
and the production code calls `admin.initializeApp()` only at module load
in `src/index.ts`, not in `src/reservation/createReservation.ts`. When the
test imports `createReservation` directly without going through `index.ts`,
no default app exists, so `getFunctions()` throws.

**Reproduction:** `cd functions && npx jest src/__tests__/createReservation.test.ts`
fails identically with **and without** any Plan 16-01 changes applied
(verified via `git stash` rerun).

**Why not fixed here:** Plan 16-01 only touches notification + invite test
files. Touching `createReservation.test.ts` would expand scope beyond the
Wave 0 RED-scaffolding plan and is unrelated to the invite accept/decline
flow being implemented.

**Recommended remediation:** Add `jest.mock("firebase-admin/functions", ...)`
to `createReservation.test.ts` (similar shape to the existing
`firebase-admin` mock), returning a stub `getFunctions().taskQueue` whose
`.enqueue()` resolves. This is a 10-line test-only patch with no production
risk.

**Owner:** Whichever future plan next touches the reservation flow, OR a
dedicated quick task. Not blocking for Phase 16.
