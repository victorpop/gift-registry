---
phase: 13-web-fallback-visual-refresh
plan: 01
subsystem: ui
tags: [react, tailwind, typescript, giftmaison, atoms, design-system]

# Dependency graph
requires:
  - phase: 13-web-fallback-visual-refresh
    provides: "Plan 00 — gm.* Tailwind tokens (paper, ink, accent, accentSoft, ...), font-display/body/mono, animate-gm-pulse, rounded-gm-*, --gm-* CSS custom properties"
provides:
  - "6 GiftMaison atom React components in web/src/components/giftmaison/ (Wordmark, Pill, Btn, Field, PulseDot, MonoCaption)"
  - "Barrel index.ts re-exporting all atoms with their public TypeScript types"
  - "Pure Tailwind-utility styling — no inline rgb/hex except documented bg-[oklch(...)] arbitrary values for ok/warn pill tones"
  - "react-hook-form-compatible Field via forwardRef"
  - "Btn variant matrix (4) × size matrix (3) ready for chrome (Plan 02) and screens (Plans 04/05/06)"
  - "PulseDot consuming the shared gm-pulse keyframe with reduced-motion fallback"
affects: [13-02-chrome, 13-04-registry-page, 13-05-reservation-modals, 13-06-auth-screen, 13-07-final-polish]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Atom-level pure-presentational components — no useState/useEffect/useTranslation; parents pass i18n strings as children"
    - "className override prop on every atom for layout-level customisation"
    - "All gm.* token consumption via Tailwind utilities (bg-gm-*, text-gm-*, border-gm-*) — never inline"
    - "Arbitrary-value px sizing where spec is precise to the px (text-[22px], px-[18px] py-[11px], rounded-[10px])"
    - "Box-shadow halos pointing at CSS variables (var(--gm-accentSoft)) so v1.2 occasion cascade swaps colour without rewriting classes"
    - "forwardRef on form atoms (Field) for react-hook-form register() spread compatibility"
    - "Tone-class object pattern for variant matrices (variantClasses[variant], toneClasses[tone], sizeClasses[size]) joined into single className string"
    - "focus-visible:outline-* (UI-SPEC accessibility row) replaces Phase 5 focus:ring-* on new atoms"

key-files:
  created:
    - "web/src/components/giftmaison/Wordmark.tsx"
    - "web/src/components/giftmaison/Pill.tsx"
    - "web/src/components/giftmaison/Btn.tsx"
    - "web/src/components/giftmaison/Field.tsx"
    - "web/src/components/giftmaison/PulseDot.tsx"
    - "web/src/components/giftmaison/MonoCaption.tsx"
    - "web/src/components/giftmaison/index.ts"
  modified: []

key-decisions:
  - "All 6 atoms shipped as pure-presentational components with no hooks — keeps them framework-light and trivially memoizable by callers"
  - "Pill ok/warn tones use Tailwind 3.4 arbitrary value bg-[oklch(0.94_0.04_150)] / bg-[oklch(0.95_0.04_70)] because UI-SPEC explicitly notes these are computed values not in the gm.* palette"
  - "PulseDot halo uses shadow-[0_0_0_4px_var(--gm-accentSoft)] (and 5px for size 10) pointing at the CSS variable directly — this lets the v1.2 occasion cascade swap halo colour automatically when --gm-accentSoft is reassigned at the registry-detail root"
  - "Btn renders <button> only — link-styled CTAs (e.g. 'Continue to {retailer}') in Plan 04 will reuse the same className recipe on a sibling <a target='_blank'> rather than overloading Btn into a polymorphic component"
  - "Field uses forwardRef so react-hook-form's register('field') spread can drive setFocus and surface the inner <input> ref to the consumer"
  - "Btn intentionally drops the Phase 5 focus:ring-2 focus:ring-primary pattern in favour of focus-visible:outline-2 outline-gm-accent + 2 px offset — UI-SPEC accessibility row mandates this; existing Phase 5 buttons (AuthModal etc.) keep the legacy focus ring until they are migrated in Plan 06"
  - "Btn padding values (px-3 py-[7px] / px-[18px] py-[11px] / px-[22px] py-[14px]) taken verbatim from web-screens.jsx Btn reference — UI-SPEC explicitly authorises these as component-internal exceptions to the ×4 spacing scale"

patterns-established:
  - "GiftMaison atoms namespace at web/src/components/giftmaison/ with a single barrel index.ts re-exporting components + their published types"
  - "Variant + size + tone matrices implemented as Record<Type, string> objects of Tailwind class strings, joined into the final className with spread + array.join(' ')"
  - "Arbitrary-value px sizing is the standard for any handoff-mandated dimension precise to the px (text-[14px], px-[22px], rounded-[10px], shadow-[0_0_0_4px_...])"
  - "Halo / glow effects via box-shadow arbitrary value referencing CSS custom properties — D-07 indirection preserved at the atom layer"

