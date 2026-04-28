---
phase: quick-260427-nkn
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - app/src/main/java/com/giftregistry/ui/common/chrome/GiftMaisonBottomNav.kt
  - app/src/main/java/com/giftregistry/ui/common/chrome/GiftMaisonFab.kt
autonomous: false
requirements:
  - QUICK-NKN-01
must_haves:
  truths:
    - "Plus icon (FAB) sits fully below the bar's top gray border line — no protrusion above it"
    - "FAB remains horizontally centered in its 5-slot column"
    - "ADD label remains aligned with the other four nav labels (HOME/STORES/LISTS/YOU)"
    - "Existing BottomNavVisibilityTest still passes"
    - "App compiles without unused imports"
  artifacts:
    - path: "app/src/main/java/com/giftregistry/ui/common/chrome/GiftMaisonBottomNav.kt"
      provides: "FabSlot with FAB sitting flush within bar (no offset lift)"
      contains: "Modifier.requiredSize(54.dp)"
    - path: "app/src/main/java/com/giftregistry/ui/common/chrome/GiftMaisonFab.kt"
      provides: "GiftMaisonFab composable (KDoc updated, implementation unchanged)"
      contains: "Box"
  key_links:
    - from: "FabSlot in GiftMaisonBottomNav.kt"
      to: "GiftMaisonFab composable"
      via: "Modifier.requiredSize(54.dp) — no .offset(...) chained after"
      pattern: "requiredSize\\(54\\.dp\\)\\s*\\)"
---

<objective>
Drop the 22 dp FAB lift in the GiftMaison bottom nav so the plus icon sits fully below the bar's top gray border line, per direct user feedback after on-device review.

Purpose: Prior fixes (260427-lwz raised bar to 72 dp; 260427-n67 mirrored FabSlot scaffold for ADD-label alignment) preserved the handoff's 22 dp upward lift on the FAB. The user has now reviewed the running app and decided the floating-FAB lift looks wrong — the plus icon is "crossed by the horizontal line" (the bar's `border(1.dp, colors.line)`). User wants the FAB inside the bar, flush, no protrusion above the top border. This **overrides** the handoff intent and any prior "22 dp lift per handoff" comments.

Output: GiftMaisonBottomNav.kt FabSlot renders FAB with `Modifier.requiredSize(54.dp)` only — no `.offset(...)`. KDoc/inline comments updated. Unused `offset` import removed. GiftMaisonFab.kt KDoc updated to match new behaviour. GiftMaisonFab implementation untouched.
</objective>

<execution_context>
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/workflows/execute-plan.md
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@/Users/victorpop/ai-projects/gift-registry/CLAUDE.md
@/Users/victorpop/ai-projects/gift-registry/.planning/STATE.md
@/Users/victorpop/ai-projects/gift-registry/app/src/main/java/com/giftregistry/ui/common/chrome/GiftMaisonBottomNav.kt
@/Users/victorpop/ai-projects/gift-registry/app/src/main/java/com/giftregistry/ui/common/chrome/GiftMaisonFab.kt
@/Users/victorpop/ai-projects/gift-registry/app/src/test/java/com/giftregistry/ui/common/chrome/BottomNavVisibilityTest.kt

<interfaces>
<!-- Key call sites involved in the fix. Extracted from the two files. -->
<!-- Executor uses these directly — no codebase exploration needed. -->

From GiftMaisonBottomNav.kt (FabSlot, lines 168–211):
```kotlin
// CURRENT (buggy):
@Composable
private fun FabSlot(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GiftMaisonTheme.colors
    val typography = GiftMaisonTheme.typography
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // 44 dp invisible scaffold — matches NavItemSlot icon-pill footprint so the
        // column wraps to the same height (~62 dp), aligning the ADD label with the
        // other four nav labels. The FAB renders OUTSIDE this footprint via
        // requiredSize(54.dp) + offset(y = -22.dp) per handoff design.
        Box(
            modifier = Modifier.size(44.dp),
            contentAlignment = Alignment.Center,
        ) {
            GiftMaisonFab(
                onClick = onClick,
                // requiredSize forces 54 dp visual despite 44 dp parent constraint;
                // offset preserves the 22 dp lift above the bar baseline (handoff).
                modifier = Modifier
                    .requiredSize(54.dp)
                    .offset(y = (-22).dp),
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.nav_fab_add),
            style = typography.monoCaps,
            color = colors.inkFaint,
            maxLines = 1,
            softWrap = false,
        )
    }
}
```

