---
phase: 13-web-fallback-visual-refresh
plan: 02
subsystem: ui
tags: [react, tailwindcss, giftmaison, top-nav, footer, sticky-mobile-bar, language-switcher, web-fallback, chrome, useauth, react-i18next]

# Dependency graph
requires:
  - phase: 13-web-fallback-visual-refresh
    provides: Plan 00 gm.* Tailwind tokens, font families, gm-pulse animation; Plan 01 Wordmark/Pill/Btn/Field/PulseDot/MonoCaption atoms + barrel index
  - phase: 05-web-fallback
    provides: web Vite/Tailwind/React scaffold, useAuth hook, AppRootPage, LanguageSwitcher, react-i18next wiring, react-router routes
provides:
  - "<TopNav /> shared chrome — wordmark left + EN/RO toggle + Sign-in ghost button (or olive avatar circle when authenticated) right; 1 px gm.line bottom border; bg-gm-paper; no hamburger collapse"
  - "<Footer /> shared chrome — single-line mono-caps 10 px gm.inkFaint copy with locale toggle button; uses i18n defaultValue fallbacks until Plan 03 ships keys"
  - "<StickyMobileBar /> generic mobile-only sticky-bottom container — paper/85 + backdrop-blur + 1 px gm.line top border + safe-area-inset-bottom padding (sm:hidden)"
  - "Restyled LanguageSwitcher — mono-caps 11 px EN / RO with active locale highlighted gm.ink, inactive gm.inkFaint"
  - "AppRootPage as design-system reference render — TopNav + Display L italic-accent headline + MonoCaption subline + Footer; full-bleed bg-gm-paper"
  - "Barrel exports added to web/src/components/giftmaison/index.ts: TopNav, Footer, StickyMobileBar (alongside the 6 atoms from Plan 01)"
affects: [13-04-onboarding-auth, 13-05-registry-detail, 13-06-reservation-flow, 13-07-regression]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "TopNav onSignInClick optional callback — defined: opens AuthModal (Phase 5 RegistryPage pattern); undefined: renders <a href='/sign-in'> link (Plan 06 route)"
    - "Footer i18n defaultValue fallbacks — keys web_footer.copyright/terms/privacy resolve to 'terms', 'privacy', '© giftmaison 2026' until Plan 03 ships them; defensive against missing-key flicker"
    - "StickyMobileBar uses Tailwind arbitrary CSS value pb-[calc(env(safe-area-inset-bottom)+12px)] — Tailwind 3.4 square-bracket arbitrary utility supports the env() function call"
    - "Avatar initials derivation in TopNav: prefers user.displayName split on whitespace (max 2 letters); falls back to user.email[0]; final fallback 'A' so the circle is never empty"

