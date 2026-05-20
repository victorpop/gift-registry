---
phase: quick/260510-pdp-fix-reservation-expiry-not-firing-in-loc
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - functions/src/reservation/releaseReservation.ts
  - functions/src/reservation/createReservation.ts
  - functions/src/__tests__/createReservation.test.ts
autonomous: false
requirements:
  - QUICK-260510-pdp
must_haves:
  truths:
    - "When the Functions emulator is running and Cloud Tasks enqueue fails, an in-process setTimeout fires releaseReservationCore at expiresAtMs and the item flips back to status: \"available\"."
    - "Production behavior is unchanged: when FUNCTIONS_EMULATOR is not set, no setTimeout is scheduled even if enqueue throws."
    - "If a giver confirms purchase before the timer fires, the late-firing setTimeout is a no-op (releaseReservationCore short-circuits on non-active status)."
    - "Existing releaseReservation.test.ts tests still pass after the refactor."
  artifacts:
    - path: "functions/src/reservation/releaseReservation.ts"
      provides: "Exports releaseReservationCore (plain async fn) AND releaseReservation (onTaskDispatched wrapper)"
      contains: "export async function releaseReservationCore"
    - path: "functions/src/reservation/createReservation.ts"
      provides: "Existing onCall handler + emulator-only setTimeout fallback after Cloud Tasks catch block"
      contains: "FUNCTIONS_EMULATOR"
    - path: "functions/src/__tests__/createReservation.test.ts"
      provides: "Jest tests for emulator fallback (fires) and production path (does not fire)"
  key_links:
    - from: "createReservation.ts catch block"
      to: "releaseReservationCore"
      via: "setTimeout(() => releaseReservationCore({...}), delayMs).unref?.()"
      pattern: "setTimeout.*releaseReservationCore"
    - from: "releaseReservation.ts onTaskDispatched wrapper"
      to: "releaseReservationCore"
      via: "direct function call inside the handler body"
      pattern: "await releaseReservationCore"
---

<objective>
Fix the local-emulator-only bug where reservation 30-minute expiry never fires because the Firebase emulator suite ships no Cloud Tasks emulator. The fix extracts releaseReservation's transaction body into a reusable releaseReservationCore() function, then schedules an in-process setTimeout fallback from createReservation when (a) the Cloud Tasks enqueue throws AND (b) FUNCTIONS_EMULATOR === "true". Production is untouched.

Purpose: Unblock local end-to-end testing of the reservation lifecycle. Currently any locally-created reservation is stuck in "reserved" forever, blocking dev work on giver flows, owner notifications, and re-reservation.

Output:
- Refactored releaseReservation.ts exporting both releaseReservationCore (plain fn) and releaseReservation (onTaskDispatched wrapper, unchanged signature).
- createReservation.ts with an emulator-only setTimeout fallback after the existing Cloud Tasks catch block.
- New createReservation.test.ts proving the fallback fires in emulator mode and does NOT fire in production mode.
- Human-verified end-to-end behavior on the user's local emulator.
</objective>

<execution_context>
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/workflows/execute-plan.md
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/STATE.md
@CLAUDE.md
@functions/src/reservation/createReservation.ts
@functions/src/reservation/releaseReservation.ts
@functions/src/__tests__/releaseReservation.test.ts

<diagnosis>
Bug already diagnosed by the orchestrator. DO NOT re-investigate.

- createReservation.ts:103-107 catches the Cloud Tasks enqueue error and only logs a warning. The Firebase emulator suite has no Cloud Tasks emulator, so enqueue throws or no-ops in local dev.
- releaseReservation handler (onTaskDispatched) is therefore never invoked locally. Item stays status: "reserved" past the 30-min mark. UI countdown is purely cosmetic.
- Production is unaffected because GCP auto-provisions the Cloud Tasks queue when onTaskDispatched Functions are deployed.