From GiftMaisonBottomNav.kt imports (line 14):
```kotlin
import androidx.compose.foundation.layout.offset
```
This is the ONLY use of `offset` in the file (verified via Read of full file). Once removed
from FabSlot, this import becomes unused and must be deleted.

From GiftMaisonFab.kt (lines 25–34) — KDoc only, do NOT touch implementation:
```kotlin
/**
 * CHROME-02: 54 dp centre FAB — accent fill, 4 dp paper ring, accent shadow.
 *
 * The handoff's 22 dp lift (`top: -22`) is applied by the CALLER
 * (GiftMaisonBottomNav's FAB slot), not here — keeping this composable slot-
 * agnostic so it can be previewed in the style guide without nav context.
 *
 * Modifier chain order is CRITICAL (Pitfall 4): fabShadow → border → background.
 * Applying background before border would paint over the paper ring.
 */
```
</interfaces>

<background>
The Row scaffold (lines 69–78) has these constraints:
- `padding(top = 4.dp, bottom = 6.dp)` — OUTSIDE the height
- `.height(72.dp)` — total content row height
- `.border(width = 1.dp, color = colors.line)` — the gray line at the top edge

The inner FabSlot column wraps to ~62 dp (44 dp Box + 4 dp Spacer + ~14 dp text). It's
centered vertically in the 72 dp row, so the inner Box scaffold sits at row y = 5..49.
Without the `-22.dp` offset, the `requiredSize(54.dp)` FAB renders centered in the 44 dp
Box, overflowing 5 dp top + 5 dp bottom — i.e. drawing at row y = 0..54.

Row y = 0 is 4 dp BELOW the top border line (the `padding(top = 4.dp)` is outside the
height). So FAB top at row y = 0 puts the FAB 4 dp below the gray border — fully inside
the bar, no protrusion. FAB center lands at row y = 27, matching the icon-pill row
center (5..49 → center 27). Vertical alignment is preserved.
</background>
</context>

<tasks>

