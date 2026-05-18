---
phase: quick-260518-ke1
plan: 01
subsystem: web/reservation-hydration
tags: [web, reservation, hydration, race-guard, ignoreItemId, j5j-followup]
requires: [quick-260518-j5j]
provides:
  - "useActiveReservationHydration options now accept ignoreItemId alongside ignoreReservationId (logical OR suppression)."
  - "RegistryPage threads both recentReleasedReservationId AND recentReleasedItemId from location.state to the hydration hook."
affects:
  - web/src/features/reservation/useActiveReservationHydration.ts
  - web/src/pages/RegistryPage.tsx
  - web/src/features/reservation/__tests__/useActiveReservationHydration.test.ts
  - web/src/features/registry/__tests__/RegistryPage.test.tsx
tech_stack_added: []
patterns:
  - "Item-scoped + reservation-scoped post-release suppression (logical OR) for stale-active rows."
key_files_created: []
key_files_modified:
  - web/src/features/reservation/useActiveReservationHydration.ts
  - web/src/pages/RegistryPage.tsx
  - web/src/features/reservation/__tests__/useActiveReservationHydration.test.ts
  - web/src/features/registry/__tests__/RegistryPage.test.tsx
decisions:
  - "Suppression logic is a logical OR over (reservationId, itemId) — defensive default, covers both transient composite-index lag (j5j) and stale-active rows with a different reservationId on the same item (ke1)."
  - "JSDoc explicitly documents the ke1 scenario (Cloud Tasks failures / emulator setTimeout drift per pdp/iux) so the rationale survives future refactors."
  - "RegistryPage call site reuses the existing recentReleasedItemId binding (already in scope from j5j for itemsForRender override) — no second location.state read introduced."
metrics:
  duration: "2 minutes"
  tasks_completed: 2
  files_modified: 4
  commits: 2
  test_total_before: 202
  test_total_after: 206
  tests_added: 4
  completed: 2026-05-18
---

# Quick Plan 260518-ke1: Harden release race guard with ignoreItemId — Summary

