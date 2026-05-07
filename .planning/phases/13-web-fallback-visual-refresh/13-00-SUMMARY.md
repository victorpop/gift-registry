---
phase: 13-web-fallback-visual-refresh
plan: 00
subsystem: ui
tags: [tailwindcss, css-variables, design-tokens, google-fonts, instrument-serif, inter, jetbrains-mono, prefers-reduced-motion, giftmaison]

# Dependency graph
requires:
  - phase: 05-web-fallback
    provides: web Vite/Tailwind/React scaffold (web/index.html, web/src/index.css, web/tailwind.config.ts)
provides:
  - Tailwind theme.colors.gm.{paper,paperDeep,ink,inkSoft,inkFaint,line,accent,accentInk,accentSoft,second,secondSoft,ok,warn} bound to CSS custom properties
  - Tailwind fontFamily.display (Instrument Serif), fontFamily.body (Inter), fontFamily.mono (JetBrains Mono); legacy sans alias retained
  - Tailwind borderRadius.gm-card/gm-card-large/gm-modal (14/18/20px) and boxShadow.gm-modal
  - Tailwind animation.gm-pulse (1.4s alternate) + matching @keyframes in index.css with prefers-reduced-motion fade-only fallback
  - 13 --gm-* :root CSS custom properties in web/src/index.css with handoff sRGB hex
  - Google Fonts preconnect+stylesheet for the 3 GiftMaison families with display=swap
  - viewport-fit=cover on the viewport meta (enables env(safe-area-inset-bottom) downstream)
  - Static OG/twitter meta tags with /og-default.png placeholder for crawler hydration
  - bg-gm-paper text-gm-ink font-body antialiased body class
  - D-01 phase-wide visual-only scope envelope anchored (no SSR/slug routing/cookies/OG-fn/re-reserve/theme-cascade/backend changes)
affects: [13-01-atoms, 13-02-shared-chrome, 13-03-strings-i18n, 13-04-onboarding-auth, 13-05-registry-detail, 13-06-reservation-flow, 13-07-regression]

# Tech tracking
tech-stack:
  added:
    - Google Fonts (CDN): Instrument Serif (ital@0;1), Inter (wght@400;500), JetBrains Mono (wght@500)
  patterns:
    - "Tailwind tokens resolve to CSS custom properties (var(--gm-*)) — D-07 indirection so v1.2 occasion cascade swaps values at the registry-detail root without renaming any utility class"
    - "Single shared @keyframes gm-pulse with prefers-reduced-motion @media re-declaration — same animation name, same Tailwind utility (animate-gm-pulse), fade-only when user opts out of motion"
    - "Phase 5 legacy tokens (primary/surface/destructive/outline) coexist with new gm.* tokens during cutover — Phase 14 removal audit"

key-files:
  created: []
  modified:
    - web/tailwind.config.ts
    - web/src/index.css
    - web/index.html

key-decisions:
  - "Tailwind gm.* utilities resolve to var(--gm-*) custom properties (D-07) instead of hex literals — enables v1.2 occasion cascade with zero class renames"
  - "Only Housewarming palette wired in this phase (D-02) — Wedding/Baby/Birthday tokens deferred to v1.2"
  - "spacing.7 explicitly pinned to 28px (override) — guards against root-font drift even though Tailwind 3 default at 16px root resolves identically"
  - "Single gm-pulse keyframe re-declared inside prefers-reduced-motion @media block (no separate gm-pulse-fade name) — Tailwind animate-gm-pulse utility resolves to whichever definition wins at runtime; simpler than the UI-SPEC's two-name variant and equivalent in behaviour"
  - "Phase 5 legacy palette retained unchanged in tailwind.config.ts and the :root font-family declaration preserved — the 107 existing tests resolve without changes (zero behavioural regressions)"
  - "OG meta tags ship with static /og-default.png placeholder (asset will be added in a follow-up) — bounded TODO acceptable per UI-SPEC and CONTEXT D-01 (per-registry OG generation deferred)"
  - "D-01 visual-only scope envelope is anchored to this plan — Phase 13 ships visual-layer code only; no firebase config, firestore rules, Cloud Functions, SSR, slug routing, cookie sessions, OG cloud function, token re-reserve, or 4-occasion theme cascade"