key-files:
  created:
    - web/src/components/giftmaison/TopNav.tsx
    - web/src/components/giftmaison/Footer.tsx
    - web/src/components/giftmaison/StickyMobileBar.tsx
  modified:
    - web/src/components/giftmaison/index.ts (added 3 chrome exports alongside Plan 01's 6 atom exports)
    - web/src/components/LanguageSwitcher.tsx (restyled to mono-caps EN / RO)
    - web/src/pages/AppRootPage.tsx (full rewrite to consume TopNav + Footer + MonoCaption)

key-decisions:
  - "TopNav.onSignInClick is optional, not required — keeps AppRootPage and Plan 06 auth-screen route as direct anchor links while RegistryPage (Plan 04) wires onSignInClick={() => setAuthModalOpen(true)} for the modal flow"
  - "Footer's terms/privacy are stub anchors (#) for v1.1 per plan spec — interactive routes deferred; Plan 03 ships the i18n keys, structure stays untouched"
  - "Footer locale toggle button is a SECOND copy of LanguageSwitcher's behaviour — duplicates the changeLanguage call rather than importing LanguageSwitcher; styling is inline-mono-caps to match the rest of the footer line, distinct visually from the top-nav switcher"
  - "LanguageSwitcher restyled to single-button toggle (EN / RO with active highlight) instead of the prototype's two-button shape — same UX outcome, fewer DOM nodes, mirrors the Phase 5 button semantics"
  - "AppRootPage drops legacy bg-surface + absolute LanguageSwitcher overlay — TopNav now owns the locale toggle on this page; the page becomes the smallest end-to-end render of (Plan 00 tokens) + (Plan 01 atoms) + (Plan 02 chrome)"

patterns-established:
  - "Chrome composition pattern: <div min-h-screen flex flex-col bg-gm-paper><TopNav /><main flex-1>...</main><Footer /></div> — Plans 04/05/06 will mirror this skeleton, only the <main> body differs"
  - "Avatar fallback chain pattern: displayName-initials → email[0] → 'A' literal — guarantees a non-empty circle even for newly-created accounts before profile data hydrates"
  - "Sticky-bottom mobile pattern: fixed positioning + sm:hidden + paper/85 + backdrop-blur-sm + safe-area-inset-bottom calc — reusable for any future mobile-only CTA slot"

requirements-completed: [D-13, D-14, D-15, D-12, D-18]

# Metrics
duration: 8min
completed: 2026-05-07
---

# Phase 13 Plan 02: Shared Chrome — TopNav + Footer + StickyMobileBar + LanguageSwitcher Summary

**Wave 2 cross-cutting chrome shipped: TopNav (wordmark + EN/RO + Sign-in/avatar), Footer (mono-caps single line), StickyMobileBar (mobile sticky-bottom container), restyled LanguageSwitcher, and AppRootPage as the first end-to-end render of the new design system.**

## Performance

- **Duration:** ~8 min
- **Started:** 2026-05-07T19:55Z (approximate)
- **Completed:** 2026-05-07T20:03Z
- **Tasks:** 2
- **Files modified:** 6 (3 created, 3 modified)

## Accomplishments
- 3 chrome components shipped: TopNav, Footer, StickyMobileBar — all importable via the `giftmaison` barrel
- LanguageSwitcher restyled to mono-caps EN / RO with active-locale highlight
- AppRootPage rewritten to consume TopNav + Footer + MonoCaption — first proof-point that Plan 00 tokens + Plan 01 atoms + Plan 02 chrome compose end-to-end
- Zero behavioural regressions: all 107 existing web tests stay green; typecheck and build both clean
- TopNav handles authenticated and anonymous users in one component (avatar circle vs Sign-in ghost / link)
- Footer ships i18n defaultValue fallbacks so it renders correctly even before Plan 03's web_footer.* keys land

## Task Commits

Each task was committed atomically:

1. **Task 1: Build TopNav, Footer, StickyMobileBar chrome components + restyle LanguageSwitcher; extend barrel index** — `5f6ef72` (feat)
2. **Task 2: Restyle AppRootPage as the design-system reference render** — `c779879` (feat)

## Files Created/Modified

### Created
- `web/src/components/giftmaison/TopNav.tsx` — Shared top navigation chrome: Wordmark + LanguageSwitcher + Sign-in ghost button (or olive avatar circle when authenticated). 1 px gm.line bottom border. Optional onSignInClick callback.
- `web/src/components/giftmaison/Footer.tsx` — Shared footer chrome: single-line mono-caps 10 px gm.inkFaint copy with copyright + terms + privacy + locale toggle. Uses i18n defaultValue fallbacks for the web_footer.* keys (Plan 03 ships them).
- `web/src/components/giftmaison/StickyMobileBar.tsx` — Generic mobile-only sticky-bottom container (sm:hidden). Paper/85 + backdrop-blur + 1 px gm.line top border + safe-area-inset-bottom padding via Tailwind arbitrary `pb-[calc(env(safe-area-inset-bottom)+12px)]`.

### Modified
- `web/src/components/giftmaison/index.ts` — Added TopNav, Footer, StickyMobileBar exports alongside Plan 01's 6 atom exports.
- `web/src/components/LanguageSwitcher.tsx` — Restyled from `text-sm` underlined CTA to mono-caps 11 px EN / RO with active-locale gm.ink highlight + inactive gm.inkFaint. Single-button toggle preserved (clicking swaps locales). Phase 5 i18next-browser-languagedetector persistence to localStorage 'lang' key untouched.
- `web/src/pages/AppRootPage.tsx` — Full rewrite. Drops legacy bg-surface + absolute LanguageSwitcher overlay. Now wraps a centred Display L italic-accent headline + MonoCaption subline in TopNav (above) + Footer (below); page-level bg-gm-paper.

## Decisions Made
- TopNav onSignInClick optional callback — supports both modal (Plan 04) and route (Plan 06) sign-in flows from one component
- Footer locale toggle inlined (not LanguageSwitcher import) — styling is mono-caps + footer-rhythm; reusing LanguageSwitcher would have forced a styling fork
- AppRootPage chosen as the first canary — minimal traffic + zero feature dependencies = lowest-risk first proof of (tokens + atoms + chrome)
- LanguageSwitcher kept as a single-button toggle (not the prototype's two-button shape) — mirrors existing Phase 5 button semantics, avoids breaking the 5-test contract anchored on click→language change

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

**Parallel-execution coordination (resolved transparently):**
This plan was launched in parallel with Plan 01 (atoms) despite a `depends_on: [00, 01]` declaration. At kickoff Plan 01's atoms (Wordmark, Pill, MonoCaption, PulseDot, Btn, Field, index.ts) did not yet exist, but the parallel agent committed `df19ebd feat(13-01): add Btn, Field atoms + barrel index export` before this plan's typecheck ran. Plan 01's barrel `index.ts` was authored to include the 6 atom exports; this plan's edit appended 3 chrome exports without conflict. Resolution: no rebasing needed; the index.ts merge converged cleanly because Plan 01 wrote first and this plan appended. Final typecheck (Task 1) and build (overall verification) ran with all atoms in place.

## User Setup Required

None — no external service configuration required.

## Next Phase Readiness

**Wave 2 chrome ready for Wave 3 screens:**
- Plan 04 (Onboarding/Auth) can compose `<TopNav onSignInClick={...} />` + `<StickyMobileBar><Btn>Continue as guest</Btn></StickyMobileBar>` + `<Footer />`
- Plan 05 (Registry Detail) can compose `<TopNav />` + reservation-banner overlay + `<Footer />`
- Plan 06 (Reservation flow) can compose `<TopNav />` + sticky reserve banner over `<StickyMobileBar />` + `<Footer />`
- Plan 03 (i18n) ships `web_footer.copyright`, `web_footer.terms`, `web_footer.privacy` keys — Footer immediately picks them up; defaultValue branches become unreachable

**No blockers.** Plan 04/05/06 each become body-only deltas — every chrome import already resolves through `'../components/giftmaison'`.

## Self-Check: PASSED

All claims verified:

- **Created files exist:**
  - FOUND: web/src/components/giftmaison/TopNav.tsx
  - FOUND: web/src/components/giftmaison/Footer.tsx
  - FOUND: web/src/components/giftmaison/StickyMobileBar.tsx
- **Modified files exist:**
  - FOUND: web/src/components/giftmaison/index.ts (with 9 exports, 3 added by this plan)
  - FOUND: web/src/components/LanguageSwitcher.tsx (restyled)
  - FOUND: web/src/pages/AppRootPage.tsx (rewritten)
- **Commits exist on main:**
  - FOUND: 5f6ef72 — Task 1 (chrome + LanguageSwitcher)
  - FOUND: c779879 — Task 2 (AppRootPage rewrite)
- **Plan-level verification:**
  - typecheck: PASS (zero output from tsc --noEmit)
  - build: PASS (vite build completed in 2.05s, 1800 modules transformed)
  - tests: PASS (107/107 green — no snapshot diffs needed)
  - grep: PASS (all 14 grep checks pass)

---
*Phase: 13-web-fallback-visual-refresh*
*Plan: 02*
*Completed: 2026-05-07*
