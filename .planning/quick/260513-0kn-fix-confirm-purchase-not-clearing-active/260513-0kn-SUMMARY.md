---
phase: quick-260513-0kn
plan: 01
subsystem: web/reservation
tags: [bug-fix, reservation, react-context, tdd]
dependency_graph:
  requires: []
  provides: [confirm-purchase-clears-active-reservation]
  affects: [StickyReserveBanner, ReserveDetailSection]
tech_stack:
  added: []
  patterns: [one-shot-ref-guard, context-clear-on-success]
key_files:
  created: []
  modified:
    - web/src/features/reservation/ConfirmPurchaseBanner.tsx
    - web/src/features/reservation/__tests__/ConfirmPurchaseBanner.test.tsx
decisions:
  - "Fix lives in ConfirmPurchaseBanner (context consumer), not in useConfirmPurchase (status machine hook) — mirrors StickyReserveBanner release-success pattern"
  - "successToastedRef one-shot guard prevents double-clear on re-renders; no new guard needed"
metrics:
  duration: ~10m
  completed: 2026-05-13
  tasks_completed: 1
  tasks_total: 2
  files_modified: 2
---

# Phase quick-260513-0kn Plan 01: Fix Confirm-Purchase Not Clearing Active Reservation Summary

**One-liner:** Added `clear()` call inside `ConfirmPurchaseBanner`'s existing success effect so that a successful `confirmPurchase` callable immediately tears down the sticky banner and in-page reserve-detail section without a page refresh.

## What Was Built

### The bug

Clicking "I completed the purchase" correctly called the Firebase `confirmPurchase` callable (setting `reservation.status = 'purchased'` and `item.status = 'purchased'` server-side), and the item card flipped to PURCHASED via Firestore snapshot. However, the `StickyReserveBanner` and `ReserveDetailSection` remained visible because both gate on `useActiveReservation().active` in the React context, and `active` was never cleared client-side after a successful purchase confirmation.

### The fix

One-line addition in the existing success `useEffect` in `ConfirmPurchaseBanner.tsx`:

```tsx
// BEFORE
useEffect(() => {
  if (status === 'success' && !successToastedRef.current) {
    successToastedRef.current = true
    showToast(t('reservation.confirm_purchase_success'), 'success')
    // ← MISSING: clear()
  }
}, [status, showToast, t])

// AFTER
useEffect(() => {
  if (status === 'success' && !successToastedRef.current) {
    successToastedRef.current = true
    showToast(t('reservation.confirm_purchase_success'), 'success')
    clear()
  }
}, [status, showToast, t, clear])
```

Plus the corresponding import and destructure:
```tsx
import { useActiveReservation } from './useActiveReservation'
// ...
const { clear } = useActiveReservation()
```

### Why it lives in the banner, not in the hook

- `useConfirmPurchase` is a pure status/error state machine that can be composed from any surface — it should not know about the global reservation context.
- The correct consumer of `useActiveReservation` on success is the same component that is already consuming `useConfirmPurchase` — `ConfirmPurchaseBanner` — exactly mirroring how `StickyReserveBanner` calls `clear()` in its release-success effect.
- The `successToastedRef` one-shot guard already ensures the block runs only once even if the effect fires multiple times due to re-renders; no additional guard is needed for `clear()`.

### Test added (TDD)

New test in `ConfirmPurchaseBanner.test.tsx`:

```
"clears active reservation context on success"
  - Mocks useActiveReservation at module scope (clearMock = vi.fn())
  - Confirms clearMock is NOT called before click (count = 0)
  - Clicks the button, awaits waitFor(() => clearMock.toHaveBeenCalledTimes(1))
```

The module-level `vi.mock("../useActiveReservation", …)` also satisfies the new `useActiveReservation()` call in all 5 pre-existing tests, which continue to pass without any modification.

**Test results:** 6/6 tests pass in `ConfirmPurchaseBanner.test.tsx`; 116/116 tests pass across the full web suite; `tsc --noEmit` exits 0.

## Commits

| Task | Commit | Description |
|------|--------|-------------|
| 1 (fix + test) | 87464b8 | fix(quick-260513-0kn-01): clear active reservation on confirm-purchase success |

## Deviations from Plan

None — plan executed exactly as written.

## Human-Verify Outcome

**Status: PENDING** — Task 2 (checkpoint:human-verify) awaiting in-browser verification.

Verification steps: start dev server + emulators, reserve an item, click "I completed the purchase", confirm that StickyReserveBanner + ReserveDetailSection both disappear within ~1 second without a page refresh, and no React warnings appear in DevTools console.

## Known Stubs

None.

## Self-Check: PASSED

- `web/src/features/reservation/ConfirmPurchaseBanner.tsx` — modified, exists
- `web/src/features/reservation/__tests__/ConfirmPurchaseBanner.test.tsx` — modified, exists
- commit `87464b8` — confirmed in git log
- `npm run test:run` — 116/116 green
- `tsc --noEmit` — exits 0
- `git diff --name-only` — exactly 2 files
