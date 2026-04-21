---
phase: quick-260421-lwi
plan: 01
subsystem: android/ui/theme
tags: [typography, letter-spacing, unit-fix, design-handoff]
dependency_graph:
  requires: []
  provides: [correct-letter-spacing-across-all-screens]
  affects: [GiftMaisonTypography, TypographyTest, every screen reading MaterialTheme.typography]
tech_stack:
  added: []
  patterns: [letterSpacing expressed as .sp for pixel-equivalent handoff values]
key_files:
  modified:
    - app/src/main/java/com/giftregistry/ui/theme/GiftMaisonTypography.kt
    - app/src/test/java/com/giftregistry/ui/theme/TypographyTest.kt
decisions:
  - "letterSpacing uses .sp (not .em) — JSX naked numerics default to pixels in React inline styles, so .sp is the pixel-equivalent Compose unit"
  - "lineHeight stays .em — CSS unitless line-height IS a multiplier, matching Compose .em semantics"
  - "KDoc updated in place to prevent future planners from repeating the em-for-letter-spacing mistake"
metrics:
  duration: ~5min
  completed_date: "2026-04-21"
  tasks_completed: 2
  tasks_total: 2
  files_modified: 2
---

# Phase quick-260421-lwi Plan 01: Fix Typography Letter-Spacing Units (em→sp) Summary

**One-liner:** Swapped all 10 GiftMaisonTypography letterSpacing values from `.em` to `.sp` so the JSX naked-numeric handoff values render as pixel-equivalent tracking instead of font-size multiples.

## What Was Built

The Phase 8 Plan 03 implementation had applied `.em` to every `letterSpacing` value, based on 08-RESEARCH.md Pitfall 3's incorrect claim that the handoff used CSS em convention for letter-spacing. In reality, the JSX source (`reference/GiftMaison - gift registry mocks.html`) uses bare numeric values — e.g. `letterSpacing: -0.2` — which React inline styles treat as **pixels**, not em.

This caused severe letter-spacing distortion on every screen:
- displayXL (32 sp, −0.8 em): rendered −25.6 sp tracking instead of −0.8 sp
- monoCaps (9.5 sp, 1.5 em): rendered 14.25 sp tracking instead of 1.5 sp

The fix is a pure unit swap: `.em` → `.sp` on all 10 `letterSpacing` lines in GiftMaisonTypography.kt. `lineHeight` values remain `.em` (correct — CSS unitless line-height is a multiplier). The KDoc was updated to document this distinction so future contributors don't repeat the mistake.

## Tasks Completed

| Task | Name | Commit | Files Modified |
|------|------|--------|---------------|
| 1 | Flip letterSpacing units em→sp (production + test) | 7743d35 | GiftMaisonTypography.kt, TypographyTest.kt |
| 2 | On-device visual check | approved by user 2026-04-21 | — |

## Verification Results

- `grep -c "letterSpacing = .*\.em" GiftMaisonTypography.kt` → 0 (zero .em letter-spacings remain)
- `grep -c "letterSpacing = .*\.sp" GiftMaisonTypography.kt` → 10 (all 10 roles in .sp)
- `grep -c "lineHeight = .*\.em" GiftMaisonTypography.kt` → 10 (unchanged)
- `grep -c "\.em, t\..*\.letterSpacing" TypographyTest.kt` → 0
- `grep -c "\.sp, t\..*\.letterSpacing" TypographyTest.kt` → 8
- `./gradlew :app:testDebugUnitTest --tests 'com.giftregistry.ui.theme.TypographyTest'` → BUILD SUCCESSFUL (11/11 tests green)
- `./gradlew :app:compileDebugKotlin` → BUILD SUCCESSFUL

## Deviations from Plan

None — plan executed exactly as written. Both files updated in one atomic commit; all done criteria verified before commit.

## Known Stubs

None. This is a pure unit correction — no data wiring or placeholder values.

## Self-Check: PASSED

- GiftMaisonTypography.kt: modified (verified via grep counts above)
- TypographyTest.kt: modified (verified via grep counts above)
- Commit 7743d35: present (`git log --oneline -1` confirms)
