---
phase: 13-web-fallback-visual-refresh
plan: 05
subsystem: ui
tags: [react, tailwind, i18next, aria-live, reservation, screen-02, sticky-banner, accent-soft, lucide-react]

# Dependency graph
requires:
  - phase: 13-web-fallback-visual-refresh-00
    provides: Design tokens (gm.ink/paper/paperDeep/accent/accentSoft/line/inkSoft/inkFaint), font stack, gm-pulse keyframe, gm-card radius
  - phase: 13-web-fallback-visual-refresh-01
    provides: Wordmark, Pill, Btn, Field atoms (Btn variants primary/accent/ghost/quiet, Pill tone accent)
  - phase: 13-web-fallback-visual-refresh-02
    provides: PulseDot, MonoCaption, TopNav, Footer, StickyMobileBar atoms (PulseDot size 10 used here)
  - phase: 13-web-fallback-visual-refresh-03
    provides: web_reserve.* split-key triples (banner_heading_pre/emphasis/post), quadruple subline keys (banner_subline_pre/retailer/separator/countdown), and the detail/confirm/how_timer key set
  - phase: 13-web-fallback-visual-refresh-04
    provides: RegistryPage rewrite (gm-paper bg, TopNav + Footer mounted, hero + filter chips + ItemGrid layout — preserved verbatim under sticky banner + reserve detail section)
  - phase: 05-web-fallback
    provides: useActiveReservation context, useCountdown hook, useConfirmPurchase callable, ReserveButton onSuccess flow (preserved unchanged)

provides:
  - StickyReserveBanner — sticky-top ink/paper bar with 10 px PulseDot, MM:SS countdown, accent Continue <a target=_blank>, quiet Release, minute-cached aria-live announcer
  - HowTimerWorks — paperDeep aside with 4 numbered ink/paper step badges; native <details> collapsible on mobile, always-open on desktop
  - ConfirmPurchaseBanner restyled — accentSoft confirm-back card with Display S headline + accent CTA, preserves Phase 5 useConfirmPurchase + toast wiring
  - ReserveDetailSection — in-page Screen 02 wrapper above hero on /registry/:id when active is non-null; italic-accent Display L headline + reserved item card with nested 30-min progress bar + lg:grid-cols-[1fr_340px] sidebar
  - ReservationBanner.tsx as deprecated re-export of StickyReserveBanner for test back-compat
affects: [13-07-test-and-copy-cleanup, 14-web-fallback-live-deploy, future occasion-cascade variable migration for accent border rgba]

# Tech tracking
tech-stack:
  added: [lucide-react ChevronUp icon (already-installed dep, first use in reservation feature)]
  patterns:
    - "Minute-cached aria-live announcement: lastMinutesRef + cachedAriaTextRef pair preserves text-node string identity between minute ticks so screen readers re-announce only on minute flip, not every 1 s tick"
    - "Native <details> + lg:hidden summary for mobile-collapsible / desktop-always-open primitives — no JS state needed"
    - "Plan 13-03 split-key triples consumed verbatim — banner heading/subline composed from t() calls without .replace() blank-substitution"
    - "Deprecated re-export pattern for back-compat: ReservationBanner.tsx → StickyReserveBanner default export, keeps existing vi.mock paths in 4 test suites resolving"

key-files:
  created:
    - web/src/features/reservation/StickyReserveBanner.tsx
    - web/src/features/reservation/HowTimerWorks.tsx
    - web/src/features/reservation/ReserveDetailSection.tsx
  modified:
    - web/src/features/reservation/ReservationBanner.tsx (replaced with deprecated re-export)
    - web/src/features/reservation/ConfirmPurchaseBanner.tsx (full restyle to accentSoft confirm-back card; +minutesLeft prop)
    - web/src/pages/RegistryPage.tsx (swap ReservationBanner mount → StickyReserveBanner; swap standalone ConfirmPurchaseBanner mount → ReserveDetailSection above hero; drop unused imports)

key-decisions:
  - "[Phase 13-05] Minute-cached aria-live string instance via useRef pair — naive useMemo that returns a new string on every minute boundary still mutates the text node, but caching the rendered string instance guarantees React doesn't re-run the text-node update path on second-level countdown ticks"
  - "[Phase 13-05] Native <details> element for HowTimerWorks mobile collapsible — chosen over useState toggle for zero-JS, accessibility-built-in primitive; lg:hidden summary + hidden lg:block heading split keeps desktop always-open without summary chevron"
  - "[Phase 13-05] ReservationBanner.tsx kept as deprecated re-export — 4 test suites (RegistryPage.test, App.test, RegistryPage.autoReserve.test, RegistryPage.confirmPurchase.test) all vi.mock '../../reservation/ReservationBanner', so deleting the file would require touching 4 test files; deferred to Phase 14 cleanup"
  - "[Phase 13-05] Progress bar uses 30 * 60 = 1800 s as the total denominator — server-side createdAt is not exposed to the client, so the elapsed fraction is approximated from countdown.totalSeconds / 1800; acceptable since the server is the timer authority and this bar is display-only"
  - "[Phase 13-05] ConfirmPurchaseBanner gains optional minutesLeft prop (default 30) — confirm_body interpolates {{minutes}}, ReserveDetailSection passes the live countdown.minutes; standalone callers get the safe default"
  - "[Phase 13-05] border-[rgba(200,98,58,0.30)] inline rgba acceptable for now — UI-SPEC explicitly authorises this form; v1.2 occasion-cascade migrates to a CSS variable (out of scope)"

