---
phase: 13-web-fallback-visual-refresh
plan: 06
subsystem: ui
tags: [react, react-router, react-hook-form, zod, radix-dialog, tailwind, giftmaison, auth, vite, i18next]

# Dependency graph
requires:
  - phase: 13-00
    provides: GiftMaison Tailwind tokens (gm-paper / gm-ink / gm-line / rounded-gm-modal / shadow-gm-modal) + bg-gm-* utilities
  - phase: 13-01
    provides: Wordmark / Btn / Field / MonoCaption atoms (with focus-visible outline + react-hook-form forwardRef support)
  - phase: 13-02
    provides: TopNav (with onSignInClick optional) / Footer / StickyMobileBar chrome
  - phase: 13-03
    provides: web_auth.* i18n keys (caption, headline_pre/emphasis/post, subline, show_password, hide_password, guest_skip_*, editorial_quote, editorial_attribution)
provides:
  - AuthScreen full-page route at /sign-in with split layout (520 px form + EditorialPhoto desktop-only)
  - EditorialPhoto desktop atmospheric photo + italic Display M quote overlay
  - GuestSkipCard dashed paperDeep card (mobile-pinned via StickyMobileBar; desktop inline)
  - Restyled AuthModal preserving Phase 5 API (open / onOpenChange / onContinueAsGuest)
  - Self-hosted /auth-editorial.jpg (Unsplash 1200x801 progressive JPEG, ~92 KB) — no runtime CDN dependency
  - 1200x630 paper-coloured /og-default.png placeholder with terracotta-accent wordmark
affects: [13-07, 14-deploy, future-occasion-cascade]

# Tech tracking
tech-stack:
  added: []  # all libraries already on the tree from earlier waves
  patterns:
    - Two-surface auth UX (full-page <AuthScreen /> at /sign-in for deep-linked users + in-page <AuthModal /> for the dominant /registry/:id flow)
    - GuestSkipCard rendered twice in AuthScreen via responsive class gates (hidden lg:block desktop inline + StickyMobileBar sm:hidden mobile-pinned) — single component, no duplication, breakpoint-coverage at every viewport
    - Test-side firebase mock when a restyled component pulls in the giftmaison barrel (which transitively imports TopNav → useAuth → firebase/auth) — pattern: vi.mock('../../../firebase', ...) + vi.mock('../useAuth', ...)
    - Self-hosted public assets for first-paint determinism (auth-editorial.jpg, og-default.png) — Vite emits both into hosting/public/ on build, removing runtime CDN dependency

key-files:
  created:
    - web/src/features/auth/AuthScreen.tsx
    - web/src/features/auth/EditorialPhoto.tsx
    - web/src/features/auth/GuestSkipCard.tsx
    - web/public/auth-editorial.jpg
    - web/public/og-default.png
    - .planning/phases/13-web-fallback-visual-refresh/deferred-items.md
  modified:
    - web/src/features/auth/AuthModal.tsx
    - web/src/features/auth/__tests__/AuthModal.test.tsx
    - web/src/App.tsx

key-decisions:
  - AuthScreen default tab = 'signin' (Phase 5 historic default); first-visit-default-to-signup heuristic deferred per UI-SPEC Claude's-discretion clause
  - AuthScreen.handleSubmit + handleGoogle use navigate(-1) to bounce back to whatever page sent the user to /sign-in (typical flow: /registry/:id → /sign-in → back authenticated); useAuth-driven early-bounce protects against arriving signed-in
  - GuestSkipCard does NOT pin itself — parent AuthScreen wraps it in <StickyMobileBar /> on mobile and renders inline below the form on desktop (lg:block); this keeps the atom decoupled from breakpoint logic
  - EditorialPhoto loads /auth-editorial.jpg from web/public/ (NOT a runtime images.unsplash.com URL); the Unsplash photo URL is the SOURCE, the production runtime is the self-hosted copy
  - Italic emphasis colour for 'Pick up' uses gm.ink (not gm.accent) per UI-SPEC 'Default to prototype = gm.ink italic' decision
  - Tab switcher active-state uses border-b-2 gm-ink underline (UI-SPEC: 'active tab = 2 px bottom border in gm.ink, NOT accent — accent is reserved for emphasis')
  - AuthModal restyle preserves Phase 5 behavioural API verbatim (open/onOpenChange/onContinueAsGuest props; signInEmail/signUpEmail/signInWithGoogle wiring) — RegistryPage modal flow continues to work identically with zero call-site changes
  - AuthModal.test.tsx mocks ../../../firebase + ../useAuth (Rule 1 auto-fix) because the restyled modal imports atoms via the giftmaison barrel which transitively imports TopNav → useAuth → firebase/auth; alternative was to bypass the barrel but the plan's verify chain mandated the barrel import literal — chose the test-side mock to satisfy both
  - og-default.png shipped as a real 1200x630 paper-coloured Pillow render with italic 'giftmaison' wordmark + terracotta accent period (NOT the 1x1 transparent fallback) — Pillow was available, so the production placeholder is a proper OG card from day 1

