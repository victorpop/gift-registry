---
quick_id: 260516-lsi
type: quick
mode: tdd
status: complete
requirements:
  - LSI-01
files_modified:
  - web/src/pages/ItemReservePage.tsx
  - web/src/pages/__tests__/ItemReservePage.test.tsx
  - web/i18n/en.json
  - web/i18n/ro.json
  - web/src/i18n/en.json
  - web/src/i18n/ro.json
commits:
  - d54f7e2: test(quick-260516-lsi-01) — RED, K-15/K-16/K-18/K-19 failing (K-17 absence-assertion green pre-impl)
  - 5b6c88f: fix(quick-260516-lsi-02) — GREEN, both blades clickable + i18n key in 4 bundles
test-delta:
  before: 190 tests (full web suite baseline)
  after: 195 tests (5 new K-15..K-19, all green)
duration: 5m 38s
completed: 2026-05-16
---

# Quick 260516-lsi: Make product blade on ItemReservePage clickable — Summary

## One-liner

Product blades on `web/src/pages/ItemReservePage.tsx` (both `ItemDetailHero` for browse branches and the inner blade card inside `renderReservedByMeDetail`) now wrap in `<a target="_blank" rel="noopener noreferrer">` when a URL is available — preferring `affiliateUrl` (commission flow) and falling back to `originalUrl`; they render as the original static `<div>` when both URLs are empty, preserving layout.

## What was broken

The product blade on `ItemReservePage` visually presented a product (image + name + price + retailer) but was a non-interactive `<div>` in all five branches that render it:

- BROWSE_AVAILABLE (`item-reserve-available`)
- BROWSE_RESERVED_BY_OTHER (`item-reserve-reserved-by-other`)
- BROWSE_PURCHASED (`item-reserve-purchased`)
- The shared `ItemDetailHero` helper covered the three browse cases above.
- The reserved-by-me hero (rendered inline inside `renderReservedByMeDetail`) — the happy path after a giver successfully reserves an item.

Givers naturally expected the card-shaped product display to be clickable. A separate "Continue to retailer" CTA exists below the blade on the reserved-by-me page, but the browse branches had no per-blade affordance at all. Result: dead-end on the giver flow.

## What was fixed

### `ItemDetailHero` (browse branches)

Refactored to compute `const href = item.affiliateUrl || item.originalUrl || null` plus `const retailer = item.merchantDomain ?? 'retailer'`, extracted the children into a `bladeContent` fragment, and rendered conditionally:

- `href` truthy → root is `<a href={href} target="_blank" rel="noopener noreferrer" aria-label={t('web_reserve.item_page.blade_link_aria', { retailer })}>` with the existing card classes plus `block cursor-pointer hover:border-gm-accent transition-colors focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gm-accent no-underline text-inherit`.
- `href` null (both URLs empty) → original static `<div>` unchanged.

Hover affordance shifts the existing border to `gm-accent` — matches the established Pill `accent`-tone idiom (accent-on-border) used elsewhere in giftmaison surfaces. Focus-visible ring matches `Btn.tsx` line 58 and `StickyReserveBanner.tsx` line 169 exactly.

### Reserved-by-me hero inner blade card

Same conditional-wrap pattern wrapped inside an IIFE (so it slots cleanly into JSX). URL resolution differs slightly because `active` does not carry `originalUrl`:

- `const bladeHref = active.affiliateUrl || item.originalUrl || null`
- `const bladeRetailer = active.merchantDomain ?? item.merchantDomain ?? 'retailer'`

