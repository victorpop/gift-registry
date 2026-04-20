---
phase: 8
slug: giftmaison-design-foundation
status: ready
nyquist_compliant: true
wave_0_complete: false
created: 2026-04-20
updated: 2026-04-20
---

# Phase 8 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit4 (Android unit tests — Robolectric unused for Phase 8) |
| **Config file** | `app/build.gradle.kts` |
| **Quick run command** | `./gradlew :app:testDebugUnitTest --tests 'com.giftregistry.ui.theme.*'` |
| **Full suite command** | `./gradlew :app:testDebugUnitTest` |
| **Full build command** | `./gradlew :app:assembleDebug` |
| **Estimated runtime** | ~30 seconds (theme tests), ~2 minutes (full suite) |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew :app:testDebugUnitTest --tests 'com.giftregistry.ui.theme.*'`
- **After every plan wave:** Run `./gradlew :app:testDebugUnitTest`
- **Before `/gsd:verify-work`:** Full suite must be green + `./gradlew :app:assembleDebug` succeeds + manual review of all 4 @Preview composables in `StyleGuidePreview.kt` via Android Studio preview pane
- **Max feedback latency:** ~30 seconds for theme tests, ~2 minutes end-to-end build

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 08-01-T1 | 08-01 | 0 | DES-01..05 (prereq) | build config | `./gradlew :app:dependencies --configuration debugRuntimeClasspath \| grep ui-text-google-fonts` | ⚠ Wave 0 creates | ⬜ pending |
| 08-01-T2 | 08-01 | 0 | DES-01..05 (RED) | compile-fail (expected) | `./gradlew :app:testDebugUnitTest --tests 'com.giftregistry.ui.theme.*'` FAILS with missing-symbol errors | ⚠ Wave 0 creates | ⬜ pending |
| 08-02-T1 | 08-02 | 1 | DES-03 | unit | `./gradlew :app:testDebugUnitTest --tests 'com.giftregistry.ui.theme.ColorsTest'` | GiftMaisonColors.kt | ⬜ pending |
| 08-03-T1 | 08-03 | 1 | DES-01 | unit | `./gradlew :app:testDebugUnitTest --tests 'com.giftregistry.ui.theme.FontsTest'` | GiftMaisonFonts.kt | ⬜ pending |
| 08-03-T2 | 08-03 | 1 | DES-02 | unit | `./gradlew :app:testDebugUnitTest --tests 'com.giftregistry.ui.theme.TypographyTest'` | GiftMaisonTypography.kt | ⬜ pending |
| 08-04-T1 | 08-04 | 1 | DES-04 (shapes+spacing) | compile + partial unit | `./gradlew :app:compileDebugKotlin` | GiftMaisonShapes.kt + GiftMaisonSpacing.kt | ⬜ pending |
| 08-04-T2 | 08-04 | 1 | DES-04 (shadows) | unit | `./gradlew :app:testDebugUnitTest --tests 'com.giftregistry.ui.theme.ShapesAndDimensTest'` | GiftMaisonShadows.kt | ⬜ pending |
| 08-05-T1 | 08-05 | 2 | DES-05 | unit | `./gradlew :app:testDebugUnitTest --tests 'com.giftregistry.ui.theme.WordmarkTest'` | GiftMaisonWordmark.kt | ⬜ pending |
| 08-05-T2 | 08-05 | 2 | DES-01..05 (integration) | build + full suite | `./gradlew :app:assembleDebug && ./gradlew :app:testDebugUnitTest --tests 'com.giftregistry.ui.theme.*'` | Theme.kt + Color.kt + Type.kt (rewires) | ⬜ pending |
| 08-05-T3 | 08-05 | 2 | DES-02, 03, 04, 05 (preview) | compile + manual | `./gradlew :app:compileDebugKotlin` + manual Android Studio preview review | preview/StyleGuidePreview.kt | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

**Requirement-to-test crosscheck (every DES-* ID has at least one automated test):**

| Requirement | Covered by task(s) | Automated? |
|-------------|--------------------|-----|
| DES-01 (fonts integrated) | 08-01-T1, 08-03-T1 | yes — dependency resolves + FontsTest |
| DES-02 (type scale) | 08-03-T2 | yes — TypographyTest (10 roles, em-equality, includeFontPadding guard) |
| DES-03 (colour palette) | 08-02-T1 | yes — ColorsTest (13 tokens with exact sRGB hex) |
| DES-04 (shapes + spacing + shadows) | 08-04-T1, 08-04-T2 | yes — ShapesAndDimensTest (shapes + spacing + elevation values) |
| DES-05 (wordmark) | 08-05-T1 | yes — WordmarkTest (AnnotatedString shape + deterministic factory) |
| Integration (theme re-skin) | 08-05-T2 | yes — full `./gradlew :app:assembleDebug` + full test suite |

---

## Wave 0 Requirements

- [ ] `app/src/test/java/com/giftregistry/ui/theme/ColorsTest.kt` — failing stub for DES-03 (13 Housewarming tokens)
- [ ] `app/src/test/java/com/giftregistry/ui/theme/FontsTest.kt` — failing stub for DES-01 (GoogleFont provider + 3 FontFamilys)
- [ ] `app/src/test/java/com/giftregistry/ui/theme/TypographyTest.kt` — failing stub for DES-02 (10 type roles + `includeFontPadding=false` guard for Pitfall 2)
- [ ] `app/src/test/java/com/giftregistry/ui/theme/ShapesAndDimensTest.kt` — failing stub for DES-04 (7 shapes + 10 spacing values + 3 shadow elevations)
- [ ] `app/src/test/java/com/giftregistry/ui/theme/WordmarkTest.kt` — failing stub for DES-05 (AnnotatedString factory shape + determinism)
- [ ] `app/src/main/res/values-v23/font_certs.xml` — Google Fonts provider certificate (copied from Jetchat sample)
- [ ] `gradle/libs.versions.toml` + `app/build.gradle.kts` — `compose-ui-text-google-fonts` dependency wired in

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Instrument Serif / Inter / JetBrains Mono render correctly on-device | DES-01 | Google Fonts downloadable font loading requires Google Play Services and first-run download; unit tests can assert FontFamily is constructed but not visual rendering | Open `StyleGuidePreview.kt` in Android Studio (or run app on GMS-equipped emulator after Plan 05 merges), confirm all 3 fonts render (not system fallback) in the `TypeScalePreview` pane |
| Housewarming colour swatches visually match handoff | DES-03 | sRGB hex comparison is automatable, but perceptual match requires eyeballing against the HTML prototype | Open `design_handoff/design_handoff_android_owner_flow/reference/GiftMaison - gift registry mocks.html` side-by-side with the `PalettePreview` pane in Android Studio |
| Type scale spacing feels right (letter-spacing / line-height perception) | DES-02 | Pixel-level accuracy is automatable; perceptual rhythm needs eyeballing | `TypeScalePreview` rendered in Android Studio — compare line-height density and letter-spacing tightness against HTML prototype type samples |
| Wordmark kerning between "giftmaison" and "." | DES-05 | Kerning is font-controlled; only visible by eye | `WordmarkPreview` renders the wordmark at 3 sizes (20/22/28 sp); inspect period spacing — the `.` should sit tight against the "n" with no visible gap |
| Existing Phase 2-7 screens re-skin cleanly to Housewarming | Integration (Pitfall 5) | The 42+ `MaterialTheme.colorScheme.*` reads produce a purple → terracotta re-skin as a side effect; no automated test covers "UI still looks right" | Launch app on emulator after Plan 05, navigate Home / Registry Detail / Add Item / Settings — confirm screens are readable and terracotta is applied consistently |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify (every task has `./gradlew :app:testDebugUnitTest`, `./gradlew :app:compileDebugKotlin`, or `./gradlew :app:assembleDebug`)
- [x] Wave 0 covers all MISSING references (5 test files + font_certs.xml + gradle dep)
- [x] No watch-mode flags (all Gradle commands are one-shot)
- [x] Feedback latency < 30s for theme tests, <2min for full suite
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** ready for execution

---

## Planner Notes

- **Wave structure:** 3 waves (0 → 1 → 2). Wave 0 ships failing tests + gradle dep (Plan 08-01). Wave 1 ships Colour, Fonts+Typography, Shapes+Spacing+Shadows in parallel (Plans 08-02, 08-03, 08-04 — no shared files). Wave 2 ships Wordmark + Theme rewire + Style-guide preview (Plan 08-05, depends on all of Wave 1).
- **Parallel execution:** Plans 08-02, 08-03, 08-04 have zero overlapping `files_modified` — they can be executed in parallel by 3 separate executor instances if the workflow supports it.
- **Integration risk:** Plan 08-05 Task 2 (Theme + Color + Type rewire) is the riskiest task — it affects 42+ existing call sites via M3 `ColorScheme`. The automated verify runs the full app build (`./gradlew :app:assembleDebug`) to catch any screen that breaks after the colour-seed swap from purple → terracotta.
- **Pitfall 5 (Research):** the purple → terracotta re-skin is INTENDED. Reviewers should expect all v1.0 screens to look different after Phase 8 lands; pixel-perfect per-screen redesign is Phases 10/11.
