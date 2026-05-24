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

---

## Pre-existing failure: `AuthViewModelTest` — 2/12 Turbine "Expected no events but found Item(Unauthenticated)"

**Discovered during:** Plan 16-04 (Task 3 full-suite verification).

**Symptom:** 2 of 12 tests in
`app/src/test/java/com/giftregistry/ui/auth/AuthViewModelTest.kt` fail with:
```
app.cash.turbine.TurbineAssertionError: Expected no events but found Item(Unauthenticated)
```
The failing tests are around cold-start auth-state observation where Turbine
expected to see no further emissions after settling but received an extra
`Unauthenticated` item.

**Root cause:** Likely a Turbine timing / `runTest` dispatcher interaction
with `MutableStateFlow` re-emission in `AuthViewModel`'s `authUserFlow`
mapping. Not related to anything Plan 16-04 touched (the Plan 16-04 changes
are confined to `ui/notifications/*`, `theme/*` reads, and resource string
files; `AuthViewModel.kt` is untouched).

**Reproduction:** `./gradlew :app:testDebugUnitTest --tests
"com.giftregistry.ui.auth.AuthViewModelTest"` fails identically **with and
without** any Plan 16-04 changes applied — verified via `git stash` at HEAD
`7e0d1a9` (Task 2 commit) BEFORE Task 3 production edits: same 2/12 failures
with the same Turbine message.

**Why not fixed here:** Plan 16-04 scope is
`ui/notifications/InviteResponseViewModel.kt`, `InviteResponseSheet.kt`,
`NotificationsViewModel.kt`, `NotificationsScreen.kt`, and string resources.
`AuthViewModel.kt` and `AuthViewModelTest.kt` are unrelated subsystems;
fixing the Turbine timing issue would expand scope beyond the invite-flow UI
and is unrelated to Phase 16's goal.

**Recommended remediation:** Investigate whether the test needs a
`testScheduler.advanceUntilIdle()` call before `expectNoEvents()`, or whether
the `MutableStateFlow` initial value should be wrapped in a
`distinctUntilChanged` to suppress re-emission. 5-10 line fix in the test
file, no production code changes needed.

**Owner:** Whichever future plan next touches AuthViewModel or auth-state
observation, OR a dedicated quick task. Not blocking for Phase 16.
