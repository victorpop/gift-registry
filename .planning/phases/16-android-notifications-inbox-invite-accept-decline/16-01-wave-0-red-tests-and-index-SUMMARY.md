---
phase: 16-android-notifications-inbox-invite-accept-decline
plan: 01
subsystem: testing
tags: [tdd, junit, jest, firebase-rules-unit-testing, mockk, turbine, firestore-indexes]

# Dependency graph
requires:
  - phase: 06-notifications-email-flows
    provides: NotificationType enum + NotificationRepository + writeNotification helper + inviteToRegistry callable that this plan extends with new wire strings and pendingInvitedUsers contract
  - phase: 15-web-invite-landing-magic-link-guest-flow
    provides: Conceptual basis for email-keyed pending invite entries (email:<addr> key prefix)
provides:
  - 5 Android RED unit tests scaffolding the invite-lifecycle inbox contract (D-07, D-11, D-25, D-27, D-28)
  - 2 backend Jest RED test files for acceptInvite + declineInvite callables (D-21, D-22, D-24)
  - Modified inviteToRegistry tests asserting pendingInvitedUsers + enriched payload (D-15, D-16, D-23)
  - 4 new firestore.rules tests in a pendingInvitedUsers describe block — all PASS today (D-18, D-19)
  - Composite Firestore index notifications(type asc, payload.registryId asc) required for inbox cleanup query (Pitfall 7)
affects:
  - 16-02 (backend callables + invite-pending) — flips the 3 backend RED suites GREEN
  - 16-03 (android domain + data layer) — flips NotificationType + NotificationRepositoryImpl RED tests GREEN
  - 16-04 (invite response sheet + viewmodel) — flips NotificationCardBranchingTest + InviteResponseViewModelTest RED tests GREEN
  - 16-05 (inbox reskin + strings) — flips LocalizationParityTest if it adds new strings without RO

# Tech tracking
tech-stack:
  added: []  # No new libraries — Turbine, MockK, jest, @firebase/rules-unit-testing already present from earlier phases
  patterns:
    - "RED-first TDD scaffolding pattern (Nyquist Wave 0): every test file written BEFORE the production symbol exists; compile/module-not-found IS the RED state"
    - "Mock-firestore FieldPath segments routing: in-memory store update() handler routes by fp.segments[0] so tests can distinguish writes to pendingInvitedUsers vs invitedUsers maps"
    - "LocalizationParityTest pattern for guarding values/ ↔ values-ro/ strings.xml key parity using pure regex over file contents (no Android resource compiler dependency)"
    - "Rules tests for new write-only field (pendingInvitedUsers) — Pattern 8: rules need NO change because isInvited() reads invitedUsers only; new tests pass against existing rules and lock in the no-access contract"

key-files:
  created:
    - "app/src/test/java/com/giftregistry/domain/model/NotificationTypeFromWireTest.kt"
    - "app/src/test/java/com/giftregistry/ui/notifications/NotificationCardBranchingTest.kt"
    - "app/src/test/java/com/giftregistry/ui/notifications/InviteResponseViewModelTest.kt"
    - "app/src/test/java/com/giftregistry/data/notifications/NotificationRepositoryImplAcceptDeclineTest.kt"
    - "app/src/test/java/com/giftregistry/LocalizationParityTest.kt"
    - "functions/src/__tests__/acceptInvite.test.ts"
    - "functions/src/__tests__/declineInvite.test.ts"
    - ".planning/phases/16-android-notifications-inbox-invite-accept-decline/deferred-items.md"
  modified:
    - "functions/src/__tests__/inviteToRegistry.test.ts (pendingInvitedUsers + enriched payload + new Test H for D-16 re-invite-of-member)"
    - "tests/rules/firestore.rules.test.ts (appended pendingInvitedUsers read-scope describe block with 4 tests)"
    - "firestore.indexes.json (added users/*/notifications composite index)"