One-liner: Added `ignoreItemId` option to `useActiveReservationHydration` (logical OR with j5j's `ignoreReservationId`) so RegistryPage suppresses ANY backend-returned active reservation on the just-released item — covers stale-active rows from a different reservationId that j5j's exact-reservationId guard could not catch.

## Outcome

- 2 source files modified (hook + page consumer); 2 test files extended.
- 4 new tests appended (H-NEW-03/04/05 + R-NEW-04).
- Final web test count: **206 passed, 0 failed** (was 202 pre-ke1).
- `tsc --noEmit` clean.
- Exactly 4 files in `git diff --name-only HEAD~2 HEAD` — no out-of-scope drift.

## Commits

| # | Hash      | Type | Message |
|---|-----------|------|---------|
| 1 | `3b874be` | test | `test(quick-260518-ke1-01): add failing tests H-NEW-03/04/05 + R-NEW-04 for ignoreItemId hardening` |
| 2 | `8fba738` | fix  | `fix(quick-260518-ke1-01): suppress stale-active reservation for same item via ignoreItemId guard` |

## What changed

### `web/src/features/reservation/useActiveReservationHydration.ts`
- Hook signature options extended: `{ ignoreReservationId?: string; ignoreItemId?: string }`.
- Suppression check inside `.then(r => …)` now evaluates BOTH options as a logical OR:
  ```ts
  if (
    options?.ignoreReservationId === r.data.active.reservationId ||
    options?.ignoreItemId === r.data.active.itemId
  ) {
    setStatus("empty")
    return
  }
  ```
- Effect dep array now includes `options?.ignoreItemId` alongside `options?.ignoreReservationId`.
- JSDoc gained a paragraph explaining the ke1 scenario (stale-active row with different reservationId from emulator restarts / Cloud Tasks failures) and clarifying OR semantics.
- Safety invariant restated: `options?.ignoreItemId` is `undefined` only when the caller omits it, and `r.data.active.itemId` is always a non-empty string — so `undefined === undefined` cannot accidentally suppress.

### `web/src/pages/RegistryPage.tsx`
- Hydration call site (around line 58) now passes BOTH:
  ```ts
  useActiveReservationHydration(id, {
    ignoreReservationId: recentReleasedReservationId,
    ignoreItemId: recentReleasedItemId,
  })
  ```
- Reused the existing `recentReleasedItemId` binding from line 50 (already in scope for j5j's `itemsForRender` override) — no duplicate `location.state` read.
- Inline comment block above the call updated to mention the ke1 hardening and explicitly call out the OR semantics.

### `web/src/features/reservation/__tests__/useActiveReservationHydration.test.ts`
- `describe` label updated to `'useActiveReservationHydration (j5j ignoreReservationId + ke1 ignoreItemId)'`.
- Three new tests appended, reusing the same `RESERVATION` fixture and `beforeEach` reset:
  - **H-NEW-03**: ignoreItemId matches → `status === 'empty'`, `set()` not called.
  - **H-NEW-04**: ignoreItemId mismatches → `status === 'hydrated'`, `set()` called with RESERVATION.
  - **H-NEW-05**: both options set, ignoreReservationId mismatches but ignoreItemId matches → suppressed (proves OR, not AND).
- Header docstring updated to list the new spec IDs.

### `web/src/features/registry/__tests__/RegistryPage.test.tsx`
- **R-NEW-04**: when `location.state` carries both `recentReleasedReservationId` and `recentReleasedItemId`, RegistryPage threads them as `{ ignoreReservationId, ignoreItemId }` to the hydration hook. Asserts the full options-object shape (both keys) to lock in the contract.

## RED → GREEN trace

After Task 1 commit, the targeted test run showed exactly the expected failure set:

| Test         | Pre-Task-2 result      | Post-Task-2 result |
|--------------|------------------------|--------------------|
| H-NEW-01     | ✓ (unchanged from j5j) | ✓                  |
| H-NEW-02     | ✓ (unchanged from j5j) | ✓                  |
| H-NEW-03     | ✗ (`'hydrated'` instead of `'empty'`) | ✓ |
| H-NEW-04     | ✓ (option was unread, so set was called as expected) | ✓ |
| H-NEW-05     | ✗ (`'hydrated'` instead of `'empty'`) | ✓ |
| R-NEW-01/02/03 | ✓ (unchanged from j5j) | ✓ |
| R-NEW-04     | ✗ (received `{ ignoreReservationId: 'res-abc' }` only) | ✓ |

Total suite: 202 → 206 passing, 0 regressions.

## Verification

- `npm --prefix web run typecheck` → exit 0, no errors.
- `npm --prefix web test -- --run` → `Test Files 28 passed (28)`, `Tests 206 passed (206)`.
- `git diff --name-only HEAD~2 HEAD` returns exactly these 4 paths:
  - `web/src/features/registry/__tests__/RegistryPage.test.tsx`
  - `web/src/features/reservation/__tests__/useActiveReservationHydration.test.ts`
  - `web/src/features/reservation/useActiveReservationHydration.ts`
  - `web/src/pages/RegistryPage.tsx`
- D-06 (reserver-name suppression) untouched — no reserver-name code paths modified.
- No backend, i18n, Android, new files, new components, or new hooks introduced.

## Deviations from Plan

None. The plan was executed exactly as written. Task 1 produced an instructive RED with one notable nuance: **H-NEW-04 was green even before the production-code change in Task 2.** This is expected — without the production-code change, `options.ignoreItemId` is silently dropped (option-bag tolerance), so for the mismatch case the hook still calls `set()` and reaches `'hydrated'`, which is what H-NEW-04 asserts. The mismatch test became a regression guard for Task 2 (ensuring the added OR check doesn't accidentally suppress valid reservations) rather than a RED indicator. The three other new tests (H-NEW-03, H-NEW-05, R-NEW-04) gave the precise expected RED signals.

## User retest guidance

The user originally reported in the j5j retest that the bug persists because `hydrateActiveReservation` is returning an active reservation with a DIFFERENT reservationId for the same item (stale-active row, likely from emulator restarts that lost the setTimeout auto-expiry per quick-260516-iux / quick-260510-pdp). Please retest the full release flow:

1. **Original j5j repro path** — Reserve an item from RegistryPage → land on ItemReservePage → click Release → confirm RegistryPage shows neither the sticky reserve banner nor the ReserveDetailSection, and the released item card renders as available. This case was already green after j5j but is preserved here.

2. **ke1 stale-active scenario** — With a registry that has an accumulated stale-active reservation row on the same item (different reservationId from any currently-tracked reservation), trigger a release-from-ItemReservePage and land on RegistryPage. Expect: banner and ReserveDetailSection do NOT appear, even though the backend's `hydrateActiveReservation` callable still returns an `active` payload for that item. The hook now treats it as null because `options.ignoreItemId` matches `r.data.active.itemId`.

3. **Negative control** — On a freshly-loaded RegistryPage (no preceding release-from-ItemReservePage, no `location.state`), expect hydration to behave exactly as before: any backend-returned active reservation populates the context and surfaces the banner. The new option is no-op when omitted.

The user can clean their database in parallel; this hardening also benefits production users with similar accumulated state (Cloud Tasks failures, manual DB drift, etc.).

## Self-Check: PASSED

- Files exist:
  - `web/src/features/reservation/useActiveReservationHydration.ts` — FOUND
  - `web/src/pages/RegistryPage.tsx` — FOUND
  - `web/src/features/reservation/__tests__/useActiveReservationHydration.test.ts` — FOUND
  - `web/src/features/registry/__tests__/RegistryPage.test.tsx` — FOUND
- Commits exist:
  - `3b874be` (test) — FOUND
  - `8fba738` (fix) — FOUND
- Test totals: 206 passed, 0 failed.
- Tsc clean.
