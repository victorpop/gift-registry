---
quick_id: 260516-lsi
type: quick
wave: 1
depends_on: []
files_modified:
  - web/src/pages/ItemReservePage.tsx
  - web/src/pages/__tests__/ItemReservePage.test.tsx
  - web/i18n/en.json
  - web/i18n/ro.json
  - web/src/i18n/en.json
  - web/src/i18n/ro.json
autonomous: true
requirements:
  - LSI-01
must_haves:
  truths:
    - "Clicking the product blade on BROWSE_AVAILABLE / BROWSE_RESERVED_BY_OTHER / BROWSE_PURCHASED opens the retailer page in a new tab (target=_blank, rel=noopener noreferrer)."
    - "Clicking the product blade on the reserved-by-me hero (renderReservedByMeDetail) opens the retailer page in a new tab; the nested time-to-purchase card continues to render inside the blade."
    - "Blade prefers item.affiliateUrl (commission flow); falls back to item.originalUrl when affiliateUrl is empty."
    - "Blade renders as a non-link <div> (no <a>) when BOTH affiliateUrl and originalUrl are empty — no broken navigation, layout preserved."
    - "Blade's aria-label uses the new web_reserve.item_page.blade_link_aria key, interpolating {{retailer}} from item.merchantDomain (fallback: literal 'retailer')."
    - "All four i18n bundles (web/i18n/en.json, web/i18n/ro.json, web/src/i18n/en.json, web/src/i18n/ro.json) contain blade_link_aria under web_reserve.item_page."
    - "190 pre-existing tests remain green; 5 new tests K-15..K-19 pass; total 195 in this file run set: confirmed via `cd web && npx vitest run src/pages/__tests__/ItemReservePage.test.tsx`."
    - "`cd web && npx tsc --noEmit` produces no new errors."
  artifacts:
    - path: "web/src/pages/ItemReservePage.tsx"
      provides: "Two blade renderers (ItemDetailHero + reserved-by-me hero inner card) wrap their root <div> in an <a target=_blank rel=noopener noreferrer> when a URL is available; render as static <div> otherwise."
    - path: "web/src/pages/__tests__/ItemReservePage.test.tsx"
      provides: "Five new tests appended (K-15..K-19) covering URL preference, fallback, missing-URL graceful render, aria-label interpolation, and the reserved-by-me variant."
    - path: "web/i18n/en.json"
      provides: "New key web_reserve.item_page.blade_link_aria = 'Open product page at {{retailer}}'"
    - path: "web/i18n/ro.json"
      provides: "New key web_reserve.item_page.blade_link_aria = 'Deschide pagina produsului pe {{retailer}}'"
    - path: "web/src/i18n/en.json"
      provides: "Mirror of EN key (this file is the bundle consumed by `import '../../i18n'` in tests — must stay in sync with web/i18n/en.json per existing convention)."
    - path: "web/src/i18n/ro.json"
      provides: "Mirror of RO key (same reasoning)."
  key_links:
    - from: "ItemDetailHero blade root"
      to: "item.affiliateUrl || item.originalUrl"
      via: "conditional <a href={...} target=_blank rel=noopener noreferrer aria-label={t('web_reserve.item_page.blade_link_aria', { retailer })}>"
      pattern: "href=\\{(?:item\\.affiliateUrl|item\\.originalUrl|...)"
    - from: "renderReservedByMeDetail nested hero card"
      to: "active.affiliateUrl"
      via: "conditional <a> wrap around the existing flex-row blade <div> (NOT around the entire grid; ONLY the inner blade-card div at lines ~656-701). Nested time-to-purchase card (lines ~686-699) is purely static — confirmed via source scan — so no nested-anchor invalidity."
      pattern: "href=\\{active\\.affiliateUrl"
    - from: "Tests"
      to: "i18n bundle"
      via: "`import '../../i18n'` in the test file resolves through web/src/i18n/index.ts → web/src/i18n/en.json + web/src/i18n/ro.json (NOT web/i18n/*.json). The web/i18n/*.json pair is the canonical source consumed by the production app build; convention from quick-260513-fk1 / quick-260512-vlg requires both pairs receive the same key."
      pattern: "i18n bundle parity"
---

<objective>
Make the product blade on `web/src/pages/ItemReservePage.tsx` clickable so that gift givers can jump straight to the retailer's product page from any state of the reservation flow — BROWSE_AVAILABLE, BROWSE_RESERVED_BY_OTHER, BROWSE_PURCHASED (rendered via the shared `ItemDetailHero`), AND the reserved-by-me happy path (rendered inline inside `renderReservedByMeDetail`).

