---
phase: 8
slug: giftmaison-design-foundation
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-04-20
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
| **Estimated runtime** | ~30 seconds |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew :app:testDebugUnitTest --tests 'com.giftregistry.ui.theme.*'`
- **After every plan wave:** Run `./gradlew :app:testDebugUnitTest`
- **Before `/gsd:verify-work`:** Full suite must be green + manual review of debug style-guide screen on emulator
- **Max feedback latency:** ~30 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| *To be populated by gsd-planner during Step 8* | | | DES-01..05 | unit + preview | | | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

*The planner should assign one unit test per DES-* requirement (fonts resolvable, type scale values match handoff, colour tokens match hex values, spacing/radii/shadows defined, wordmark composition renders) plus a `@Preview` for the style-guide harness.*

---

## Wave 0 Requirements

- [ ] `app/src/test/java/com/giftregistry/ui/theme/FontsTest.kt` — stub for DES-01 (Instrument Serif / Inter / JetBrains Mono resolve to non-null FontFamily)
- [ ] `app/src/test/java/com/giftregistry/ui/theme/TypographyTest.kt` — stub for DES-02 (type scale TextStyle values match handoff sizes/weights/letter-spacing/line-height)
- [ ] `app/src/test/java/com/giftregistry/ui/theme/ColorsTest.kt` — stub for DES-03 (all 13 Housewarming tokens match handoff sRGB hex values)
- [ ] `app/src/test/java/com/giftregistry/ui/theme/ShapesAndDimensTest.kt` — stub for DES-04 (spacing, radii 8/10/12/14/16/22/999, shadow specs defined)
- [ ] `app/src/test/java/com/giftregistry/ui/theme/WordmarkTest.kt` — stub for DES-05 (wordmark AnnotatedString contains "giftmaison" in one SpanStyle and "." in accent SpanStyle)
- [ ] `app/src/main/res/values-v23/font_certs.xml` — Google Fonts provider certificate (copied from Jetchat sample)

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Instrument Serif / Inter / JetBrains Mono render correctly on-device | DES-01 | Google Fonts downloadable font loading requires Google Play Services and first-run download; unit tests can assert FontFamily is constructed but not visual rendering | Launch debug style-guide screen on GMS-equipped emulator, confirm all 3 fonts render (not system fallback) in type-scale preview |
| Housewarming colour swatches visually match handoff | DES-03 | sRGB hex comparison is automatable, but perceptual match requires eyeballing against the HTML prototype | Open `design_handoff_android_owner_flow/reference/GiftMaison - gift registry mocks.html` side-by-side with the debug style-guide screen |
| Type scale spacing feels right (letter-spacing / line-height perception) | DES-02 | Pixel-level accuracy is automatable; perceptual rhythm needs eyeballing | Debug style-guide screen with Display XL/L/M/S + Body L/M/S/XS + Mono samples rendered; compare to HTML prototype type samples |
| Wordmark kerning between "giftmaison" and "." | DES-05 | Kerning is font-controlled; only visible by eye | Debug style-guide screen renders wordmark at multiple sizes; inspect period spacing vs. HTML prototype |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 30s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
