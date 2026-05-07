---
phase: 13-web-fallback-visual-refresh
plan: 03
subsystem: i18n
tags: [i18n, i18next, json, localization, romanian, english, web]

# Dependency graph
requires:
  - phase: 13-web-fallback-visual-refresh-00
    provides: Tailwind gm.* tokens + index.css :root variables (visual foundation that consumers of these keys will style against)
  - phase: 05-web-fallback
    provides: i18next runtime + legacy seed sync contract (web/i18n/*.json byte-identical to web/src/i18n/*.json)
provides:
  - 6 new feature-namespaced translation namespaces (web_nav, web_footer, web_hero, web_reserve, web_pill, web_auth) with 60 new keys per locale (61 EN keys verified, 104 total leaf keys including pre-existing app/common/auth/registry/reservation/notifications/settings)
  - Banner heading triple (banner_heading_pre/emphasis/post) and banner subline quadruple (banner_subline_pre/retailer/separator/countdown) so Plan 13-05's StickyReserveBanner can compose italic Display spans + retailer slots without `.replace()` chains or blank-substitution
  - Banner aria-live key (banner_aria_live) for minute-flip-only screen-reader announcements
  - Detail headline pre/emphasis/post triples and step-by-step "How the timer works" copy for Plan 13-05's reserve detail page
  - Editorial quote + attribution copy for Plan 13-06's auth landing
affects: [13-04-registry-restyle, 13-05-sticky-banner-reserve-detail, 13-06-auth-restyle, 13-07-reservation-feature-tests]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Feature-namespaced i18n keys: web_nav, web_footer, web_hero, web_reserve, web_pill, web_auth — extends existing app/common/auth/registry convention"
    - "Banner triple/quadruple key shape: _pre + _emphasis + _post (heading) and _pre + _retailer + _separator + _countdown (subline) — mirrors Phase 5 detail_headline_pre/emphasis/post pattern; consumer concatenates without manual whitespace handling, leaving each translator slot independently editable"
    - "Trailing/leading whitespace lives inside _pre and _post values intentionally so consumers compose pre + emphasis + post without inserting spaces in the call site"
    - "Legacy seed sync (Phase 5 D-locked): web/i18n/*.json byte-identical to web/src/i18n/*.json via plain `cp` — manual contract for now, future plans may add postupdate npm script"

key-files:
  created: []
  modified:
    - web/src/i18n/en.json
    - web/src/i18n/ro.json
    - web/i18n/en.json
    - web/i18n/ro.json

key-decisions:
  - "Banner heading + subline shipped as triples/quadruples (not single interpolated strings) — pre-empts the brittle .replace() chain Plan 05 would have needed for italic-Display item-name span and separately-testable countdown digit element"
  - "Separator key web_reserve.banner_subline_separator='  ·  ' (two-space middle-dot two-space) lives as its own slot so RO can choose a different separator if a future translator request lands"
  - "web_auth.headline_pre kept empty string ('') because both EN and RO headlines start with the italic span (Pick up / Continuă) — the empty pre slot is the contract that pinpoints whether the italic emphasis renders first"
  - "Reused existing detail_headline_pre/emphasis/post pattern (already shipped in earlier Phase 5/13 work) as the structural template for the new banner triples — preserves a single mental model for Plan 05 implementation"
  - "104-key parity verified between en.json and ro.json (zero missing in RO, zero extra in RO) — single source of truth for parity is automated check in Task 2 verify"

patterns-established:
  - "Translation key parity check: `node -e 'flatKeys(en) === flatKeys(ro)'` — runs in <50ms, future plans should re-run after adding any key"
  - "Legacy seed sync gate: `diff -q web/src/i18n/*.json web/i18n/*.json` must report no differences before any commit touching i18n"

requirements-completed: [D-18]

# Metrics
duration: 3min
completed: 2026-05-07
---

# Phase 13 Plan 03: Wave 2 i18n Key Migration Summary

**Six feature-namespaced i18n namespaces (web_nav, web_footer, web_hero, web_reserve, web_pill, web_auth) shipped with 60 new keys per locale and banner triple/quadruple key shape so Wave 3+ screen plans can compose italic spans without .replace() chains.**

## Performance

- **Duration:** ~3 min
- **Started:** 2026-05-07T19:59:13Z
- **Completed:** 2026-05-07T20:01:52Z
- **Tasks:** 3
- **Files modified:** 4

## Accomplishments
- 60 new EN translation keys live across 6 web_* namespaces in web/src/i18n/en.json (verified by 61-key dot-notation lookup test)
- 60 matching RO translations in web/src/i18n/ro.json with native Romanian typographic quotes (low double / right double on editorial quote and step3_h)
- 104-key byte-precise parity between EN and RO confirmed (zero keys missing in RO, zero keys extra in RO)
- Banner heading triple + banner subline quadruple shipped as separate i18n slots so Plan 05 StickyReserveBanner can wrap item names in italic Display spans and render countdown digits in a separately-testable element without post-processing
- web/i18n/{en,ro}.json legacy seed re-synced byte-identical to web/src/i18n/{en,ro}.json (Phase 5 D-locked decision honored)
- All 107 existing web tests stay green (additive change, no consumer is yet calling t('web_*'))

## Task Commits

Each task was committed atomically (with --no-verify per parallel-executor convention):

1. **Task 1: Add web_* namespaces to en.json** — `e50c079` (feat)
2. **Task 2: Add web_* namespaces to ro.json with Romanian translations** — `71a39a8` (feat)
3. **Task 3: Sync legacy i18n seed byte-identical to src/i18n** — `1d98177` (chore)

## Files Created/Modified
- `web/src/i18n/en.json` — Extended with 6 new top-level namespaces (web_nav, web_footer, web_hero, web_reserve, web_pill, web_auth); existing 7 namespaces (app, common, auth, registry, reservation, notifications, settings) preserved verbatim
- `web/src/i18n/ro.json` — Mirror of en.json with native Romanian translations including typographic quotes („…”) on editorial quote and how_timer_step3_h
- `web/i18n/en.json` — Legacy seed, byte-identical to web/src/i18n/en.json
- `web/i18n/ro.json` — Legacy seed, byte-identical to web/src/i18n/ro.json

## Decisions Made

- **Banner key shape change (v1.1 plan revision honored):** Shipped banner_heading as `_pre` / `_emphasis` / `_post` triple and banner_subline as `_pre` / `_retailer` / `_separator` / `_countdown` quadruple — replaces the v1.0 single-interpolated-string design that would have forced `.replace('  ·  ', '').replace('· ', '')` chains in Plan 05's StickyReserveBanner. Each slot is independently editable by translators, the separator slot survives RO localization preferences, and Plan 05 can render the countdown digits in a separately-testable `<span data-testid="banner-mmss">` while keeping the trailing word "remaining" inline.
- **Trailing/leading whitespace lives inside _pre and _post:** `_pre` ends with a space, `_post` may start with a space — consumer concatenates without manual whitespace handling. This matches the pre-existing `detail_headline_*` triple convention.
- **Used literal Unicode characters (not \u escapes) in the new namespaces:** Plan suggested mirroring the existing file's \u escape pattern, but the existing file inconsistently mixes literal and escaped (e.g., line 49 of original ro.json had literal `Redirecționare` while line 11 had `încarcă`). After Task 1 verify passed cleanly with literal chars, kept literal chars throughout for readability — JSON parsers handle both identically.

## Deviations from Plan

None - plan executed exactly as written. The v1.1 banner triple/quadruple revision was already baked into the plan body; Task 1 used the literal-character form rather than \u escapes throughout the new namespaces (a presentational nuance, not a contract change — verify checks resolved all 61 keys).

## Issues Encountered
None.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Plan 13-04 (Registry restyle) can call `t('web_hero.section_title')`, `t('web_hero.public_pill')`, `t('web_hero.progress_copy', { n: 4, total: 12 })`, `t('web_hero.reserve_cta')` etc. without further key edits.
- Plan 13-05 (sticky banner + reserve detail) can call:
  - Banner: `t('web_reserve.banner_heading_pre')` + wrap `t('web_reserve.banner_heading_emphasis', { itemName })` in italic span + `t('web_reserve.banner_heading_post')`
  - Subline: `t('web_reserve.banner_subline_pre')` + `t('web_reserve.banner_subline_retailer', { retailer })` + `t('web_reserve.banner_subline_separator')` + `t('web_reserve.banner_subline_countdown', { mmss })` — split the countdown render so digits sit in `<span data-testid="banner-mmss">`
  - aria-live: `t('web_reserve.banner_aria_live', { minutes })` — cache by minute so screen readers only re-announce on minute flip
  - Reserve detail body: `t('web_reserve.detail_caption')`, `t('web_reserve.detail_headline_*')`, `t('web_reserve.how_timer_step1_h')` etc.
- Plan 13-06 (Auth restyle) can call `t('web_auth.caption')`, `t('web_auth.headline_emphasis')`, `t('web_auth.editorial_quote')` etc.
- Plan 13-07 (reservation tests) can assert against the new banner aria-live + countdown copy.

---
*Phase: 13-web-fallback-visual-refresh*
*Plan: 03*
*Completed: 2026-05-07*

## Self-Check: PASSED

- web/src/i18n/en.json: FOUND
- web/src/i18n/ro.json: FOUND
- web/i18n/en.json: FOUND
- web/i18n/ro.json: FOUND
- .planning/phases/13-web-fallback-visual-refresh/13-03-SUMMARY.md: FOUND
- Commit e50c079 (Task 1): FOUND
- Commit 71a39a8 (Task 2): FOUND
- Commit 1d98177 (Task 3): FOUND
