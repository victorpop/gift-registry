---
phase: quick-260513-g9g
plan: 01
subsystem: web-reservation, functions
tags: [routing, reservation, callable, per-item, navigation]
dependency_graph:
  requires: [quick-260513-fk1, quick-260512-x5d]
  provides: [per-item-reserve-detail-route, getReservationForItem-callable, useReservationForItem-hook]
  affects: [RegistryPage, ReserveButton, ItemCard, App.tsx]
tech_stack:
  added: []
  patterns:
    - "Route-scoped hook (useReservationForItem) owns local state — does NOT write to shared useActiveReservation context"
    - "Item-status flip detection (item.status === 'purchased' | 'available') in useEffect to trigger post-confirm navigate-back without wiring into ConfirmPurchaseBanner internals"
    - "Key-based StrictMode deduplication guard (lastKeyRef) mirrors useActiveReservationHydration pattern"
key_files:
  created:
    - functions/src/reservation/getReservationForItem.ts
    - functions/src/__tests__/getReservationForItem.test.ts
    - web/src/features/reservation/useReservationForItem.ts
    - web/src/features/reservation/__tests__/useReservationForItem.test.tsx
    - web/src/pages/ItemReservePage.tsx
    - web/src/pages/__tests__/ItemReservePage.test.tsx
  modified:
    - functions/src/index.ts
    - web/src/App.tsx
    - web/src/pages/RegistryPage.tsx
    - web/src/features/reservation/ReserveButton.tsx
    - web/src/features/registry/ItemCard.tsx
    - web/src/features/registry/__tests__/ItemCard.test.tsx
    - web/src/features/reservation/__tests__/ReserveButton.test.tsx
    - web/src/features/reservation/__tests__/RegistryPage.autoReserve.test.tsx
    - web/src/i18n/en.json
    - web/src/i18n/ro.json
    - web/i18n/en.json
    - web/i18n/ro.json
decisions:
  - "Accept ConfirmPurchaseBanner's internal clear() side effect: when called from ItemReservePage, it wipes the shared active context (which may hold a different reservation). Navigate-back + re-hydration on /registry/:id recovers the next most-recent reservation automatically."
  - "Detect confirm-success via item.status flip in useItemsQuery rather than wiring useConfirmPurchase state from inside ConfirmPurchaseBanner — preserves scope guard on ConfirmPurchaseBanner."
  - "Keep reserved_by_me_scroll_aria key in i18n files (not removed) — grep confirmed zero TS/TSX references so it's orphaned but harmless. Tracked as deferred removal below."
metrics:
  duration: ~90min
  completed_date: "2026-05-13"
  tasks_completed: 3
  tasks_total: 4
  files_created: 6
  files_modified: 12
---

# Quick Task 260513-g9g: Add Per-Item Reserve-Detail Route Summary

**One-liner:** Per-item reserve-detail route `/registry/:id/item/:itemId` backed by a new `getReservationForItem` callable + hook, with auto-navigate after every reserve success and click-from-card navigation replacing the old scroll-to-anchor.

## Tasks Completed

| # | Name | Commit | Status |
|---|------|--------|--------|
| 1 | Backend `getReservationForItem` callable + tests | `4754f10` | Done |
| 2 | Web hook `useReservationForItem` + tests | `285ec68` | Done |
| 3 | `ItemReservePage` + route wiring + auto-navigate + i18n | `d88eda1` | Done |
| 4 | Human verify — 2-reservation case, friendly states, release+confirm | — | Awaiting |

## What Was Shipped

### Task 1 — `getReservationForItem` callable

New `onCall` function at `europe-west3` mirroring `hydrateActiveReservation` with an added `itemId` scope. Validation order: `registryId` → `itemId` → `giverEmail` (guest path). Signed-in callers use `auth.uid`; guest callers send `giverEmail + giverId==null`. Returns `{ active: ActiveReservation | null }`. Legacy empty `affiliateUrl` guard preserved.

9 jest tests added (G-01 through G-09): signed-in happy, guest happy, defence-in-depth (uid wins over giverEmail), wrong itemId → null, legacy empty affiliateUrl → null, missing registryId/itemId/giverEmail errors, unknown uid → null. All pass.

### Task 2 — `useReservationForItem` hook

