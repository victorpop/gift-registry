---
status: resolved
trigger: "AddActionSheet renders incorrectly: bottom nav stays full-colour above the scrim and action rows do not appear (only drag handle + title visible)"
created: 2026-04-27T00:00:00Z
updated: 2026-04-28T00:00:00Z
resolved: 2026-04-28T00:00:00Z
---

## Resolution

**Confirmed fixed on-device by user 2026-04-28.**

Two compounding causes in `app/src/main/java/com/giftregistry/ui/common/chrome/AddActionSheet.kt`:

1. **`Modifier.bottomSheetShadow()` on the `ModalBottomSheet` modifier** — wrapped the Surface chain in an outermost `graphicsLayer { clip = true }`, fighting M3's internal predictive-back graphicsLayer + the Surface shape clip. Three competing shape clips caused the action rows (composed inside the inner predictive-back layer) to render transparently while the title (composed before that layer) stayed visible.

2. **Asymmetric dragHandle padding** (`padding(top = spacing.edgeWide)`, no bottom padding) — left no clearance for the Instrument Serif italic title's `displayM` glyphs, clipping the top of letters.

**Fix applied:**
- Removed `modifier = Modifier.bottomSheetShadow()` from the `ModalBottomSheet` call; dropped the unused `bottomSheetShadow` import.
- Changed dragHandle Box modifier to `padding(vertical = 12.dp)` (symmetric).

Compile clean; user verified on-device.

## Current Focus

hypothesis: CONFIRMED-PENDING-RUNTIME — Both fixes applied per user decision (Option A):
  1. Removed `modifier = Modifier.bottomSheetShadow()` from the ModalBottomSheet call (and dropped the now-unused `bottomSheetShadow` import).
  2. Changed custom dragHandle Box modifier from `padding(top = spacing.edgeWide)` (asymmetric 20dp top only) to `padding(vertical = 12.dp)` (symmetric — gives the title room and prevents glyph clipping).
test: Compile verification with `./gradlew :app:compileDebugKotlin`, then human-verify on-device runtime test (install debug build, tap FAB, observe action rows + title clearance + scrim coverage).
expecting: Compile clean; on-device action rows render in their paper container, title is no longer top-clipped, scrim covers nav bar area as design intends.
next_action: Compile, then issue human-verify checkpoint.

## Symptoms

expected:
  - Tapping FAB opens AddActionSheet with: drag handle, italic title "What are you adding?", and 4 ActionRow items (New registry, Item from URL, Browse stores, Add manually)
  - Scrim (ink.copy(alpha = 0.55f)) covers the entire screen including the bottom nav (the comment at AppNavigation.kt:329 explicitly states the sheet is hoisted above Scaffold "so the sheet's scrim covers the nav bar")
  - Sheet content (title + 4 rows) all render inside the paper-coloured sheet container

actual:
  - Bottom nav remains full-colour and visibly on top of the sheet — scrim does NOT cover it
  - Only the drag handle and title "What are you adding?" are visible inside the sheet
  - The italic title appears clipped at the top (only the bottom of the letters is fully visible)
  - The 4 ActionRow items are NOT rendered (or are rendered but invisible — registry card text bleeds through where they should be)

errors:
  - No runtime crash, no logcat error reported by user
  - Likely a layout / window-inset / z-order issue, not a thrown exception

reproduction:
  - Build & run the app: ./gradlew :app:installDebug
  - Sign in / land on Home (HomeKey) — registry list visible with bottom nav
  - Tap the centre + FAB in the bottom nav
  - Sheet animates up but renders incorrectly per "actual" above

started:
  - First reported 2026-04-27 after three preceding bottom-nav quick fixes (260427-lwz, 260427-n67, 260427-nkn) — none touched AddActionSheet.kt or AppNavigation.kt
  - Bug may predate those fixes — Phase 09 originally shipped CHROME-03; check 09-VERIFICATION.md and 09-HUMAN-UAT.md

## Eliminated

- hypothesis: "Color.Unspecified theme tokens cause invisible rendering"
  evidence: ModalBottomSheet inherits CompositionLocals from where it's composed via rememberCompositionContext. AddActionSheet is composed inside AppNavigation which is inside GiftRegistryTheme. So housewarmingColors() ARE provided. Furthermore, the title in the screenshot IS rendered visibly with dark color, proving colors.ink is correctly resolved.
  timestamp: 2026-04-27T00:25:00Z

- hypothesis: "skipPartiallyExpanded = false is causing PartiallyExpanded anchor to stick"
  evidence: AddActionSheet.kt:63 explicitly sets `rememberModalBottomSheetState(skipPartiallyExpanded = true)`. The M3 source at lines 301-305 only adds the PartiallyExpanded anchor if `!sheetState.skipPartiallyExpanded`, so the only anchors are Hidden and Expanded. Sheet IS in Expanded state.
  timestamp: 2026-04-27T00:25:00Z

