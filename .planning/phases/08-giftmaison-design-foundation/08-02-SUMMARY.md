---
phase: 08-giftmaison-design-foundation
plan: 02
subsystem: ui-theme
tags: [compose, colors, des-03, wave-1]

requires:
  - plan: 08-01
    provides: ColorsTest.kt scaffold in RED state
provides:
  - GiftMaisonColors @Immutable data class with 13 Housewarming tokens
  - housewarmingColors() factory returning sRGB hex Color values
  - LocalGiftMaisonColors CompositionLocal
affects: [08-05-theme-rewire, phase-9-chrome, phase-10-screens]

tech-stack:
  patterns:
    - "sRGB hex Color(0xFF...) values per handoff endorsement (oklch→sRGB conversion done ahead-of-time)"
    - "@Immutable data class + staticCompositionLocalOf for stable theme tokens"

key-files:
  created:
    - app/src/main/java/com/giftregistry/ui/theme/GiftMaisonColors.kt

key-decisions:
  - "Default for LocalGiftMaisonColors is Color.Unspecified across all 13 fields — crashes loudly if read outside GiftRegistryTheme scope rather than silently rendering black"

commits:
  - hash: b5ba125
    message: "feat(08-02): add GiftMaisonColors data class + housewarmingColors() factory + LocalGiftMaisonColors"
---

# Plan 08-02: Housewarming colour palette (SUMMARY)

## What shipped

`GiftMaisonColors.kt` — the authoritative colour token module for v1.1:

- 13-field `@Immutable data class GiftMaisonColors` (paper/paperDeep/ink/inkSoft/inkFaint/line/accent/accentInk/accentSoft/second/secondSoft/ok/warn)
- `housewarmingColors()` factory returning handoff-exact sRGB hex values
- `LocalGiftMaisonColors` CompositionLocal, wired into `GiftRegistryTheme` by Plan 08-05

## Verification

- `./gradlew :app:testDebugUnitTest --tests 'com.giftregistry.ui.theme.ColorsTest'` → 14 tests green (13 per-token + 1 count)
- Every hex value verified against `design_handoff_android_owner_flow/README.md` Housewarming table

## Requirements closed

- **DES-03** (Housewarming colour palette): complete

## Downstream

Plan 08-05 consumes `housewarmingColors()` to build `LightColorScheme` in `Color.kt`. Phase 9 chrome reads `GiftMaisonTheme.colors.accent` / `accentSoft` / `inkFaint` for bottom nav + FAB + status chips.