patterns-established:
  - "Two-surface auth pattern: full-page route + in-page modal sharing the same atoms but different chrome (TopNav+Footer vs Radix Dialog shell)"
  - "Responsive single-component duplication: render the same React node twice with hidden lg:block + StickyMobileBar sm:hidden gates so a single <GuestSkipCard /> covers desktop-inline AND mobile-pinned without prop drilling"
  - "Test-side firebase mock for components that consume the giftmaison barrel: vi.mock('../../../firebase', () => ({...stubs})) + vi.mock('../useAuth', () => ({useAuth: () => ({user: null, isReady: true})}))"
  - "Self-hosted Unsplash assets at web/public/ via curl --fail --silent --location during plan execution — Vite emits to hosting/public/ on build, no runtime CDN dependency"

requirements-completed: [D-03, D-05, D-09, D-12]

# Metrics
duration: 4min
completed: 2026-05-07
---

# Phase 13 Plan 06: Auth Screen Restyle Summary

**Two-surface Screen 03 auth — full-page <AuthScreen /> at /sign-in with split layout + editorial photo, plus restyled in-page <AuthModal /> preserving Phase 5 API; both consume GiftMaison atoms, both share i18n + showpassword + tab anatomy.**

## Performance

- **Duration:** ~4 min (23:09:09Z → 23:12:43Z)
- **Started:** 2026-05-07T20:09:09Z
- **Completed:** 2026-05-07T20:12:43Z
- **Tasks:** 4 / 4
- **Files modified:** 8 (3 new auth components, 2 new public assets, 1 modified auth component, 1 modified router, 1 modified test file)

## Accomplishments

- AuthScreen full-page route at /sign-in renders TopNav + 520 px form column + EditorialPhoto + Footer with mobile-pinned GuestSkipCard
- EditorialPhoto desktop-only atmospheric image with bottom gradient + italic Display M quote + mono attribution, loading self-hosted /auth-editorial.jpg
- GuestSkipCard dashed paperDeep card with copy block + Skip ghost button (D-05 mandate: must be reachable in 1 tap on mobile)
- AuthModal in-page version restyled with rounded-gm-modal + shadow-gm-modal Radix shell + Btn/Field/MonoCaption atoms — all 6 behavioural tests still pass
- /sign-in route wired in App.tsx; production build green (1.05 MB JS, ~288 KB gzipped); all 21 auth tests pass

## Task Commits

Each task committed atomically with --no-verify (parallel-executor protocol):

1. **Task 1: Build EditorialPhoto + GuestSkipCard atoms (with self-hosted JPEG)** - `f206927` (feat)
2. **Task 2: Build AuthScreen full-page route with split layout + sticky guest skip** - `d8fe996` (feat)
3. **Task 3: Restyle AuthModal with GiftMaison atoms (preserved API)** - `1fd2f2b` (feat)
4. **Task 4: Wire /sign-in route + ship og-default.png placeholder** - `cca55b6` (feat)

**Plan metadata commit:** (this SUMMARY + STATE.md + ROADMAP.md update — final docs commit follows separately)

## Files Created/Modified

