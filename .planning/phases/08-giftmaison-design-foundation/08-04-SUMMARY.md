---
phase: 08-giftmaison-design-foundation
plan: 04
subsystem: ui-theme
tags: [compose, shapes, spacing, shadows, des-04, wave-1]

requires:
  - plan: 08-01
    provides: ShapesAndDimensTest.kt scaffold
provides:
  - GiftMaisonShapes @Immutable data class (7 radii: 8/10/12/14/16/22/CircleShape)
  - GiftMaisonSpacing @Immutable data class (edge/edgeWide + 8 gaps)
  - GiftMaisonShadows object (fab/googleBanner/bottomSheet elevation tokens)
  - Modifier.fabShadow(tint) / googleBannerShadow(tint) / bottomSheetShadow() extensions
affects: [08-05-theme-rewire, phase-9-chrome-FAB-nav-sheet, phase-10-screens, phase-11-screens]

tech-stack:
  patterns:
    - "Radius 999 = CircleShape (Compose semantic for fully-rounded pills — adapts to container height)"
    - "Shadows exposed as BOTH elevation Dp values (object GiftMaisonShadows) AND Modifier extensions — ergonomic at callsites (Modifier.fabShadow(accent)) and testable at data level"

key-files:
  created:
    - app/src/main/java/com/giftregistry/ui/theme/GiftMaisonShapes.kt
    - app/src/main/java/com/giftregistry/ui/theme/GiftMaisonSpacing.kt
    - app/src/main/java/com/giftregistry/ui/theme/GiftMaisonShadows.kt

key-decisions:
  - "Spacing canonicalized via enumeration (edge/edgeWide + gap4..gap20) instead of a 4-step base grid — handoff uses ad-hoc values (6/10/12/14/16/20) that don't cleanly ladder. Resolves Research Open Question 3"
  - "Shadows use API 28+ spot/ambient colour; on API 23-27 fallback to default Compose shadow rendering (handoff shadows are ornamental — colour loss is acceptable)"

commits:
  - hash: 6c55fc1
    message: "feat(08-04): add GiftMaisonShapes + GiftMaisonSpacing tokens"
  - hash: 78afcba
    message: "feat(08-04): GiftMaisonShadows — FAB / Google banner / bottom sheet"

deviations:
  - "Wave 1 agent was rate-limited mid-execution. Shapes+Spacing committed (6c55fc1); Shadows.kt landed on disk uncommitted. Orchestrator committed inline on 2026-04-21 as 78afcba after rate limit reset."
---

# Plan 08-04: Shapes + Spacing + Shadows (SUMMARY)

## What shipped

- **GiftMaisonShapes.kt** — 7 RoundedCornerShape values at handoff radii (8/10/12/14/16/22 dp) plus `pill = CircleShape` for 999-radius semantics
- **GiftMaisonSpacing.kt** — named `edge` (16 dp) / `edgeWide` (20 dp) + 8 gap values (4/6/8/10/12/14/16/20 dp)
- **GiftMaisonShadows.kt** — elevation tokens (FAB 20dp / banner 24dp / bottom sheet 40dp) + Modifier extensions that wrap the correct shape + elevation + tint tuple

## Verification

- `./gradlew :app:testDebugUnitTest --tests 'com.giftregistry.ui.theme.ShapesAndDimensTest'` → green

## Requirements closed

- **DES-04** (spacing / radii / shadows): complete
