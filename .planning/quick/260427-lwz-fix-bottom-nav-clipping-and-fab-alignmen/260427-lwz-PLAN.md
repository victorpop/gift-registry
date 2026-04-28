---
phase: quick-260427-lwz
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - app/src/main/java/com/giftregistry/ui/common/chrome/GiftMaisonBottomNav.kt
autonomous: false
requirements:
  - QUICK-LWZ-01
must_haves:
  truths:
    - "All 4 icon-row labels (HOME / STORES / LISTS / YOU) render in full, not clipped at the bottom"
    - "The 'ADD' caption under the centre FAB is fully visible"
    - "The centre FAB sits visually centred relative to the icon-row pills (no apparent vertical drift)"
    - "The FAB's 22 dp lift above the bar baseline is preserved"
  artifacts:
    - path: "app/src/main/java/com/giftregistry/ui/common/chrome/GiftMaisonBottomNav.kt"
      provides: "GiftMaisonBottomNav composable with content height that fits icon + spacer + label without clipping"
      contains: ".height(72.dp)"
  key_links:
    - from: "GiftMaisonBottomNav Row"
      to: "NavItemSlot Column (44 dp pill + 4 dp spacer + monoCaps label)"
      via: "Row.height(72.dp) — must be >= total NavItemSlot stack height"
      pattern: "\\.height\\(72\\.dp\\)"
    - from: "FabSlot"
      to: "GiftMaisonFab"
      via: "offset(y = (-22).dp) — UNCHANGED, must remain after fix"
      pattern: "offset\\(y = \\(-22\\)\\.dp\\)"
---

<objective>
Fix the GiftMaison bottom nav clipping bug — labels (HOME / STORES / LISTS / YOU / ADD) are
cut off at the bottom and the centre FAB looks vertically misaligned vs the icon row.

Purpose: The bar's `.height(56.dp)` content area is too small for the icon-pill (44 dp) +
spacer (4 dp) + monoCaps label (~14 dp) = ~62 dp stack used by NavItemSlot. The FabSlot
overflows similarly. Both cause label clipping, and the FabSlot's `Arrangement.Center` of
overflowing content visually shifts the FAB off the icon-row optical centre.

Output: GiftMaisonBottomNav.kt with the Row's `.height()` bumped from 56 dp → 72 dp.
Single-line change. FAB's `offset(y = (-22).dp)` lift behaviour is intentionally preserved.
</objective>

<execution_context>
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/workflows/execute-plan.md
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@CLAUDE.md
@app/src/main/java/com/giftregistry/ui/common/chrome/GiftMaisonBottomNav.kt
@app/src/main/java/com/giftregistry/ui/common/chrome/GiftMaisonFab.kt
@app/src/test/java/com/giftregistry/ui/common/chrome/BottomNavVisibilityTest.kt

<measurements>
<!-- Stack heights derived from reading the source files above. -->

NavItemSlot content stack (Column, Arrangement.Center):
  - Box pill (icon container): .size(44.dp)
  - Spacer:                     .height(4.dp)
  - Text (monoCaps caption):    ~14 dp glyph + line metrics
  Total:                       ~62 dp

FabSlot content stack (Column, Arrangement.Center):
  - GiftMaisonFab:              .size(54.dp), then offset(y = (-22).dp)
  - Text ("ADD" caption):       ~14 dp glyph + line metrics
  Note: offset() does NOT reduce measured height — the FAB still consumes 54 dp in layout,
  so the FabSlot's content stack is also > 56 dp.

Current Row constraints (the bug):
  - .padding(top = 4.dp, bottom = 6.dp)  // outer breathing room above/below content
  - .height(56.dp)                        // CONTENT area — too small, clips labels

Target:
  - .height(72.dp)  // 44 (pill) + 4 (spacer) + 14 (label) + ~10 dp breathing room
</measurements>

<related_history>
- 260420-hua: introduced original bottom nav (Home / Add list / Browse stores / Preferences)
- 260420-iro: persistent bottom nav across screens
- 260421-moi: weighted 5 slots evenly + maxLines/softWrap=false guards on labels (fixed truncation)
- Phase 09 P03 (CHROME-01): GiftMaison 5-slot redesign — Home · Stores · [FAB] · Lists · You
- 260427-gxu / 260427-lnq: Instrument Serif bundling + auth headline colour fixes (font work,
  unrelated to this layout bug — but confirms that recent chrome edits have been minimal and
  surgical)

Today's fix is the second iteration of this nav bar in 6 days — keep the change tiny and
focused on the height token only. Do NOT touch slot weighting, label softWrap, FAB lift, or
the navigationBarsPadding chain.
</related_history>
</context>

<tasks>

<task type="auto">
  <name>Task 1: Bump GiftMaisonBottomNav Row content height 56 dp → 72 dp</name>
  <files>app/src/main/java/com/giftregistry/ui/common/chrome/GiftMaisonBottomNav.kt</files>
  <action>
In `GiftMaisonBottomNav` (the top-level @Composable, lines ~69–78), change the Row modifier
chain's `.height(56.dp)` to `.height(72.dp)`.

