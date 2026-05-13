---
phase: quick-260513-fnm
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - web/i18n/en.json
  - web/i18n/ro.json
  - web/src/features/reservation/ReserveDetailSection.tsx
  - web/src/pages/RegistryPage.tsx
autonomous: false
requirements:
  - QUICK-FNM-01
must_haves:
  truths:
    - "Giver viewing the active reservation purchase-step card sees a secondary CTA labelled 'Check other products in the registry' (EN) / 'Vezi alte produse din registru' (RO)."
    - "Clicking the CTA smooth-scrolls the page down to the registry item list (the 'The list' section heading), so the giver lands on the item grid and can reserve more gifts."
    - "No new route is introduced — the action stays on /registry/:id."
    - "No hardcoded strings are introduced — both EN and RO copies live in web/i18n/{en,ro}.json under the existing web_reserve namespace."
    - "The CTA uses the existing Btn primitive (ghost variant) — no new visual primitive is introduced."
  artifacts:
    - path: web/i18n/en.json
      provides: "EN translation key web_reserve.check_other_products_cta"
      contains: "check_other_products_cta"
    - path: web/i18n/ro.json
      provides: "RO translation key web_reserve.check_other_products_cta"
      contains: "check_other_products_cta"
    - path: web/src/features/reservation/ReserveDetailSection.tsx
      provides: "Renders the secondary 'Check other products' CTA below ConfirmPurchaseBanner inside the active-reservation detail section."
      contains: "check_other_products_cta"
    - path: web/src/pages/RegistryPage.tsx
      provides: "Stable scroll anchor (id='registry-list-section') on the 'The list' section so the CTA has a target."
      contains: "registry-list-section"
  key_links:
    - from: web/src/features/reservation/ReserveDetailSection.tsx
      to: web/src/pages/RegistryPage.tsx
      via: "document.getElementById('registry-list-section').scrollIntoView({ behavior: 'smooth', block: 'start' })"
      pattern: "registry-list-section"
    - from: web/src/features/reservation/ReserveDetailSection.tsx
      to: web/i18n/en.json
      via: "useTranslation().t('web_reserve.check_other_products_cta')"
      pattern: "check_other_products_cta"
---

<objective>
Add a secondary CTA on the web giver-facing reservation purchase-step screen — labelled "Check other products in the registry" — that smooth-scrolls the page back down to the registry item list so the giver can keep reserving gifts.

Purpose: After reserving an item, many givers want to add more gifts for the same recipient, but the existing purchase-step UI (Release / Continue to retailer / I completed the purchase) gives no path back to the item grid. This is a no-friction nudge to keep browsing.

Output: One new i18n key (EN + RO), a stable scroll anchor on the registry list, and a ghost-variant Btn rendered below the ConfirmPurchaseBanner inside the active-reservation detail section.

Investigation outcome (from codebase reading, NOT a future TODO):
- web/src/App.tsx routes /registry/:id to a SINGLE page component (RegistryPage). The purchase-step UI (`ReserveDetailSection`) and the item grid live on the SAME route — `RegistryPage` renders `ReserveDetailSection` ABOVE the item grid when `active` reservation is non-null. → The correct approach is SMOOTH-SCROLL, not navigation.
- The existing reserved-by-me click handler already uses the smooth-scroll pattern (`document.getElementById('reserve-detail-section').scrollIntoView({ behavior: 'smooth', block: 'start' })` at RegistryPage.tsx:54–55). We mirror that pattern in the opposite direction, scrolling DOWN to a new `id="registry-list-section"` anchor on the "The list" section.
- Placement: directly below the ConfirmPurchaseBanner ("I completed the purchase") inside the left/main column of `ReserveDetailSection`. This is the spot where a giver who isn't ready to confirm purchase yet is most likely to look for "what else can I do here?".
- Styling: existing `Btn` primitive with `variant="ghost"` size="md" — the only existing variant that fits a low-emphasis secondary CTA on the paperDeep surface (primary=ink, accent=already used by confirm, quiet=for dark backgrounds only).
</objective>

<execution_context>
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/workflows/execute-plan.md
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@CLAUDE.md
@web/src/App.tsx
@web/src/pages/RegistryPage.tsx
@web/src/features/reservation/ReserveDetailSection.tsx
@web/src/features/reservation/ConfirmPurchaseBanner.tsx
@web/src/components/giftmaison/Btn.tsx
@web/i18n/en.json
@web/i18n/ro.json

<interfaces>
<!-- Key contracts extracted from the codebase — executor should use these directly. -->

From web/src/components/giftmaison/Btn.tsx:
```typescript
export type BtnVariant = 'primary' | 'accent' | 'ghost' | 'quiet'
export type BtnSize = 'sm' | 'md' | 'lg'
export interface BtnProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: BtnVariant
  size?: BtnSize
  icon?: ReactNode
  children: ReactNode
}
// Renders <button>. ghost = transparent bg / ink fg / line border. md = 18/11 px, 13.5 px text.
// import: import { Btn } from '../../components/giftmaison'  (already imported in ReserveDetailSection)
```