key-decisions:
  - "Mirrored the mock-firestore harness shape of confirmPurchase.test.ts (not inviteToRegistry.test.ts) for accept/declineInvite tests because confirmPurchase is the cleanest minimal template and the accept/decline flow doesn't need the FCM/email mock surface."
  - "Chose failed-precondition for declineInvite idempotency-when-uid-not-anywhere — symmetric with acceptInvite. Plan 16-02 may switch to no-op success if the UX demands; only the test assertion needs to flip."
  - "Used a chainable .where() no-op in the mock-firestore makeCollRef so the inbox-cleanup query (.where(type)==invite + .where(payload.registryId)==registryId) doesn't crash in tests — the .get() returns the full inbox collection unfiltered, which is fine for assertion since tests seed only the doc they care about."
  - "Left existing inviteToRegistry Tests C/D/E/F/G untouched — only Tests A and B (which assert the actual mutation target) needed to flip from invitedUsers to pendingInvitedUsers. Pre-existing tests still verify FCM, stale-token cleanup, email failure paths."
  - "Did NOT modify firestore.rules — Pattern 8 holds: isInvited() reads invitedUsers only, so the new pendingInvitedUsers field is invisible to the rule and grants no access by default. The new D-18 tests lock this in as a behavioral contract."

patterns-established:
  - "Wave 0 RED scaffolding: each downstream plan's read_first MUST reference the test file path it flips GREEN — creates explicit traceability between failing-test commits and feature commits."
  - "FieldPath-aware mock-firestore update handler: route by fp.segments[0] when ANY new field uses a map-key write pattern (registries/*/pendingInvitedUsers, registries/*/invitedUsers, registries/*/anotherMap)."
  - "LocalizationParityTest pattern: enumerate <string name='...'> with regex from both res/values/strings.xml + res/values-ro/strings.xml; fail with diff of missing keys in each direction. Cheap and locale-compiler-independent."

requirements-completed:
  - D-07
  - D-11
  - D-18
  - D-19
  - D-21
  - D-22
  - D-24
  - D-25
  - D-27
  - D-28

# Metrics
duration: 9min
completed: 2026-05-24
---

# Phase 16 Plan 01: Wave 0 RED Tests and Composite Index Summary

**Wave 0 RED-test scaffolding for the Android notifications inbox + invite accept/decline flow — 5 Android unit tests, 2 Functions Jest tests, modified inviteToRegistry/rules tests, and the inbox-cleanup composite Firestore index — all committed in a RED-by-design state that downstream plans 16-02/03/04 will flip GREEN.**

## Performance

- **Duration:** ~9 min
- **Started:** 2026-05-24T17:17:08Z
- **Completed:** 2026-05-24T17:25:51Z
- **Tasks:** 2
- **Files modified/created:** 10 (8 new + 2 modified + 1 deferred-items log + composite index json update)

## Accomplishments

- Locked in the wire-string contract for the 3 new invite-lifecycle notification types (invite_accepted_self / invite_accepted / invite_declined) via `NotificationTypeFromWireTest`, plus 5 regression cases guarding the existing mappings.
- Locked in the sheet-vs-navigate branching predicate `shouldOpenInviteSheet` via `NotificationCardBranchingTest` (8 tests covering INVITE+pendingEntryKey, legacy INVITE without key, and 6 non-INVITE types).
- Locked in the `InviteResponseViewModel` state-machine contract (Idle → Submitting → AcceptedSuccess / DeclinedSuccess / Error(action) with retry()) via 5 Turbine tests.
- Locked in the `NotificationRepositoryImpl.acceptInvite/declineInvite` httpsCallable contract — name "acceptInvite"/"declineInvite", payload `mapOf("registryId" to ...)`, runCatching wrap — via 4 MockK tests.
- Added `LocalizationParityTest` guarding values/ ↔ values-ro/ strings.xml key parity for all future plans (267 keys parity verified manually today).
- Added `acceptInvite.test.ts` and `declineInvite.test.ts` covering happy-path transaction + 5 error cases each (auth, missing arg, not-found, no-pending, idempotency).
- Modified `inviteToRegistry.test.ts` Tests A + B to expect writes to `pendingInvitedUsers` (not `invitedUsers`) per D-23, plus payload enrichment (`pendingEntryKey`, `occasion`, `coverUrl`, `eventDateMs`) per D-15, plus a new Test H for the D-16 re-invite-of-existing-member behavior.
- Added 4 firestore.rules tests in a `pendingInvitedUsers read scope (D-18)` describe block — all PASS today against the existing rules (Pattern 8 holds: the new field is invisible to `isInvited()`).
- Added `notifications(type asc, payload.registryId asc)` composite Firestore index to `firestore.indexes.json` so the acceptInvite/declineInvite inbox-cleanup query won't fail at runtime in production (Pitfall 7).

