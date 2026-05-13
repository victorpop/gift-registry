---
phase: quick-260513-j8a
plan: 01
subsystem: web/reservation
tags: [web, reservation, bugfix, context, tdd]
dependency-graph:
  requires:
    - web/src/features/reservation/useActiveReservation.ts (existing hook)
    - web/src/features/reservation/useReleaseReservation.ts (existing hook)
  provides:
    - ItemReservePage that clears shared useActiveReservation context on release success
  affects:
    - RegistryPage StickyReserveBanner rendering after release-from-detail-page
tech-stack:
  added: []
  patterns:
    - "Mirror StickyReserveBanner/ConfirmPurchaseBanner release-success pattern: ref-guard + clear() + toast + navigate"
key-files:
  created: []
  modified:
    - web/src/pages/ItemReservePage.tsx
    - web/src/pages/__tests__/ItemReservePage.test.tsx
decisions:
  - "Unconditional clear() on release success (accepted tradeoff documented in plan): if another concurrent reservation is in the shared context it will be wiped; useActiveReservationHydration re-resolves on RegistryPage remount."
  - "Rolled RED+GREEN into a single atomic commit per quick-task constraint, instead of separate TDD commits."
metrics:
  duration: 58s
  completed: 2026-05-13T10:53:48Z
  tasks_completed: 1
  tasks_total: 1
  files_changed: 2
  loc_added: 25
  loc_removed: 3
requirements:
  - "Q-260513-j8a-01: Release initiated from ItemReservePage clears useActiveReservation context"
---

# Quick Task 260513-j8a: Fix release-from-ItemReservePage not clearing useActiveReservation context — Summary

Surgical two-file fix that wires `useActiveReservation().clear()` into ItemReservePage's release-success `useEffect`, eliminating the phantom `StickyReserveBanner` that continued to render on `/registry/:id` after a viewer released from `/registry/:id/item/:itemId`.

## What Was Built

- **Source change** (`web/src/pages/ItemReservePage.tsx`):
  - Added `import { useActiveReservation } from '../features/reservation/useActiveReservation'`.
  - Destructured `clear` (aliased to `clearActiveReservation`) from the hook inside the component body, next to the other hook calls.
  - Inserted `clearActiveReservation()` in the release-success `useEffect`, between the toast and the `navigate(...)` call.
  - Added `clearActiveReservation` to the effect's dependency array.
  - Updated the file-level JSDoc to document that release-from-this-page now clears the shared context.
  - Preserved the existing `releaseSuccessHandledRef` one-shot guard so toast + clear + navigate fire exactly once per release-success transition.

- **Test change** (`web/src/pages/__tests__/ItemReservePage.test.tsx`):
  - Added `activeMock.clear = vi.fn()` to `beforeEach` to isolate the new spec.
  - Added new spec **P-06b** asserting:
    - `activeMock.clear` is called exactly once on release success.
    - `<div data-testid="registry-page">` appears (i.e. `navigate('/registry/:id')` ran).
    - `showToast` is called once with `'success'` severity.

## Pattern Parity

The new call mirrors the existing release-success pattern in:

- `web/src/features/reservation/StickyReserveBanner.tsx:65` — `clear()` inside `releaseStatus === 'success'` effect.
- `web/src/features/reservation/ConfirmPurchaseBanner.tsx:42` — `clear()` inside `status === 'success'` effect.

Same ref-guard structure, same dependency-array layout.

## Verification Results

| Check | Command | Result |
| --- | --- | --- |
| TypeScript | `cd web && npx tsc --noEmit` | Clean (no output, exit 0) |
| Targeted suite | `npx vitest run src/pages/__tests__/ItemReservePage.test.tsx` | 13/13 pass (P-01, P-01b, P-02, P-03, P-04, P-05, P-06, P-06b, P-07, P-08, P-09, P-10, P-11) |
| Full web suite | `npx vitest run` | 148/148 pass across 27 test files |
| RED → GREEN | Test run before fix | New P-06b failed with `expected "spy" to be called 1 times, but got 0 times` — confirming the missing call was the only blocker. After applying the source change, P-06b passes. |

## Success Criteria

- [x] After release from `/registry/:id/item/:itemId`, `useActiveReservation.active` is `null` in the shared context.
- [x] Returning to `/registry/:id` no longer shows StickyReserveBanner for the released reservation (verified by P-06b: `clear()` called once + navigate fires + no banner since `active` is null).
- [x] Toast and navigation still fire exactly once on release success (ref-guard preserved; P-06 still passes).
- [x] All existing ItemReservePage tests continue to pass (P-01 through P-11 — 12 specs, all green).
- [x] TypeScript clean.

## Deviations from Plan

**None — plan executed exactly as written**, with two minor procedural notes:

1. **TDD commit shape:** Plan task is marked `tdd="true"`, but the executor constraints in the prompt specified a single atomic commit (`fix(quick-260513-j8a-01): ...`). Rolled the RED test + GREEN implementation into one commit instead of two. The TDD discipline was still followed end-to-end: test was authored first and confirmed failing (`expected "spy" to be called 1 times, but got 0 times`) before the source change was written.
2. **Toast assertion shape:** The plan offered two assertion options for the toast — `expect.stringMatching(/release/i)` (i18n-resolved) or the structural fallback (`mock.calls[0][1]).toBe('success')`). Used the structural fallback for stability against future i18n string edits. This satisfies the plan's documented option B verbatim.

## Authentication Gates

None encountered.

## Known Stubs

None introduced.

## Commits

| Commit | Type | Description | Files |
| --- | --- | --- | --- |
| `586f85c` | fix | clear useActiveReservation on release from ItemReservePage | `web/src/pages/ItemReservePage.tsx`, `web/src/pages/__tests__/ItemReservePage.test.tsx` |

## Self-Check: PASSED

- Files confirmed modified:
  - `web/src/pages/ItemReservePage.tsx` — present, contains `useActiveReservation` import and `clearActiveReservation()` call.
  - `web/src/pages/__tests__/ItemReservePage.test.tsx` — present, contains `P-06b` spec and `activeMock.clear = vi.fn()` reset in `beforeEach`.
- Commit confirmed: `git log --oneline | grep 586f85c` → found.
- Verification command from plan executed successfully: `cd web && npx tsc --noEmit && npx vitest run --reporter=basic` → both pass.
