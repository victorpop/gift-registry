---
quick_task: 260427-lnq
title: "Reorder InstrumentSerifFamily — bundled fonts first"
date: 2026-04-21
commit: abd6028
subsystem: ui-theme
tags: [fonts, compose, typography, quick-fix]
key-files:
  modified:
    - app/src/main/java/com/giftregistry/ui/theme/GiftMaisonFonts.kt
decisions:
  - Remove GMS async Font entries from InstrumentSerifFamily entirely; bundled TTFs cover all weights/styles synchronously
  - Remove unused private val instrumentSerif (GoogleFont) — no references remain
metrics:
  duration: "~5 min"
  completed: 2026-04-21
---

# Quick Task 260427-lnq: Reorder InstrumentSerifFamily — bundled fonts first

**One-liner:** Promoted bundled Instrument Serif TTFs to sole FontFamily entries, eliminating GMS async download that caused inconsistent serif fallback behavior app-wide.

## What Was Done

Compose's `Font(googleFont = ...)` constructor is asynchronous — it requests the font from
Google Play Services at runtime. While loading (or if loading fails / times out), Compose falls
back to system serif because the `FontFamily` resolver picks the FIRST matching entry for a
given weight/style. With GMS entries listed first, the bundled `Font(resId = R.font.xxx)` entries
below were never reached during normal rendering.

**Fix:** Removed all `Font(googleFont = instrumentSerif, ...)` entries from `InstrumentSerifFamily`.
The two bundled entries now cover both weight=Normal and Normal+Italic — everything the handoff
requires. The `giftMaisonFontProvider` and GMS entries remain for `InterFamily` and
`JetBrainsMonoFamily` (no bundled fallback exists for those; system-sans/mono fallback acceptable).

The unused private `instrumentSerif = GoogleFont("Instrument Serif")` val was also removed.

## Files Changed

| File | Change |
|------|--------|
| `app/src/main/java/com/giftregistry/ui/theme/GiftMaisonFonts.kt` | Replaced 4-entry InstrumentSerifFamily (GMS+bundled×2) with 2-entry bundled-only; removed unused `instrumentSerif` GoogleFont val; updated KDoc |

## Verification

- `./gradlew :app:compileDebugKotlin` — BUILD SUCCESSFUL
- `./gradlew :app:testDebugUnitTest` — BUILD SUCCESSFUL (all tests pass)
- `./gradlew :app:assembleDebug` — BUILD SUCCESSFUL

## Deviations from Task Spec

Removed `private val instrumentSerif = GoogleFont("Instrument Serif")` in addition to the GMS
Font entries. The task spec said "can stay as dead reference OR be removed (Claude's discretion)".
Removing it is cleaner — dead references mislead future readers into thinking GMS is still used.

## Self-Check: PASSED

- File modified: `app/src/main/java/com/giftregistry/ui/theme/GiftMaisonFonts.kt` — confirmed
- Commit abd6028 exists: confirmed