requirements-completed: [D-13, D-16, D-08]

# Metrics
duration: 3min
completed: 2026-05-07
---

# Phase 13 Plan 01: GiftMaison Atom Layer Summary

**Six pure-Tailwind React atoms (Wordmark, Pill, Btn, Field, PulseDot, MonoCaption) shipped to web/src/components/giftmaison/ with barrel export, ready for chrome and screen consumers.**

## Performance

- **Duration:** ~3 min
- **Started:** 2026-05-07T19:59:09Z
- **Completed:** 2026-05-07T20:01:50Z
- **Tasks:** 2
- **Files created:** 7

## Accomplishments

- 6 GiftMaison atom components shipped in `web/src/components/giftmaison/` — each TypeScript-typed, Tailwind-only, accepting `className` for layout overrides
- Wordmark renders italic Instrument Serif `giftmaison.` with the terminal period in `gm.accent` (CONTEXT D-13) — size variants 22 px (default) / 24 px (top nav) + optional `withTag` mono-caps tagline
- Pill ships with 5 tones (neutral / accent / second / ok / warn) × 2 sizes (sm 11 px / md 12 px) with mono-caps uppercase styling, 0.3 px letter-spacing, full pill radius (CONTEXT D-16)
- Btn ships with 4 variants (primary / accent / ghost / quiet) × 3 sizes (sm 7/12 / md 11/18 / lg 14/22 padding) with focus-visible outline ring on `gm.accent` and disabled-state opacity guard
- Field uses `forwardRef<HTMLInputElement, FieldProps>` so react-hook-form's `register('email')` spread compiles directly; supports prefix/suffix/error/autofilled slots and the 1.5 px `gm.accent` focus-within border
- PulseDot renders 8 px (status pill / in-card banner) or 10 px (sticky banner) circle in `gm.accent` with an `accentSoft` halo pointing at `var(--gm-accentSoft)` and the shared `animate-gm-pulse` utility (D-11)
- MonoCaption is a generic mono-caps span with 3 sizes (10/11/12 px) × 2 tones (soft `inkSoft` / faint `inkFaint`) for labels, meta, and pill copy reuse
- Barrel `index.ts` re-exports all 6 atoms + their public types so consumers can `import { Wordmark, Pill, Btn, Field, PulseDot, MonoCaption } from '../components/giftmaison'`

## Task Commits

Each task was committed atomically:

1. **Task 1: Build Wordmark, Pill, MonoCaption, PulseDot atoms** — `6d9641b` (feat)
2. **Task 2: Build Btn + Field atoms + barrel index export** — `df19ebd` (feat)

## Files Created/Modified

- `web/src/components/giftmaison/Wordmark.tsx` — italic Instrument Serif wordmark with terracotta period (CONTEXT D-13)
- `web/src/components/giftmaison/Pill.tsx` — 5-tone × 2-size mono-caps pill (CONTEXT D-16)
- `web/src/components/giftmaison/MonoCaption.tsx` — generic mono-caps caption span with size + tone variants
- `web/src/components/giftmaison/PulseDot.tsx` — 8/10 px pulsing accent dot with halo (D-11, UI-SPEC Animation)
- `web/src/components/giftmaison/Btn.tsx` — 4-variant × 3-size pill button with focus-visible outline + disabled state
- `web/src/components/giftmaison/Field.tsx` — forwardRef'd form field with mono-caps label, prefix/suffix/error/autofilled slots, react-hook-form compatible
- `web/src/components/giftmaison/index.ts` — barrel re-export for all atoms + their public TypeScript types

## Decisions Made

- All 6 atoms shipped as pure-presentational components with no hooks — keeps them framework-light, trivially memoizable, and stylable from the call site
- Pill `ok`/`warn` tones use Tailwind 3.4 arbitrary `bg-[oklch(...)]` values — UI-SPEC explicitly classifies these as computed colours that intentionally sit outside the `gm.*` palette
- PulseDot halo uses `shadow-[0_0_0_Npx_var(--gm-accentSoft)]` so v1.2 occasion cascade can swap the halo colour by reassigning the CSS custom property at the registry-detail root, with zero atom-level changes
- Btn renders `<button>` only — link-styled CTAs in Plan 04 will reuse the same className recipe on a sibling `<a>` rather than making Btn polymorphic
- Field uses `forwardRef` so react-hook-form's `register('field')` spread surfaces the inner input ref to consumers (needed for `setFocus`)
- Btn drops Phase 5's `focus:ring-2 focus:ring-primary` and adopts `focus-visible:outline-2 outline-gm-accent` per UI-SPEC accessibility row — Phase 5 components retain legacy ring until migrated in Plan 06
- Btn padding values (`px-3 py-[7px]`, `px-[18px] py-[11px]`, `px-[22px] py-[14px]`) taken verbatim from `web-screens.jsx` reference — UI-SPEC authorises these as component-internal exceptions to the ×4 spacing scale

