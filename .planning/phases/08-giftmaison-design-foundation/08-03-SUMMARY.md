---
phase: 08-giftmaison-design-foundation
plan: 03
subsystem: ui-theme
tags: [compose, google-fonts, typography, des-01, des-02, wave-1]

requires:
  - plan: 08-01
    provides: ui-text-google-fonts dep + font_certs.xml + FontsTest/TypographyTest scaffolds
provides:
  - giftMaisonFontProvider (GoogleFont.Provider) at com.google.android.gms.fonts
  - InstrumentSerifFamily / InterFamily / JetBrainsMonoFamily
  - GiftMaisonTypography @Immutable data class (10 TextStyle roles)
  - giftMaisonTypography() factory
  - LocalGiftMaisonTypography CompositionLocal
affects: [08-05-theme-rewire, phase-9-nav-labels, phase-10-screens, phase-11-screens]

tech-stack:
  patterns:
    - "Google Fonts downloadable-fonts API via androidx.compose.ui:ui-text-google-fonts (no bundled .ttf fallback for v1.1)"
    - "em-based letter-spacing + line-height matches handoff CSS em values exactly"
    - "PlatformTextStyle(includeFontPadding = false) + LineHeightStyle.Center on every TextStyle — avoids ~20% vertical inflation (Pitfall 2)"

key-files:
  created:
    - app/src/main/java/com/giftregistry/ui/theme/GiftMaisonFonts.kt
    - app/src/main/java/com/giftregistry/ui/theme/GiftMaisonTypography.kt
  modified:
    - app/src/test/java/com/giftregistry/ui/theme/FontsTest.kt  # GoogleFont.Provider internal-prop refactor

key-decisions:
  - "Provider authority/package exposed as module-level constants (GMS_FONTS_AUTHORITY / GMS_FONTS_PACKAGE) because GoogleFont.Provider's properties became internal in ui-text-google-fonts 1.10.5 — tests now assert against the constants"
  - "Body M ships as TWO variants: bodyM (W400 default) + bodyMEmphasis (W500) — resolves handoff's '400-500' range explicitly per Research Open Question 5"
  - "monoCaps letterSpacing set to 1.5.em (upper end of handoff's 0.6-1.5 range) — Phase 9 callsites may override per label"

commits:
  - hash: 830935f
    message: "feat(08-03): GiftMaisonFonts + Typography — GoogleFont provider, 10 TextStyle roles"

deviations:
  - "Wave 1 agent was rate-limited mid-execution (2026-04-20 19:44). GiftMaisonFonts.kt landed on disk uncommitted; GiftMaisonTypography.kt was never written. Orchestrator finished both inline on 2026-04-21 after rate limit reset. FontsTest required a small refactor (internal-prop access was invalid) — same test assertions, different access path."
---

# Plan 08-03: Fonts + Typography (SUMMARY)

## What shipped

- **GiftMaisonFonts.kt** — `giftMaisonFontProvider` (GoogleFont.Provider pointing at Google Play Services) + 3 `FontFamily` values (InstrumentSerif/Inter/JetBrainsMono) with the weights the handoff uses (Normal + Italic for serif; 400/500/600 for Inter + Mono)
- **GiftMaisonTypography.kt** — `@Immutable data class GiftMaisonTypography` with 10 TextStyle roles (displayXL/L/M/S + bodyL/M/MEmphasis/S/XS + monoCaps), exact handoff sizes/weights/letter-spacing/line-heights, `PlatformTextStyle(includeFontPadding = false)` + `LineHeightStyle.Center` on every role

## Verification

- `./gradlew :app:testDebugUnitTest --tests 'com.giftregistry.ui.theme.FontsTest'` → 6 tests green
- `./gradlew :app:testDebugUnitTest --tests 'com.giftregistry.ui.theme.TypographyTest'` → 11 tests green (10 per-role + `everyRole_disablesFontPadding_pitfall2` iterator)

## Requirements closed

- **DES-01** (fonts integrated): complete
- **DES-02** (type scale): complete