<task type="auto">
  <name>Task 1: Remove FAB lift — drop .offset() from FabSlot, remove unused import, update KDocs</name>
  <files>
    app/src/main/java/com/giftregistry/ui/common/chrome/GiftMaisonBottomNav.kt,
    app/src/main/java/com/giftregistry/ui/common/chrome/GiftMaisonFab.kt
  </files>
  <action>
    Make THREE edits in `GiftMaisonBottomNav.kt`:

    **Edit 1 — Remove unused import (line 14):**
    Delete the line `import androidx.compose.foundation.layout.offset`.
    The import is used ONLY by FabSlot's `.offset(y = (-22).dp)` call (verified by reading
    the full file). After Edit 2 removes that call, this import becomes unused and the
    Kotlin compiler / ktlint will flag it.

    **Edit 2 — Drop the FAB lift (FabSlot, lines ~193–200):**
    Replace the modifier chain on the GiftMaisonFab call. Change:
    ```kotlin
                modifier = Modifier
                    .requiredSize(54.dp)
                    .offset(y = (-22).dp),
    ```
    To:
    ```kotlin
                modifier = Modifier.requiredSize(54.dp),
    ```
    Also remove the inline comment immediately above (currently reads
    `// requiredSize forces 54 dp visual despite 44 dp parent constraint;`
    `// offset preserves the 22 dp lift above the bar baseline (handoff).`)
    Replace it with a single-line comment explaining the new behaviour:
    ```kotlin
                // requiredSize forces 54 dp visual; FAB centers in 44 dp Box,
                // overflowing 5 dp top/bottom. With Row padding(top=4.dp) outside
                // the 72.dp height, the FAB top lands 4 dp below the gray border —
                // fully inside the bar, no protrusion. (Removed handoff 22 dp lift
                // per user feedback 2026-04-27 — looked crossed by border line.)
    ```

    **Edit 3 — Update FabSlot KDoc (lines 168–172):**
    Change the doc comment block above `private fun FabSlot(` from:
    ```kotlin
    /**
     * FAB slot: 44 dp invisible scaffold (mirrors NavItemSlot icon-pill footprint
     * so the ADD label aligns with the other nav labels) wrapping a 54 dp FAB
     * lifted 22 dp above the bar baseline + "ADD" caption.
     */
    ```
    To:
    ```kotlin
    /**
     * FAB slot: 44 dp invisible scaffold (mirrors NavItemSlot icon-pill footprint
     * so the ADD label aligns with the other nav labels) wrapping a 54 dp FAB
     * that sits flush within the bar (no upward lift) + "ADD" caption.
     *
     * The FAB used to lift 22 dp above the bar per handoff JSX (`top: -22`), but
     * on-device review showed the plus icon getting crossed by the bar's top
     * border line. User feedback 2026-04-27 (quick-260427-nkn): keep the FAB
     * inside the bar, no protrusion above the gray line.
     */
    ```

    Also update the inline scaffold comment inside FabSlot (currently lines 185–188):
    ```kotlin
            // 44 dp invisible scaffold — matches NavItemSlot icon-pill footprint so the
            // column wraps to the same height (~62 dp), aligning the ADD label with the
            // other four nav labels. The FAB renders OUTSIDE this footprint via
            // requiredSize(54.dp) + offset(y = -22.dp) per handoff design.
    ```
    Replace with:
    ```kotlin
            // 44 dp invisible scaffold — matches NavItemSlot icon-pill footprint so the
            // column wraps to the same height (~62 dp), aligning the ADD label with the
            // other four nav labels. The FAB visually overflows this footprint via
            // requiredSize(54.dp) (5 dp top/bottom overflow), but stays fully inside
            // the bar — see Edit 2 comment for vertical-position math.
    ```

    **Edit 4 — Update GiftMaisonFab.kt KDoc (lines 25–34):**
    In `app/src/main/java/com/giftregistry/ui/common/chrome/GiftMaisonFab.kt`,
    change the KDoc from:
    ```kotlin
    /**
     * CHROME-02: 54 dp centre FAB — accent fill, 4 dp paper ring, accent shadow.
     *
     * The handoff's 22 dp lift (`top: -22`) is applied by the CALLER
     * (GiftMaisonBottomNav's FAB slot), not here — keeping this composable slot-
     * agnostic so it can be previewed in the style guide without nav context.
     *
     * Modifier chain order is CRITICAL (Pitfall 4): fabShadow → border → background.
     * Applying background before border would paint over the paper ring.
     */
    ```
    To:
    ```kotlin
    /**
     * CHROME-02: 54 dp centre FAB — accent fill, 4 dp paper ring, accent shadow.
     *
     * Slot-agnostic: caller controls placement. As of 2026-04-27 (quick-260427-nkn)
     * the FAB no longer lifts above the bar — the handoff's 22 dp upward offset
     * (`top: -22`) was dropped after on-device review showed the plus icon crossing
     * the bar's top border line. The FAB now renders flush within GiftMaisonBottomNav,
     * vertically centered with the icon-pill row.
     *
     * Modifier chain order is CRITICAL (Pitfall 4): fabShadow → border → background.
     * Applying background before border would paint over the paper ring.
     */
    ```

    Do **NOT** modify the GiftMaisonFab function body (lines 36–62) — implementation
    is untouched. Do **NOT** modify any other element in GiftMaisonBottomNav.kt:
    Row scaffold (height 72 dp, border, padding), 44 dp Box scaffold, 4 dp Spacer,
    NavItemSlot, weight(1f), padding(horizontal = 4.dp) all preserved.

    **Why this fix works (vertical-position math, for executor confidence):**
    - Row: `padding(top=4.dp, bottom=6.dp)` outside `.height(72.dp)`. Border at top edge.
    - FabSlot Column wraps to ~62 dp, vertically centered in 72 dp Row → top gap ~5 dp,
      bottom gap ~5 dp. So the inner 44 dp Box scaffold sits at row y = 5..49.
    - `requiredSize(54.dp)` makes the FAB ignore the 44 dp parent constraint and render
      at 54 dp, centered in the 44 dp Box. Center of Box at row y = 27 → FAB at y = 0..54.
    - Row y = 0 is 4 dp below the gray border (padding is OUTSIDE height).
    - Therefore FAB top sits 4 dp below the gray line. Fully inside the bar. Plus icon
      no longer crossed by the border. User's requested fix.
  </action>
  <verify>
    <automated>./gradlew :app:compileDebugKotlin :app:testDebugUnitTest --tests "com.giftregistry.ui.common.chrome.BottomNavVisibilityTest"</automated>
    Compile must succeed (no unresolved-reference errors from removed `offset` import,
    no unused-import warnings). BottomNavVisibilityTest must still pass (14 tests).
    Also visually grep the file to confirm no `.offset(` remains in the FAB modifier
    chain and no `import ...layout.offset` remains in imports.
  </verify>
  <done>
    - `GiftMaisonBottomNav.kt` no longer contains `.offset(y = (-22).dp)` anywhere
    - `GiftMaisonBottomNav.kt` no longer imports `androidx.compose.foundation.layout.offset`
    - FabSlot KDoc + inline comment + GiftMaisonFab.kt KDoc reflect "no lift" behaviour
    - `:app:compileDebugKotlin` succeeds
    - `BottomNavVisibilityTest` (14 tests) all green
    - `GiftMaisonFab.kt` function body (Box + Modifier chain) unchanged
  </done>
