---
phase: quick-260512-wht
plan: 01
subsystem: web-fallback
tags: [navigation, item-detail, i18n, react-router, tdd]
dependency_graph:
  requires: [useItemsQuery, useActiveReservation, useConfirmPurchase, ReserveButton]
  provides: [ItemDetailPage, /registry/:id/item/:itemId route]
  affects: [ItemCard, ItemGrid, RegistryPage, App router]
tech_stack:
  added: []
  patterns: [react-router Link sibling pattern, Btn sibling <a> for retailer CTAs]
key_files:
  created:
    - web/src/pages/ItemDetailPage.tsx
    - web/src/features/registry/__tests__/ItemDetailPage.test.tsx
  modified:
    - web/src/App.tsx
    - web/src/features/registry/ItemCard.tsx
    - web/src/features/registry/ItemGrid.tsx
    - web/src/pages/RegistryPage.tsx
    - web/src/features/registry/__tests__/ItemCard.test.tsx
    - web/src/i18n/en.json
    - web/src/i18n/ro.json
    - web/i18n/en.json
    - web/i18n/ro.json
decisions:
  - "Reserve-slot rendered as sibling of Link (not descendant) to prevent navigate-on-reserve-click"
  - "Go-to-retailer uses item.affiliateUrl (not originalUrl) — EMAG monetization requirement"
  - "Mark-as-purchased CTA gated on active?.itemId === item.id (in-memory only — refresh loses this; read-only banner shown instead, which is D-06-compatible)"
  - "useItemsQuery reused (same cache key) — no new Firestore query on the detail page"
metrics:
  duration: ~20min
  completed: "2026-05-12T20:39:00Z"
  tasks_completed: 2
  tasks_total: 3
  files_changed: 10
---

# Quick Task 260512-wht: Make Web Product Cards Clickable to a Per-Item Detail Page — Summary

**One-liner:** Per-item detail route /registry/:id/item/:itemId with affiliateUrl Go-to-retailer CTA and Mark-as-purchased action, wired via react-router Link on ItemCard.

## Status

Tasks 1 and 2 complete. Task 3 (human-verify checkpoint) pending.

## What Was Built

### New route and page (Task 1)

Added `{ path: '/registry/:id/item/:itemId', element: <ItemDetailPage />, errorElement: <NotFoundPage /> }` as a sibling route in App.tsx (not nested under /registry/:id).

`ItemDetailPage` (web/src/pages/ItemDetailPage.tsx, 164 lines):
- Calls `useItemsQuery(registryId)` — reuses the same react-query cache key `['registry', registryId, 'items']` that RegistryPage uses, so live Firestore snapshots flow through automatically
- Shows loading skeleton with `role="status"` while `itemsQ.data === undefined`
- Delegates to `<NotFoundPage />` when data loaded but item not found (no enumeration leakage)
- Status-driven action zone:
  - `available` → `<ReserveButton>` (same auth/guest gating as RegistryPage — zero duplication)
  - `reserved` AND `active?.itemId === item.id` → "I completed the purchase" `<Btn>` calling `useConfirmPurchase.confirm()`
  - `reserved` AND NOT ours → read-only banner `t('web_item_detail.reserved_read_only')` — NO reserver name (D-06)
  - `purchased` → read-only banner, no CTAs
- Go-to-retailer: `<a href={item.affiliateUrl} target="_blank" rel="noopener noreferrer">` (sibling `<a>` pattern per Btn JSDoc — used by StickyReserveBanner)

### ItemCard clickable wrap

ItemCard now accepts a required `registryId: string` prop and renders a react-router `<Link to={/registry/${registryId}/item/${item.id}>` wrapping the image and body content. The reserve-slot (`<div data-testid="reserve-slot">`) is a sibling of the Link, NOT a descendant — this prevents clicks on the Reserve button from triggering card navigation.

### ItemGrid and RegistryPage plumbing

ItemGrid accepts and forwards the new required `registryId` prop to each ItemCard. RegistryPage passes `registryId={registryQ.data!.id}` (guaranteed non-null at that render branch).

### i18n keys

`web_item_detail` namespace added to all four i18n files:
- `web/src/i18n/en.json` + `web/src/i18n/ro.json` (runtime path)
- `web/i18n/en.json` + `web/i18n/ro.json` (legacy mirror, kept in sync per quick-260512-vlg learning)

Keys: `page_caption`, `go_to_retailer`, `back_to_registry`, `mark_as_purchased_cta`, `mark_as_purchased_loading`, `reserved_read_only`, `purchased_label`, `notes_label`, `loading`.

### Tests (Task 2)

12 tests in `web/src/features/registry/__tests__/ItemDetailPage.test.tsx`:
1. Loading state: `role="status"` element present when data undefined
2. NotFoundPage: "Registry not available" when item not in list
3. Title + AVAILABLE pill + Go-to-retailer link (href=affiliateUrl, target=_blank, rel contains noopener+noreferrer)
4. ReserveButton rendered for available status
5. Mark-as-purchased CTA rendered when active.itemId matches
6. confirm() called with reservationId on click
7. Read-only banner when reserved + active=null; D-06: uid not in DOM textContent
8. Read-only banner when reserved + active.itemId !== item.id
9. Purchased label only (no CTAs except Go-to-retailer + Back)
10. Back-to-registry link href = /registry/reg-1
11. Notes rendered when non-null (with Notes label)
12. Notes block absent when null

ItemCard.test.tsx updated: all 10 existing assertions retained, renders wrapped in MemoryRouter, new assertion verifies Link href.

Full suite: 127 tests across 24 files, all green. TypeScript exits 0.

## Known Trade-off

After a hard refresh, `useActiveReservation.active` is null (in-memory session state, not persisted). On the detail page this means:
- The "Mark as purchased" CTA is hidden after refresh — read-only banner shown instead
- The affiliate URL (Go-to-retailer) remains accessible, which directly fixes the reported bug

This is the documented D-06-compatible trade-off. The reserving session has the action; anonymous sessions on refresh see the read-only banner with no names and still get the retailer URL.

## Deviations from Plan

None — plan executed exactly as written.

## Task 3: Human Verify (PENDING)

The checkpoint details are returned to the orchestrator. Do not mark this plan complete until the human-verify signal is received.

## Test Results (Human-Verify)

Pending — see Task 3 checkpoint below.

## Commits

| Task | Commit | Message |
|------|--------|---------|
| 1 | c42bc99 | feat(quick-260512-wht-01): add ItemDetailPage route + clickable ItemCard + i18n keys |
| 2 | 82b3606 | test(quick-260512-wht-02): add 12-test ItemDetailPage suite + verify full suite green |

## Self-Check

- web/src/pages/ItemDetailPage.tsx: exists
- web/src/features/registry/__tests__/ItemDetailPage.test.tsx: exists
- Commit c42bc99: present in git log
- Commit 82b3606: present in git log