patterns-established:
  - "CSS-variable-backed Tailwind tokens: theme.colors.gm.* -> var(--gm-*) declared at :root — design-token indirection ready for runtime theme swap"
  - "Cross-app pulse cadence: 1.4s alternate matches Android Phase 9 PulsingDot constants — consistent reservation status motion between mobile and web"
  - "Reduced-motion @media re-declaration: same @keyframes name, scale removed — single Tailwind utility (animate-gm-pulse) honours user preference automatically"
  - "Self-hosted preconnect for Google Fonts: explicit preconnect to fonts.googleapis.com + fonts.gstatic.com (with crossorigin) prefigures the css2 stylesheet roundtrip"
  - "OG meta + viewport-fit body baseline: every screen downstream inherits bg-gm-paper text-gm-ink font-body antialiased and the iOS safe-area inset envelope from index.html alone"

requirements-completed: [D-01, D-02, D-07, D-08, D-09, D-11]

# Metrics
duration: 2 min
completed: 2026-05-07
---

# Phase 13 Plan 00: Web Fallback Visual Refresh — Foundation Summary

**GiftMaison design-system foundation: 13 gm.* Housewarming tokens via CSS-variable-backed Tailwind theme, 3 Google Font families (Instrument Serif + Inter + JetBrains Mono), gm-pulse keyframe with reduced-motion fallback, and viewport-fit=cover body baseline — every Wave 2+ atom and screen now consumes them directly from utilities.**

## Performance

- **Duration:** 2 min
- **Started:** 2026-05-07T19:53:41Z
- **Completed:** 2026-05-07T19:55:57Z
- **Tasks:** 3
- **Files modified:** 3

## Accomplishments

- Tailwind theme extended with gm.* colour palette (13 tokens), display/body/mono fontFamily, spacing.7 override, gm-card/gm-card-large/gm-modal radii, gm-modal shadow, and gm-pulse animation+keyframe — Phase 5 legacy palette retained for cutover
- 13 --gm-* :root CSS custom properties declared with handoff sRGB hex, plus a single shared @keyframes gm-pulse and a prefers-reduced-motion fade-only re-declaration
- index.html wired with Google Fonts preconnect (googleapis + gstatic crossorigin), the 3 GiftMaison stylesheet families with display=swap, viewport-fit=cover, 5 OG/twitter meta tags, and the bg-gm-paper text-gm-ink font-body antialiased body class
- D-01 phase-wide visual-only scope envelope structurally anchored — diff scope confined to web/tailwind.config.ts + web/src/index.css + web/index.html (zero firebase/firestore-rules/functions/SSR/cookie/OG-fn/re-reserve/theme-cascade touches)
- Build green: vite build emitted hosting/public/index.html with the new body class and hosting/public/assets/index-*.css with --gm-paper:#F7F2E9 and the @keyframes gm-pulse block — foundation reaches the deployed bundle
- Test suite green: 107/107 tests pass (no behavioural regressions)

## Task Commits

Each task was committed atomically:

1. **Task 1: Extend tailwind.config.ts with gm.* colour palette + fontFamily + radii + shadow + animation** — `00ba510` (feat)
2. **Task 2: Add :root --gm-* CSS variables and gm-pulse keyframe (with reduced-motion fallback) to web/src/index.css** — `674b0d4` (feat)
3. **Task 3: Wire Google Fonts preconnect + stylesheet + viewport-fit=cover + body class swap in web/index.html** — `1c6b8c5` (feat)

**Plan metadata:** _committed in `git_commit_metadata` step below_

## Files Created/Modified

- `web/tailwind.config.ts` — Added gm.* colours bound to var(--gm-*), display/body/mono fontFamily, spacing.7=28px, gm-card/gm-card-large/gm-modal radii, gm-modal shadow, gm-pulse animation+keyframe; preserved Phase 5 legacy primary/surface/destructive/outline + sans alias
- `web/src/index.css` — Added 13 --gm-* :root tokens with handoff sRGB hex, @keyframes gm-pulse (1.4s alternate cadence), and prefers-reduced-motion @media re-declaration with no scale; preserved Phase 5 legacy :root font-family/font-synthesis/text-rendering rules
- `web/index.html` — Added preconnect to fonts.googleapis.com + fonts.gstatic.com (crossorigin), Google Fonts css2 stylesheet for Instrument Serif (ital@0;1) + Inter (wght@400;500) + JetBrains Mono (wght@500) with display=swap; added viewport-fit=cover; added 5 OG/twitter meta tags with /og-default.png placeholder; replaced body class bg-surface text-surface-on -> bg-gm-paper text-gm-ink font-body antialiased; updated title to GiftMaison branding

