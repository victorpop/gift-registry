---
phase: quick-260427-lwz
plan: 01
subsystem: ui-chrome
tags: [bottom-nav, layout, giftmaison, bug-fix]
dependency_graph:
  requires:
    - quick-260421-moi (5-slot equal-width weighting + softWrap=false guards)
    - phase-09-P03 (GiftMaison 5-slot bottom nav + FabSlot)
  provides:
    - "Properly sized bottom nav content area (72 dp) — no label clipping, FAB optically centred"
  affects:
    - "All screens that render GiftMaisonBottomNav (Home, RegistryDetail per showsBottomNav())"
tech_stack:
  added: []
  patterns:
    - "Layout dimension fix: surgical single-token change preserves all existing behaviour (slot weighting, softWrap, FAB lift)"
key_files:
  created: []
  modified:
    - app/src/main/java/com/giftregistry/ui/common/chrome/GiftMaisonBottomNav.kt
decisions:
  - "Row content height = 72 dp (was 56 dp): NavItemSlot stack (44 pill + 4 spacer + ~14 label = ~62 dp) needs ~10 dp breathing room; same height also contains the FabSlot 54 dp FAB stack without overflow"
  - "FAB offset(y = (-22).dp) intentionally preserved — reads as intentional lift now that the content area is sized correctly"
requirements:
  - QUICK-LWZ-01
metrics:
  duration_minutes: 2
  completed_date: 2026-04-27
  tasks_completed: 1
  tasks_total: 2  # 1 auto + 1 human-verify checkpoint (outstanding)
  files_modified: 1
---

# Quick Task 260427-lwz: Fix bottom nav clipping and FAB alignment Summary

**One-liner:** Bumped GiftMaisonBottomNav Row content height from 56 dp to 72 dp so all five icon-row labels (HOME / STORES / LISTS / YOU) and the centre FAB's "ADD" caption render fully without bottom clipping, and the FAB's 22 dp lift now reads as intentional rather than a layout glitch.

## What Changed

**File:** `app/src/main/java/com/giftregistry/ui/common/chrome/GiftMaisonBottomNav.kt`

Single-token swap inside the top-level `GiftMaisonBottomNav` Row's modifier chain (line 76):

```kotlin
.padding(top = 4.dp, bottom = 6.dp)
.height(56.dp),    // before — too small, clipped labels and FAB content
```

becomes:

```kotlin
.padding(top = 4.dp, bottom = 6.dp)
.height(72.dp),    // after — fits 44 + 4 + ~14 dp stack + ~10 dp breathing room
```

## Why 72 dp

NavItemSlot's content stack inside its `Column(verticalArrangement = Arrangement.Center)`:

| Element                         | Height    |
| ------------------------------- | --------- |
| Box pill (icon container)       | 44 dp     |
| Spacer                          | 4 dp      |
| Text (`monoCaps` caption)       | ~14 dp    |
| **Total**                       | **~62 dp** |

The bar's old `.height(56.dp)` was 6 dp short, which Compose resolves by clipping the bottom of the centred content — exactly the symptom reported (labels cut at the bottom).

For the FabSlot, the 54 dp `GiftMaisonFab` plus the "ADD" caption similarly overflowed the 56 dp content area. `Modifier.offset(y = (-22).dp)` on the FAB does **not** reduce its measured height (offset is post-layout), so the FAB's measured stack also exceeded 56 dp. With the content area now correctly sized, `Arrangement.Center` no longer needs to clip — the FAB sits visually centred relative to the icon-row pills, with the 22 dp lift reading as intentional.

72 dp = 62 dp content + ~10 dp breathing room (split top/bottom by `Arrangement.Center`).

## Things Deliberately NOT Changed

Per plan constraints, the following were preserved:

- `Modifier.offset(y = (-22).dp)` on `GiftMaisonFab` inside `FabSlot` — handoff 22 dp lift remains
- `Modifier.weight(1f)` on all 5 slots (fixed by 260421-moi)
- `softWrap = false` / `maxLines = 1` on labels (fixed by 260421-moi)
- `.navigationBarsPadding()` chain
- Outer `.padding(top = 4.dp, bottom = 6.dp)` breathing room

## Verification

**Automated (passed):**

- `./gradlew :app:compileDebugKotlin -q` — succeeded
- `./gradlew :app:testDebugUnitTest --tests "com.giftregistry.ui.common.chrome.BottomNavVisibilityTest" -q` — succeeded (visibility predicate untouched, all 14 tests remain GREEN)
- File contains exactly one `.height(72.dp)` on the Row, zero `.height(56.dp)` occurrences
- File still contains `offset(y = (-22).dp)` on `GiftMaisonFab` inside `FabSlot`

**Outstanding — human-verify checkpoint (Task 2):**

The plan defines an on-device visual verification step that this executor intentionally did NOT block on (per execution constraints — quick task, automated verification only). Task 2 is left for the user to confirm:

1. Run `./gradlew :app:installDebug` (or Android Studio Run) to deploy to a device / emulator.
2. Open the app and reach a screen showing the bottom nav (Home is easiest; RegistryDetail also shows the bar).
3. Verify:
   - **Labels fully visible:** HOME / STORES / LISTS / YOU all render their full glyphs — no bottom clipping
   - **ADD caption visible:** the "ADD" monoCaps label under the centre FAB renders (was clipped entirely)
   - **FAB optically aligned:** the centre FAB's circle sits centred on the icon row's vertical midline, with its top half lifted above the bar (the 22 dp lift); should not look "shifted up" relative to the icon pills any more
   - **Touch targets unchanged:** tapping each of the 5 slots still navigates / triggers the action; selected-slot pill highlight (Home / Lists) still renders as `accentSoft`
   - **Gesture bar:** on a device with system gesture nav, the bar still sits clear of the system bar — no overlap
4. Optional: check both light and dark theme.

## Deviations from Plan

None — plan executed exactly as written. Single-line dimension swap, no auto-fixes triggered, no architectural changes needed.

## Commits

| Task | Description                                              | Commit  |
| ---- | -------------------------------------------------------- | ------- |
| 1    | Bump GiftMaisonBottomNav Row height 56 dp -> 72 dp        | b21e24e |
| 2    | (checkpoint:human-verify — outstanding, user to confirm) | n/a     |

## Self-Check: PASSED

- FOUND: `app/src/main/java/com/giftregistry/ui/common/chrome/GiftMaisonBottomNav.kt` (modified, contains `.height(72.dp)` and `offset(y = (-22).dp)`)
- FOUND: commit `b21e24e` in git log
- Compilation: succeeded
- Unit tests: BottomNavVisibilityTest passed