## Task Commits

Each task was committed atomically:

1. **Task 1: Android RED unit tests + LocalizationParityTest** — `3246505` (test)
2. **Task 2: Functions RED tests + extended rules + composite index** — `c160c14` (test)

**Plan metadata:** _(pending — final commit after STATE.md / ROADMAP.md updates)_

## Files Created/Modified

### Created (8)
- `app/src/test/java/com/giftregistry/domain/model/NotificationTypeFromWireTest.kt` — fromWire mapping for 3 new wire strings + UNKNOWN/null fallback + regression for existing 5 mappings
- `app/src/test/java/com/giftregistry/ui/notifications/NotificationCardBranchingTest.kt` — shouldOpenInviteSheet predicate test (8 cases)
- `app/src/test/java/com/giftregistry/ui/notifications/InviteResponseViewModelTest.kt` — Turbine state-machine test (5 cases including retry)
- `app/src/test/java/com/giftregistry/data/notifications/NotificationRepositoryImplAcceptDeclineTest.kt` — MockK httpsCallable wrapper test (4 cases)
- `app/src/test/java/com/giftregistry/LocalizationParityTest.kt` — strings.xml key parity guard
- `functions/src/__tests__/acceptInvite.test.ts` — 6 transaction + error tests
- `functions/src/__tests__/declineInvite.test.ts` — 5 transaction + error tests
- `.planning/phases/16-android-notifications-inbox-invite-accept-decline/deferred-items.md` — pre-existing createReservation.test.ts failure logged out-of-scope

### Modified (3)
- `functions/src/__tests__/inviteToRegistry.test.ts` — fixtures + Tests A/B expect pendingInvitedUsers + enriched payload; new Test H for D-16; FieldPath update handler routes by segments[0]
- `tests/rules/firestore.rules.test.ts` — appended `pendingInvitedUsers read scope (D-18)` describe block with 4 tests (3 D-18 + 1 D-19)
- `firestore.indexes.json` — added `notifications(type asc, payload.registryId asc)` composite index

## Decisions Made