Route-scoped hook that calls `getReservationForItem` and returns `{ status: HydrationStatus; active: ActiveReservation | null }`. Does NOT write to the shared `useActiveReservation` context. Gating identical to `useActiveReservationHydration` (authReady, user or identity present, key-based StrictMode dedup guard).

7 vitest specs added (U-01 through U-07): bails when not ready, bails when no identity, signed-in and guest happy paths, empty → status=empty, error → status=error + console.warn, StrictMode dedup (callable fires exactly once per key). All pass.

### Task 3 — `ItemReservePage` + wiring + i18n

**New page** at `/registry/:id/item/:itemId` rendering five states:
- `item-reserve-loading` — items query not ready or lookup idle/loading
- `item-reserve-not-found` — itemId doesn't exist in the registry
- `item-reserve-not-yours` — item exists but not reserved by the viewer (friendly, NOT a 404)
- `item-reserve-expired` — countdown.expired === true
- `item-reserve-detail` — full reserve-detail UI (caption + headline + hero card + ConfirmPurchaseBanner + Release CTA + Continue anchor + HowTimerWorks)

Post-confirm navigate-back: watches `item.status` flip to `'purchased'` or `'available'` via a `useEffect` with a `hasNavigatedRef` guard — avoids needing to wire into `ConfirmPurchaseBanner` internals. Release toast + navigate-back wired with a `releaseSuccessHandledRef` guard.

**Auto-navigate after reserve** at all 3 sites:
- `RegistryPage.autoReserveMutation.onSuccess`: navigate to `/registry/${id}/item/${vars.itemId}` after setActive + showToast
- `ReserveButton.useCreateReservation.onSuccess`: navigate to `/registry/${registryId}/item/${item.id}` after setActive + showToast
- `RegistryPage.handleGuestSubmitForAutoReserve`: shares the same `autoReserveMutation` — covered by the above

**ItemCard** reserved-banner aria-label changed from `reserved_by_me_scroll_aria` → `reserved_by_me_navigate_aria`; RegistryPage's `renderReservedByMeClick` factory now calls `navigate(…/item/${item.id})` instead of `el.scrollIntoView`.

**i18n**: 12 new keys added under `web_reserve.item_page.*` + `web_pill.reserved_by_me_navigate_aria` in all four files (`web/src/i18n/en.json`, `web/src/i18n/ro.json`, `web/i18n/en.json`, `web/i18n/ro.json`). Both i18n directories byte-identical post-change.

7 component tests added for `ItemReservePage` (P-01 through P-07). ReserveButton, ItemCard, and RegistryPage.autoReserve tests updated to assert navigate calls.

## Verification Results (pre-checkpoint)

```
cd web && npm run test:run        → 138 tests pass across 26 test files  ✓
cd web && npx tsc --noEmit        → clean                                 ✓
cd functions && npm run test      → 87 tests pass                         ✓
cd functions && npx tsc --noEmit  → clean                                 ✓
diff web/i18n/en.json web/src/i18n/en.json  → no output                  ✓
diff web/i18n/ro.json web/src/i18n/ro.json  → no output                  ✓
git diff --name-only HEAD (scope guard)     → none of the 8 guarded files ✓
```

## Deviations from Plan

None — plan executed exactly as written. The `renderReservedByMeClick` factory prop was kept (not dropped) and RegistryPage now supplies `navigate` calls, matching the plan's preferred approach.

## Deferred Items

**Orphan i18n key `web_pill.reserved_by_me_scroll_aria`**: Kept in all four i18n JSON files for migration safety. `git grep reserved_by_me_scroll_aria` returns only i18n file matches — no TS/TSX source references. Safe to remove in a follow-up task.

## Known Stubs

None — all states render from real data sources. `useReservationForItem` drives the detail state; `useItemsQuery` drives item lookup.

## Self-Check

### Files exist:
- `functions/src/reservation/getReservationForItem.ts` — FOUND
- `web/src/features/reservation/useReservationForItem.ts` — FOUND
- `web/src/pages/ItemReservePage.tsx` — FOUND

### Commits exist:
- `4754f10` — feat(quick-260513-g9g-01) — FOUND
- `285ec68` — feat(quick-260513-g9g-02) — FOUND
- `d88eda1` — feat(quick-260513-g9g-03) — FOUND

## Self-Check: PASSED
