---
phase: quick-260513-hon
plan: 01
subsystem: ui
tags: [react, react-router, useEffect, useRef, vitest, rtl]

requires: []
provides:
  - "Transition-detector for item.status flip out of 'reserved' in ItemReservePage"
  - "prevStatusRef guard preventing stale-on-mount navigation bounce-back"
affects:
  - web-reserve-flow
  - ItemReservePage

tech-stack:
  added: []
  patterns:
    - "Two-ref pattern: prevStatusRef tracks last-observed status; itemStatusNavigatedRef is the one-shot guard"
    - "ItemReservePageWithForceUpdate owner wrapper enables re-render injection in RTL tests without unmounting"

key-files:
  created: []
  modified:
    - web/src/pages/ItemReservePage.tsx
    - web/src/pages/__tests__/ItemReservePage.test.tsx

key-decisions:
  - "prevStatusRef must be updated BEFORE early returns so the ref is always current, even on the initial render when prev=undefined"
  - "ForceUpdateWrapper children-prop approach fails due to React.memo(DataRoutes) in RouterProvider — replaced with ItemReservePageWithForceUpdate that OWNS ItemReservePage in its render output"

patterns-established:
  - "Two-ref transition-detector: prev = ref.current; ref.current = current; guard on prev === expected-from-state"

requirements-completed:
  - HON-01
  - HON-02
  - HON-03

duration: 25min
completed: 2026-05-13
---

# quick-260513-hon Plan 01: ItemReservePage Navigate-Back Fix Summary

**Two-ref transition-detector (prevStatusRef + itemStatusNavigatedRef) prevents stale-on-mount 'available' snapshot from bouncing user off the reserve-detail page**

## Performance

- **Duration:** ~25 min
- **Started:** 2026-05-13T09:30:00Z
- **Completed:** 2026-05-13T09:55:50Z
- **Tasks:** 1 of 2 (Task 2 is a human-verify checkpoint — not self-completable)
- **Files modified:** 2

## Accomplishments

- Fixed the navigate-back-on-status-flip effect that fired on initial render when the Firestore snapshot was stale (still 'available') right after a successful reservation
- Introduced `prevStatusRef` so the effect only fires when it observes a REAL transition from 'reserved' to 'available' or 'purchased' — not on a stale initial snapshot
- Added 3 new specs (P-08, P-09, P-10) covering the stale-mount case and both real transition directions
- Full suite green: 141 tests pass; `npx tsc --noEmit` exits 0

## Task Commits

1. **Task 1: Add failing transition-detector specs, then fix the navigate-back effect** - `06f20be` (fix)

## Files Created/Modified

- `web/src/pages/ItemReservePage.tsx` — Added `import type { ItemStatus }`, added `prevStatusRef`, rewrote the navigate-back `useEffect` body with the two-ref transition-detector
- `web/src/pages/__tests__/ItemReservePage.test.tsx` — Refactored `renderPage` helper (adds `ItemReservePageWithForceUpdate` owner wrapper + `rerenderSame()` handle), added P-08/P-09/P-10 specs

## Decisions Made

- **prevStatusRef updated before early returns:** The ref update `prevStatusRef.current = currentItemStatus` runs as the very first line of the effect body, BEFORE the `if (!active) return` guard. This ensures the ref is always current even when the effect exits early (e.g., no active reservation on first render).
- **ItemReservePageWithForceUpdate owner wrapper instead of children-prop wrapper:** The initial `ForceUpdateWrapper({ children })` approach failed because `MemoizedDataRoutes = React.memo(DataRoutes)` inside React Router's `RouterProvider` bails out on re-renders, and React does not re-render components received as the `children` prop when only the wrapper re-renders (the "children as props" bail-out). Switching to a wrapper that renders `<ItemReservePage />` directly in its own function body means `ItemReservePage` is owned by the wrapper — React's standard parent-re-renders-child rule applies, and `ItemReservePage` re-renders when the wrapper's state increments.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] ForceUpdateWrapper children-prop pattern doesn't cascade re-renders through React.memo(DataRoutes)**

