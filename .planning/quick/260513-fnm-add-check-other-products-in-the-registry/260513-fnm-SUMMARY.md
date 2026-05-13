---
phase: quick-260513-fnm
plan: "01"
subsystem: web
tags: [web, i18n, ux, reservation, scroll]
dependency_graph:
  requires: []
  provides: [check-other-products-cta]
  affects: [ReserveDetailSection, RegistryPage]
tech_stack:
  added: []
  patterns: [smooth-scroll, ghost-btn, i18n-namespace]
key_files:
  created: []
  modified:
    - web/i18n/en.json
    - web/i18n/ro.json
    - web/src/features/reservation/ReserveDetailSection.tsx
    - web/src/pages/RegistryPage.tsx
decisions:
  - "Used ghost Btn variant (not accent/primary) to keep visual hierarchy clear — ConfirmPurchaseBanner above already uses accent, StickyReserveBanner uses accent for Continue to retailer; a third coloured CTA would compete."
  - "Smooth-scroll via document.getElementById (mirrors the existing renderReservedByMeClick pattern in RegistryPage at lines 49-55) — no new navigation, stays on /registry/:id."
  - "Added Btn to the existing giftmaison import in ReserveDetailSection (it was not imported before — plan comment was slightly off, but Btn was already exported from the barrel)."
metrics:
  duration: "~10 minutes"
  completed_date: "2026-05-13"
  tasks_completed: 1
  tasks_total: 2
  files_modified: 4
---

# Quick Task 260513-fnm: Add 'Check other products in the registry' CTA — Summary

**One-liner:** Ghost-variant Btn below ConfirmPurchaseBanner smooth-scrolls to `id="registry-list-section"` anchor, with EN + RO i18n keys under `web_reserve` namespace.

## What Was Built

A single coordinated change across four files adds a secondary CTA to the giver-facing reservation purchase-step screen:

1. **web/i18n/en.json** — added `"check_other_products_cta": "Check other products in the registry"` inside the `web_reserve` namespace (after `how_timer_step4_b`).

2. **web/i18n/ro.json** — added `"check_other_products_cta": "Vezi alte produse din registru"` in the same position in the `web_reserve` namespace.

3. **web/src/pages/RegistryPage.tsx** — added `id="registry-list-section"` attribute to the `<section>` wrapping the section title + FilterChips + ItemGrid (line 230). This is the stable scroll anchor target.

4. **web/src/features/reservation/ReserveDetailSection.tsx** — added `Btn` to the existing giftmaison import; inserted a `<div className="flex justify-center sm:justify-start">` wrapper containing a `<Btn variant="ghost" size="md" data-testid="check-other-products-cta">` directly after `<ConfirmPurchaseBanner ... />` inside the left column. The `onClick` calls `document.getElementById('registry-list-section')?.scrollIntoView({ behavior: 'smooth', block: 'start' })` with a null guard.

## Decisions Made

- **Ghost variant rationale:** The ConfirmPurchaseBanner above already occupies the accent-CTA slot ("I completed the purchase"). The StickyReserveBanner above that occupies the accent slot for "Continue to retailer". A third coloured CTA would create visual competition. `ghost` (transparent bg, ink fg, line border) reads as a low-emphasis secondary action — matching the existing auth-screen Skip pattern.

- **Smooth-scroll (not navigation):** Investigation confirmed that `ReserveDetailSection` and the item grid live on the SAME route (`/registry/:id`). The correct approach is smooth-scroll, mirroring the existing `renderReservedByMeClick` handler in RegistryPage that scrolls UP to `#reserve-detail-section`. This change scrolls DOWN to the new `#registry-list-section` anchor.

- **Null guard on getElementById:** The anchor `registry-list-section` only exists on RegistryPage. If the CTA is ever rendered in a different context, the null guard prevents a throw (silent no-op).

- **No prefers-reduced-motion handling:** Browsers automatically downgrade `behavior: 'smooth'` to instant when the user has reduce-motion enabled — no extra code needed.

## Commits

| Task | Commit | Description |
|------|--------|-------------|
| Task 1 | 07ba565 | feat(quick-260513-fnm-01): add 'Check other products' CTA with scroll anchor |

## Verification

- `npm run typecheck` — passed, no new TS errors.
- `npm run build` — succeeded (proves i18n JSON is valid and React tree compiles).
- Human visual verification (Task 2) — pending.

## Deviations from Plan

**1. [Rule 2 - Missing import] Added Btn to giftmaison import**
- **Found during:** Task 1 implementation
- **Issue:** Plan comment stated "Btn already imported on line 7 — do NOT re-import" but line 7 only imported `{ Pill, MonoCaption }`. Btn was not present.
- **Fix:** Added `Btn` to the existing destructured import from `../../components/giftmaison`. No new import statement, just extended the existing one.
- **Files modified:** web/src/features/reservation/ReserveDetailSection.tsx
- **Commit:** 07ba565

## Known Stubs

None — the CTA renders live i18n strings and the scroll target is a real DOM element.

## Human Verify Outcome

Pending — checkpoint:human-verify returned to user (Task 2).
