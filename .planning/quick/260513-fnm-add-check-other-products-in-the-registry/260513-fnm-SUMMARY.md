---
phase: quick-260513-fnm
plan: "01"
subsystem: web
tags: [web, i18n, ux, reservation, scroll, text-link]
dependency_graph:
  requires: []
  provides: [check-other-products-cta]
  affects: [ReserveDetailSection, RegistryPage]
tech_stack:
  added: []
  patterns: [smooth-scroll, text-link-cta, i18n-namespace]
key_files:
  created: []
  modified:
    - web/i18n/en.json
    - web/i18n/ro.json
    - web/src/i18n/en.json
    - web/src/i18n/ro.json
    - web/src/features/reservation/ReserveDetailSection.tsx
    - web/src/pages/RegistryPage.tsx
decisions:
  - "Smooth-scroll via document.getElementById (mirrors the existing renderReservedByMeClick pattern in RegistryPage) — no new navigation, stays on /registry/:id."
  - "Final styling: accent-coloured text link (not a button) — right-aligned below ConfirmPurchaseBanner, font/size matches HowTimerWorks body copy (font-body 12.5px / leading-1.45). Initial ghost-Btn variant was reverted after user feedback (heavy / wrong placement)."
  - "i18n keys live in BOTH web/i18n/ (legacy duplicate) AND web/src/i18n/ (the path React actually imports). Initial implementation only updated web/i18n/, which made the button render the raw key — same trap previously hit by quick-260512-vlg."
metrics:
  completed_date: "2026-05-13"
  tasks_completed: 2
  tasks_total: 2
  files_modified: 6
---

# Quick Task 260513-fnm: Add 'Check other products in the registry' CTA — Summary

**One-liner:** Accent-coloured text-link below ConfirmPurchaseBanner smooth-scrolls to `id="registry-list-section"`, with EN + RO i18n keys synced to both `web/src/i18n/` (runtime) and `web/i18n/` (legacy duplicate).

## What Was Built

A secondary CTA on the giver-facing reservation purchase-step screen that helps the user browse the rest of the registry to reserve more gifts:

1. **`web/src/i18n/en.json` / `web/src/i18n/ro.json`** (runtime path) — added `"check_other_products_cta"` inside the `web_reserve` namespace.
2. **`web/i18n/en.json` / `web/i18n/ro.json`** (legacy duplicate path) — same key added to keep both copies in sync, matching the convention established by quick-260512-vlg.
3. **`web/src/pages/RegistryPage.tsx`** — added `id="registry-list-section"` to the section wrapping the section title + FilterChips + ItemGrid. Stable scroll anchor.
4. **`web/src/features/reservation/ReserveDetailSection.tsx`** — text-link `<button>` placed directly below `ConfirmPurchaseBanner` in the left column, right-aligned, with `-mt-2` to pair it visually with the "I completed the purchase" CTA above. Click handler calls `document.getElementById('registry-list-section')?.scrollIntoView({ behavior: 'smooth', block: 'start' })`. Styling: `font-body text-[12.5px] leading-[1.45] text-gm-accent underline underline-offset-[3px] decoration-[1px] hover:decoration-2` + standard focus-visible outline. No new component primitive — element built inline.

## Decisions Made

- **Text link, not a button:** First implementation used `<Btn variant="ghost">`, which felt heavy and competed with the accent CTA above. User feedback steered the redesign to a text-link rendered in `gm-accent` (#C8623A) with a subtle underline — clearly secondary, clearly clickable, no extra chrome.

- **Right-aligned below "I completed the purchase":** User-directed placement (annotated screenshot). The link sits in its own row directly below the pink ConfirmPurchaseBanner, right-aligned so it pairs visually with the accent CTA above it rather than competing.

- **Font/size matches HowTimerWorks body copy:** `font-body 12.5px / leading-1.45 / text-gm-inkSoft`-equivalent sizing keeps the link visually subordinate to the surrounding banner copy.

- **Smooth-scroll (not navigation):** `ReserveDetailSection` and the item grid render on the same route (`/registry/:id`). Mirrors the existing `renderReservedByMeClick` handler in RegistryPage that scrolls UP to `#reserve-detail-section`; this change scrolls DOWN to the new `#registry-list-section` anchor.

- **i18n keys in both locations:** The project ships duplicated translation files at `web/i18n/` (legacy/unused) and `web/src/i18n/` (the React runtime path). Adding to both prevents future drift, per the convention quick-260512-vlg landed on.

## Commits

| # | Commit | Description |
|---|--------|-------------|
| 1 | 07ba565 | feat: initial ghost-Btn implementation + scroll anchor + i18n key (wrong path) |
| 2 | 6493b1c | fix: add i18n key to web/src/i18n/{en,ro}.json so the button renders translated text |
| 3 | f8700e5 | style: restyle CTA as accent text-link below confirm-purchase |

## Verification

- `npx tsc --noEmit` (web) — passed
- `npx vitest run src/__tests__/i18n.test.ts` — 5/5 passed (EN/RO parity intact)
- Human visual UAT — **approved** by user (placement + font/size + accent-link styling confirmed)

## Deviations from Plan

**1. i18n keys landed in the wrong path on first try**
- **Found during:** Task 2 (human visual verify) — button rendered raw key `web_reserve.check_other_products_cta`.
- **Root cause:** Plan and initial executor wrote to `web/i18n/{en,ro}.json`, but `web/src/i18n/index.ts` imports from `./en.json` / `./ro.json` (i.e. `web/src/i18n/`). Same trap caught by quick-260512-vlg.
- **Fix:** Added the key to `web/src/i18n/{en,ro}.json` (the loaded path); left `web/i18n/` copies in place per the existing duplicate-sync convention.
- **Commit:** 6493b1c

**2. Styling pivot from ghost Btn to accent text-link**
- **Found during:** Task 2 visual review (annotated screenshot from user).
- **Issue:** Ghost button felt too heavy; user wanted a link-like affordance in the orange theme accent, placed below the "I completed the purchase" CTA rather than below the banner on the left.
- **Fix:** Removed `Btn` import + the centered ghost-Btn block; inlined a styled `<button>` with `text-gm-accent` + underline + body-12.5px sizing matching HowTimerWorks copy; wrapped in `<div className="flex justify-end -mt-2">` to right-align it directly under the accent CTA.
- **Commit:** f8700e5

## Human Verify Outcome

**Approved 2026-05-13** — user confirmed final placement, accent link color, font/size match, and scroll behaviour all read correctly.
