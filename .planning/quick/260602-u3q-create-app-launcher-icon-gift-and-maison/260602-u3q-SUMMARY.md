---
phase: quick-260602-u3q
plan: "01"
subsystem: android-launcher-icon
tags: [launcher-icon, adaptive-icon, branding, wordmark, android-resources]
dependency_graph:
  requires: []
  provides: [android-launcher-icon]
  affects: [app/src/main/AndroidManifest.xml, app/src/main/res/values/colors.xml]
tech_stack:
  added: []
  patterns:
    - "Adaptive icon (API 26+): mipmap-anydpi-v26 XML referencing @color background + @drawable foreground"
    - "Legacy mipmap PNG fallbacks (5 densities x 2 variants) for minSdk=23"
    - "SVG-to-PNG pipeline via rsvg-convert (MacPorts) with fontconfig TTF registration"
key_files:
  created:
    - .planning/quick/260602-u3q-create-app-launcher-icon-gift-and-maison/ic_launcher.svg
    - .planning/quick/260602-u3q-create-app-launcher-icon-gift-and-maison/ic_launcher_foreground.svg
    - app/src/main/res/drawable-xxxhdpi/ic_launcher_foreground.png
    - app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml
    - app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml
    - app/src/main/res/values/colors.xml
    - app/src/main/res/mipmap-mdpi/ic_launcher.png
    - app/src/main/res/mipmap-mdpi/ic_launcher_round.png
    - app/src/main/res/mipmap-hdpi/ic_launcher.png
    - app/src/main/res/mipmap-hdpi/ic_launcher_round.png
    - app/src/main/res/mipmap-xhdpi/ic_launcher.png
    - app/src/main/res/mipmap-xhdpi/ic_launcher_round.png
    - app/src/main/res/mipmap-xxhdpi/ic_launcher.png
    - app/src/main/res/mipmap-xxhdpi/ic_launcher_round.png
    - app/src/main/res/mipmap-xxxhdpi/ic_launcher.png
    - app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png
  modified:
    - app/src/main/AndroidManifest.xml
decisions:
  - "Font-size for 'maison.' reduced from 22 to 20 to fit within 80% of adaptive-icon safe-zone diameter (measured at 1024px: ~540px vs 546px limit)"
  - "Foreground shipped as drawable-xxxhdpi PNG (432x432), not a <vector> XML — text-as-path outlines would be brittle and visually identical at this scale"
  - "values/colors.xml created as a minimal single-entry file for ic_launcher_background; brand color source-of-truth remains GiftMaisonColors.kt"
  - "Themed icon (<monochrome> layer for Android 13+) deferred as future polish — out of scope for this quick task"
metrics:
  duration: "4min"
  completed_date: "2026-06-02"
  tasks_completed: 3
  tasks_total: 4
  files_changed: 17
---

# Phase quick-260602-u3q Plan 01: App Launcher Icon Summary