- **Mock-firestore harness mirrored confirmPurchase.test.ts** — smaller, cleaner template than inviteToRegistry.test.ts for the accept/decline flow which doesn't need the FCM/email mock surface.
- **declineInvite idempotency throws failed-precondition** — symmetric with acceptInvite. Plan 16-02 may flip to no-op success if UX demands; only the test assertion needs to change.
- **Did NOT modify firestore.rules** — Pattern 8 verified: existing `isInvited()` reads `invitedUsers` only; the new `pendingInvitedUsers` field is invisible to the rule and grants no access by default. The 4 new tests lock this contract in.
- **LocalizationParityTest uses pure-regex over file contents** — no Android resource compiler dependency, runs as a standard JVM unit test. Tradeoff: doesn't catch malformed XML, but a malformed strings.xml would also break the production build, so this is acceptable.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Added inbox-doc fixture + `where()` chainable shim to acceptInvite test harness**
- **Found during:** Task 2 (writing acceptInvite.test.ts)
- **Issue:** Plan example A shows `deleteInviteNotificationsForRegistry` calling `.where("type","==","invite").where("payload.registryId","==",registryId).get()` followed by a batch delete. The mock-firestore in confirmPurchase.test.ts doesn't implement `.where()` — running the happy-path test would crash on `coll.where is not a function`.
- **Fix:** Added a chainable `.where(_field,_op,_value) => coll` no-op to `makeCollRef` in BOTH acceptInvite.test.ts and declineInvite.test.ts. `.get()` returns the full collection unfiltered, which is fine for assertions since each test seeds only the inbox doc it cares about. Also seeded `users/invitee-1/notifications` with `inv-doc-1` (a fake INVITE doc) in `resetStore()` so the happy-path assertion `expect(inviteeInbox["inv-doc-1"]).toBeUndefined()` is meaningful.
- **Files modified:** `functions/src/__tests__/acceptInvite.test.ts`, `functions/src/__tests__/declineInvite.test.ts`
- **Verification:** Tests compile-fail only on the expected `Cannot find module '../registry/acceptInvite'` / `declineInvite` (no harness errors). When Plan 16-02 ships the production code, the tests will exercise the cleanup path correctly.
- **Committed in:** `c160c14`

**2. [Rule 3 - Blocking] FieldPath update handler in inviteToRegistry mock-firestore had to route by `fp.segments[0]`**
- **Found during:** Task 2 (modifying inviteToRegistry.test.ts to assert pendingInvitedUsers writes)
- **Issue:** The existing handler unconditionally wrote to the `invitedUsers` field regardless of the FieldPath's first segment. After modifying tests to assert `pendingInvitedUsers["invited-uid"] === true`, the handler would still mutate `invitedUsers` and the new assertion would fail FOR THE WRONG REASON (handler bug, not production-code bug).
- **Fix:** Generalized handler to route by `fp.segments[0]` — now `new FieldPath("pendingInvitedUsers", uid)` mutates the `pendingInvitedUsers` map and `new FieldPath("invitedUsers", uid)` mutates the `invitedUsers` map. Either pattern works correctly.
- **Files modified:** `functions/src/__tests__/inviteToRegistry.test.ts`
- **Verification:** Tests A + B now fail with the CORRECT RED reason (`pendingInvitedUsers["invited-uid"]` is undefined — production code still writes to `invitedUsers`). Plan 16-02 flipping the production handler will make them GREEN.
- **Committed in:** `c160c14`

---

**Total deviations:** 2 auto-fixed (both Rule 3 - blocking test-harness issues)
**Impact on plan:** Both fixes were test-harness preconditions, not production-code changes. No scope creep — the tests still assert exactly the contracts the plan specified. Plan 16-02's GREEN flip is unaffected.

## Issues Encountered

- **Pre-existing failure in `functions/src/__tests__/createReservation.test.ts`** discovered when running the full `npx jest` suite to scope breakage. 3 of 3 tests fail with `The default Firebase app does not exist` thrown from `getFunctions()`. Verified via `git stash` that this failure existed BEFORE Plan 16-01 changes. Logged out-of-scope to `.planning/phases/16-android-notifications-inbox-invite-accept-decline/deferred-items.md` per executor scope-boundary rule. Not blocking for Phase 16.

## RED State Verification

Per Wave 0 / Nyquist contract, all 4 production-coupled Android tests + 2 backend test files MUST fail in the expected way today:

