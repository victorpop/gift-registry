---
phase: 9
slug: shared-chrome-status-ui
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-04-21
---

# Phase 9 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4 + MockK + Turbine (unit tests); Compose UI test (`createComposeRule`) for instrumented — RED stubs landing in Wave 0 |
| **Config file** | none — JUnit 4 is auto-detected by AGP |
| **Quick run command** | `./gradlew :app:testDebugUnitTest --tests "com.giftregistry.ui.common.*"` |
| **Full suite command** | `./gradlew :app:testDebugUnitTest` |
| **Estimated runtime** | ~60 seconds (unit suite); Compose UI tests add ~90s (device/emulator only) |

Compose UI tests require `app/src/androidTest/` plus `androidx.compose.ui:ui-test-junit4` and `ui-test-manifest` deps added in Wave 0.

---

## Sampling Rate

- **After every task commit:** Run `./gradlew :app:testDebugUnitTest --tests "com.giftregistry.ui.common.*"`
- **After every plan wave:** Run `./gradlew :app:testDebugUnitTest`
- **Before `/gsd:verify-work`:** Full unit suite must be green; StyleGuidePreview visually verified in Android Studio
- **Max feedback latency:** 60 seconds (unit suite)

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| TBD | TBD | TBD | TBD | TBD | TBD | TBD | ⬜ pending |

*Populated by gsd-planner from the Phase Requirements → Test Map in 09-RESEARCH.md. Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `app/src/test/java/com/giftregistry/ui/common/chrome/BottomNavVisibilityTest.kt` — RED stubs for CHROME-01 visibility predicate (pure Kotlin, no Android)
- [ ] `app/src/test/java/com/giftregistry/ui/common/status/ReservedChipTest.kt` — RED stubs for STAT-01 countdown logic (`computeMinutesLeft`)
- [ ] `app/src/test/java/com/giftregistry/ui/common/status/PulsingDotTest.kt` — RED stubs for STAT-01 animation spec parameters
- [ ] `app/src/test/java/com/giftregistry/ui/common/status/StatusChipDispatcherTest.kt` — RED stubs for STAT-01/02/03 dispatcher routing (AVAILABLE→Open, RESERVED→Reserved, PURCHASED→Given)
- [ ] `app/src/test/java/com/giftregistry/ui/common/status/PurchasedRowModifierTest.kt` — RED stubs for STAT-04
- [ ] Optional: `app/src/androidTest/` scaffolding + `ui-test-junit4` / `ui-test-manifest` deps added to `build.gradle.kts` for CHROME-02 / CHROME-03 / STAT-02 / STAT-03 Compose UI tests — planner decides Wave 0 scope

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Pulse animation timing feels correct (1.4 s cadence) | STAT-01 | Animations can't be reliably screenshot-diffed; requires human eye | Open StyleGuidePreview on device, observe Reserved chip dot pulsing; confirm cadence matches handoff `pulse` keyframe (≈1.4 s full cycle) |
| FAB 4 dp paper ring is visually distinct from bar | CHROME-02 | Ring thickness and contrast depend on rendered pixel density | Open Home on device, inspect FAB; confirm paper-coloured ring visible around the circle against the nav background |
| Scrim + blur underneath Add-action sheet looks correct | CHROME-03 | `Modifier.blur()` is API 31+; visual fallback on older devices | Trigger Add sheet on API 31+ device; confirm home content blurs at 1 dp behind scrim; test on API 30 device to confirm graceful scrim-only fallback |
| Purchased-row grayscale + ink tint + strikethrough compose correctly | STAT-04 | Requires real image rendering, Coil + ColorMatrix interop | Open RegistryDetail with a purchased item; confirm row at 55 % opacity, image grayscale with ink tint, centred ✓ mark, title strikethrough |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references (unit test RED stubs for CHROME-01 predicate, STAT-01/02/03/04 dispatcher + modifier)
- [ ] No watch-mode flags
- [ ] Feedback latency < 60 s (unit suite)
- [ ] `nyquist_compliant: true` set in frontmatter after Wave 0 lands

**Approval:** pending