- hypothesis: "Sheet hoisting outside Scaffold is wrong z-order"
  evidence: M3 ModalBottomSheet uses its own Dialog window via ModalBottomSheetDialogWrapper (ModalBottomSheet.android.kt). Window setLayout(MATCH_PARENT, MATCH_PARENT) covers the activity. Dialog windows are z-ordered above activity, so the scrim WOULD cover everything. Where the sheet is composed in the Compose tree (inside vs outside Scaffold) doesn't change z-order. This means the comment at AppNavigation.kt:329 is also misleading — but is not the bug cause.
  timestamp: 2026-04-27T00:25:00Z

## Evidence

- timestamp: 2026-04-27T00:00:00Z
  checked: AddActionSheet.kt:72-79 (custom dragHandle slot)
  found: dragHandle is a Box with `padding(top = spacing.edgeWide)`. spacing.edgeWide = 20.dp (per GiftMaisonSpacing.kt:45). The Box itself is `size(36.dp, 4.dp)`. Total dragHandle slot height ≈ 24dp.
  implication: Custom dragHandle adds asymmetric top padding only. Framework expects centered-padding drag handle (standard `BottomSheetDefaults.DragHandle()` uses `padding(vertical = DragHandleVerticalPadding)`).

- timestamp: 2026-04-27T00:00:00Z
  checked: AppNavigation.kt:329-360
  found: AddActionSheet is hoisted OUTSIDE the Scaffold's content lambda but INSIDE the AppNavigation composable body — it sits as a sibling to Scaffold, not above it. The comment claims this hoisting allows scrim to cover nav bar. NOT TRUE — see Eliminated.
  implication: Z-order is determined by the Dialog window, not the Compose tree position.

- timestamp: 2026-04-27T00:10:00Z
  checked: gradle/libs.versions.toml
  found: Compose BOM = 2026.03.00 → Material3 1.4.0. AGP 8.13.0, Kotlin 2.3.20.
  implication: Recent Material3 has the new `contentWindowInsets` API (renamed from `windowInsets` to be evaluable inside the new dialog window). Default = `BottomSheetDefaults.windowInsets = WindowInsets.safeDrawing.only(top + bottom)`.

- timestamp: 2026-04-27T00:15:00Z
  checked: M3 ModalBottomSheet.kt source (cached at /tmp/m3-sources)
  found: At line 277-358, the user's `modifier` parameter is applied to the Surface chain FIRST (outermost), BEFORE `align(Alignment.TopCenter)`, `widthIn(max=sheetMaxWidth)`, `fillMaxWidth()`, `nestedScroll`, `draggableAnchors{}` (line 295-326), `draggable`, `consumeWindowInsets`, `graphicsLayer{predictiveBack}`, `verticalScaleUp(sheetState)`. The shadow modifier is part of the modifier chain that wraps everything.
  implication: `Modifier.bottomSheetShadow()` = `Modifier.shadow(elevation=40.dp, shape=RoundedCornerShape(top=22, bottom=0))` becomes the OUTERMOST modifier on the Surface chain. The shadow modifier creates a graphicsLayer with `clip=true` to the shape. While graphicsLayer should not affect layout measurement directly, applying a `clip=true` graphicsLayer at the OUTER position means descendants are clipped during DRAW (not layout). This is consistent with: action rows ARE measured (and contribute to sheet height ~400dp anchoring at 47%), but rendered transparently because they're outside the clip bounds of the shape OR their backgrounds clip incorrectly. This needs runtime testing to confirm.

- timestamp: 2026-04-27T00:18:00Z
  checked: Phase 09 verification docs (.planning/phases/09-shared-chrome-status-ui/09-VERIFICATION.md, 09-HUMAN-UAT.md)
  found: UAT items 9-14 (FAB tap, sheet slide-up, layout inspection, blur, dismiss, navigation routing) are listed under "Outstanding Human UAT" — never run on-device. The verification of CHROME-03 was code-only.
  implication: This bug has been latent since Phase 09 shipped (2026-04-21). The three subsequent quick fixes (260427-lwz/n67/nkn) did NOT touch AddActionSheet or AppNavigation, so they cannot have introduced it.