Exact line to change (inside the Row's `modifier = modifier ...` chain):

```kotlin
.padding(top = 4.dp, bottom = 6.dp)
.height(56.dp),                       // ← change to .height(72.dp)
```

becomes:

```kotlin
.padding(top = 4.dp, bottom = 6.dp)
.height(72.dp),
```

Why 72 dp: NavItemSlot stack = 44 (pill) + 4 (spacer) + ~14 (label) = ~62 dp. 72 dp gives the
label its full glyph height plus ~10 dp of breathing room above and below the icon row, which
is also enough vertical space for the FabSlot's 54 dp FAB + label stack to be centred without
overflow. The FAB's `offset(y = (-22).dp)` continues to lift it visually above the bar; with a
properly sized content area, the lift now reads as intentional rather than a layout glitch.

DO NOT change:
- `.navigationBarsPadding()` — gesture-bar handling is correct
- `.padding(top = 4.dp, bottom = 6.dp)` — outer breathing room is correct
- The `Modifier.offset(y = (-22).dp)` on `GiftMaisonFab` inside `FabSlot` — handoff value, must
  remain (per <constraints> in the planning brief)
- Slot weighting (`Modifier.weight(1f)` on each of the 5 slots) — fixed by 260421-moi
- Label `softWrap = false` / `maxLines = 1` guards — fixed by 260421-moi

Touch nothing else in this file or in GiftMaisonFab.kt.
  </action>
  <verify>
    <automated>./gradlew :app:compileDebugKotlin -q && ./gradlew :app:testDebugUnitTest --tests "com.giftregistry.ui.common.chrome.BottomNavVisibilityTest" -q</automated>
  </verify>
  <done>
- GiftMaisonBottomNav.kt contains exactly one occurrence of `.height(72.dp)` on the Row, and
  no remaining occurrences of `.height(56.dp)` in this file.
- File still contains `offset(y = (-22).dp)` on GiftMaisonFab inside FabSlot (FAB lift preserved).
- `:app:compileDebugKotlin` succeeds.
- `BottomNavVisibilityTest` (14 tests) still passes — the visibility predicate is untouched, so
  tests must remain GREEN.
  </done>
</task>

<task type="checkpoint:human-verify" gate="blocking">
  <name>Task 2: On-device visual verification of bottom nav</name>
  <what-built>
Bottom nav Row content area expanded from 56 dp → 72 dp in GiftMaisonBottomNav.kt. This is
purely a layout dimension change — no logic, no string, no theme edits. The FAB's 22 dp lift
above the bar baseline is unchanged.
  </what-built>
  <how-to-verify>
1. Run `./gradlew :app:installDebug` (or use Android Studio's Run button) to deploy a debug
   build to your connected device / emulator.
2. Open the app and reach a screen that shows the bottom nav — Home (registry list) is the
   easiest. RegistryDetail is the only other surface where the bar appears.
3. Verify each of the following:
   a. **Labels fully visible**: HOME, STORES, LISTS, YOU all render their full glyphs — no
      bottom clipping. Compare against the bug screenshot if available.
   b. **ADD caption visible**: The "ADD" monoCaps label under the centre FAB now renders.
      Previously it was clipped entirely.
   c. **FAB optically aligned**: The centre FAB's circle sits centred on the icon row's
      vertical midline (pills row), with its top half lifted above the bar (the 22 dp lift).
      It should not look "shifted up" relative to the icon pills any more.
   d. **Touch targets unchanged**: Tapping each of the 5 slots still navigates / triggers the
      action. Selected-slot pill highlight (Home / Lists) still renders as accentSoft.
   e. **Gesture bar**: On a device with system gesture nav (3-button or pill), the bar still
      sits clear of the system bar — no overlap.
4. Optionally check both light and dark theme if your build supports a runtime toggle.

Expected outcome: all 5 labels readable, ADD caption visible, FAB centred and lifted, no
regression on gesture bar handling.
  </how-to-verify>
  <resume-signal>Type "approved" if all checks pass, or describe what's still wrong.</resume-signal>
</task>

</tasks>

<verification>
Combined verification across both tasks:
- Source file diff shows exactly one substitution: `.height(56.dp)` → `.height(72.dp)` on the
  Row inside `GiftMaisonBottomNav`.
- `:app:compileDebugKotlin` and `BottomNavVisibilityTest` GREEN.
- Human on-device confirmation that all 5 labels render, ADD is visible, and FAB sits centred.
</verification>

<success_criteria>
- Row content height = 72 dp in GiftMaisonBottomNav.kt
- No other modifier or composable in this file or GiftMaisonFab.kt has been touched
- Compilation passes; existing unit tests pass
- User has visually confirmed labels are uncut and FAB is aligned
- FAB's `offset(y = (-22).dp)` lift remains in source
</success_criteria>

<output>
After completion, create `.planning/quick/260427-lwz-fix-bottom-nav-clipping-and-fab-alignmen/260427-lwz-SUMMARY.md`
documenting: file changed, the one-line dimension swap, and a STATE.md row reference for the
Quick Tasks Completed table.
</output>