Approach: extract releaseReservation's runTransaction body into releaseReservationCore({ reservationId, db }) so the createReservation catch block can invoke it directly via setTimeout when FUNCTIONS_EMULATOR === "true".

Idempotency safety: releaseReservationCore short-circuits on status !== "active" (line 50 in current file) and on nowSeconds < expiresAtSeconds (line 57). Late-firing timers are safe no-ops.

Edge cases:
- Functions emulator restart mid-reservation: in-process timer is lost. Acceptable for dev — document in SUMMARY. Production is durable via Cloud Tasks.
- expiresAtMs already in the past: Math.max(0, expiresAtMs - Date.now()) clamps to 0, setTimeout fires next tick.
- Multiple createReservation calls: each schedules its own independent timer. No shared state.
- Existing releaseReservation.test.ts tests exercise releaseReservation.run(...) (the wrapper). Refactor must keep the wrapper signature/behavior so those tests stay green.

FUNCTIONS_EMULATOR is the canonical Firebase emulator-detection env var: set automatically to "true" by `firebase emulators:start`, never set in deployed Functions.
</diagnosis>

<interfaces>
Current releaseReservation.ts exports (will be preserved + extended):

```ts
// EXISTING (kept):
export const releaseReservation = onTaskDispatched<ReleasePayload>({...}, async (req) => { /* body */ });

// NEW (added by Task 1):
export interface ReleaseReservationCoreArgs {
  reservationId: string;
  db: admin.firestore.Firestore;
}
export async function releaseReservationCore(args: ReleaseReservationCoreArgs): Promise<void>;
```

The releaseReservation wrapper body becomes a thin shell:
```ts
export const releaseReservation = onTaskDispatched<ReleasePayload>({...}, async (req) => {
  const { reservationId } = req.data;
  if (!reservationId) {
    console.warn("[releaseReservation] missing reservationId; no-op");
    return;
  }
  await releaseReservationCore({ reservationId, db: admin.firestore() });
});
```

Existing createReservation.ts catch block (lines 103-107) — extended in Task 2:
```ts
} catch (err) {
  console.warn("[createReservation] Cloud Tasks enqueue failed (emulator?):", err);
  if (process.env.FUNCTIONS_EMULATOR === "true") {
    const delayMs = Math.max(0, expiresAtMs - Date.now());
    console.info(`[createReservation] Emulator fallback: scheduling release of ${reservationId} in ${delayMs}ms`);
    const timer = setTimeout(() => {
      releaseReservationCore({ reservationId, db: admin.firestore() })
        .catch((e) => console.error(
          `[createReservation] Emulator fallback release failed for ${reservationId}:`, e
        ));
    }, delayMs);
    timer.unref?.();
  }
}
```
</interfaces>

<test_framework>
- Project uses Jest 29 (functions/package.json: "test": "jest --passWithNoTests").
- Existing pattern: hand-rolled in-file mock for firebase-admin (see releaseReservation.test.ts lines 34-106). Follow this same pattern in the new createReservation.test.ts — DO NOT introduce a new mocking style.
- Jest fake timers: use `jest.useFakeTimers()` / `jest.advanceTimersByTime(ms)` / `jest.useRealTimers()`. The existing test does NOT use fake timers, so set them up locally in the new test file's relevant describe block.
- @google-cloud/tasks must be mocked at the top of the test file so `tasksClient.createTask` can be controlled (made to throw for the fallback test).
</test_framework>