The blade currently renders as a non-interactive `<div>` in both locations. After this change, when an `affiliateUrl` (preferred — preserves the commission flow per the existing "Continue to retailer" anchor pattern at StickyReserveBanner.tsx:164-170 and ItemReservePage.tsx:719-728) or `originalUrl` is available, the blade root becomes an `<a target="_blank" rel="noopener noreferrer">`. When both URLs are empty, the blade gracefully renders as the existing static `<div>` — no broken navigation, layout unchanged.

Purpose: Removes a dead-end on the giver flow. The product blade visually presents a product (image + name + price + shop) — users naturally expect it to be clickable. Today they have to find a separate CTA. After this fix, the entire blade is the affordance.

Output:
- Two clickable blades on `ItemReservePage.tsx` (one in the `ItemDetailHero` helper, one inline inside `renderReservedByMeDetail`).
- 1 new i18n key (`web_reserve.item_page.blade_link_aria`) in all 4 i18n files.
- 5 new tests (K-15..K-19) appended to `web/src/pages/__tests__/ItemReservePage.test.tsx` — total 195.
- No new components, no new hooks, no new files.
- No backend / app / functions changes.
</objective>

<execution_context>
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/workflows/execute-plan.md
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/STATE.md
@CLAUDE.md

# THE file to patch (CRITICAL — read all 743 lines)
@web/src/pages/ItemReservePage.tsx

# Reference: existing "Continue to retailer" anchor pattern (target/rel convention)
@web/src/features/reservation/StickyReserveBanner.tsx

# Reference: Item interface — affiliateUrl: string, originalUrl: string, merchantDomain: string | null
@web/src/lib/firestore-mapping.ts

# Reference: Btn/Pill atoms — hover/focus class patterns used elsewhere in giftmaison surface
@web/src/components/giftmaison/Btn.tsx
@web/src/components/giftmaison/Pill.tsx

# THE test file to append (current 801 lines, 27 it() blocks in this file; ~190 across whole suite)
@web/src/pages/__tests__/ItemReservePage.test.tsx

# i18n — all four files MUST receive the new key (parity convention from quick-260513-fk1 / quick-260512-vlg)
@web/i18n/en.json
@web/i18n/ro.json
@web/src/i18n/en.json
@web/src/i18n/ro.json

<interfaces>
<!-- Pre-extracted from the codebase so the executor does not need to re-explore. -->

From web/src/lib/firestore-mapping.ts — `Item` interface (lines 163-177):
```typescript
export interface Item {
  id: string
  title: string
  imageUrl: string | null
  price: number | null
  currency: string | null
  notes: string | null
  status: ItemStatus
  reservedBy: string | null
  reservedAt: Date | null
  expiresAt: Date | null
  affiliateUrl: string       // empty string "" when absent
  originalUrl: string        // empty string "" when absent
  merchantDomain: string | null
}
```

From web/src/pages/ItemReservePage.tsx — `ReservedByMeDetailParams.active` shape (line 595):
```typescript
active: {
  reservationId: string;
  itemName: string;
  affiliateUrl: string;       // empty string "" when absent
  merchantDomain: string | null;
  expiresAtMs: number;
  itemId: string;
}
```
NOTE: `active` does NOT carry an `originalUrl`. The reserved-by-me blade can ONLY check `active.affiliateUrl`. If `active.affiliateUrl === ''`, it must read `item.originalUrl` from the sibling `item: Item` prop already on `ReservedByMeDetailParams`. (Both `active` and `item` are in scope inside `renderReservedByMeDetail`.)

From web/src/features/reservation/StickyReserveBanner.tsx (lines 164-170) — REFERENCE for target/rel pattern:
```tsx
<a
  href={active.affiliateUrl}
  target="_blank"
  rel="noopener noreferrer"
  className="..."
>
```

From web/src/pages/ItemReservePage.tsx (lines 720-728) — the existing "Continue to retailer" anchor sits in the BUTTON ROW below the blade (NOT inside the blade card):
```tsx
{active.affiliateUrl && (
  <a
    href={active.affiliateUrl}
    target="_blank"
    rel="noopener noreferrer"
    aria-label={t('web_reserve.continue_cta', { retailer })}
    ...
```
This anchor is at the SAME nesting level as the blade card div (both inside `<div className="flex flex-col gap-5">` at line 654), NOT nested inside it. Confirmed via source scan: making the blade card itself an `<a>` does NOT create an `<a>`-inside-`<a>` HTML violation.