patterns-established:
  - "Aria-live string-instance caching for second-granularity countdowns: when a hook re-emits every 1 s but the live region must announce at minute granularity, cache the rendered string in a useRef pair (lastMinutesRef + cachedAriaTextRef) and return the cached instance from useMemo so text-node identity is preserved between second-ticks"
  - "Sticky banner above sticky TopNav: bg-gm-ink + sticky top-0 z-30 sits above TopNav while the page scrolls; visible MM:SS in mono small-text outside the live region, hidden minute-only sr-only span inside aria-live=polite"
  - "In-page reserve detail block: display L italic-accent headline (split keys), 4:3 mobile / 160 px square desktop thumbnail, nested time-to-purchase bar (h-[3px] gm-line track + gm-accent fill, transition-[width] duration-1000 ease-linear)"

requirements-completed: [D-04, D-11, D-12]

# Metrics
duration: 6min
completed: 2026-05-07
---

# Phase 13 Plan 05: Reserve Flow (Screen 02) Translation Summary

**In-page Screen 02 reserve flow — sticky-top ink/paper banner with 10 px PulseDot, MM:SS countdown, minute-cached aria-live announcer, accent Continue <a> + quiet Release; in-page italic-accent Display L headline above hero with reserved item card, nested 30-min progress bar, accentSoft confirm-back card, and 4-step HowTimerWorks sidebar.**

## Performance

- **Duration:** ~6 min
- **Started:** 2026-05-07T20:18:37Z
- **Completed:** 2026-05-07T20:24:21Z
- **Tasks:** 3
- **Files modified:** 6 (3 created, 3 modified)

## Accomplishments

- StickyReserveBanner replaces the Phase 5 single-line `h-12 bg-primary` banner with the dark ink/paper sticky bar specified in CONTEXT D-04 — 10 px PulseDot + MM:SS in mono + accent <a target=_blank> Continue + quiet Release; mobile stacks message + buttons in 2 rows, desktop tablet+ single row.
- Aria-live polite minute-only announcement implemented via the lastMinutesRef + cachedAriaTextRef pair — text-node string identity preserved between minute ticks so SR's only re-announce on minute flip; uses Plan 13-03 `web_reserve.banner_aria_live` `{{minutes}}` key.
- HowTimerWorks 4-step paperDeep aside built with native <details> mobile-collapsible / desktop-always-open primitive (no useState toggle); 22 px ink-bg/paper-fg numeric badges + 13.5 px headline / 12.5 px sublabel each step.
- ConfirmPurchaseBanner restyled to the accentSoft confirm-back card variant — Display S 20/22 px headline + Body S body + accent Btn CTA; Phase 5 useConfirmPurchase contract preserved verbatim (status, error, successToastedRef, errorToastedForRef, role=status aria-live=polite).
- ReserveDetailSection in-page wrapper renders above hero on /registry/:id when active is non-null — Display L italic-accent headline (split keys), reserved item card (4:3 mobile / 160 px square desktop thumbnail, Pill accent badge, price + retailer line, nested 30-min progress bar transition-[width] 1s linear), confirm-back delegated child, HowTimerWorks delegated child in lg:grid-cols-[1fr_340px].
- RegistryPage rewired: imports + mounts both new components, drops unused ReservationBanner + ConfirmPurchaseBanner direct imports; auto-reserve test 6/6 still green; typecheck + build clean.
- ReservationBanner.tsx kept as deprecated re-export of StickyReserveBanner — 4 test suites resolve their `vi.mock('../../reservation/ReservationBanner')` paths without modification.

## Task Commits

Each task was committed atomically:

1. **Task 1: Build StickyReserveBanner + replace ReservationBanner with deprecated re-export** — `562b328` (feat)
2. **Task 2: Build HowTimerWorks + restyle ConfirmPurchaseBanner as accentSoft card** — `c35ad6e` (feat)
3. **Task 3: Build ReserveDetailSection + rewire RegistryPage to in-page reserve flow** — `1dd27fc` (feat)

**Plan metadata:** _pending — final docs commit added after this SUMMARY is written_

## Files Created/Modified