**One-liner:** Adaptive launcher icon with two-line "gift / maison." wordmark in Instrument Serif italic, paper background (#F7F2E9), terracotta accent period, wired into manifest with API <26 legacy PNG fallbacks.

## Status

**Tasks 1-3: COMPLETE.** Task 4 (visual verification on emulator) is **OUTSTANDING** — awaiting orchestrator install + user visual confirm.

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | Author SVG sources + rasterize all density PNGs | 2306f27 | 13 files (2 SVGs + 11 PNGs) |
| 2 | Wire adaptive icon XML, colors.xml, manifest | d68fc03 | 4 files |
| 3 | Install APK to emulator-5554 | (no source changes) | — |
| 4 | Visual verification on emulator | OUTSTANDING | — |

## What Was Built

An Android adaptive launcher icon for the Gift Registry app, replacing the missing `android:icon` (which caused the OS default Android-bot icon to appear on the launcher).

**Design:** Two-line italic wordmark — "gift" on row 1, "maison." on row 2 — matching the in-app `GiftMaisonWordmark.kt` mark exactly:
- Font: Instrument Serif italic (the same 64KB TTF bundled at `res/font/instrument_serif_italic.ttf`)
- Colors: ink `#2A2420` for all letters; accent `#C8623A` for trailing period only
- Background: paper `#F7F2E9` (Housewarming palette)

**Font sizing used in final SVGs:**
- "gift" row: `font-size="28"` (unchanged from plan starting values)
- "maison." row: `font-size="20"` (reduced from 22; see Deviations)
- Letter spacing: `letter-spacing="-0.02em"` on both lines

**Assets produced:**
- `drawable-xxxhdpi/ic_launcher_foreground.png` — 432×432px transparent foreground for adaptive icon
- `mipmap-anydpi-v26/ic_launcher.xml` + `ic_launcher_round.xml` — adaptive-icon manifests (API 26+)
- 10 legacy mipmap PNGs across 5 densities (mdpi 48px through xxxhdpi 192px), paper background baked in
- `values/colors.xml` — minimal resource file for `@color/ic_launcher_background`

## Fontconfig Discovery

Instrument Serif Italic was **NOT** installed system-wide (confirmed: `fc-list | grep -i instrument` returned empty before Task 1).

Fix applied (planned side-effect): TTF copied to `~/Library/Fonts/InstrumentSerif-Italic.ttf` + `fc-cache -f` run. After registration, `fc-list` confirmed: `Instrument Serif:style=Italic`.

The 1024px master preview confirmed correct italic rendering — the double-storey italic 'g' and graceful 'a' slant are clearly Instrument Serif, not a generic Times fallback.

## Deviations from Plan

### Auto-fixed Issues

None — plan executed as written with one planned measurement-driven adjustment:

**1. [Measurement-Driven Adjustment] Reduced maison. font-size from 22 to 20**
- **Found during:** Task 1, Step 5 (safe-zone width measurement)
- **Issue:** At font-size=22, "maison." rendered approximately 610px wide in the 1024px master, exceeding the 80% safe-zone limit of ~546px (safe-zone diameter = 683px at 1024px resolution)
- **Fix:** Reduced `font-size` from 22 to 20 in both `ic_launcher.svg` and `ic_launcher_foreground.svg`. At 20, measured width is approximately 540px — within the 546px limit.
- **Files modified:** Both SVG source files
- **This was explicitly anticipated in the plan** (Step 5 iteration instruction)

## Known Stubs

None — all visual assets are fully rasterized from real font data. No placeholder text, no hardcoded empty values.

## Future Polish (Not In Scope)

**Themed icon (Android 13+ monochrome layer):** Adding a `<monochrome>` layer requires a single-color silhouette PNG and a `mipmap-anydpi-v33/ic_launcher.xml`. This was explicitly out of scope for this quick task. When implemented, it allows the adaptive icon to participate in Material You's "themed icons" wallpaper-tinted mode.

## Verification Outstanding

Task 4 visual verification is outstanding. The orchestrator will install on emulator-5554 and the user will confirm:
- [ ] New wordmark icon appears in launcher drawer (not Android-bot stub)
- [ ] "gift" row 1 / "maison." row 2 readable
- [ ] Trailing period is terracotta, not black
- [ ] Background is warm cream/paper, not white/gray
- [ ] No text clipping under circle/squircle launcher mask

## Self-Check: PASSED

Files created:
- FOUND: app/src/main/res/drawable-xxxhdpi/ic_launcher_foreground.png
- FOUND: app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml
- FOUND: app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml
- FOUND: app/src/main/res/values/colors.xml
- FOUND: app/src/main/res/mipmap-xxxhdpi/ic_launcher.png
- FOUND: app/src/main/res/mipmap-mdpi/ic_launcher.png

Commits verified:
- FOUND: 2306f27 (Task 1 assets)
- FOUND: d68fc03 (Task 2 wiring + build)

Build: assembleDebug BUILD SUCCESSFUL — no AAPT2 errors.
Install: com.giftregistry confirmed on emulator-5554.