From web/src/pages/RegistryPage.tsx (existing smooth-scroll precedent, lines 49–57):
```typescript
const renderReservedByMeClick = useCallback((item: Item) => {
  if (!effectiveEmail) return undefined
  if (item.status !== 'reserved') return undefined
  if (item.reservedBy !== effectiveEmail) return undefined
  return () => {
    const el = document.getElementById('reserve-detail-section')
    if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}, [effectiveEmail])
// MIRROR this pattern in ReserveDetailSection — target id will be 'registry-list-section'.
// prefers-reduced-motion: browsers downgrade behavior:'smooth' to instant automatically (no extra code needed).
```

From web/src/features/reservation/ReserveDetailSection.tsx (current shape, lines 51–119):
- Outermost `<section id="reserve-detail-section" className="bg-gm-paperDeep border-b border-gm-line">`.
- Inner grid: `grid grid-cols-1 lg:grid-cols-[1fr_340px] gap-8 items-start`.
- LEFT column (`<div className="flex flex-col gap-5">`) already contains:
    1. Reserved item card
    2. `<ConfirmPurchaseBanner reservationId={active.reservationId} minutesLeft={minutesLeft} />`
- RIGHT column: `<HowTimerWorks retailer={retailer} />` (desktop sidebar).
- Insertion point for the new CTA: a sibling AFTER `<ConfirmPurchaseBanner ... />` and BEFORE the closing `</div>` of the left column.

From web/src/pages/RegistryPage.tsx (registry list section, lines 230–261):
- The section that contains the item grid currently has `<section className="px-4 sm:px-7 lg:px-10 pt-8 max-w-7xl mx-auto w-full">` with NO id.
- Add `id="registry-list-section"` to that `<section>` opening tag (NEXT to the existing className) — this is the stable scroll target.

From web/i18n/en.json and ro.json (web_reserve namespace already exists at line 87):
- All purchase-step strings live under `web_reserve.*`.
- New key MUST be added to BOTH en.json and ro.json under the same namespace.
</interfaces>
</context>

<tasks>

<task type="auto">
  <name>Task 1: Add i18n key + scroll anchor + secondary CTA (single coordinated change)</name>
  <files>
    web/i18n/en.json
    web/i18n/ro.json
    web/src/pages/RegistryPage.tsx
    web/src/features/reservation/ReserveDetailSection.tsx
  </files>
  <action>
    Make four coordinated edits — all needed for the CTA to work end-to-end.

    1) web/i18n/en.json — inside the existing `web_reserve` object (after the
       `how_timer_step4_b` key, before the closing `}` of `web_reserve`), add:

           "check_other_products_cta": "Check other products in the registry"

       Mind the trailing comma on the previous key — `how_timer_step4_b`'s line
       must end with `,` once this key is appended.

    2) web/i18n/ro.json — symmetric change in the `web_reserve` object:

           "check_other_products_cta": "Vezi alte produse din registru"

       (Romanian translation. Keep the same key path so `t('web_reserve.check_other_products_cta')` resolves in both locales.)

    3) web/src/pages/RegistryPage.tsx — at the line that currently reads:

           {/* Section title + filter chips */}
           <section className="px-4 sm:px-7 lg:px-10 pt-8 max-w-7xl mx-auto w-full">

       (around line 230), add `id="registry-list-section"` as an attribute on
       that `<section>`. Final tag:

           <section id="registry-list-section" className="px-4 sm:px-7 lg:px-10 pt-8 max-w-7xl mx-auto w-full">

       Do NOT change anything else in this file. The id is the ONLY scroll
       anchor target — keep it stable (no scroll-margin override needed; the
       sticky banner at the top is already z-30 and sits over the page chrome,
       not over this section).

    4) web/src/features/reservation/ReserveDetailSection.tsx — inside the left
       column `<div className="flex flex-col gap-5">` (around line 70), AFTER
       the `<ConfirmPurchaseBanner ... />` element (around line 111) and BEFORE
       the closing `</div>` of the left column (around line 112), insert a
       wrapper div with the new ghost-variant Btn. The Btn must:

         - Use the existing `Btn` import from '../../components/giftmaison'
           (already imported on line 7 — do NOT re-import).
         - variant="ghost", size="md".
         - onClick handler: locate `document.getElementById('registry-list-section')`
           and call `scrollIntoView({ behavior: 'smooth', block: 'start' })` on
           it. Guard against null (the anchor only exists on RegistryPage; if
           it's missing, do nothing — no throw, no console.error).
         - Children: `{t('web_reserve.check_other_products_cta')}` (useTranslation()'s
           `t` is already destructured on line 32 — do NOT re-destructure).
         - data-testid="check-other-products-cta" for the verification test.

       Suggested markup (insert as a sibling AFTER `<ConfirmPurchaseBanner ... />`):

           <div className="flex justify-center sm:justify-start">
             <Btn
               variant="ghost"
               size="md"
               data-testid="check-other-products-cta"
               onClick={() => {
                 const el = document.getElementById('registry-list-section')
                 if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' })
               }}
             >
               {t('web_reserve.check_other_products_cta')}
             </Btn>
           </div>

       Why ghost (not primary or accent): the ConfirmPurchaseBanner directly
       above this CTA already uses `variant="accent"` for "I completed the
       purchase" (the primary action). The sticky banner above uses `accent`
       for "Continue to retailer". A third coloured CTA would compete visually.
       `ghost` (transparent bg, ink fg, line border) reads as a low-emphasis
       secondary action — matching the project's existing pattern (the auth
       screen Skip and Sign-in CTAs also use ghost).

       Why center/start alignment: mirrors the natural flow of the
       ConfirmPurchaseBanner content above it (centered on mobile via stacked
       layout, left-aligned on tablet+).

       Do NOT introduce a new import, a new component file, or any styling
       outside the existing Btn primitive. Do NOT add prefers-reduced-motion
       handling — browsers already downgrade `behavior:'smooth'` automatically
       when the user has reduce-motion enabled.
  </action>
  <verify>
    <automated>cd web && npm run typecheck && npm run build</automated>
  </verify>
  <done>
    - `web/i18n/en.json` contains the key path `web_reserve.check_other_products_cta` with value "Check other products in the registry".
    - `web/i18n/ro.json` contains the same key path with value "Vezi alte produse din registru".
    - Both JSON files remain valid JSON (no trailing comma errors, no syntax breakage — `npm run build` will fail if either is malformed).
    - `web/src/pages/RegistryPage.tsx` has `id="registry-list-section"` on the section wrapping the section title + FilterChips + ItemGrid.
    - `web/src/features/reservation/ReserveDetailSection.tsx` renders a `Btn` with `data-testid="check-other-products-cta"`, `variant="ghost"`, inside the left column AFTER ConfirmPurchaseBanner.
    - `npm run typecheck` passes (no TS errors introduced).
    - `npm run build` succeeds (locked-in proof that i18n JSON is valid AND React tree compiles).
  </done>