**Android (`./gradlew :app:compileDebugUnitTestKotlin`):**
```
e: ... NotificationTypeFromWireTest.kt: Unresolved reference 'INVITE_ACCEPTED_SELF'
e: ... NotificationTypeFromWireTest.kt: Unresolved reference 'INVITE_ACCEPTED'
e: ... NotificationTypeFromWireTest.kt: Unresolved reference 'INVITE_DECLINED'
e: ... NotificationCardBranchingTest.kt: Unresolved reference 'shouldOpenInviteSheet'
e: ... InviteResponseViewModelTest.kt: Unresolved reference 'InviteResponseViewModel'
e: ... NotificationRepositoryImplAcceptDeclineTest.kt: Too many arguments for 'constructor(firestore: FirebaseFirestore): NotificationRepositoryImpl'
e: ... NotificationRepositoryImplAcceptDeclineTest.kt: Unresolved reference 'acceptInvite'
e: ... NotificationRepositoryImplAcceptDeclineTest.kt: Unresolved reference 'declineInvite'
```

**Functions (`cd functions && npx tsc --noEmit`):**
```
src/__tests__/acceptInvite.test.ts(198,30): error TS2307: Cannot find module '../registry/acceptInvite'
src/__tests__/declineInvite.test.ts(183,31): error TS2307: Cannot find module '../registry/declineInvite'
```
Exactly 2 errors — no other regressions.

**Rules (`cd tests/rules && npm test`):** 40/40 PASS including the 4 new D-18/D-19 tests. Pattern 8 confirmed.

**inviteToRegistry suite (`cd functions && npx jest src/__tests__/inviteToRegistry.test.ts`):** Tests A + B fail with `expect(reg.pendingInvitedUsers["..."]) === true; Received: undefined` — the production handler still writes to `invitedUsers`. This is the expected RED state; Plan 16-02 flips it GREEN.

**localization parity:** Verified manually via `grep -o '<string name="..."'` — 267 keys in EN, 267 in RO, diff empty. LocalizationParityTest will run as soon as the Android test source set compiles (after Plan 16-03/04 ship the missing production symbols).

## Self-Check: PASSED

Files verified to exist:
- `app/src/test/java/com/giftregistry/LocalizationParityTest.kt`
- `app/src/test/java/com/giftregistry/data/notifications/NotificationRepositoryImplAcceptDeclineTest.kt`
- `app/src/test/java/com/giftregistry/domain/model/NotificationTypeFromWireTest.kt`
- `app/src/test/java/com/giftregistry/ui/notifications/InviteResponseViewModelTest.kt`
- `app/src/test/java/com/giftregistry/ui/notifications/NotificationCardBranchingTest.kt`
- `functions/src/__tests__/acceptInvite.test.ts`
- `functions/src/__tests__/declineInvite.test.ts`
- `.planning/phases/16-android-notifications-inbox-invite-accept-decline/deferred-items.md`

Commits verified to exist:
- `3246505` (Task 1)
- `c160c14` (Task 2)

## User Setup Required

None — no external service configuration required.

## Next Phase Readiness

- **Plan 16-02 (backend callables + invite-pending) ready to start.** It needs to:
  - Create `functions/src/registry/acceptInvite.ts` and `functions/src/registry/declineInvite.ts` to flip `acceptInvite.test.ts` and `declineInvite.test.ts` GREEN.
  - Modify `functions/src/registry/inviteToRegistry.ts` to write to `pendingInvitedUsers` (not `invitedUsers`) using `new FieldPath("pendingInvitedUsers", inviteKey)` and to enrich the inbox payload with `pendingEntryKey`, `occasion`, `coverUrl`, `eventDateMs`. Also add the D-16 short-circuit: skip pending write when uid is already in `invitedUsers`, but still send FCM + write inbox doc.
  - Export the two new callables from `functions/src/index.ts`.
  - Deploy the new composite Firestore index alongside the deploy plan (16-06) so production queries don't fail with FAILED_PRECONDITION at runtime.

- **No blockers.** Rules tests already pass; production-code work is the only remaining piece for Plan 16-02's GREEN flip.

---
*Phase: 16-android-notifications-inbox-invite-accept-decline*
*Completed: 2026-05-24*
