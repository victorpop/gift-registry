---
phase: 08-giftmaison-design-foundation
plan: 05
subsystem: ui-theme
tags: [compose, wordmark, theme-wrapper, material3-integration, des-05, wave-2]

requires:
  - plan: 08-02
    provides: housewarmingColors() + LocalGiftMaisonColors
  - plan: 08-03
    provides: giftMaisonTypography() + InstrumentSerifFamily + LocalGiftMaisonTypography
  - plan: 08-04
    provides: giftMaisonShapes() + giftMaisonSpacing() + LocalGiftMaisonShapes/Spacing
provides:
  - wordmarkAnnotatedString(ink, accent) pure-Kotlin factory
  - GiftMaisonWordmark(modifier, fontSize) @Composable
  - GiftRegistryTheme rewrite — wraps MaterialTheme with 4 CompositionLocals
  - GiftMaisonTheme accessor object (colors/typography/shapes/spacing)
  - LightColorScheme / DarkColorScheme mapped from Housewarming tokens
  - GiftRegistryTypography mapped from GiftMaisonTypography roles
  - StyleGuidePreview.kt — 4 @Preview composables
affects: [phase-9-chrome, phase-10-onboarding-home, phase-11-detail-create-additem, ALL phase-2-7-screens-via-re-skin]

tech-stack:
  patterns:
    - "Wrap-not-replace MaterialTheme via CompositionLocalProvider — 42+ existing MaterialTheme.colorScheme.* callsites re-skin to Housewarming automatically (intended side effect)"
    - "Wordmark as single Text(AnnotatedString) with two SpanStyles — preserves glyph-to-glyph kerning vs two adjacent Text composables"
    - "remember(ink, accent) key on wordmark AnnotatedString — future v1.2 theme switching invalidates cache correctly (Pitfall 6)"
    - "@Composable getters on GiftMaisonTheme object — idiomatic Compose API surface (mirrors MaterialTheme accessor pattern)"

key-files:
  created:
    - app/src/main/java/com/giftregistry/ui/theme/GiftMaisonWordmark.kt
    - app/src/main/java/com/giftregistry/ui/theme/preview/StyleGuidePreview.kt
  modified:
    - app/src/main/java/com/giftregistry/ui/theme/Theme.kt
    - app/src/main/java/com/giftregistry/ui/theme/Color.kt
    - app/src/main/java/com/giftregistry/ui/theme/Type.kt

key-decisions:
  - "M3 ColorScheme mapping: primary=accent, primaryContainer=accentSoft, secondary=second, background=paper, surfaceVariant=paperDeep, outline=line, error=warn — resolves 42+ existing callsites to Housewarming values"
  - "M3 Typography mapping: displayLarge=displayXL, headlineLarge=displayL, titleLarge=displayS, bodyLarge=bodyL, labelLarge=bodyMEmphasis, labelSmall=monoCaps"
  - "v1.1 forces light mode (Pitfall 4). darkTheme parameter preserved for source-compat with MainActivity.kt:48 but ignored. DarkColorScheme retained structurally (values = LightColorScheme); restoring dark mode is v1.2+."
  - "Wordmark default fontSize = 20.sp with per-callsite override parameter — resolves Research Open Question 1"

commits:
  - hash: fb3fb5a
    message: "feat(08-05): wordmark + theme wrapper rewire + style guide preview"

verification:
  automated:
    - "./gradlew :app:testDebugUnitTest --tests 'com.giftregistry.ui.theme.*' → all 5 suites green"
    - "./gradlew :app:testDebugUnitTest → full suite green, no Phase 2-7 regressions"
    - "./gradlew :app:assembleDebug → BUILD SUCCESSFUL, all existing screens compile through new theme"
  manual:
    - "Open StyleGuidePreview.kt in Android Studio → 4 @Preview panels render type scale / palette / radii+shadow / wordmark on 360 dp canvas (reviewer compares against design_handoff HTML prototype)"
---

# Plan 08-05: Wordmark + Theme Wrapper Rewire + Style Guide Preview (SUMMARY)

## What shipped

### 1. `GiftMaisonWordmark.kt` — DES-05

- `wordmarkAnnotatedString(ink, accent)` pure Kotlin factory; returns `AnnotatedString("giftmaison.")` with two SpanStyle blocks (ink body, accent period) preserving kerning
- `@Composable GiftMaisonWordmark(modifier, fontSize = 20.sp)` — reads `GiftMaisonTheme.colors.ink/accent`, renders via `Text(AnnotatedString)` in Instrument Serif italic with `letterSpacing = (-0.4).em`

### 2. Theme wrapper rewire — integration

- **Theme.kt** — `GiftRegistryTheme` now wraps `MaterialTheme` via `CompositionLocalProvider` exposing `LocalGiftMaisonColors/Typography/Shapes/Spacing`. Adds `GiftMaisonTheme` accessor object with 4 `@Composable get()` fields for Phase 9+ bespoke-token consumption.
- **Color.kt** — `LightColorScheme` / `DarkColorScheme` rebuilt from `housewarmingColors()` with explicit M3 slot mapping (primary=accent, surfaceVariant=paperDeep, etc.)
- **Type.kt** — `GiftRegistryTypography` rebuilt from `giftMaisonTypography()` with M3 slot mapping (displayLarge=displayXL, labelSmall=monoCaps, etc.)

### 3. `StyleGuidePreview.kt` — verification harness

- 4 private `@Preview` composables covering success criteria 2/3/4/5: type scale (10 roles), palette (13 swatches), radii+spacing+shadow, wordmark (3 sizes)

## Verification

- All 5 theme test suites green (Colors / Fonts / Typography / ShapesAndDimens / Wordmark)
- Full unit test suite green — no Phase 2-7 regressions
- `./gradlew :app:assembleDebug` — BUILD SUCCESSFUL
- Style-guide previews render in Android Studio (pending human review for colour fidelity vs HTML prototype)

## Requirements closed

- **DES-05** (wordmark): complete
- Integration success criteria 1-5 from ROADMAP Phase 8: all covered

## Side effect — existing screens re-skin automatically

Phase 2-7 screens (42+ `MaterialTheme.colorScheme.*` read sites) now render in Housewarming terracotta + Instrument Serif / Inter / JetBrains Mono without any screen-level code changes. This is the intended behaviour per Plan 08-05 Pitfall 5. Per-screen pixel-accurate redesigns are owned by Phases 10/11 and will not roll back this automatic re-skin.
