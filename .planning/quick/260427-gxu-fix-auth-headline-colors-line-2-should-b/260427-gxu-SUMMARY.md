---
phase: quick-260427-gxu
plan: 01
subsystem: android-ui
tags: [fonts, compose, auth-screen, annotated-string, design-fix]
dependency_graph:
  requires: []
  provides: [offline-instrument-serif, auth-headline-correct-colors]
  affects: [GiftMaisonFonts, AuthHeadline, AuthHeadlineTest]
tech_stack:
  added: [Instrument Serif TTF (bundled fallback)]
  patterns: [GMS-first + bundled-fallback FontFamily, 2-span AnnotatedString for headline]
key_files:
  created:
    - app/src/main/res/font/instrument_serif_regular.ttf
    - app/src/main/res/font/instrument_serif_italic.ttf
    - app/src/main/assets/licenses/OFL_instrument_serif.txt
  modified:
    - app/src/main/java/com/giftregistry/ui/theme/GiftMaisonFonts.kt
    - app/src/main/java/com/giftregistry/ui/auth/AuthHeadline.kt
    - app/src/test/java/com/giftregistry/ui/auth/AuthHeadlineTest.kt
decisions:
  - OFL.txt placed in assets/licenses/ (not res/font/) — AAPT rejects non-font file extensions in res/font/
  - inkSoft parameter retained on authHeadlineAnnotatedString signature for API stability; suppressed via @Suppress("UNUSED_PARAMETER")
  - Line 1 uses solid ink (not inkSoft) per user constraint override of RESEARCH.md suggestion
metrics:
  duration: 4min
  completed: 2026-04-27T09:19:59Z
  tasks: 2
  files: 6
---

# Quick Task 260427-gxu: Bundle Instrument Serif + Fix Auth Headline Colors Summary

**One-liner:** Bundled Instrument Serif Regular+Italic as GMS-first/bundled-fallback FontFamily entries, and fixed Auth headline to 2 spans — solid ink for "Start your", full accent for "first registry." (period included).

## Tasks Completed

| # | Task | Commit | Files |
|---|------|--------|-------|
| 1 | Bundle Instrument Serif TTFs + wire fallback | 35f932e | GiftMaisonFonts.kt, instrument_serif_regular.ttf, instrument_serif_italic.ttf, OFL_instrument_serif.txt |
| 2 | Fix Auth headline colour split | d4fccf8 | AuthHeadline.kt, AuthHeadlineTest.kt |

## Final InstrumentSerifFamily Structure (4 entries)

```kotlin
val InstrumentSerifFamily: FontFamily = FontFamily(
    Font(googleFont = instrumentSerif, fontProvider = giftMaisonFontProvider, weight = FontWeight.Normal),
    Font(resId = R.font.instrument_serif_regular, weight = FontWeight.Normal),
    Font(googleFont = instrumentSerif, fontProvider = giftMaisonFontProvider, weight = FontWeight.Normal, style = FontStyle.Italic),
    Font(resId = R.font.instrument_serif_italic, weight = FontWeight.Normal, style = FontStyle.Italic),
)
```

GMS entries are listed first (take priority when Google Play Services is available). Bundled TTF entries follow as synchronous fallback for non-GMS devices or before async download completes.

## Final authHeadlineAnnotatedString Colour Contract (2 spans)

```kotlin
fun authHeadlineAnnotatedString(
    prefix: String,       // "Start your"
    accent: String,       // "first registry"
    ink: Color,           // solid black — applied to line 1
    accentColor: Color,   // orange — applied to line 2 including period
    @Suppress("UNUSED_PARAMETER") inkSoft: Color,  // retained for API stability; unused
): AnnotatedString = buildAnnotatedString {
    withStyle(SpanStyle(color = ink)) { append(prefix) }      // "Start your"
    append("\n")
    withStyle(SpanStyle(color = accentColor)) {
        append(accent)   // "first registry"
        append(".")      // period inside accent span
    }
}
```

| Span | Text | Color |
|------|------|-------|
| 1 | "Start your" | `ink` (solid black) |
| (no span) | "\n" | — |
| 2 | "first registry." | `accent` (orange, period included) |

## Test Outcome

6/6 AuthHeadlineTest tests pass:

| Test | Status | Notes |
|------|--------|-------|
| text_containsBothLines | PASS | unchanged |
| text_hasNewlineBetweenLines | PASS | unchanged |
| periodIsLastCharacter | PASS | unchanged |
| period_isAccentColored | PASS | satisfied by merged line-2 accent span |
| firstRegistryLine_isAccentColored | PASS | replaced firstRegistryWords_areInkColored |
| startYourLine_isInkColored | PASS | replaced startYourLine_isInkSoftColored |

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] OFL.txt moved from res/font/ to assets/licenses/**
- **Found during:** Task 1 — first `./gradlew :app:compileDebugKotlin` run
- **Issue:** AAPT rejected `OFL.txt` in `res/font/` because the directory only accepts `.xml`, `.ttf`, `.ttc`, or `.otf` files
- **Fix:** Created `app/src/main/assets/licenses/OFL_instrument_serif.txt` — the `assets/` directory has no file extension restrictions, and is included in the APK for OFL redistribution compliance
- **Files modified:** `app/src/main/assets/licenses/OFL_instrument_serif.txt` (created in assets instead of res/font)
- **Commit:** 35f932e

## Known Stubs

None — all data wired. InstrumentSerifFamily now has live bundled fallback TTF entries. authHeadlineAnnotatedString produces the correct 2-span colour structure with no placeholders.

## Self-Check: PASSED

All 6 expected files exist. Both task commits (35f932e, d4fccf8) confirmed in git log.