</task>

<task type="checkpoint:human-verify" gate="blocking">
  <name>Task 2: Human-verify — plus icon sits below the gray border line on-device</name>
  <what-built>
    Removed the 22 dp upward offset from the FAB in GiftMaisonBottomNav. The plus icon
    should now sit fully INSIDE the bottom navigation bar, with its top edge 4 dp below
    the gray horizontal border line that runs across the top of the bar. The FAB is no
    longer "floating" above the bar — it's flush within it.
  </what-built>
  <how-to-verify>
    1. Build and install the debug APK: `./gradlew :app:installDebug`
    2. Launch the app, sign in (or use existing session), navigate to Home (registry list).
    3. Inspect the bottom nav at the centre slot (the green plus FAB):
       - **Expected:** The plus icon's top edge sits BELOW the thin gray horizontal line
         that runs along the top of the bottom nav bar. There should be a small gap
         (~4 dp) between the gray line and the top of the green FAB circle.
       - **Expected:** The plus icon is vertically centered with the other nav icons
         (Home, Stores, Lists, You). They should all appear at roughly the same height.
       - **Expected:** The "ADD" caption below the FAB still aligns horizontally with the
         "HOME" / "STORES" / "LISTS" / "YOU" captions of the other slots.
       - **NOT expected:** Any part of the green FAB circle protruding ABOVE the gray
         border line. No part of the plus icon should be crossed/intersected by that line.
    4. Navigate to a Registry Detail screen — verify the same FAB position there
       (bottom nav is visible on HomeKey + RegistryDetailKey only).
    5. Tap the FAB to confirm it still functions (opens Add action sheet / Add Item flow).
       Tap behaviour is unchanged by this fix.
    6. Optional: use Layout Inspector or "Show layout bounds" in dev settings to confirm
       the FAB's bounding box is fully within the bar's bounding box.

    If the FAB still appears to protrude above the gray line, or the plus icon is still
    crossed by it, report back with a screenshot — the layout math may need a second
    look (e.g. the inner Box scaffold size or Row padding may need adjusting).
  </how-to-verify>
  <resume-signal>Type "approved" when the plus icon sits cleanly below the gray border, or describe what's still wrong.</resume-signal>
</task>

</tasks>

<verification>
- `./gradlew :app:compileDebugKotlin` succeeds with no warnings about unused imports
- `./gradlew :app:testDebugUnitTest --tests "com.giftregistry.ui.common.chrome.BottomNavVisibilityTest"` — all 14 tests pass
- Manual on-device check (Task 2): plus icon top edge sits below the bar's gray border line
- No other tests in `app/src/test/.../chrome/` regress (sanity: `./gradlew :app:testDebugUnitTest --tests "com.giftregistry.ui.common.chrome.*"` if other chrome tests exist)
</verification>

<success_criteria>
- [ ] FAB modifier chain in FabSlot is exactly `Modifier.requiredSize(54.dp)` — no `.offset(...)`
- [ ] `import androidx.compose.foundation.layout.offset` removed from GiftMaisonBottomNav.kt
- [ ] FabSlot KDoc + inline comment updated to reflect "no lift" behaviour, with reason (user feedback 2026-04-27)
- [ ] GiftMaisonFab.kt KDoc (lines 25–34) updated to remove the "caller applies 22 dp lift" claim
- [ ] GiftMaisonFab function body unchanged
- [ ] Compile succeeds, BottomNavVisibilityTest passes
- [ ] Human-verify approved on-device (plus icon below gray line, no protrusion)
</success_criteria>

<output>
After completion, create `.planning/quick/260427-nkn-drop-fab-lift-plus-icon-must-sit-below-t/260427-nkn-SUMMARY.md` documenting the fix, the layout math, and any deviations.
</output>
