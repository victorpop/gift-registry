# Deferred Items — 260522-iqv

Out-of-scope discoveries during execution. Do NOT fix in this task.

## Pre-existing test failures (NOT caused by this task)

**File:** `web/src/features/__tests__/firebase.test.ts`

**Failing tests (3):**
- `firebase.ts > uses browserLocalPersistence (WEB-D-12)`
- `firebase.ts > does NOT connect emulators when VITE_USE_EMULATORS is not true`
- (one more in the same file)

**Cause:** `initializeAppCheck` call added to `src/firebase.ts` in Phase 14-04 (commit `78fed8d`) cannot run inside vitest because the Firebase App provider chain (`getProvider`) isn't initialized in the test environment.

**Verified pre-existing:** Confirmed by stashing this task's edits and re-running `npm test -- --run firebase.test` → 3 failures persist. These failures existed on `main` HEAD before this quick task started.

**Recommendation for a future task:**
- Mock `firebase/app-check` in `firebase.test.ts`, OR
- Wrap the `initializeAppCheck` block in a try/catch in `src/firebase.ts`, OR
- Guard it behind a test-mode env flag

**Out of scope for 260522-iqv:** This quick task only fixes the "Registry not available" flash via the two registry hooks. Touching `firebase.ts` or its tests is outside scope.
