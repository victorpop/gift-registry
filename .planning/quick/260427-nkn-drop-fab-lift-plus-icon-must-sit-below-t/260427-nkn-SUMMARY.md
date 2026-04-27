---
phase: quick-260427-nkn
plan: 01
subsystem: ui-chrome
tags: [bottom-nav, fab, layout, design-handoff-deviation]
requires:
  - GiftMaisonBottomNav.FabSlot scaffold (quick-260427-n67)
  - 72 dp bar Row content height (quick-260427-lwz)
provides:
  - FAB renders flush within bar (no upward offset, no protrusion above gray border)
affects:
  - app/src/main/java/com/giftregistry/ui/common/chrome/GiftMaisonBottomNav.kt
  - app/src/main/java/com/giftregistry/ui/common/chrome/GiftMaisonFab.kt
tech-stack:
  added: []
  patterns:
    - requiredSize(54.dp) without chained .offset() — FAB centers in 44 dp Box scaffold,
      overflows 5 dp top/bottom but stays inside the 72 dp Row because Row's
      padding(top=4.dp) lives OUTSIDE the height
key-files:
  created: []
  modified:
    - app/src/main/java/com/giftregistry/ui/common/chrome/GiftMaisonBottomNav.kt
    - app/src/main/java/com/giftregistry/ui/common/chrome/GiftMaisonFab.kt
decisions:
  - User feedback overrides handoff JSX `top: -22` lift — FAB now flush within bar
metrics:
  duration: ~2 min
  completed: 2026-04-27
  tasks: 1 of 2 auto-tasks executed (Task 2 is a human-verify checkpoint, outstanding)
  files: 2
---

# Quick Task quick-260427-nkn: Drop FAB lift — plus icon must sit below the bar's top border line — Summary

Removed the 22 dp upward offset from the GiftMaison bottom-nav FAB so the plus icon sits fully inside the bar (no protrusion above the gray top border line) per direct user feedback after on-device review.

## What Changed

**`app/src/main/java/com/giftregistry/ui/common/chrome/GiftMaisonBottomNav.kt`**

1. Removed unused import `androidx.compose.foundation.layout.offset` (was the only consumer in the file).
2. `FabSlot`: replaced the FAB modifier chain
   ```kotlin
   modifier = Modifier
       .requiredSize(54.dp)
       .offset(y = (-22).dp),
   ```
   with
   ```kotlin
   modifier = Modifier.requiredSize(54.dp),
   ```
3. Rewrote the `FabSlot` KDoc and the two inline comments around the FAB call site to:
   - Drop the "lifted 22 dp above the bar baseline" claim.
   - Add a one-paragraph note explaining the override (handoff JSX `top: -22` rejected after on-device review under user feedback 2026-04-27 / quick-260427-nkn).
   - Document the new vertical-position math (5 dp overflow on each side of the 44 dp Box; Row's `padding(top=4.dp)` lives outside the 72 dp height, so FAB top sits 4 dp below the gray border).

**`app/src/main/java/com/giftregistry/ui/common/chrome/GiftMaisonFab.kt`**

4. Updated the KDoc on `GiftMaisonFab` to drop the "caller applies 22 dp lift" claim and document the user-feedback-driven design override. Function body (`Box` + `fabShadow → border → background → clickable → semantics` modifier chain + 24 dp `Add` icon) is **untouched**.

## Layout Math (Why the Fix Works)

The Row scaffold around the bottom nav has these constraints (unchanged by this plan):

- `padding(top = 4.dp, bottom = 6.dp)` — applied OUTSIDE the height
- `.height(72.dp)` — total content row height
- `.border(width = 1.dp, color = colors.line)` — the gray line at the top edge

After this fix:

1. The `FabSlot` Column wraps to ~62 dp (44 dp Box + 4 dp Spacer + ~14 dp text), vertically centered in the 72 dp Row → top gap ≈5 dp, bottom gap ≈5 dp. So the inner 44 dp Box scaffold sits at row y = 5..49.
2. `requiredSize(54.dp)` makes the FAB ignore the 44 dp parent constraint and render at 54 dp, centered in the 44 dp Box. Center of Box at row y = 27 → FAB at y = 0..54.
3. Row y = 0 is 4 dp below the gray border (because `padding(top=4.dp)` is OUTSIDE the height).
4. **Therefore: FAB top sits 4 dp below the gray line.** Fully inside the bar. Plus icon no longer crossed by the border. FAB center at row y = 27 matches the icon-pill row center (icon-pill Box at row y = 5..49 → center 27), so vertical alignment with HOME / STORES / LISTS / YOU icons is preserved.

## Verification

- `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest --tests "com.giftregistry.ui.common.chrome.BottomNavVisibilityTest"` → BUILD SUCCESSFUL (15s).
- 14/14 tests in `BottomNavVisibilityTest` pass.
- No new compile warnings introduced (no unused-import warning; pre-existing deprecation warnings in unrelated files are unchanged).
- `Grep "offset"` against `GiftMaisonBottomNav.kt` returns zero matches — neither the import nor any modifier chain reference remains.

## Outstanding (Task 2 — human-verify checkpoint)

Per the plan, Task 2 is a `checkpoint:human-verify` requiring an on-device build and visual confirmation. Per orchestrator instructions, this executor did not block on it; the user must complete the manual verification described below.

**How to verify on-device:**

1. `./gradlew :app:installDebug`
2. Launch app, navigate to Home (registry list). Inspect the centre slot of the bottom nav (green plus FAB):
   - **Expected:** The plus icon's top edge sits BELOW the thin gray horizontal line that runs along the top of the bottom nav bar. Small ~4 dp gap between the gray line and the top of the green FAB circle.
   - **Expected:** The plus icon is vertically centered with HOME / STORES / LISTS / YOU icons.
   - **Expected:** "ADD" caption aligns horizontally with HOME / STORES / LISTS / YOU captions.
   - **NOT expected:** Any part of the green FAB circle protruding ABOVE the gray border line.
3. Navigate to a Registry Detail screen — verify same FAB position there.
4. Tap the FAB to confirm it still functions (opens AddActionSheet). Tap behaviour is untouched by this fix.

If the FAB still appears crossed by the gray line on-device, the layout assumption (Row `padding(top=4.dp)` lives outside `height(72.dp)`) needs re-validation — the math here assumes Compose's `Modifier.padding` followed by `Modifier.height` interprets the height as content-only, which is the documented behaviour but worth confirming visually.

## Deviations from Plan

None. Plan executed exactly as written: 4 edits across 2 files, 1 commit.

## Commits

- `d4d9a4a` — `fix(quick-260427-nkn-01): drop FAB lift — plus icon now sits below the bar's top border line`

## Self-Check: PASSED

- `app/src/main/java/com/giftregistry/ui/common/chrome/GiftMaisonBottomNav.kt`: FOUND (modified)
- `app/src/main/java/com/giftregistry/ui/common/chrome/GiftMaisonFab.kt`: FOUND (modified)
- Commit `d4d9a4a`: FOUND in `git log`
- `Grep "offset"` against `GiftMaisonBottomNav.kt`: 0 matches (import + offset() call both removed)
- `BottomNavVisibilityTest`: 14/14 pass; `:app:compileDebugKotlin` clean