Hover affordance survey (Btn.tsx, Pill.tsx, BrowseShell back-link line 509, ConfirmPurchaseBanner — sampled):
- `cursor-pointer` is universal on Btn (line 57) and on anchor wrappers.
- `hover:` patterns observed: `hover:opacity-90` (primary CTA at 402), `hover:decoration-2` (back link at 509), `hover:bg-gm-paper/10` (StickyReserveBanner Release Btn at 160).
- The blade uses `border border-gm-line bg-gm-paper` (ItemDetailHero, line 536) / `bg-gm-paperDeep border border-gm-line` (reserved-by-me inner card, line 656).
- PICK (consistent + visible without fighting the card aesthetic): `cursor-pointer hover:border-gm-accent transition-colors` — shifts the existing border to the accent stroke on hover, leaving the bg untouched. This is the cleanest match because Pill `accent` tone already uses `border-transparent` over `bg-gm-accentSoft` (line 25), establishing accent-on-border as a known affordance idiom in this surface.
- Add focus-visible ring for keyboard: `focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gm-accent` (matches Btn.tsx line 58, StickyReserveBanner anchor line 169 — exact same pattern).
- Also add `block` (or omit it — `<a>` inline by default; wrapping the flex container needs `block` so the flex layout works) — final class string in Task 2.

