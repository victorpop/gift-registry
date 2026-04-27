---
phase: quick-260427-gld
plan: "01"
subsystem: ui/theme
tags: [visual-fix, typography, layout, wordmark, auth-screen]
dependency_graph:
  requires: [quick-260421-lwi]
  provides: [readable-wordmark, correct-banner-height]
  affects: [GiftMaisonWordmark, ConcentricRings, GoogleBanner, Auth screen]
tech_stack:
  added: []
  patterns: [matchParentSize overlay pattern, caller-controlled Canvas sizing]
key_files:
  modified:
    - app/src/main/java/com/giftregistry/ui/theme/GiftMaisonWordmark.kt
    - app/src/main/java/com/giftregistry/ui/common/ConcentricRings.kt
    - app/src/main/java/com/giftregistry/ui/auth/GoogleBanner.kt
decisions:
  - "letterSpacing expressed in .sp (not .em) — consistent with quick-260421-lwi pattern for all 10 type roles"
  - "ConcentricRings Canvas uses caller modifier directly — no internal .size(70.dp); callers pass matchParentSize() for overlay use"
  - "GoogleBanner wraps rings in matchParentSize Box sibling — post-measurement sizing does not affect parent's intrinsic height"
metrics:
  duration: ~5 min
  completed: "2026-04-27T09:01:30Z"
  tasks_completed: 1
  tasks_total: 2
  files_modified: 3
---

# Quick Task 260427-gld: Fix Wordmark LetterSpacing em→sp and Decouple ConcentricRings from Banner Height — Summary

**One-liner:** Fixed two Auth screen visual bugs: wordmark letterSpacing flipped from `.em` (−8 sp crush) to `.sp` (−0.4 sp gentle tracking), and ConcentricRings decoupled from GoogleBanner height so banner measures at ~68 dp instead of ~98 dp.

## What Was Built

Three coordinated surgical edits that resolve two surface-level visual bugs introduced during Phase 8/10 redesign work:

**Bug 1 — Wordmark unreadable (em→sp):** `GiftMaisonWordmark.kt` had `letterSpacing = (-0.4).em`. At 20 sp font size that's −8 sp per character — mashing "giftmaison." into an unreadable dark smear. This is the same em→sp unit bug fixed across GiftMaisonTypography.kt in quick-260421-lwi (commit 7743d35) but the wordmark file was missed because it lives outside the type-scale file. Fix: `(-0.4).em` → `(-0.4).sp` and removed the now-unused `import androidx.compose.ui.unit.em`.

**Bug 2 — Google banner height inflated by ConcentricRings:** `ConcentricRings.kt` hardcoded `Modifier.size(70.dp)` on its Canvas. In `GoogleBanner.kt` the rings were a sibling inside the outer Box, claiming 70 dp intrinsic height. This forced the banner's measured height to `max(Row ≈ 40 dp, Rings 70 dp) + padding 14×2 = ~98 dp` instead of the handoff-intended ~68 dp.

Fix decomposition:
1. `ConcentricRings.kt` — dropped `.size(70.dp)`, now `Canvas(modifier = modifier)`. Caller controls sizing. KDoc updated to note `Modifier.matchParentSize()` for overlay use. Drawing math (offsets, radii, alpha, stroke) untouched.
2. `GoogleBanner.kt` — wrapped the `ConcentricRings(...)` call in a `Box(modifier = Modifier.matchParentSize())` sibling. The matchParentSize Box does not contribute to the parent's intrinsic height (post-measurement sizing). Banner height is now governed solely by Row content + vertical padding = ~68 dp. Rings still draw at `Offset(size.width, 0)` so the corner decoration is visually identical.

## Commits

| Hash | Message |
|------|---------|
| 82d8125 | fix(quick-260427-gld): wordmark em→sp + decouple ConcentricRings from banner height |

## Deviations from Plan

None — plan executed exactly as written.

## Verification

- `./gradlew :app:compileDebugKotlin` — BUILD SUCCESSFUL (exits 0)
- `./gradlew :app:testDebugUnitTest --tests WordmarkTest --tests TypographyTest` — BUILD SUCCESSFUL; all 5 WordmarkTest assertions + 11 TypographyTest assertions green
- All done-criteria grep checks passed (letterSpacing .sp: 1, .em: 0, matchParentSize: 2, TopEnd alignment: 0, Canvas modifier: 1, size import: 0)

## Known Stubs

None introduced by this plan.

## Self-Check: PASSED

- `/Users/victorpop/ai-projects/gift-registry/app/src/main/java/com/giftregistry/ui/theme/GiftMaisonWordmark.kt` — exists, modified
- `/Users/victorpop/ai-projects/gift-registry/app/src/main/java/com/giftregistry/ui/common/ConcentricRings.kt` — exists, modified
- `/Users/victorpop/ai-projects/gift-registry/app/src/main/java/com/giftregistry/ui/auth/GoogleBanner.kt` — exists, modified
- Commit 82d8125 — verified in git log