The existing `bg-gm-paperDeep` background is preserved (different from `ItemDetailHero`'s `bg-gm-paper`). The nested time-to-purchase card (`reserve-detail-mmss`) stays INSIDE the new anchor — K-19 explicitly asserts this. The Release `Btn`, `ConfirmPurchaseBanner`, and the existing "Continue to retailer" anchor at lines 720-728 are siblings of the blade card and are unchanged.

### i18n key

Added `web_reserve.item_page.blade_link_aria` to all 4 i18n bundles (parity convention from quick-260513-fk1 / quick-260512-vlg):

| File                    | Key value                                     |
| ----------------------- | --------------------------------------------- |
| `web/i18n/en.json`      | `Open product page at {{retailer}}`           |
| `web/i18n/ro.json`      | `Deschide pagina produsului pe {{retailer}}`  |
| `web/src/i18n/en.json`  | `Open product page at {{retailer}}`           |
| `web/src/i18n/ro.json`  | `Deschide pagina produsului pe {{retailer}}`  |

All 4 JSON files parse-validated post-edit.

## Test delta

| Suite                                                     | Before | After | New |
| --------------------------------------------------------- | -----: | ----: | --: |
| `src/pages/__tests__/ItemReservePage.test.tsx`            |     27 |    32 |   5 |
| Full web suite (`npm test -- --run`)                      |    190 |   195 |   5 |

New tests K-15..K-19:

- **K-15** — BROWSE_AVAILABLE blade wraps in anchor preferring `affiliateUrl`; asserts `href`, `target=_blank`, `rel` contains both `noopener` + `noreferrer`.
- **K-16** — BROWSE_AVAILABLE blade falls back to `originalUrl` when `affiliateUrl === ''`.
- **K-17** — BROWSE_PURCHASED blade renders as static `<div>` (no link role inside the container) when BOTH URLs are empty; sanity: title still renders, container still mounts.
- **K-18** — BROWSE_RESERVED_BY_OTHER blade `aria-label` interpolates `merchantDomain` correctly (asserted with literal string `Open product page at altex.ro`).
- **K-19** — Reserved-by-me hero blade is an anchor (`href === active.affiliateUrl`, `target=_blank`, `rel` ok); nested `reserve-detail-mmss` renders WITHIN the anchor (proves time-to-purchase card was not broken by the wrap); regression guard: the lower "Continue to retailer" CTA in the button row still exists.

## Precheck: no `<a>`-inside-`<a>` violation

Source scan of both blade locations confirmed clean:

- **`ItemDetailHero` (lines 534-567 pre-patch)**: children are `<img>`, `<Pill>` (renders `<span>`), `<MonoCaption>` (renders `<span>`), `<h2>`, `<div>`, `<span>`. No `<a>`, `<Link>`, or `<button>`. Safe to wrap.
- **Reserved-by-me blade card (lines 656-701 pre-patch)**: children are `<img>`, `<Pill>`, `<h2>`, price text divs (containing `<span>`/`<strong>`), nested time-to-purchase `<div>` (containing `<MonoCaption>`, `<span>`, `<div>`). No `<a>`, `<Link>`, or `<button>`. The Release `Btn` (line 708) and the "Continue to retailer" `<a>` (line 720) are SIBLINGS of the blade card under `<div className="flex flex-col gap-5">` at line 654 — NOT descendants of the blade card. Safe to wrap.

## Deviations from Plan

1. **K-17 is GREEN pre-implementation (RED phase shows 4 failures, not 5).** K-17 asserts the *absence* of a link when both URLs are empty — this absence already held pre-impl because the blade was a `<div>`. The plan's "Expected: 5 failing" is structurally off-by-one for absence-assertion tests. No action needed: the test correctly validates the post-impl behavior contract (must remain a `<div>` when URLs are empty), and the GREEN run still confirms all 5 K-tests pass after Task 2. Documented here for transparency.

2. **Worktree rebase performed before Task 1 (per the CRITICAL note in the constraints).** The worktree's base was at `683a122` (j8a era), 10+ commits behind main at `1e71bef` (post-lbf). Rebased cleanly onto `main` before starting work to avoid the worktree-drift issues seen during the lbf cherry-pick. Post-rebase: worktree HEAD = main HEAD = `1e71bef`.

3. **node_modules symlinked from main repo into worktree** (`web/node_modules` → main `web/node_modules`) so `npx vitest` and `npx tsc` can resolve dependencies inside the worktree without a fresh `npm install`. Also copied `web/.env.local` from main → worktree so the Firebase test bootstrap (`src/firebase.ts`) can resolve required env vars during vitest setup. Both are dev-environment artifacts, not committed.

## TypeScript

`cd web && npx tsc --noEmit` — clean, no errors.

## Files touched (6 total)

| File                                                | Purpose                                               |
| --------------------------------------------------- | ----------------------------------------------------- |
| `web/src/pages/ItemReservePage.tsx`                 | Both blade renderers wrap conditionally in `<a>`      |
| `web/src/pages/__tests__/ItemReservePage.test.tsx`  | 5 new tests K-15..K-19 (import: added `within`)       |
| `web/i18n/en.json`                                  | New `blade_link_aria` key                             |
| `web/i18n/ro.json`                                  | New `blade_link_aria` key                             |
| `web/src/i18n/en.json`                              | New `blade_link_aria` key                             |
| `web/src/i18n/ro.json`                              | New `blade_link_aria` key                             |

No new components, no new hooks, no new files. No backend / app / functions / registry-tile changes. `D-06` (no reserver/giver name/email rendered) unchanged.

## Verification

Final check from worktree HEAD:

```
$ npx tsc --noEmit                            # clean
$ npx vitest run src/pages/__tests__/ItemReservePage.test.tsx
  Test Files  1 passed (1)
  Tests       32 passed (32)
$ npm test -- --run
  Test Files  27 passed (27)
  Tests       195 passed (195)
```

All goal-backward truths from the plan's `must_haves.truths` are verified by tests K-15..K-19; the 5 truths about i18n parity are verified implicitly because the tests resolve through `web/src/i18n/` and the regex `/open product page at .../i` would not match the bare key path returned when a key is missing.

## Self-Check: PASSED

- SUMMARY.md exists at `.planning/quick/260516-lsi-make-product-blade-on-itemreservepage-cl/260516-lsi-SUMMARY.md`
- Both commits present in git history: `d54f7e2` (test/RED), `5b6c88f` (fix/GREEN)
- `blade_link_aria` key confirmed via grep in all 4 i18n files
- 5 source files + 1 doc file modified (exactly as scoped)
- Worktree HEAD: 2 commits ahead of `main` (1e71bef → 5b6c88f)