## Decisions Made

- **CSS-variable indirection (D-07):** Tailwind classes resolve to var(--gm-*) instead of hex literals so v1.2 occasion cascade can swap values at the registry-detail root with zero class renames
- **Housewarming-only scope (D-02):** Three other handoff palettes (Wedding/Baby/Birthday) deliberately not declared — deferred to v1.2 occasion-cascade work
- **Single gm-pulse keyframe with prefers-reduced-motion re-declaration:** Simpler than the UI-SPEC's gm-pulse + gm-pulse-fade two-name variant; the Tailwind animate-gm-pulse utility resolves to whichever @keyframes gm-pulse definition is active under the @media query — equivalent at consumer site
- **Explicit spacing.7=28px override:** Tailwind 3 default at 16px root font is also 28px, but pinning it explicit guards against root-font drift downstream
- **Phase 5 legacy retention:** primary/surface/destructive/outline tokens kept in tailwind.config.ts and the :root font-family declaration preserved — the 107 existing tests resolve without behavioural change; legacy removal audit deferred to Phase 14
- **D-01 phase-wide visual-only scope envelope anchored here:** Plan 13-00 owns the hard rule — Phase 13 ships visual-layer code only (no SSR migration, no /r/{slug} routing, no HttpOnly cookie sessions, no OG Cloud Function, no token re-reserve, no 4-occasion theme cascade, no backend / Firestore-rules / Cloud-Functions changes); diff confirms three-file scope

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None - all three tasks completed cleanly. Tailwind typecheck exits 0; vite build emits the deployed body class + --gm-paper hex + @keyframes gm-pulse into the production bundle; the existing 107-test suite stays 107/107 green (plan's "92 existing tests" reference appears stale by 15 tests — actual count is higher, all passing, zero behavioural regressions).

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Foundation green. Plans 13-01 (atoms), 13-02 (shared chrome), 13-03 (strings/i18n), 13-04+ (screens), and 13-07 (regression sweep) can `className="bg-gm-paper text-gm-ink font-display"` directly without redefining any token, font, or animation.
- /og-default.png asset needs to land before any production deploy that expects crawler hydration; tracked as a bounded TODO per UI-SPEC SEO section.
- Plan 13-07's regression sweep will need to re-baseline any snapshot tests that capture the body class string (bg-surface text-surface-on -> bg-gm-paper text-gm-ink font-body antialiased); behavioural tests are unaffected and stay green.

## Self-Check: PASSED

Verified:
- web/tailwind.config.ts exists with `gm: {`, all 13 var(--gm-*) bindings, fontFamily.display/body/mono, gm-card/gm-card-large/gm-modal radii, gm-modal shadow, gm-pulse animation, and Phase 5 legacy primary/surface/destructive/outline retained
- web/src/index.css exists with 13 --gm-* declarations, @keyframes gm-pulse, prefers-reduced-motion @media re-declaration
- web/index.html exists with both font preconnects (crossorigin on gstatic), 3 family stylesheet, display=swap, viewport-fit=cover, 5 OG/twitter meta tags, bg-gm-paper text-gm-ink font-body antialiased body class
- Commits 00ba510, 674b0d4, 1c6b8c5 present in `git log --oneline`
- npm --prefix web run typecheck exits 0
- npm --prefix web run build exits 0; deployed hosting/public/index.html contains the new body class; hosting/public/assets/index-*.css contains --gm-paper:#F7F2E9 and the @keyframes gm-pulse block
- npm --prefix web run test:run exits 0; 107/107 tests pass
- D-01 envelope structurally honoured — git diff scope is exactly 3 files (web/tailwind.config.ts + web/src/index.css + web/index.html)

---
*Phase: 13-web-fallback-visual-refresh*
*Completed: 2026-05-07*