- **Found during:** Task 1 (RED→GREEN, test wiring)
- **Issue:** The plan's suggested `rerenderSame: () => result.rerender(tree)` approach and the `ForceUpdateWrapper({ children })` wrapper both failed to re-render `ItemReservePage` after mock updates. Root cause: React Router 7's `RouterProvider` uses `MemoizedDataRoutes = React.memo(DataRoutes)` to bail out on state-identical re-renders; additionally React does not re-render children-as-props when only the parent's own state changes (the owner of `<Inner />` JSX didn't re-render).
- **Fix:** Replaced `ForceUpdateWrapper({ children })` with `ItemReservePageWithForceUpdate()` — a wrapper that renders `<ItemReservePage />` directly in its own JSX output rather than accepting it as a `children` prop. This makes `ItemReservePage` an owned child of the wrapper, so wrapper state updates cascade normally.
- **Files modified:** `web/src/pages/__tests__/ItemReservePage.test.tsx`
- **Verification:** All 3 new specs (P-08, P-09, P-10) pass; all 11 ItemReservePage specs pass
- **Committed in:** `06f20be` (Task 1 commit)

---

**Total deviations:** 1 auto-fixed (Rule 1 — test infrastructure bug)
**Impact on plan:** Necessary to make P-09/P-10 work as intended. Source file change is unaffected — deviation is test-helper only.

## The Two-Ref Pattern Explained

```
prevStatusRef    — tracks the LAST OBSERVED status value (updated every effect run, even on early returns)
itemStatusNavigatedRef — one-shot guard: once navigate fires, never fires again for this mount
```

The effect body:
1. Capture `prev = prevStatusRef.current`
2. Update `prevStatusRef.current = currentItemStatus` (always, before any guard)
3. Guard: `!active` → return
4. Guard: `itemStatusNavigatedRef.current` → return (already fired)
5. Guard: `prev !== 'reserved'` → return (haven't observed 'reserved' yet — stale snapshot)
6. Guard: `currentItemStatus` not in `['purchased', 'available']` → return
7. Fire: set one-shot guard + `navigate('/registry/:id')`

On INITIAL render with stale snapshot (`status: 'available'`): step 2 records `'available'`; step 5 exits (`prev = undefined ≠ 'reserved'`). No navigate.

On SUBSEQUENT render after real reservation completes (`status: 'reserved'`): step 2 records `'reserved'`; step 5 exits again (`prev = undefined ≠ 'reserved'` on first run, `prev = 'available' ≠ 'reserved'` if stale was first). Actually — if the initial snapshot was stale `'available'` and Firestore then delivers `'reserved'`, the effect records `'available'` on first run (prev=undefined), then records `'reserved'` on second run (prev='available'). Neither fires navigate. Good.

On release/confirm-purchase (`status → 'available'` or `'purchased'` after `'reserved'`): `prev = 'reserved'` (recorded from prior render), `currentItemStatus = 'available'/'purchased'` → navigate fires once.

## Three New Specs

- **P-08 (HON-01):** Stale 'available' on mount with active reservation present — `item-reserve-detail` stays rendered, no redirect. Proves the fix's core invariant.
- **P-09 (HON-02):** 'reserved' → 'available' real transition (mock flipped mid-test, re-render forced via `ItemReservePageWithForceUpdate`) — `registry-page` appears, `item-reserve-detail` gone.
- **P-10 (HON-03):** 'reserved' → 'purchased' real transition — same as P-09 but via the confirm-purchase path.

## Issues Encountered

React Router 7 internal `React.memo(DataRoutes)` and the "children-as-props bail-out" behavior required investigation before the right test helper pattern was found. The fix took 4 iterations to identify the root cause (memo bail-out vs children ownership). No source code changes were needed — only the test helper.

## Human Verification Pending

Task 2 is a `checkpoint:human-verify` — the following four scenarios must be tested against a running dev server before this task can be closed:

- **(a) Guest auto-reserve:** Reserve as guest → land on `/registry/:id/item/:itemId` with full detail UI (no bounce-back to `/registry/:id`)
- **(b) Signed-in, second item:** Reserve item A, go back, reserve item B → URL changes to `/registry/:id/item/:itemB` (not stuck on item A's URL)
- **(c) Confirm-purchase regression:** Clicking "I completed the purchase" still navigates back to `/registry/:id`
- **(d) Release regression:** Clicking "Release reservation" still navigates back to `/registry/:id`

Start the dev server: `cd web && npm run dev`

## Known Stubs

None — the fix is behavioural only. `ItemReservePage` renders live Firestore-backed data; no stubs introduced.

## Next Phase Readiness

- Source fix is minimal: 1 import + 1 ref + 1 rewritten effect body
- Human verification clears once the four scenarios above pass
- No deferred items relating to this fix
- Potential future follow-up: extract the transition-detector into a reusable `useStatusTransitionNavigate` hook if a second use-case emerges (none currently)

---
*Phase: quick-260513-hon*
*Completed: 2026-05-13*