</task>

<task type="checkpoint:human-verify" gate="blocking">
  <name>Task 2: Human verification — visual placement and scroll behaviour</name>
  <what-built>
    A new "Check other products in the registry" ghost-styled secondary CTA
    rendered below the "I completed the purchase" card on the active-reservation
    purchase-step screen. Clicking it smooth-scrolls the page down to the
    registry item list. Both EN and RO strings are localized.
  </what-built>
  <how-to-verify>
    1. Start the web dev server: `cd web && npm run dev` (Vite usually serves on http://localhost:5173).
    2. In a separate terminal, start the Firebase emulator suite if your local dev workflow normally needs it (e.g. `firebase emulators:start`) — only if needed to make a reservation flow work.
    3. Open a public registry URL that has at least 2–3 available items, e.g. http://localhost:5173/registry/{some-registry-id} (use an existing emulator-seeded registry or the one you normally test against).
    4. Reserve one item (Reserve this gift → Continue as guest → fill in name/email → submit). The page should now show:
         - StickyReserveBanner at the top (with Release + Continue to retailer)
         - ReserveDetailSection (with the reserved item card + "I completed the purchase" card)
         - The registry item list below
    5. Confirm the NEW button "Check other products in the registry" is visible directly BELOW the "I completed the purchase" accent card, styled as a ghost-pill (ink text, line border, transparent background). It should be visually subordinate to "I completed the purchase".
    6. Click the new button. The page should SMOOTH-SCROLL down so the "The list" section heading (or close to it) is visible near the top of the viewport. The reservation banner stays sticky at the top.
    7. Toggle the language to RO via the EN/RO switcher in the top nav. The button label should change to "Vezi alte produse din registru". Click again to verify the same scroll behaviour.
    8. With reduce-motion enabled (system Accessibility setting), the scroll should be near-instant (no smooth animation) — proof we did not break the OS preference.
    9. Confirm no visual regressions: the existing Release, Continue to retailer, and I completed the purchase CTAs still work normally.
  </how-to-verify>
  <resume-signal>Type "approved" once visual placement, scroll behaviour, and RO copy are all confirmed. Describe any issues otherwise.</resume-signal>
</task>

</tasks>

<verification>
- `cd web && npm run typecheck` — passes with no new errors.
- `cd web && npm run build` — succeeds; proves both i18n JSON files are well-formed and the React tree compiles.
- Human verification (Task 2) confirms placement, ghost styling, smooth-scroll behaviour, and RO translation render correctly.
</verification>

<success_criteria>
- Giver on the active-reservation purchase-step screen has a clear, visible, secondary CTA to go back to the registry list.
- The CTA scrolls (does not navigate) — confirmed by code reading: same route, anchor lives on the same page.
- EN + RO copy added under the existing `web_reserve` namespace; no hardcoded strings.
- No new dependencies; uses existing Btn primitive (ghost variant) and existing scrollIntoView pattern already used in RegistryPage.
- `npm run typecheck` and `npm run build` pass.
- Human approves visual placement and scroll behaviour in Task 2.
</success_criteria>

<output>
After completion, create `.planning/quick/260513-fnm-add-check-other-products-in-the-registry/260513-fnm-SUMMARY.md` describing the four edits, the chosen placement, the rationale for ghost variant, and the human-verify outcome.
</output>