Existing keys under `web_reserve.item_page` (verified from web/i18n/en.json lines 116-138 and the three parity files):
`loading_label, back_to_registry, not_yours_title, not_yours_body, item_not_found_title, item_not_found_body, expired_title, expired_body, page_caption, headline_pre, headline_emphasis, headline_post, available_title, available_subline, reserve_cta, reserve_cta_pending, reserved_by_other_title, reserved_by_other_body, purchased_title, purchased_body, notes_label`.
The new key `blade_link_aria` will be added as a new sibling under `web_reserve.item_page` in all four files.
</interfaces>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1 (RED): Append 5 failing tests K-15..K-19 to ItemReservePage.test.tsx</name>
  <files>web/src/pages/__tests__/ItemReservePage.test.tsx</files>
  <behavior>
    Append 5 new `it(...)` blocks inside the existing `describe('ItemReservePage', () => { ... })` at the end of the file (currently closes at line 801 with `})` on line 801; insert the new tests immediately before the closing `})` and the trailing `}` on line 802 — i.e. between line 800 (`activeMock.active = null`) and line 801 (`})`). Each test isolates one assertion; all use the existing `renderPage`, `makeItem`, `ACTIVE_RES` helpers and existing mocks. Default `beforeEach` already populates `itemsQueryMock` and `reservationForItemMock` — tests override only what they need.

    Test specs (verbatim assertions — executor MUST implement these exact contracts):

    **K-15 (BROWSE_AVAILABLE blade — affiliateUrl preferred):**
    - Setup: `itemsQueryMock.useItemsQuery.mockReturnValue({ data: [makeItem({ status: 'available', reservedBy: null, affiliateUrl: 'https://emag.ro/aff/it1', originalUrl: 'https://emag.ro/it1' })] })`; `reservationForItemMock.useReservationForItem.mockReturnValue({ status: 'empty', active: null })`.
    - Render `renderPage()`.
    - Locate the blade by `screen.getByTestId('item-reserve-available')`, then query within it for the anchor wrapping the title 'Coffee Machine' (use `within(...).getByRole('link', { name: /open product page at emag\.ro/i })`).
    - Assert `link.getAttribute('href') === 'https://emag.ro/aff/it1'`.
    - Assert `link.getAttribute('target') === '_blank'`.
    - Assert `link.getAttribute('rel')` matches `/noopener/` AND `/noreferrer/`.
    - Import `within` from `@testing-library/react` (add to existing import on line 7 — currently imports `render, screen, waitFor, act`).

    **K-16 (BROWSE_AVAILABLE blade — affiliateUrl empty → falls back to originalUrl):**
    - Setup: same as K-15 but `affiliateUrl: ''` and `originalUrl: 'https://emag.ro/it1'`; status='available'; lookup empty.
    - Render.
    - Locate blade root anchor via `within(screen.getByTestId('item-reserve-available')).getByRole('link', { name: /open product page at emag\.ro/i })`.
    - Assert `href === 'https://emag.ro/it1'`.
    - Assert `target === '_blank'` and `rel` includes both noopener + noreferrer.

    **K-17 (BROWSE_PURCHASED blade — both URLs empty → renders as <div>, not <a>):**
    - Setup: `itemsQueryMock.useItemsQuery.mockReturnValue({ data: [makeItem({ status: 'purchased', affiliateUrl: '', originalUrl: '' })] })`; lookup empty.
    - Render.
    - Within `screen.getByTestId('item-reserve-purchased')`, query for ANY link whose aria-label matches `/open product page/i` and assert it is null (i.e. no blade link rendered).
    - Sanity: the blade content still renders — assert the title 'Coffee Machine' is visible (`screen.getByText('Coffee Machine')`).
    - Sanity: the `[data-testid="item-reserve-purchased"]` container itself still mounts.

    **K-18 (BROWSE_RESERVED_BY_OTHER aria-label interpolates merchantDomain):**
    - Setup: `itemsQueryMock.useItemsQuery.mockReturnValue({ data: [makeItem({ status: 'reserved', affiliateUrl: 'https://altex.ro/aff', originalUrl: 'https://altex.ro/it1', merchantDomain: 'altex.ro' })] })`; lookup empty.
    - Render.
    - Locate the blade anchor via `within(screen.getByTestId('item-reserve-reserved-by-other')).getByRole('link', { name: /open product page at altex\.ro/i })`.
    - Assert that link exists AND that `link.getAttribute('aria-label') === 'Open product page at altex.ro'`.
    - Additional sanity: if `merchantDomain: null` were passed (cover in a single inline sub-assert OR skip — keep K-18 focused on the happy interpolation; the null case is implicit from K-17's '' URLs).

    **K-19 (reserved-by-me hero blade — active.affiliateUrl present → blade is anchor; nested time-to-purchase card still renders inside):**
    - Setup: rely on the `beforeEach` default — `ACTIVE_RES` (with `affiliateUrl: 'https://emag.ro/item1'`) is already returned by `reservationForItemMock`.
    - Render `renderPage()`.
    - Sanity precondition: `screen.getByTestId('item-reserve-detail')` exists (proves reserved-by-me branch is active).
    - Locate the blade card anchor: `within(screen.getByTestId('item-reserve-detail')).getByRole('link', { name: /open product page at emag\.ro/i })`.
    - Assert `href === 'https://emag.ro/item1'`.
    - Assert `target === '_blank'` and `rel` includes noopener + noreferrer.
    - Assert the nested time-to-purchase mmss element still renders WITHIN that anchor: `expect(within(bladeAnchor).getByTestId('reserve-detail-mmss')).toBeInTheDocument()` — proves the time card was NOT broken by the anchor wrap.
    - Assert the existing "Continue to retailer" CTA in the button row below STILL renders (regression guard for the unrelated lower CTA at lines 720-728): `screen.getAllByRole('link', { name: /continue.*emag/i })` returns at least 1 (this is the lower CTA — distinct from the blade anchor whose accessible name is "Open product page at ...").

    All 5 tests MUST fail when run against the unmodified source — they assert anchors that do not exist yet. RED is required before GREEN.
  </behavior>
  <action>
    1. Re-read `web/src/pages/__tests__/ItemReservePage.test.tsx` ENTIRELY (it is 801 lines) before editing. Verify the closing `})` of the `describe` block is on line 801 and the file ends on line 802. (If line numbers have drifted, locate `^})\s*$` followed by EOF and insert before it — do NOT rely on hard-coded line numbers.)

    2. Edit the import on line 7 from:
       `import { render, screen, waitFor, act } from '@testing-library/react'`
       to:
       `import { render, screen, waitFor, act, within } from '@testing-library/react'`

    3. Insert the 5 new tests AS A GROUP immediately before the closing `})` of the `describe('ItemReservePage', () => { ... })` block. Group them under a marker comment for easy navigation:
       ```ts
       // ---- quick-260516-lsi — clickable product blade on ItemReservePage ----
       ```
       Place this marker comment immediately after the K-14 closing `})` (the `activeMock.active = null` cleanup line at 799 is INSIDE K-14; the K-14 closing `})` is on line 800).

    4. Implement the 5 `it(...)` blocks per the `<behavior>` spec above. Each test:
       - Uses the existing `makeItem(overrides)` helper for items.
       - Uses `ACTIVE_RES` for the K-19 active reservation (already the default).
       - Does NOT redefine mocks — overrides only what the test needs via the mock return values.
       - Wraps the link query in `within(screen.getByTestId(...))` to scope to the relevant branch container.

    5. Run JUST the new tests to confirm they FAIL (RED) against the unmodified source:
       `cd web && npx vitest run src/pages/__tests__/ItemReservePage.test.tsx -t "K-15|K-16|K-17|K-18|K-19"`
       Expected: 5 failing (cannot find link / cannot find aria-label / etc.).

    6. Also run the full ItemReservePage test file to confirm the other 27 in-file tests are still green:
       `cd web && npx vitest run src/pages/__tests__/ItemReservePage.test.tsx`
       Expected: 27 passed, 5 failed = 32 total. (The full suite-wide count of 190 includes 27 from this file + 163 from other files; this command runs ONLY this file.)

    7. Commit RED via gsd-tools (per project commit convention):
       `node "/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/bin/gsd-tools.cjs" commit "test(quick-260516-lsi-01): add failing K-15..K-19 for clickable product blade" --files web/src/pages/__tests__/ItemReservePage.test.tsx`

    No source changes in Task 1. No i18n changes in Task 1. ONLY the test file.
  </action>
  <verify>
    <automated>cd /Users/victorpop/ai-projects/gift-registry/web && npx vitest run src/pages/__tests__/ItemReservePage.test.tsx -t "K-15|K-16|K-17|K-18|K-19" 2>&1 | tail -25</automated>
    Expected output: exactly 5 tests fail (RED). Pre-existing 27 tests in the file remain green when the full file is run (run separately as a sanity check, NOT required to gate Task 1 — the gating verify is the RED state of K-15..K-19).
  </verify>
  <done>
    - 5 new `it(...)` blocks (K-15..K-19) exist in `web/src/pages/__tests__/ItemReservePage.test.tsx`.
    - `within` is imported.
    - Marker comment `// ---- quick-260516-lsi — clickable product blade on ItemReservePage ----` precedes K-15.
    - Running `npx vitest run src/pages/__tests__/ItemReservePage.test.tsx -t "K-15|K-16|K-17|K-18|K-19"` shows 5 failures (RED).
    - Running full file shows 27 prior tests still pass.
    - Commit landed via gsd-tools with message `test(quick-260516-lsi-01): add failing K-15..K-19 for clickable product blade`.
  </done>
</task>

<task type="auto" tdd="true">
  <name>Task 2 (GREEN): Implement clickable blade + add blade_link_aria to 4 i18n files</name>
  <files>web/src/pages/ItemReservePage.tsx, web/i18n/en.json, web/i18n/ro.json, web/src/i18n/en.json, web/src/i18n/ro.json</files>
  <behavior>
    After Task 2, all 5 RED tests turn GREEN, all 27 pre-existing tests in `ItemReservePage.test.tsx` stay GREEN, and the full web suite (~190 → 195) is GREEN. `tsc --noEmit` produces no new errors.

    Behavioral contracts (verified by tests):
    - `ItemDetailHero` blade: when `item.affiliateUrl || item.originalUrl` is truthy (non-empty string), the root `<div className="flex flex-col sm:flex-row gap-5 p-5 bg-gm-paper rounded-gm-card border border-gm-line">` is wrapped in (or replaced by) an `<a href={item.affiliateUrl || item.originalUrl} target="_blank" rel="noopener noreferrer" aria-label={t('web_reserve.item_page.blade_link_aria', { retailer: item.merchantDomain ?? 'retailer' })}>` with consistent hover/focus affordance. When BOTH URLs are empty, the original `<div>` renders unchanged.
    - Reserved-by-me hero inner blade card (the `<div className="flex flex-col sm:flex-row gap-5 p-5 bg-gm-paperDeep rounded-gm-card border border-gm-line">` at lines ~656-701 in the current source): same wrap logic, but URL resolution is `active.affiliateUrl || item.originalUrl` (active does not carry originalUrl; the sibling `item` does). The nested time-to-purchase card (`reserve-detail-mmss`) stays INSIDE the anchor.
    - aria-label key: `web_reserve.item_page.blade_link_aria`, interpolation token `{{retailer}}`. EN: `"Open product page at {{retailer}}"`. RO: `"Deschide pagina produsului pe {{retailer}}"`. Retailer value: `item.merchantDomain ?? 'retailer'` (or `active.merchantDomain ?? 'retailer'` in the reserved-by-me variant — both resolve to the same string for the same item because both are derived from the same source doc).
  </behavior>
  <action>
    Execute the steps in order. Do NOT skip the source-scan precheck — it confirms the no-`<a>`-inside-`<a>` constraint.

    **Precheck (mandatory):**

    1. Re-read `web/src/pages/ItemReservePage.tsx` lines 534-567 (the `ItemDetailHero` helper). Confirm:
       - The blade root is a single `<div>` opened at line 536 and closed at line 565.
       - No `<a>`, `<Link>`, `<button>`, or other interactive element is nested inside (only `<img>`, `<Pill>`, `<MonoCaption>`, `<h2>`, `<div>`, `<span>` — all non-interactive at runtime).
       - Wrapping this `<div>` in an `<a>` is HTML-valid.

    2. Re-read `web/src/pages/ItemReservePage.tsx` lines 654-701 (the reserved-by-me hero inner blade card and its nested time-to-purchase card). Confirm:
       - The blade card opens at line 656 and closes at line 701.
       - Inside: `<img>` at 659, `<Pill>` at 668, `<h2>` at 669, a price/retailer `<div>` at 672 (contains only text + `<span>` + `<strong>`), and the nested time-to-purchase `<div>` at 686 (contains `<MonoCaption>`, `<span>`, `<div>` — all non-interactive).
       - No `<a>`, `<Link>`, or `<button>` inside the card. Wrapping the card `<div>` in an `<a>` is HTML-valid.
       - The "Continue to retailer" anchor at lines 720-728 sits OUTSIDE the card (sibling under `<div className="flex flex-col gap-5">` at line 654). It is NOT affected by the blade wrap and stays unchanged.
       - The `<ConfirmPurchaseBanner>` and the Release `<Btn>` row are also siblings — not children of the blade card. Confirmed.

       If EITHER scan reveals an interactive child (current OR a defensive-coding concern), STOP and flag — but per the source as of 2026-05-16, both scans pass cleanly.

    **Step A — Add the i18n key to all 4 files (parity convention).**

    For EACH of the 4 files below, add `"blade_link_aria": "..."` as a new key under `web_reserve.item_page`, placed AFTER the existing `notes_label` key (so it is the new last key under `item_page`). Use the Edit tool — locate the `"notes_label": "..."` line, change its closing `"` to `",`, and append the new key on the next line.

    - `web/i18n/en.json` (line 137: `"notes_label": "From the registry owner"`) → add:
      `"blade_link_aria": "Open product page at {{retailer}}"`
    - `web/i18n/ro.json` (line 137: `"notes_label": "De la persoana care a creat lista"`) → add:
      `"blade_link_aria": "Deschide pagina produsului pe {{retailer}}"`
    - `web/src/i18n/en.json` (line 160: `"notes_label": "From the registry owner"`) → add:
      `"blade_link_aria": "Open product page at {{retailer}}"`
    - `web/src/i18n/ro.json` (line 160: `"notes_label": "De la persoana care a creat lista"`) → add:
      `"blade_link_aria": "Deschide pagina produsului pe {{retailer}}"`

    NOTE: line numbers above are as-of read on 2026-05-16; if they drift, locate by string `notes_label`. Validate each JSON file remains parseable: `cd /Users/victorpop/ai-projects/gift-registry && for f in web/i18n/en.json web/i18n/ro.json web/src/i18n/en.json web/src/i18n/ro.json; do node -e "JSON.parse(require('fs').readFileSync('$f','utf8'))" && echo "OK $f"; done`. All four MUST print OK.

    **Step B — Patch `ItemDetailHero` (browse blades).**

    Locate the function at line 534 in `web/src/pages/ItemReservePage.tsx`. Replace the body so the root `<div>` is conditionally wrapped in an `<a>`. Approach (minimal-diff, readable):

    1. Compute the URL + decide wrap at the top of the function body:
       ```tsx
       const href = item.affiliateUrl || item.originalUrl || null
       const retailer = item.merchantDomain ?? 'retailer'
       ```

    2. Extract the existing root `<div>` contents into a JSX expression (or keep inline — author's choice). Then render conditionally:
       ```tsx
       const bladeClasses = "flex flex-col sm:flex-row gap-5 p-5 bg-gm-paper rounded-gm-card border border-gm-line"
       const bladeContent = (<>... existing children of the root div, unchanged ...</>)
       if (href) {
         return (
           <a
             href={href}
             target="_blank"
             rel="noopener noreferrer"
             aria-label={t('web_reserve.item_page.blade_link_aria', { retailer })}
             className={`${bladeClasses} block cursor-pointer hover:border-gm-accent transition-colors focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gm-accent no-underline text-inherit`}
           >
             {bladeContent}
           </a>
         )
       }
       return <div className={bladeClasses}>{bladeContent}</div>
       ```

    3. The added classes do exactly four things:
       - `block` — forces the inline `<a>` to behave as a block-level flex container (the children expect flex layout).
       - `cursor-pointer` — visual affordance.
       - `hover:border-gm-accent transition-colors` — accent-stroke hover, consistent with Pill `accent` tone idiom (border-only change, bg untouched, won't fight the card aesthetic).
       - `focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gm-accent` — keyboard a11y, matches Btn.tsx line 58 and StickyReserveBanner anchor line 169 exactly.
       - `no-underline text-inherit` — anchors default to blue + underline; this is a card-shaped click target, not a text link. Suppress both. (Vite + Tailwind: `no-underline` maps to `text-decoration: none`; `text-inherit` keeps child colors.)

    4. Do NOT change any inner JSX — the `<img>`, `<Pill>`, `<MonoCaption>`, `<h2>`, price div all render unchanged.

    **Step C — Patch the reserved-by-me hero inner blade card.**

    Locate the blade card `<div>` at line 656 in `renderReservedByMeDetail`. Apply the same conditional-wrap pattern, but with these differences:

    1. URL source: `active.affiliateUrl || item.originalUrl || null` (because `active` does not carry `originalUrl` — see interfaces block). `item` is in scope as the sibling param.
    2. Retailer: `active.merchantDomain ?? item.merchantDomain ?? 'retailer'` (active typically carries the same merchantDomain; defensive fallback).
    3. Existing class string: `"flex flex-col sm:flex-row gap-5 p-5 bg-gm-paperDeep rounded-gm-card border border-gm-line"` (note the `bg-gm-paperDeep` — DIFFERENT from the ItemDetailHero `bg-gm-paper`; preserve as-is, do not normalize).
    4. Append the same affordance classes: `block cursor-pointer hover:border-gm-accent transition-colors focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gm-accent no-underline text-inherit`.
    5. The nested time-to-purchase card (lines 686-699) stays INSIDE the anchor. K-19 asserts this.
    6. The Release Btn (line 708) and the "Continue to retailer" anchor (line 720) are SIBLINGS of the blade card — they stay OUTSIDE the new `<a>` wrap. Confirmed by precheck.

    Same code shape as Step B — compute `const href = active.affiliateUrl || item.originalUrl || null`, conditionally render `<a>` vs `<div>`.

    **Step D — Verify.**

    1. TypeScript: `cd /Users/victorpop/ai-projects/gift-registry/web && npx tsc --noEmit 2>&1 | tail -20`. Expected: no new errors (any pre-existing errors are unrelated — but a clean run is the target).

    2. Full ItemReservePage test file: `cd /Users/victorpop/ai-projects/gift-registry/web && npx vitest run src/pages/__tests__/ItemReservePage.test.tsx 2>&1 | tail -15`. Expected: 32 passed, 0 failed (27 pre-existing + 5 new).

    3. Full web test suite (CI mode): `cd /Users/victorpop/ai-projects/gift-registry/web && npm test -- --run 2>&1 | tail -25`. Expected: 195 passed (190 baseline + 5 new K-15..K-19), 0 failed. (If the baseline number drifts due to unrelated work, the assertion is: pre-existing count + 5.)

    4. JSON validity (re-run from Step A): all four JSON files parse OK.

    **Step E — Commit.**

    `node "/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/bin/gsd-tools.cjs" commit "fix(quick-260516-lsi-02): make ItemReservePage product blade clickable to retailer" --files web/src/pages/ItemReservePage.tsx web/i18n/en.json web/i18n/ro.json web/src/i18n/en.json web/src/i18n/ro.json`
  </action>
  <verify>
    <automated>cd /Users/victorpop/ai-projects/gift-registry/web && npx tsc --noEmit 2>&1 | tail -5 && echo "---" && npx vitest run src/pages/__tests__/ItemReservePage.test.tsx 2>&1 | tail -10 && echo "---" && npm test -- --run 2>&1 | tail -10</automated>
    Expected:
    - `tsc --noEmit`: no new errors.
    - ItemReservePage file: 32 passed, 0 failed.
    - Full web suite: 195 passed (190 baseline + 5 new), 0 failed.
  </verify>
  <done>
    - `web/src/pages/ItemReservePage.tsx`: `ItemDetailHero` and the reserved-by-me hero inner blade card both conditionally render as `<a target="_blank" rel="noopener noreferrer" aria-label={t('web_reserve.item_page.blade_link_aria', { retailer })}>` when a URL is available, and as the original static `<div>` otherwise. Hover affordance is `hover:border-gm-accent`; focus-visible ring matches existing Btn/anchor pattern.
    - `web/i18n/en.json`, `web/i18n/ro.json`, `web/src/i18n/en.json`, `web/src/i18n/ro.json` each contain a new `blade_link_aria` key under `web_reserve.item_page`. All 4 files parse as valid JSON.
    - K-15..K-19 pass; pre-existing 27 in-file tests pass; full web suite passes at 195 tests (or whatever pre-existing baseline + 5).
    - `tsc --noEmit` is clean (no new errors).
    - Commit landed via gsd-tools with message `fix(quick-260516-lsi-02): make ItemReservePage product blade clickable to retailer`.
    - No backend / app / functions files touched. No new components / hooks / files created.
  </done>
</task>

</tasks>

<verification>
After Task 2 completes, run a final cross-check:

```bash
# 1. Confirm no untouched directories were modified.
cd /Users/victorpop/ai-projects/gift-registry && git status --short | grep -vE '^(M|A|\?\?)\s+(web/|.planning/)' | tee /tmp/lsi-stray.txt
# Expected: empty output (no files outside web/ or .planning/ modified).

# 2. Confirm exactly the 6 expected files changed (5 source + this PLAN).
cd /Users/victorpop/ai-projects/gift-registry && git diff --name-only main -- 'web/src/pages/ItemReservePage.tsx' 'web/src/pages/__tests__/ItemReservePage.test.tsx' 'web/i18n/en.json' 'web/i18n/ro.json' 'web/src/i18n/en.json' 'web/src/i18n/ro.json'
# Expected: all 5 source files listed (the PLAN.md lives under .planning/quick/).

# 3. Re-run final test totals.
cd /Users/victorpop/ai-projects/gift-registry/web && npm test -- --run 2>&1 | tail -10
# Expected: 195 passed (or baseline + 5), 0 failed.
```

Goal-backward truth check (each truth from `must_haves.truths` is verified by a test):
- Click opens retailer in new tab → K-15 (target/rel) + K-19 (target/rel on reserved-by-me).
- affiliateUrl preferred → K-15.
- originalUrl fallback → K-16.
- Graceful non-link render → K-17.
- aria-label interpolation → K-18 (and asserted in K-15, K-19 via `getByRole('link', { name: ... })`).
- All four i18n bundles have the key → tests fail if missing because `t('web_reserve.item_page.blade_link_aria', { retailer: 'emag.ro' })` returns the key path verbatim (e.g. `"web_reserve.item_page.blade_link_aria"`) and the regex `/open product page at emag\.ro/i` wouldn't match.
- Tests stay green at 195 → final `npm test -- --run` in Step D.
</verification>

<success_criteria>
- Both product blades on `ItemReservePage.tsx` are clickable when a URL is available (affiliateUrl preferred, originalUrl fallback) and render as a static `<div>` when both are empty.
- All 5 new tests (K-15..K-19) pass; all 190 pre-existing tests in the web suite stay green; total 195 (or baseline + 5 if the baseline drifts).
- `cd web && npx tsc --noEmit` is clean.
- New i18n key `web_reserve.item_page.blade_link_aria` exists in all 4 i18n files (EN + RO × `web/i18n/` + `web/src/i18n/`) with the exact strings: EN `"Open product page at {{retailer}}"`, RO `"Deschide pagina produsului pe {{retailer}}"`.
- No HTML-invalidity introduced (no `<a>` nested inside another `<a>` — confirmed via the precheck source scan in Task 2 step 1-2).
- D-06 unchanged (no reserver/giver name/email rendered on the page in any state — no change to that contract).
- Only 5 source files + the PLAN.md changed. No app/, functions/, registry tile, or other component touched.
</success_criteria>

<output>
After completion, create `.planning/quick/260516-lsi-make-product-blade-on-itemreservepage-cl/260516-lsi-SUMMARY.md` summarizing:
- The 2 commits that landed (RED tests, GREEN impl).
- Final test count.
- The exact files touched (count: 5).
- Confirmation that the no-`<a>`-inside-`<a>` source scan came back clean.
- Append a row to `.planning/STATE.md`'s recent-activity ledger (Verified) per the existing convention (see lbf / ku3 / k4f rows in the head of STATE.md as of 2026-05-16).
</output>