</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Refactor releaseReservation.ts to extract releaseReservationCore + write RED test</name>
  <files>functions/src/reservation/releaseReservation.ts, functions/src/__tests__/createReservation.test.ts</files>
  <behavior>
    releaseReservation.ts:
      - Exports new function `releaseReservationCore({ reservationId, db })` containing the entire current onTaskDispatched body (transaction + email + notifications).
      - The exported `releaseReservation` (onTaskDispatched) handler still exists with the same signature and behavior, but its body is now just: validate reservationId, then `await releaseReservationCore({ reservationId, db: admin.firestore() })`.
      - All existing tests in releaseReservation.test.ts must still pass without modification.

    NEW createReservation.test.ts (RED — these tests will fail until Task 2 ships):
      - Test 1 (emulator mode): set `process.env.FUNCTIONS_EMULATOR = "true"`, mock @google-cloud/tasks so createTask rejects, mock firebase-admin (copy pattern from releaseReservation.test.ts), spy on setTimeout (or use jest.useFakeTimers + jest.advanceTimersByTime past expiresAtMs), call createReservation.run(...), assert that releaseReservationCore was invoked with the right reservationId. Easiest path: `jest.mock("../reservation/releaseReservation", ...)` to expose a spy on `releaseReservationCore`.
      - Test 2 (production mode): unset / delete process.env.FUNCTIONS_EMULATOR, mock createTask to reject, call createReservation.run(...), assert releaseReservationCore was NOT invoked even after timers advance.
      - Test 3 (happy production path): FUNCTIONS_EMULATOR unset, createTask resolves successfully, assert no fallback timer scheduled and no releaseReservationCore call.
  </behavior>
  <action>
    Step 1 — Refactor releaseReservation.ts:
    1. Read functions/src/reservation/releaseReservation.ts in full.
    2. Add an exported interface `ReleaseReservationCoreArgs { reservationId: string; db: admin.firestore.Firestore }` near the top.
    3. Define `export async function releaseReservationCore(args: ReleaseReservationCoreArgs): Promise<void>` containing the EXACT current body of the onTaskDispatched handler (the runTransaction + email send + writeNotification calls), but reading `reservationId` and `db` from args instead of from req.data and admin.firestore().
    4. Reduce the onTaskDispatched handler body to:
       ```ts
       const { reservationId } = req.data;
       if (!reservationId) { console.warn("[releaseReservation] missing reservationId; no-op"); return; }
       await releaseReservationCore({ reservationId, db: admin.firestore() });
       ```
    5. Run `npm test -- releaseReservation` from /Users/victorpop/ai-projects/gift-registry/functions to confirm the existing 4 tests (Test A/B/C/D) still pass. They MUST pass — if any fail, the refactor introduced regression.

    Step 2 — Create RED test file functions/src/__tests__/createReservation.test.ts:
    1. Copy the firebase-admin mock pattern from releaseReservation.test.ts (mockStore + makeDocRef + makeCollRef + fakeDb with runTransaction).
       - Seed mockStore.registries.reg1 = { ownerId: "owner1", title: "Test Registry" }
       - Seed mockStore["registries/reg1/items"].it1 = { status: "available", affiliateUrl: "https://emag.ro/x" }
    2. Mock @google-cloud/tasks at the top of the file BEFORE imports:
       ```ts
       const mockCreateTask = jest.fn();
       jest.mock("@google-cloud/tasks", () => ({
         CloudTasksClient: jest.fn().mockImplementation(() => ({
           queuePath: () => "projects/p/locations/l/queues/q",
           createTask: mockCreateTask,
         })),
       }));
       ```
    3. Mock writeNotification (from "../notifications/writeNotification") with `jest.fn().mockResolvedValue(undefined)` so the post-transaction notification write doesn't crash on the fake registry.
    4. Mock the releaseReservation module to expose a spy on releaseReservationCore:
       ```ts
       const mockReleaseReservationCore = jest.fn().mockResolvedValue(undefined);
       jest.mock("../reservation/releaseReservation", () => ({
         releaseReservationCore: mockReleaseReservationCore,
         releaseReservation: { run: jest.fn() }, // unused in these tests
       }));
       ```
    5. Import createReservation AFTER mocks: `import { createReservation } from "../reservation/createReservation";`
    6. Helper: `function makeReq() { return { data: { registryId: "reg1", itemId: "it1", giverName: "G", giverEmail: "g@x.com", giverId: null } }; }`
    7. beforeEach: resetStore(); mockCreateTask.mockReset(); mockReleaseReservationCore.mockReset(); mockReleaseReservationCore.mockResolvedValue(undefined); delete process.env.FUNCTIONS_EMULATOR;
    8. afterEach: jest.useRealTimers(); delete process.env.FUNCTIONS_EMULATOR;

    Tests to write:
    - Test 1: "schedules setTimeout fallback when FUNCTIONS_EMULATOR=true and enqueue fails"
      - process.env.FUNCTIONS_EMULATOR = "true"
      - mockCreateTask.mockRejectedValue(new Error("no Cloud Tasks emulator"))
      - jest.useFakeTimers()
      - await createReservation.run(makeReq())
      - jest.advanceTimersByTime(31 * 60 * 1000) // past 30-min expiry
      - await Promise.resolve(); // let setTimeout callback run
      - expect(mockReleaseReservationCore).toHaveBeenCalledWith(expect.objectContaining({ reservationId: expect.any(String) }))
    - Test 2: "does NOT schedule fallback when FUNCTIONS_EMULATOR is unset (production path) even if enqueue fails"
      - delete process.env.FUNCTIONS_EMULATOR
      - mockCreateTask.mockRejectedValue(new Error("real prod failure"))
      - jest.useFakeTimers()
      - await createReservation.run(makeReq())
      - jest.advanceTimersByTime(31 * 60 * 1000)
      - await Promise.resolve()
      - expect(mockReleaseReservationCore).not.toHaveBeenCalled()
    - Test 3: "does NOT schedule fallback when enqueue succeeds (happy production path)"
      - process.env.FUNCTIONS_EMULATOR = "true" // even in emulator mode, success skips fallback
      - mockCreateTask.mockResolvedValue([{ name: "projects/p/locations/l/queues/q/tasks/t1" }])
      - jest.useFakeTimers()
      - await createReservation.run(makeReq())
      - jest.advanceTimersByTime(31 * 60 * 1000)
      - await Promise.resolve()
      - expect(mockReleaseReservationCore).not.toHaveBeenCalled()

    Step 3 — Confirm RED:
    - Run `npm test -- createReservation.test` from /Users/victorpop/ai-projects/gift-registry/functions.
    - Test 1 MUST FAIL (current code has no fallback). Tests 2 and 3 should already pass (no fallback exists, so neither is wrongly invoked).
    - This is correct RED state. Do NOT proceed to Task 2 if Test 1 unexpectedly passes — that means the spec is wrong.

    Commit message: `refactor(functions): extract releaseReservationCore + add RED test for emulator fallback`
  </action>
  <verify>
    <automated>cd /Users/victorpop/ai-projects/gift-registry/functions && npm test -- releaseReservation createReservation 2>&1 | tail -40</automated>
    Expected: releaseReservation.test.ts — all 4 tests pass (refactor preserved behavior). createReservation.test.ts — Test 1 FAILS (no fallback yet); Tests 2 and 3 PASS.
  </verify>
  <done>
    - releaseReservationCore is exported from releaseReservation.ts.
    - releaseReservation onTaskDispatched wrapper still exists with same signature and delegates to releaseReservationCore.
    - All existing releaseReservation.test.ts tests pass.
    - createReservation.test.ts file exists with 3 tests; Test 1 is failing (RED), Tests 2/3 pass.
    - Refactor + RED test committed.
  </done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Wire emulator-only setTimeout fallback in createReservation.ts (GREEN)</name>
  <files>functions/src/reservation/createReservation.ts</files>
  <behavior>
    After the existing `console.warn("[createReservation] Cloud Tasks enqueue failed (emulator?):", err)` line, add an emulator-mode branch that schedules a setTimeout to invoke releaseReservationCore at expiresAtMs. After this task, all 3 tests in createReservation.test.ts pass (GREEN), and all 4 tests in releaseReservation.test.ts still pass (no regression).
  </behavior>
  <action>
    1. Read functions/src/reservation/createReservation.ts (full file).
    2. Add an import at the top: `import { releaseReservationCore } from "./releaseReservation";`
    3. In the catch block at lines 103-107, AFTER the existing console.warn, append:
       ```ts
       // Emulator-only fallback: setTimeout to invoke release directly.
       // Production never hits this path because Cloud Tasks enqueue succeeds when deployed.
       // FUNCTIONS_EMULATOR is set automatically by `firebase emulators:start`, never in prod.
       if (process.env.FUNCTIONS_EMULATOR === "true") {
         const delayMs = Math.max(0, expiresAtMs - Date.now());
         console.info(
           `[createReservation] Emulator fallback: scheduling release of ${reservationId} in ${delayMs}ms`
         );
         const timer = setTimeout(() => {
           releaseReservationCore({ reservationId, db: admin.firestore() })
             .catch((e) => console.error(
               `[createReservation] Emulator fallback release failed for ${reservationId}:`, e
             ));
         }, delayMs);
         // .unref() lets the Functions emulator process exit cleanly without waiting for pending timers.
         timer.unref?.();
       }
       ```
    4. Do NOT touch anything else in the file. The Cloud Tasks enqueue, the post-commit reservation update, and the notification write all stay exactly as-is.
    5. Run `npm test -- releaseReservation createReservation` from /Users/victorpop/ai-projects/gift-registry/functions.
       - Expected: ALL tests pass (releaseReservation 4/4 + createReservation 3/3 = 7/7).
       - If Test 1 in createReservation still fails: check the spy is wired correctly (mockReleaseReservationCore was reset, fake timers advanced past delayMs, and FUNCTIONS_EMULATOR was set BEFORE createReservation.run was awaited).
    6. Run `npm run build` (or `npx tsc --noEmit`) from /Users/victorpop/ai-projects/gift-registry/functions to confirm no TypeScript errors. The releaseReservationCore import must type-check.

    Commit message: `feat(functions): emulator-only setTimeout fallback for reservation expiry`
  </action>
  <verify>
    <automated>cd /Users/victorpop/ai-projects/gift-registry/functions && npm test -- releaseReservation createReservation 2>&1 | tail -30 && npx tsc --noEmit 2>&1 | tail -10</automated>
    Expected: 7/7 tests pass. tsc reports no errors.
  </verify>
  <done>
    - createReservation.ts has the emulator-only fallback branch immediately after the Cloud Tasks catch warning.
    - All 7 tests pass (4 releaseReservation + 3 createReservation).
    - TypeScript compiles cleanly.
    - GREEN commit pushed.
  </done>