- `web/src/features/reservation/StickyReserveBanner.tsx` (NEW, ~115 lines) — sticky-top ink/paper banner with minute-cached aria-live
- `web/src/features/reservation/HowTimerWorks.tsx` (NEW, ~64 lines) — 4-step paperDeep aside with native <details> mobile collapse
- `web/src/features/reservation/ReserveDetailSection.tsx` (NEW, ~115 lines) — in-page Screen 02 wrapper rendered above hero when active is non-null
- `web/src/features/reservation/ReservationBanner.tsx` (REPLACED, 6 lines) — deprecated re-export of StickyReserveBanner
- `web/src/features/reservation/ConfirmPurchaseBanner.tsx` (REWRITTEN, ~80 lines) — accentSoft confirm-back card, +optional minutesLeft prop
- `web/src/pages/RegistryPage.tsx` (MODIFIED, +5/-9) — swap ReservationBanner→StickyReserveBanner, swap standalone ConfirmPurchaseBanner→ReserveDetailSection mount above hero

## Decisions Made

See key-decisions in frontmatter — minute-cached string instance for aria-live, native <details> over useState toggle, deprecated re-export to avoid touching 4 test mocks, 30-min approximation for progress bar denominator, optional minutesLeft prop with default 30, inline rgba acceptable for v1.1 (variable migration deferred to v1.2).

## Deviations from Plan

None — plan executed exactly as written. The plan revisions in commits 15ead72 (banner triples + cached aria-live string instance) and b3d2ece (subline quadruple-split + python3 fallback for image self-hosting) were already in the v1.1 plan body, so all task actions matched the plan verbatim.

## Issues Encountered

**6 pre-existing test failures persist (out of scope per Plan 13-05's verification note):**

| Test file | Failures | Status |
|-----------|---------:|--------|
| `web/src/features/registry/__tests__/ItemCard.test.tsx` | 4 | Pre-existing — Plan 13-04 ItemCard restyle changed the surface-variant/bg-primary/bg-surface-on token classes; tests still assert on old class names |
| `web/src/features/registry/__tests__/RegistryPage.test.tsx` | 1 | Pre-existing — `getByText(/Wedding · /)` now `Wedding • ` or split copy from Plan 13-04 RegistryHeader rewrite |
| `web/src/features/reservation/__tests__/ConfirmPurchaseBanner.test.tsx` | 1 | Plan 13-05 explicitly flags: "the existing ConfirmPurchaseBanner.test.tsx will likely need the getByText('Did you complete the purchase?') query updated... That's a copy/visual diff handled in Plan 07" |

Confirmed identical 6-failure set on commit `c35ad6e` (before Task 3 changes) — none introduced by this plan. All deferred to Plan 13-07 (test + copy cleanup).

The auto-reserve test suite (`RegistryPage.autoReserve.test.tsx`, the only test the plan explicitly asks the verifier to run) passes 6/6.

`web/src/lib/firestore-mapping.ts` exposes `Item.merchantDomain` so the reserved item card's "sold at {retailer}" line falls back to the active reservation's merchantDomain when the items query is still loading; if the item lookup misses entirely (e.g., snapshot delay), the price + retailer line is suppressed and only the title + Pill render — graceful degradation per UI-SPEC.

## User Setup Required

None — no external service configuration required. All design tokens and i18n keys were already shipped by Plans 13-00, 13-01, 13-02, 13-03.

## Next Phase Readiness

- **Plan 13-06** is already complete (deferred-items.md exists, summary present). Plan 13-05 unblocks **Plan 13-07** (test + copy cleanup), which will:
  - Update `ConfirmPurchaseBanner.test.tsx` copy assertion to "Bought it? Mark it as purchased."
  - Refresh `ItemCard.test.tsx` token expectations to gm-* classes
  - Refresh `RegistryPage.test.tsx` hero + occasion separator copy
- **Phase 14** (Web Fallback Live Deploy + Guest UAT) becomes addressable once 13-07 lands.
- No blockers. The Phase 5 reservation API surface (useActiveReservation + useCountdown + useConfirmPurchase + ReserveButton) is preserved verbatim; the visual transformation is purely additive at the component layer.

## Self-Check: PASSED

**Files created/modified verified:**
- FOUND: web/src/features/reservation/StickyReserveBanner.tsx
- FOUND: web/src/features/reservation/HowTimerWorks.tsx
- FOUND: web/src/features/reservation/ReserveDetailSection.tsx
- FOUND: web/src/features/reservation/ReservationBanner.tsx (deprecated re-export)
- FOUND: web/src/features/reservation/ConfirmPurchaseBanner.tsx (restyled)
- FOUND: web/src/pages/RegistryPage.tsx (rewired)

**Commits verified in git log:**
- FOUND: 562b328 feat(13-05): add StickyReserveBanner with minute-cached aria-live
- FOUND: c35ad6e feat(13-05): add HowTimerWorks + restyle ConfirmPurchaseBanner
- FOUND: 1dd27fc feat(13-05): add ReserveDetailSection + rewire RegistryPage to in-page reserve flow

**Verification gates:**
- typecheck: PASSED (`tsc --noEmit` exits 0)
- build: PASSED (`vite build` succeeds, 1807 modules transformed)
- auto-reserve test: PASSED (6/6 in RegistryPage.autoReserve.test.tsx)

---
*Phase: 13-web-fallback-visual-refresh*
*Completed: 2026-05-07*