## Deviations from Plan

### Coordination Note (Parallel Execution)

**1. [Coordination — not a deviation rule] Barrel `index.ts` extended by parallel Plan 02 agent**
- **Found during:** Task 2 (after writing the 6-line barrel)
- **Observation:** A parallel agent executing Plan 13-02 (chrome layer) created `TopNav.tsx`, `Footer.tsx`, `StickyMobileBar.tsx` in the same `web/src/components/giftmaison/` directory and appended their exports to `index.ts` (lines 7-9). The barrel now has 9 exports instead of the 6 specified in this plan.
- **Decision:** Committed `index.ts` in its 9-export form rather than reverting it to 6 exports — this is the intended end-state of Phase 13 (atoms + chrome share one barrel) and reverting would break the parallel Plan 02 commit. Functionally identical to what would happen if the two plans ran sequentially.
- **Files affected:** `web/src/components/giftmaison/index.ts` (committed in `df19ebd`)
- **Verification:** `npm --prefix web run typecheck` and `npm --prefix web run build` both pass with all 9 exports resolving cleanly; all 107 existing tests still pass.

### Auto-fixed Issues

None — plan executed exactly as written. No bugs, no missing critical functionality, no blockers, no architectural changes needed.

---

**Total deviations:** 0 auto-fixed (1 parallel-execution coordination note, no rule-based deviations)
**Impact on plan:** Plan executed as specified. Barrel-export coordination note is expected behaviour for parallel-wave execution and matches the intended Phase 13 end-state.

## Issues Encountered

None. All 4 atom files (Task 1) created and verified in a single pass; all 3 files (Task 2) likewise. Typecheck (`tsc --noEmit`) clean, production build succeeds, all 107 existing web tests pass with no regressions.

## User Setup Required

None — no external service configuration. Atoms are net-new files and not yet imported by any production code, so they cannot regress existing behaviour.

## Verification Results

| Check | Result |
| ---- | ---- |
| `test -f` for all 7 files | PASS (all exist) |
| Wordmark uses `font-display italic text-gm-ink` + `text-gm-accent` for the period span | PASS |
| Pill uses `bg-gm-accentSoft text-gm-accent` for accent tone + `uppercase whitespace-nowrap tracking-[0.3px]` | PASS |
| PulseDot uses `animate-gm-pulse` + `bg-gm-accent` | PASS |
| MonoCaption uses `font-mono uppercase font-medium` | PASS |
| Btn uses `bg-gm-ink text-gm-paper border-gm-ink` (primary) + `bg-gm-accent text-gm-accentInk border-gm-accent` (accent) | PASS |
| Btn uses `focus-visible:outline-gm-accent` and `px-[22px] py-[14px]` (lg size) | PASS |
| Field uses `forwardRef<HTMLInputElement, FieldProps>` + `focus-within:border-gm-accent` + `rounded-[10px]` | PASS |
| Barrel exports `Wordmark` + `Btn` (and 4 other atoms) | PASS (9 lines including 3 chrome exports added by parallel Plan 02) |
| `npm --prefix web run typecheck` | PASS (exit 0) |
| `npm --prefix web run build` | PASS (1790 modules transformed; production bundle output succeeds) |
| `npm --prefix web run test:run` (107 existing tests) | PASS (21 files, 107 tests, 0 failures) |
| `grep -r "from '../components/giftmaison'" web/src/` | empty (no consumers yet — atoms are net-new; chrome/screens wire them in Plans 02/04/05/06) |

## Next Phase Readiness

- Atom layer is ready for consumption by chrome (Plan 02 — already in flight as a parallel sibling) and the screen layers (Plans 04 / 05 / 06)
- No blockers, no concerns, no deferred items for this plan
- Phase 13 barrel `web/src/components/giftmaison/index.ts` is the canonical consumer-side import path for everything in the new namespace

## Self-Check: PASSED

All 7 files exist on disk:
- `web/src/components/giftmaison/Wordmark.tsx` — FOUND
- `web/src/components/giftmaison/Pill.tsx` — FOUND
- `web/src/components/giftmaison/MonoCaption.tsx` — FOUND
- `web/src/components/giftmaison/PulseDot.tsx` — FOUND
- `web/src/components/giftmaison/Btn.tsx` — FOUND
- `web/src/components/giftmaison/Field.tsx` — FOUND
- `web/src/components/giftmaison/index.ts` — FOUND

Both task commits exist:
- `6d9641b` (Task 1: feat — Wordmark, Pill, MonoCaption, PulseDot) — FOUND
- `df19ebd` (Task 2: feat — Btn, Field, barrel index) — FOUND

---
*Phase: 13-web-fallback-visual-refresh*
*Completed: 2026-05-07*
