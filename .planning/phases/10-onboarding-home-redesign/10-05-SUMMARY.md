---
phase: 10-onboarding-home-redesign
plan: "05"
subsystem: android-ui-preview
tags: [style-guide, preview, uat, scr-06, scr-07, checkpoint]
dependency_graph:
  requires: [10-03, 10-04]
  provides: [StyleGuidePreview-phase10-sections, SCR-06-UAT, SCR-07-UAT]
  affects: [StyleGuidePreview.kt]
tech_stack:
  added: []
  patterns: ["@Preview private composable wrapping GiftRegistryTheme", "top-level val for mock domain objects in preview file"]
key_files:
  created: []
  modified:
    - app/src/main/java/com/giftregistry/ui/theme/preview/StyleGuidePreview.kt
decisions:
  - "previewRegistry uses imageUrl=null to exercise the paperDeep placeholder path in AsyncImage (no URL needed to validate card layout)"
  - "AuthScreen-as-whole not previewed due to hiltViewModel() preview limitation — individual components (AuthHeadline + GoogleBanner) preview instead"
  - "5 new @Preview functions are private (preview-only, no app binary surface)"
metrics:
  duration: "~2 min (Task 1 automated; Task 2 UAT deferred)"
  completed_date: "2026-04-21"
  tasks_completed: 2
  tasks_total: 2
  files_modified: 1
status: uat-deferred
---

# Phase 10 Plan 05: StyleGuidePreview Sections + On-Device UAT Summary

**One-liner:** Appended 5 Phase 10 @Preview composables (AuthHeadline, GoogleBanner, SegmentedTabs × 3 states, RegistryCardPrimary, RegistryCardSecondary) to StyleGuidePreview.kt; APK installed on AVD API 36 — awaiting on-device human verification of SCR-06 + SCR-07.

## Status: Task 2 UAT deferred to 10-HUMAN-UAT.md

Task 1 (automated) is complete and committed. Task 2 (on-device UAT for SCR-06 + SCR-07) has been **deferred by the user 2026-04-21** to unblock Phase 11. The 21 UAT items (17 SCR checks + RO locale + 3 regression guards) are persisted to `10-HUMAN-UAT.md` and will surface in `/gsd:progress` and `/gsd:audit-uat` until resolved.

## Task 1 — Completed

**Task:** Append 5 Phase 10 @Preview composables to StyleGuidePreview.kt
**Commit:** `29292f5` — `feat(10-05): Wave 2 — append 5 Phase 10 @Preview composables to StyleGuidePreview`
**Files modified:** `app/src/main/java/com/giftregistry/ui/theme/preview/StyleGuidePreview.kt` (+106 lines)

New previews added:
| Preview name | Composable | Height |
|---|---|---|
| `AuthHeadlinePreview` | `AuthHeadline()` | 140 dp |
| `GoogleBannerPreview` | `GoogleBanner(onClick = {})` | 120 dp |
| `SegmentedTabsPreview` | `SegmentedTabs` × 3 selected states | 220 dp |
| `RegistryCardPrimaryPreview` | `RegistryCardPrimary(previewRegistry)` | 320 dp |
| `RegistryCardSecondaryPreview` | `RegistryCardSecondary(previewRegistry)` | 320 dp |

All previews:
- Wrapped in `GiftRegistryTheme { ... }` so `LocalGiftMaisonColors` providers populate
- Use `showBackground = true, backgroundColor = 0xFFF7F2E9` per Phase 8/9 convention
- `widthDp = 360` per handoff "360 dp width" preview target
- `private` functions — not included in app binary

Verification results:
- `@Preview` count: 9 (pre) → 14 (post) — exactly +5
- `./gradlew :app:compileDebugKotlin` → BUILD SUCCESSFUL
- `./gradlew :app:testDebugUnitTest` → BUILD SUCCESSFUL (all 40 Phase 10 Wave 0 tests + Phase 8/9 regression green)
- `./gradlew :app:installDebug` → BUILD SUCCESSFUL — APK installed on `Medium_Phone_API_36.1(AVD)`

## Task 2 — Awaiting Human Verification

**Checkpoint type:** human-verify (blocking gate)
**APK:** installed on Medium_Phone_API_36.1(AVD) — API 36
**Launch command:**
```bash
adb shell am start -n com.giftregistry/.MainActivity
```

See CHECKPOINT section in executor response for the full 17-check + locale + regression verification list.

## Deviations from Plan

None — plan executed exactly as written for Task 1.

## Known Stubs

Per plan's `<success_criteria>` known follow-ups (not stubs blocking plan goal):
- `firstName`/`lastName` not persisted to `Firebase User.displayName` — deferred non-visual enhancement
- Real Terms / Privacy / Support URLs are empty placeholders per CONTEXT.md discretion
- Per-registry stats aggregation (itemCount / reservedCount / givenCount) — Phase 10 renders zeros via `statsLine()` returning `"0 items • 0 reserved • 0 given"`
- Notifications bell placement deferred to Phase 11
- Old pre-Phase-10 string key cleanup deferred to post-phase follow-up commit
- `Registry.imageUrl` Firestore backfill — existing registries without imageUrl show paperDeep placeholder

## Self-Check: PARTIAL (Task 1 complete, Task 2 pending human sign-off)

- [x] StyleGuidePreview.kt modified — FOUND
- [x] Commit `29292f5` exists — FOUND
- [ ] Human UAT sign-off — PENDING checkpoint