</task>

<task type="checkpoint:human-verify" gate="blocking">
  <name>Task 3: User verifies fix on local emulator</name>
  <files>(no files changed by this task — verification only)</files>
  <what-built>
    - releaseReservation.ts refactored to expose releaseReservationCore (production wrapper unchanged).
    - createReservation.ts schedules an in-process setTimeout when FUNCTIONS_EMULATOR=true and Cloud Tasks enqueue throws.
    - Jest tests prove the fallback fires in emulator mode and is dormant in production.
    - Production code path (deployed Functions hitting real Cloud Tasks) is untouched — when enqueue succeeds, no fallback runs.
  </what-built>
  <action>
    Pause here and ask the user to manually verify the fix on their local Firebase emulator. Do not proceed past this checkpoint without a "approved" (or equivalent) response. The verification steps below are for the user to execute, not Claude.

    Manual emulator end-to-end verification (user runs this):

    1. Stop any running Firebase emulator: `firebase emulators:stop` (or kill the process).
    2. Start the emulator suite fresh: `firebase emulators:start` from the project root.
    3. Confirm the Functions emulator log shows `FUNCTIONS_EMULATOR=true` is set in the environment (it is by default — no manual config needed).
    4. Reproduce the original bug scenario: from the web fallback OR Android client, create a new reservation against an item in a registry served by the local emulator.
    5. In the Functions emulator log, confirm both lines appear:
       - `[createReservation] Cloud Tasks enqueue failed (emulator?): ...`
       - `[createReservation] Emulator fallback: scheduling release of <reservationId> in 1800000ms` (~30 minutes)
    6. To skip waiting 30 minutes, choose one option:
       a) Wait the full 30 minutes and watch for the release log + verify in the Firestore emulator UI that the item flipped back to status: "available" and the reservation is status: "expired".
       b) For a faster sanity check: temporarily lower RESERVATION_DURATION_MS in createReservation.ts to 30_000 (30 seconds), restart the emulator, repeat steps 4-5, watch for release within ~30s. REVERT the change before committing/merging.
    7. Verify in the Firestore emulator UI (http://localhost:4000/firestore):
       - The item document under registries/{registryId}/items/{itemId} has `status: "available"` and `reservedBy`/`reservedAt`/`expiresAt` fields removed.
       - The reservation document under reservations/{reservationId} has `status: "expired"`.
    8. Verify the Android / web giver UI: when the user re-opens the registry after expiry, the item shows as available again (no longer locked).
    9. Optional regression check: confirm that confirming a purchase BEFORE the timer fires still works — the late-firing setTimeout should be a no-op (releaseReservationCore short-circuits on status !== "active"). Check Functions logs for the no-op short-circuit log line when the timer eventually fires.

    KNOWN LIMITATION (document this, don't fix): if the Functions emulator is restarted while a reservation is pending, the in-process setTimeout is lost. The reservation will stay "reserved" forever in that emulator session. Workaround: manually advance status in the Firestore emulator UI, or restart with a clean emulator data dir. Production is unaffected because Cloud Tasks is durable.
  </action>
  <verify>
    <automated>echo "MANUAL — user verifies on local emulator per <action> steps; no automated check"</automated>
    Manual: user observes in Functions emulator log and Firestore emulator UI that the item flipped back to status: "available" at the 30-min mark (or 30s with the test-only RESERVATION_DURATION_MS reduction), and the reservation is status: "expired".
  </verify>
  <done>
    - User reports "approved" (or equivalent confirmation) after observing the item auto-release on their local emulator.
    - Functions emulator log shows the `[createReservation] Emulator fallback: scheduling release ...` line for new reservations.
    - Firestore emulator UI confirms item status flipped to "available" and reservation status flipped to "expired" at expiry.
  </done>
  <resume-signal>Type "approved" if the item flips back to available at the 30-min mark (or 30s with the test-only RESERVATION_DURATION_MS reduction). Otherwise describe what you observed in the Functions emulator log and the Firestore emulator UI.</resume-signal>
</task>

</tasks>

<verification>
- `cd functions && npm test` — all suites pass (the existing releaseReservation tests + new createReservation tests).
- `cd functions && npx tsc --noEmit` — clean.
- Manual emulator verification per Task 3.
- Production code path untouched: diff of createReservation.ts shows ONLY additions inside the existing catch block (no changes to the transaction, the enqueue call, or the post-commit notification write). Diff of releaseReservation.ts is a pure refactor (function extraction) — the onTaskDispatched export name and signature are unchanged.
</verification>

<success_criteria>
- A reservation created in the local Firebase emulator transitions from `status: "reserved"` to `status: "available"` automatically at the 30-minute mark, with the matching reservation document marked `status: "expired"`.
- All existing tests still pass.
- New createReservation tests prove fallback is emulator-gated and tied to enqueue failure.
- No changes to production code paths (Cloud Tasks enqueue, onTaskDispatched handler signature, deployed function names).
</success_criteria>

<output>
After completion, create `.planning/quick/260510-pdp-fix-reservation-expiry-not-firing-in-loc/260510-pdp-SUMMARY.md` documenting:
- The refactor (releaseReservationCore extraction)
- The emulator-only fallback wiring
- The known limitation (Functions emulator restart loses pending timers)
- Confirmation that production is unaffected
</output>