- timestamp: 2026-04-27T00:20:00Z
  checked: User-supplied screenshot at /Users/victorpop/.claude/image-cache/a8f76e2b-a71a-426c-8c0c-d0b23bf7cdcd/11.png (600 × 1260 px)
  found: White sheet container starts at ~y=600px (47% from top), drag handle at ~y=625, "What are you adding?" italic title at ~y=665 with the TOP of letters clipped by the rounded sheet edge. White sheet container ends at ~y=685 — only ~85px (~50dp) of visible white. Below the white area: registry list content visible (NOT scrim-dimmed) — "test" name, "0 items · 0 reserved · 0 given", then WEDDING card. Bottom nav fully visible/full color at bottom. Scrim covers ONLY the upper half of the screen (above the sheet), not the lower half.
  implication: This is the smoking gun. If the sheet were anchored at 47% from top with content height = (1260 - 600) = 660dp ≈ 360-400dp on a small phone, the entire sheet container should fill from y=600 to y=1260. But only the top ~85px is rendered as solid white. The remaining sheet body — where the 4 action rows should be — is rendering TRANSPARENTLY, allowing the registry list behind it to show through. Sheet IS expanded; sheet IS measured at proper height; but the drawing is failing.

- timestamp: 2026-04-27T00:25:00Z
  checked: Comparison with InviteBottomSheet.kt (working ModalBottomSheet in same project)
  found: InviteBottomSheet does NOT use: custom dragHandle, custom shape, custom colors, custom modifier, or `Modifier.bottomSheetShadow()`. Uses defaults throughout. AddActionSheet differs in 4 ways: custom dragHandle, custom shape (top-rounded only), custom colors, custom modifier (bottomSheetShadow).
  implication: One or more of those 4 customizations is the root cause. Most likely: `modifier = Modifier.bottomSheetShadow()` because it's an unusual pattern (M3 expects elevation via `tonalElevation` parameter, not via `Modifier.shadow`). The shadow modifier with elevation=40dp + clip=true wraps the Surface in a graphicsLayer, which IS known to interact with M3 sheet rendering subtly — especially the predictive-back graphicsLayer applied at line 339-349 inside the framework. Two graphicsLayers stacked on the same Surface can produce render ordering bugs.

## Resolution

root_cause:
  Primary: `Modifier.bottomSheetShadow()` applied via the `modifier` parameter of `ModalBottomSheet` in AddActionSheet.kt:80. This wraps the entire ModalBottomSheet's Surface chain in a `Modifier.shadow(elevation=40.dp, shape=RoundedCornerShape(top=22, bottom=0))` which creates an outermost graphicsLayer with `clip=true`. M3's framework then layers ANOTHER graphicsLayer (predictive-back, ModalBottomSheet.kt:339-349) inside the Surface, plus the Surface's own internal shape clip — three layers fighting over the same render bounds. The action rows render transparently while the title (composed in the dragHandle slot region, before the predictive-back graphicsLayer) remains visible.
  Secondary: Custom dragHandle uses `padding(top = 20.dp)` only (asymmetric, no bottom padding). The 4dp pill + 20dp top padding leaves zero breathing room before the title text. With Instrument Serif italic at 22sp + `includeFontPadding=false` + `LineHeightStyle.Trim.None`, the rendered glyphs are taller than the layout slot, clipping the top of letters.

fix: APPLIED 2026-04-27T01:00:00Z (user chose Option A — both fixes)
  Primary: Removed `modifier = Modifier.bottomSheetShadow()` from the ModalBottomSheet call at AddActionSheet.kt:80. Also dropped the now-unused `import com.giftregistry.ui.theme.bottomSheetShadow` line. M3's default Surface rendering takes over for elevation. If the handoff's upward-cast shadow is needed later, it must live INSIDE the sheet's content slot (not on the sheet's `modifier` parameter), because that parameter wraps the entire Surface chain and stacks an outermost graphicsLayer with `clip=true` that fights M3's internal predictive-back graphicsLayer.
  Secondary: Changed custom dragHandle Box modifier from `padding(top = spacing.edgeWide)` (asymmetric — 20dp top only, no bottom) to `padding(vertical = 12.dp)` (symmetric — 12dp top + 12dp bottom, total 24dp slot height including the 4dp pill). This gives the Instrument Serif italic title at displayM size room to render its full glyph height without top clipping.

  Diff applied:
  ```
  -import com.giftregistry.ui.theme.bottomSheetShadow
  ...
           dragHandle = {
               Box(
                   modifier = Modifier
  -                    .padding(top = spacing.edgeWide)
  +                    .padding(vertical = 12.dp)
                       .size(width = 36.dp, height = 4.dp)
                       .background(colors.line, GiftMaisonTheme.shapes.pill),
               )
           },
  -        modifier = Modifier.bottomSheetShadow(),
       ) {
  ```

verification: pending — user must install debug build, tap FAB, confirm action rows render and title is not clipped
files_changed:
  - app/src/main/java/com/giftregistry/ui/common/chrome/AddActionSheet.kt
