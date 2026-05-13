---
phase: quick-260513-ge9
plan: 01
subsystem: ui
tags: [react, i18n, i18next, web, progress-strip, owner-detection]

requires:
  - phase: quick-260512-vlg
    provides: "i18n mirror convention (web/src/i18n + web/i18n kept in lockstep)"
  - phase: quick-260513-fnm
    provides: "web_hero namespace confirmed as runtime i18n source"
provides:
  - "Fixed duplicate count in PROGRESS tile — body reads 'of N chosen' (EN) / 'din N alese' (RO)"
  - "Share CTA gated on isOwner — guests and non-owners never see it"
  - "First web-side isOwner pattern (inline one-liner in RegistryPage, not a hook)"
  - "ProgressStrip.test.tsx with 4 behavioral specs locking both fixes"
affects: [web-registry-page, progress-strip, i18n-web-hero, owner-ux]

tech-stack:
  added: []
  patterns:
    - "isOwner as inline one-liner: !!user && registryQ.data?.ownerId === user.uid — not extracted to a hook when used in exactly one place"
    - "i18n mirror convention: web/src/i18n/{en,ro}.json (runtime) and web/i18n/{en,ro}.json (legacy/tooling) edited in lockstep"

key-files:
  created:
    - web/src/features/registry/__tests__/ProgressStrip.test.tsx
  modified:
    - web/src/features/registry/ProgressStrip.tsx
    - web/src/pages/RegistryPage.tsx
    - web/src/i18n/en.json
    - web/src/i18n/ro.json
    - web/i18n/en.json
    - web/i18n/ro.json

key-decisions:
  - "isOwner computed inline in RegistryPage (not extracted to useIsOwner hook) — used in exactly one place, one-liner is clearer inline"
  - "{{n}} removed entirely from progress_copy i18n template rather than conditionally hiding the span — simpler and removes the bug at the source"
  - "ProgressStrip isOwner prop defaults to false — safe for any caller that omits it (giver/guest view is the default)"

patterns-established:
  - "Web isOwner pattern: !!user && registry.ownerId === user.uid — first web usage, established as inline not hook"

requirements-completed:
  - QUICK-GE9-01
  - QUICK-GE9-02

duration: 8min
completed: 2026-05-13
---

# Quick Task 260513-ge9: PROGRESS Tile Duplicate Count Fix and Owner-Gated Share CTA

**Removed duplicate count from web PROGRESS tile ({{n}} dropped from i18n template) and gated the Share CTA on isOwner, hiding it from guests and non-owner signed-in users.**

## Performance

- **Duration:** ~8 min
- **Started:** 2026-05-13T11:50:00Z
- **Completed:** 2026-05-13T11:58:00Z
- **Tasks:** 1 of 1 complete (Task 2 is a human-verify checkpoint)
- **Files modified:** 7

## Root Cause and Fix

**Duplicate count bug:** `web_hero.progress_copy` was `"{{n}} of {{total}} chosen"` with the t() call passing `{ n: totalChosen, total }`. The component also rendered `{totalChosen}` in a separate display span above — so the count appeared twice ("2 2 of 3 chosen"). Fix: dropped `{{n}}` from all 4 i18n files (EN+RO, runtime `web/src/i18n/*` and mirror `web/i18n/*`) and removed the `n: totalChosen` argument from the t() call. Body now reads "of 3 chosen" with the big numeral "2" above it.

**Share CTA gating:** The `<Btn>` for "Share this registry" previously rendered unconditionally regardless of who was viewing. Added `isOwner?: boolean` prop to `ProgressStrip` (defaults to `false`). In `RegistryPage`, computed `const isOwner = !!user && registryQ.data?.ownerId === user.uid` immediately after `totalChosen`/`total` derivation, then passed to `<ProgressStrip isOwner={isOwner} />`. When `registryQ.data` is undefined (loading) or when `user` is null (guest), the expression safely evaluates to `false` and the Share button is absent.

## Accomplishments

- Duplicate "2 2 of 3 chosen" rendering eliminated — body always reads "of N chosen" (EN) / "din N alese" (RO)
- Share CTA hidden for guests and non-owner signed-in users; visible only when `user.uid === registry.ownerId`
- i18n mirror convention respected — all 4 JSON files updated in lockstep
- New `ProgressStrip.test.tsx` with 4 vitest specs locks both behaviors; full suite (120 tests, 24 files) remains green
- TypeScript compiles clean with no errors

## Task Commits

1. **Task 1: Fix duplicate count and gate Share button on isOwner (TDD)** - `b682190` (fix)

## Files Created/Modified

- `web/src/features/registry/__tests__/ProgressStrip.test.tsx` - New vitest spec: 4 cases covering body copy, Share hidden, Share shown, zero state
- `web/src/features/registry/ProgressStrip.tsx` - Added `isOwner?: boolean` prop; removed `n` from t() call; wrapped Share Btn in `{isOwner && (...)}`
- `web/src/pages/RegistryPage.tsx` - Added `const isOwner = !!user && registryQ.data?.ownerId === user.uid`; passed to `<ProgressStrip>`
- `web/src/i18n/en.json` - `progress_copy`: `"{{n}} of {{total}} chosen"` → `"of {{total}} chosen"`
- `web/src/i18n/ro.json` - `progress_copy`: `"{{n}} din {{total}} alese"` → `"din {{total}} alese"`
- `web/i18n/en.json` - Mirror of above (EN)
- `web/i18n/ro.json` - Mirror of above (RO)

## i18n Mirror Convention

Both `web/src/i18n/` (runtime, loaded by Vite) and `web/i18n/` (legacy mirror for tooling/parity) were updated in lockstep per the convention from quick-260512-vlg and quick-260513-fnm. The `{{n}}` placeholder is absent from all 4 files.

## Decisions Made

- `isOwner` computed inline in `RegistryPage` — deliberately NOT extracted to a hook. Used in exactly one place; a one-liner is clearer than a named abstraction. Android's `isOwner` helper (from 260507-uzv) lives in Kotlin and doesn't apply here.
- Dropped `{{n}}` from the i18n template rather than rendering a conditional span — simpler, less JSX, removes the bug at the source rather than papering over it in the component.

## Deviations from Plan

None - plan executed exactly as written.

## Known Stubs

None.

## Issues Encountered

None.

## Next Phase Readiness

- Task 2 is a human-verify checkpoint requiring manual visual confirmation at `/registry/:id` for owner, guest, non-owner, and RO locale views.
- After approval, this task is fully complete.

---
*Phase: quick-260513-ge9*
*Completed: 2026-05-13*