### Created
- `web/src/features/auth/AuthScreen.tsx` (203 lines) — Full-page /sign-in route. TopNav + Wordmark + caption + Display L italic-emphasis headline + tab switcher + Field-based email+password form (with show/hide suffix toggle) + 'OR' divider + ghost Google CTA + GuestSkipCard (desktop inline + mobile sticky). Uses signInEmail/signUpEmail/signInWithGoogle verbatim; useAuth-driven signed-in early bounce; navigate(-1) post-success.
- `web/src/features/auth/EditorialPhoto.tsx` (38 lines) — Desktop-only (hidden lg:block) atmospheric image. Loads /auth-editorial.jpg with absolute object-cover positioning, 180deg gradient overlay (rgba(42,36,32,0.53) gm.ink at 53%), italic font-display 28 px gm.paper quote, mono caps gm.paper/80 attribution. alt="" (decorative — quote is the semantic content).
- `web/src/features/auth/GuestSkipCard.tsx` (29 lines) — Dashed border-gm-line paperDeep card with two-column flex (copy block + Skip ghost button). Does NOT pin itself; parent decides positioning (StickyMobileBar on mobile, inline on desktop).
- `web/public/auth-editorial.jpg` (~92 KB, 1200x801 progressive JPEG) — Self-hosted Unsplash photo-1513694203232-719a280e022f. Curated atmospheric housewarming-couple image; production has no runtime images.unsplash.com dependency.
- `web/public/og-default.png` (~19 KB, 1200x630 PNG) — Paper-coloured (#F7F2E9) placeholder with italic 'giftmaison' wordmark in gm.ink + terracotta gm.accent (#C8623A) period. Pillow render with system Georgia Italic font fallback.
- `.planning/phases/13-web-fallback-visual-refresh/deferred-items.md` — Logs out-of-scope discoveries (5 pre-existing ItemCard/RegistryPage test failures from Plan 13-04, slated for Plan 13-07 re-baseline).

### Modified
- `web/src/features/auth/AuthModal.tsx` (172 lines, full rewrite preserving API) — Replaced Phase 5 bg-surface/primary tokens with rounded-gm-modal (20 px) + shadow-gm-modal Radix shell + bg-gm-ink/40 backdrop. Same atom anatomy as AuthScreen: tab switcher (border-b-2 gm-ink), top-placed ghost Google CTA (Phase 5 layout preserved), 'OR' divider, Field email + password (with show/hide suffix), primary submit Btn, underlined 'Continue as guest' link. Dialog.Description sr-only kept (Radix warning suppressor). All 6 behavioural tests pass without changes to assertions.
- `web/src/features/auth/__tests__/AuthModal.test.tsx` — Added vi.mock for ../../../firebase + ../useAuth so the giftmaison barrel's transitive firebase/auth pull doesn't crash jsdom test env (Rule 1 deviation, see below).
- `web/src/App.tsx` — Added `{ path: '/sign-in', element: <AuthScreen /> }` route. Imports AuthScreen from './features/auth/AuthScreen'.

## Decisions Made

See frontmatter `key-decisions` array — 9 decisions covering: default tab heuristic, navigate(-1) UX, GuestSkipCard parent-driven positioning, self-hosted asset rule, italic colour choice, tab underline colour, AuthModal API preservation, test-side firebase mock strategy, og-default.png Pillow render quality.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Removed literal `images.unsplash.com` substring from EditorialPhoto.tsx KDoc**
- **Found during:** Task 1 verify chain
- **Issue:** The plan's verify grep `! grep -q "images.unsplash.com" web/src/features/auth/EditorialPhoto.tsx` failed because the KDoc comment mentioned "removes the runtime dependency on images.unsplash.com" — even though the runtime URL was self-hosted, the docstring still contained the literal substring.
- **Fix:** Reworded the KDoc to "removes the runtime dependency on the upstream Unsplash CDN (offline emulator dev, first-paint determinism, no upstream drift)". Semantic content preserved; the literal `images.unsplash.com` substring no longer appears anywhere in the file.
- **Files modified:** web/src/features/auth/EditorialPhoto.tsx
- **Verification:** `! grep -q "images.unsplash.com" web/src/features/auth/EditorialPhoto.tsx` now succeeds; `grep -q "/auth-editorial.jpg"` still succeeds.
- **Committed in:** f206927 (Task 1 commit)

**2. [Rule 1 - Bug] Added vi.mock for firebase + useAuth in AuthModal.test.tsx**
- **Found during:** Task 3 test run
- **Issue:** AuthModal now imports `Btn, Field, MonoCaption` via the giftmaison barrel (`from '../../components/giftmaison'`). The barrel re-exports `TopNav`, which imports `useAuth`, which imports `firebase/auth` and triggers `getAuth(app)` with `auth/invalid-api-key` because jsdom test env has no real Firebase config. All 6 tests crashed at module load.
- **Fix:** Added two `vi.mock` calls at the top of AuthModal.test.tsx: `vi.mock('../../../firebase', () => ({ auth: {_kind:'fakeAuth'}, app, db, functions }))` and `vi.mock('../useAuth', () => ({ useAuth: () => ({ user: null, isReady: true }) }))`. Same pattern is used elsewhere in the test tree (useAuth.test.tsx, reservation hook tests).
- **Files modified:** web/src/features/auth/__tests__/AuthModal.test.tsx
- **Verification:** All 6 AuthModal tests pass; all 21 auth-suite tests pass.
- **Committed in:** 1fd2f2b (Task 3 commit)
- **Why this path (not the barrel-bypass alternative):** The plan's Task 3 verify chain explicitly mandated `import { Btn, Field, MonoCaption } from '../../components/giftmaison'` as a literal grep assertion. Bypassing the barrel (importing from atom files directly) would have failed verify. The test-side mock satisfies both the verify gate and the test green requirement.

### Out-of-scope items (logged, not auto-fixed)

5 pre-existing test failures in `ItemCard.test.tsx` (4) and `RegistryPage.test.tsx` (1) discovered during the full-suite regression check. These are caused by Plan 13-04's ItemCard restyle (commit b8efae4 — replaced legacy bg-primary / bg-surface-* tokens with new GiftMaison Pill atom) and predate Plan 13-06. Logged in `.planning/phases/13-web-fallback-visual-refresh/deferred-items.md` for Plan 13-07 to address as part of the planned regression sweep.

A `git stash pop` from an earlier WIP also briefly reintroduced direct-import workarounds in `web/src/features/registry/ItemCard.tsx` and `web/src/pages/RegistryPage.tsx`. Those changes were reverted (out of Plan 13-06's scope) and noted in `deferred-items.md` for Plan 13-07.

---

**Total deviations:** 2 auto-fixed (2 Rule 1 bugs)
**Impact on plan:** Both auto-fixes were necessary for plan success (one for verify gate, one for test green). No scope creep — both fixes were on files already in Plan 13-06's `files_modified` list.

## Issues Encountered

- **Firebase init in jsdom test env when AuthModal pulls the giftmaison barrel** — solved via test-side firebase + useAuth mock (see Rule 1 deviation above).
- **5 pre-existing failing tests in ItemCard / RegistryPage** — out of scope, logged in deferred-items.md, deferred to Plan 13-07.

## User Setup Required

None — no external service configuration required. All assets ship with the repo; auth flows use existing Phase 5 Firebase config; routing requires no DNS or hosting change beyond the next deploy.

## Next Phase Readiness

- **Plan 13-07 (regression sweep):** Now unblocked. The auth surface is fully restyled; remaining waves (Plan 13-04 ItemCard, Plan 13-05 reserve banner) need test re-baseline. Plan 13-06 SUMMARY's Known Stubs section is empty (no UI stubs ship with this plan).
- **Plan 14 (deploy + UAT):** AuthScreen full-page route ready for production. The /sign-in route resolves on every breakpoint; mobile-pinned guest skip is a 1-tap affordance per D-05.
- **Production deploy:** Both `web/public/auth-editorial.jpg` and `web/public/og-default.png` are self-hosted; `npm --prefix web run build` emits both into `hosting/public/` on every build with no runtime CDN dependency. og-default.png remains a placeholder for v1.1 — per-registry OG cards via Cloud Function are deferred per CONTEXT D-01 visual-only scope envelope.

## Self-Check: PASSED

Verified after writing this SUMMARY:
- [x] `web/src/features/auth/AuthScreen.tsx` exists (created in Task 2, commit d8fe996)
- [x] `web/src/features/auth/EditorialPhoto.tsx` exists (created in Task 1, commit f206927)
- [x] `web/src/features/auth/GuestSkipCard.tsx` exists (created in Task 1, commit f206927)
- [x] `web/public/auth-editorial.jpg` exists (~92 KB, 1200x801 JPEG; created in Task 1, commit f206927)
- [x] `web/public/og-default.png` exists (~19 KB, 1200x630 PNG; created in Task 4, commit cca55b6)
- [x] `web/src/features/auth/AuthModal.tsx` modified (full rewrite preserving API; commit 1fd2f2b)
- [x] `web/src/App.tsx` modified (added /sign-in route; commit cca55b6)
- [x] All 4 task commits exist on main: f206927, d8fe996, 1fd2f2b, cca55b6
- [x] Final typecheck + build green; all 21 auth-suite tests green

---
*Phase: 13-web-fallback-visual-refresh*
*Plan: 06*
*Completed: 2026-05-07*
