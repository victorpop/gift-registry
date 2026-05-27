# Deferred items — discovered during Phase 17 execution

## 2026-05-27 — Pre-existing Functions test failures (Plan 17-02)

`functions/src/__tests__/createReservation.test.ts` — 3 tests in the
"emulator-only setTimeout fallback" describe block fail with
`The default Firebase app does not exist. Make sure you call initializeApp()
before using any of the Firebase services.`

These failures pre-date Plan 17-02. Origin commit:
`8672900 refactor(functions): extract releaseReservationCore + add RED test
for emulator fallback`. The test file appears to import code that touches the
Firebase Admin SDK at module load before `admin.initializeApp()` runs in the
test harness.

Out of scope for Phase 17 (Discover) — log only, do not fix.
Other 164 tests in functions/ pass.
