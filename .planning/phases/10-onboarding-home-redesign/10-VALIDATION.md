---
phase: 10
slug: onboarding-home-redesign
status: draft
nyquist_compliant: false
wave_0_complete: true
created: 2026-04-21
---

# Phase 10 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4 + MockK + Turbine (unit tests under `app/src/test/`) |
| **Config file** | none — JUnit 4 auto-detected by AGP |
| **Quick run command** | `./gradlew :app:testDebugUnitTest --tests "com.giftregistry.ui.auth.*" --tests "com.giftregistry.ui.registry.list.*"` |
| **Full suite command** | `./gradlew :app:testDebugUnitTest` |
| **Estimated runtime** | ~60 seconds (unit suite) |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew :app:testDebugUnitTest --tests "com.giftregistry.ui.auth.*" --tests "com.giftregistry.ui.registry.list.*"`
- **After every plan wave:** Run `./gradlew :app:testDebugUnitTest`
- **Before `/gsd:verify-work`:** Full unit suite green; visual verification of AuthScreen + RegistryListScreen in StyleGuidePreview
- **Max feedback latency:** 60 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| TBD | TBD | TBD | TBD | TBD | TBD | TBD | ⬜ pending |

*Populated by gsd-planner from 10-RESEARCH.md's Validation Architecture. Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

RED stub unit test files (per 10-RESEARCH.md Key Finding 6):
- [x] `app/src/test/java/com/giftregistry/ui/registry/list/TabFilterPredicateTest.kt` — Active/Past predicate over `eventDateMs` and `startOfToday()` boundary
- [x] `app/src/test/java/com/giftregistry/ui/registry/list/DraftHeuristicTest.kt` — `title.isBlank() OR items.isEmpty()` classification
- [x] `app/src/test/java/com/giftregistry/ui/registry/list/IsPrimarySelectionTest.kt` — `maxByOrNull { updatedAt }` on registries list
- [x] `app/src/test/java/com/giftregistry/ui/common/AvatarInitialsTest.kt` — `fromNames` / `fromEmail` / fallback ordering
- [x] `app/src/test/java/com/giftregistry/ui/auth/AuthHeadlineTest.kt` — `buildAnnotatedString` structure for italic-accent period in "first registry."
- [x] `app/src/test/java/com/giftregistry/ui/auth/AuthFormStateTest.kt` — firstName/lastName wiring, sign-up-default `isSignUpMode = true`, mode toggle

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Google banner shadow + concentric rings render correctly | SCR-06 | Shadow + ring opacity require rendered pixels | Open StyleGuidePreview / Auth screen on device; inspect Google banner |
| Pulsing caret animation on focused email field at 1.1 s cadence | SCR-06 | Animation timing cannot be reliably screenshot-diffed | Tap email field on device; observe caret pulses at ≈1.1 s full cycle |
| Primary card's darkened image (70 % brightness) | SCR-07 | ColorMatrix brightness requires real image rendering | Open Home on device with registries containing images; confirm most-recent-updated card renders with dimmed image |
| 16:9 hero image aspect ratio | SCR-07 | Coil `AsyncImage` layout requires rendered image | Open Home, inspect cards; aspect ratio should be exactly 16:9 |
| Segmented-tab state + selection visually matches handoff | SCR-07 | Custom composable rendering | Open Home on device; tap Active/Drafts/Past; selected pill should have white fill + shadow; inactive tabs are flat on paperDeep track |
| RO locale correctness for new string keys | I18N-01 | Locale switching requires device-level change | Switch device locale to Romanian; verify Phase 10 string keys render correctly |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references (6 RED stub test files listed above)
- [ ] No watch-mode flags
- [ ] Feedback latency < 60 s (unit suite)
- [ ] `nyquist_compliant: true` set in frontmatter after Wave 0 lands

**Approval:** pending
