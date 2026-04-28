---
phase: quick-260427-gld
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - app/src/main/java/com/giftregistry/ui/theme/GiftMaisonWordmark.kt
  - app/src/main/java/com/giftregistry/ui/common/ConcentricRings.kt
  - app/src/main/java/com/giftregistry/ui/auth/GoogleBanner.kt
autonomous: true
requirements: [WORDMARK-FIX, GOOGLE-BANNER-HEIGHT-FIX]
must_haves:
  truths:
    - "The 'giftmaison.' wordmark in the top-left of the Auth screen renders as readable text — letters visually separated, not mashed into a dark smear"
    - "GiftMaisonWordmark.kt expresses letterSpacing in .sp (matching the quick-260421-lwi pattern), not .em"
    - "The Google sign-in banner on the Auth screen renders at ~68 dp tall (40 dp Row content + 14×2 vertical padding), not the current ~98 dp"
    - "ConcentricRings still draws three rings anchored at the banner's top-right corner, but no longer dictates the banner's measured height"
    - "Caller controls ConcentricRings sizing — Canvas uses the modifier passed in, with no internal .size(70.dp)"
    - "All existing WordmarkTest, ColorsTest, FontsTest, ShapesAndDimensTest, TypographyTest assertions continue to pass — no test changes required (none assert on letter-spacing for wordmark, none assert on banner height)"
  artifacts:
    - path: "app/src/main/java/com/giftregistry/ui/theme/GiftMaisonWordmark.kt"
      provides: "Wordmark composable with letterSpacing in .sp matching the typography pattern"
      contains: "letterSpacing = (-0.4).sp"
    - path: "app/src/main/java/com/giftregistry/ui/common/ConcentricRings.kt"
      provides: "Caller-sized decorative rings — Canvas drawn within whatever bounds the caller's modifier provides"
      contains: "Canvas(modifier = modifier)"
    - path: "app/src/main/java/com/giftregistry/ui/auth/GoogleBanner.kt"
      provides: "Banner where ConcentricRings sits inside a matchParentSize() Box so rings overlay parent bounds without forcing parent height"
      contains: "Modifier.matchParentSize()"
  key_links:
    - from: "GiftMaisonWordmark.kt"
      to: "Phase 8 typography unit decision (quick-260421-lwi)"
      via: "letterSpacing in .sp matches the JSX naked-numeric-as-pixels mapping established for the 10 type roles"
      pattern: "letterSpacing = \\(?-?[0-9.]+\\)?\\.sp"
    - from: "GoogleBanner.kt"
      to: "ConcentricRings.kt"
      via: "matchParentSize Box wrapper — rings consume the wrapper's full size (= the banner's natural Row-driven height) without the Canvas claiming an intrinsic 70 dp"
      pattern: "Box\\(\\s*modifier = Modifier\\.matchParentSize\\(\\)\\s*\\)"
    - from: "ConcentricRings.kt"
      to: "GoogleBanner.kt (and any future caller)"
      via: "Caller-supplied modifier sizing — no internal .size() override"
      pattern: "Canvas\\(modifier = modifier\\)"
---

<objective>
Fix two visual bugs on the redesigned Auth screen (Phase 10 / SCR-06):

**Bug 1 — Wordmark unreadable.** `GiftMaisonWordmark.kt:56` applies `letterSpacing = (-0.4).em`. For 20 sp text that's −8 sp per character, mashing all 10 characters of "giftmaison." into an unreadable dark smear in the top-left of the Auth screen. This is the same em→sp unit bug fixed across `GiftMaisonTypography.kt` (10 roles) and `TypographyTest.kt` in quick-260421-lwi (commit 7743d35) — but the wordmark file was missed because it was created in Phase 8 Plan 02 and lives outside the type-scale file. Fix: `(-0.4).em` → `(-0.4).sp`. Also remove the now-unused `import androidx.compose.ui.unit.em` (no other em usage in this file — confirmed by grep).

**Bug 2 — Google banner height inflated by ConcentricRings.** `ConcentricRings.kt` hardcodes `Modifier.size(70.dp)` on its Canvas. In `GoogleBanner.kt`, `ConcentricRings(...)` is placed as a sibling of the Row inside the banner Box with `.align(Alignment.TopEnd)`. Because the Canvas claims an intrinsic 70 dp, the parent Box's measured height becomes max(Row content ≈ 40 dp, Rings 70 dp) + padding 14×2 = ~98 dp instead of the handoff-intended ~68 dp.

Fix decomposition (mirrors the layered-decoration pattern: decorative overlays must not contribute to layout):
1. **ConcentricRings.kt** — drop `.size(70.dp)`. Make it `Canvas(modifier = modifier)` so the caller controls sizing. Drawing math (corner-anchored `Offset(size.width, 0)`, radii 30/20/12 dp, alpha 0.08/0.12/0.18, 1 dp stroke) stays untouched. Update KDoc to note callers should pass `Modifier.matchParentSize()` for decorative-overlay use.
2. **GoogleBanner.kt** — wrap the `ConcentricRings(...)` call in a sibling `Box(modifier = Modifier.matchParentSize())` inside the outer Box. The matchParentSize wrapper takes its size from the parent (which is now driven by the Row's natural height + padding), and the rings fill that wrapper. The Canvas's `size.width` matches the banner's actual rendered width, so corner-anchored circles still draw at the top-right correctly. Color stays `colors.accentInk`. Row, padding, shadow, clip, background, and click modifiers are untouched.

After the fix, the rings still decorate the corner with the same visual footprint (~70 dp diameter circles drawn into the banner's bounds, naturally clipped at the rounded corner), but the banner's measured height is governed solely by its Row content + vertical padding.

Purpose: Restore legibility of the brand wordmark on Auth and bring the Google banner back to its handoff-intended ~68 dp height. Both are surface-level visual bugs from the Phase 10/Phase 8 redesign that the user can immediately see on screen.

Output: Three files modified — wordmark composable, decorative-rings composable, banner composable. No test changes (no existing test asserts on the affected dimensions). One commit.
</objective>

<execution_context>
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/workflows/execute-plan.md
</execution_context>

<context>
@app/src/main/java/com/giftregistry/ui/theme/GiftMaisonWordmark.kt
@app/src/main/java/com/giftregistry/ui/common/ConcentricRings.kt
@app/src/main/java/com/giftregistry/ui/auth/GoogleBanner.kt
@.planning/quick/260421-lwi-fix-typography-letter-spacing-units-em-t/260421-lwi-PLAN.md

<interfaces>
<!-- Current shape of the three files. The fix is surgical — only the marked lines change. -->

**GiftMaisonWordmark.kt (current — 58 lines):**
- Line 15: `import androidx.compose.ui.unit.em` ← REMOVE (only used by line 56)
- Line 16: `import androidx.compose.ui.unit.sp` ← KEEP (used by fontSize default param + the new letterSpacing)
- Lines 31–39: `wordmarkAnnotatedString(ink, accent)` factory — UNTOUCHED
- Lines 41–58: `@Composable fun GiftMaisonWordmark(...)` — only line 56 changes:
  - Line 56 BEFORE: `letterSpacing = (-0.4).em,`
  - Line 56 AFTER:  `letterSpacing = (-0.4).sp,`
- All other params (fontFamily, fontStyle, fontWeight, fontSize) UNTOUCHED.

**ConcentricRings.kt (current — 54 lines):**
- Line 4: `import androidx.compose.foundation.layout.size` ← REMOVE (no longer used)
- Lines 12–27: KDoc — append a sentence noting callers should pass `Modifier.matchParentSize()` for decorative-overlay use; existing handoff/spec language stays.
- Line 33 BEFORE: `Canvas(modifier = modifier.size(70.dp)) {`
- Line 33 AFTER:  `Canvas(modifier = modifier) {`
- Lines 34–52: drawCircle calls (offset, radii, alpha, stroke) UNTOUCHED.
- Line 10: `import androidx.compose.ui.unit.dp` ← KEEP (still used by `30.dp.toPx()`, `20.dp.toPx()`, `12.dp.toPx()`, `1.dp.toPx()`).

**GoogleBanner.kt (current — 113 lines):**
- Imports — no changes (Box, Alignment, Modifier, ConcentricRings already imported).
- Outer `Box` (lines 58–66): UNTOUCHED. Padding stays `vertical = spacing.gap14, horizontal = spacing.edge`.
- Inner `Row` (lines 67–105): UNTOUCHED. White G circle, label, arrow all stay.
- Lines 107–111 (current ConcentricRings call):
  ```kotlin
  // Top-end concentric-rings overlay (ornamental, clipped at radius16 corner)
  ConcentricRings(
      color = colors.accentInk,
      modifier = Modifier.align(Alignment.TopEnd),
  )
  ```
- Replace with sibling matchParentSize Box housing the rings:
  ```kotlin
  // Top-end concentric-rings overlay (ornamental, clipped at radius16 corner).
  // matchParentSize wrapper ensures the rings draw within the banner's actual bounds
  // (driven by the Row's natural height + padding) without the Canvas claiming an
  // intrinsic 70 dp that would inflate the banner from ~68 dp to ~98 dp.
  Box(modifier = Modifier.matchParentSize()) {
      ConcentricRings(
          color = colors.accentInk,
          modifier = Modifier.matchParentSize(),
      )
  }
  ```
- Why two matchParentSize calls: the outer Box stretches to the parent banner's bounds (does NOT contribute to parent's intrinsic height — matchParentSize is a child constraint, not a height claim). The inner ConcentricRings then fills the outer Box. The Canvas's `size.width` equals the banner's rendered width; circles drawn at `Offset(size.width, 0)` still appear at the top-right corner.
- Net layout effect: parent Box height = max(Row content ≈ 40 dp, matchParentSize-Box ≈ 0 intrinsic) + padding 14×2 = ~68 dp. The matchParentSize Box does not push the parent taller because matchParentSize is a *post-measurement* sizing directive (child sizes itself to match parent's already-decided size).
</interfaces>

<out_of_scope>
Per constraints — explicitly NOT touched:
- Any other Phase 8/9/10/11 composable.
- Any other typography role in `GiftMaisonTypography.kt` (already fixed in quick-260421-lwi).
- Accent / color values in either file (Color references, alpha values stay).
- Drawing math in `ConcentricRings.kt` (offset, radii 30/20/12 dp, alpha 0.08/0.12/0.18, 1 dp stroke).
- The Row, padding, shadow, clip, background, or click modifiers in `GoogleBanner.kt`.
- `fontSize`, `fontFamily`, `fontStyle`, `fontWeight`, or color logic in `GiftMaisonWordmark.kt`.
- Any other em→sp instances elsewhere in the codebase (would need a separate audit task).
- New tests — no banner-height visual test, no wordmark letter-spacing assertion. WordmarkTest.kt asserts only on the AnnotatedString factory (text + span styles + colors), not on letter-spacing — so it remains green without modification. Visual contract verified at the human checkpoint.
</out_of_scope>
</context>

<tasks>

<task type="auto">
  <name>Task 1: Fix wordmark em→sp and decouple ConcentricRings sizing from GoogleBanner height</name>
  <files>app/src/main/java/com/giftregistry/ui/theme/GiftMaisonWordmark.kt, app/src/main/java/com/giftregistry/ui/common/ConcentricRings.kt, app/src/main/java/com/giftregistry/ui/auth/GoogleBanner.kt</files>
  <action>
    Three coordinated edits — wordmark unit fix, rings sizing-decoupling, banner overlay-wrapping. Apply all three in one task so the build stays green at the commit boundary.

    **Edit 1 — `app/src/main/java/com/giftregistry/ui/theme/GiftMaisonWordmark.kt`**

    Mirror the quick-260421-lwi pattern (em→sp for letter-spacing only). Two changes:

    1. Line 56: `letterSpacing = (-0.4).em,` → `letterSpacing = (-0.4).sp,`
    2. Line 15: remove `import androidx.compose.ui.unit.em`. (Confirmed by grep: line 56 is the only `.em` usage in this file. Line 16 `import androidx.compose.ui.unit.sp` stays — used by `fontSize: TextUnit = 20.sp` default param and the new letterSpacing.)

    Do NOT change: `wordmarkAnnotatedString` factory (lines 31–39), Text composable's `fontFamily = InstrumentSerifFamily` / `fontStyle = FontStyle.Italic` / `fontWeight = FontWeight.Normal` / `fontSize = fontSize`. Do NOT touch the KDoc (lines 18–26) — its claims about Instrument Serif italic + accent period remain accurate.

    **Edit 2 — `app/src/main/java/com/giftregistry/ui/common/ConcentricRings.kt`**

    Make the Canvas caller-sized. Three changes:

    1. Line 4: remove `import androidx.compose.foundation.layout.size` (no longer used after the next change).
    2. Line 33: `Canvas(modifier = modifier.size(70.dp)) {` → `Canvas(modifier = modifier) {`
    3. Append to the existing KDoc (after the existing closing paragraph at line 27, before the `@Composable` annotation at line 28) a one-line note about caller-controlled sizing:

       ```
        *
        * The Canvas inherits its size from `modifier`. For decorative-overlay use
        * (e.g. inside a parent Box), pass `Modifier.matchParentSize()` so the rings
        * fill the parent's bounds without contributing to its measured height.
       ```

    Do NOT change: drawCircle calls (offsets, radii 30/20/12 dp, alpha values 0.08/0.12/0.18, 1 dp stroke), the Color parameter, the function signature (the `color: Color, modifier: Modifier = Modifier` param order stays). `import androidx.compose.ui.unit.dp` stays — `dp` is still used by `30.dp.toPx()`, `20.dp.toPx()`, `12.dp.toPx()`, `1.dp.toPx()` inside the drawCircle calls.

    **Edit 3 — `app/src/main/java/com/giftregistry/ui/auth/GoogleBanner.kt`**

    Wrap the existing `ConcentricRings(...)` call (lines 107–111) in a sibling `Box(modifier = Modifier.matchParentSize())`. Replace lines 107–111:

    ```kotlin
            // Top-end concentric-rings overlay (ornamental, clipped at radius16 corner)
            ConcentricRings(
                color = colors.accentInk,
                modifier = Modifier.align(Alignment.TopEnd),
            )
    ```

    With:

    ```kotlin
            // Top-end concentric-rings overlay (ornamental, clipped at radius16 corner).
            // matchParentSize wrapper draws within the banner's natural bounds
            // (Row + padding ≈ 68 dp) without ConcentricRings' Canvas inflating
            // the banner's measured height.
            Box(modifier = Modifier.matchParentSize()) {
                ConcentricRings(
                    color = colors.accentInk,
                    modifier = Modifier.matchParentSize(),
                )
            }
    ```

    Why this works: `Modifier.matchParentSize()` is a BoxScope modifier that sizes a child to match the parent's *already-determined* size — it does NOT contribute to the parent's intrinsic measurement. The outer Box (parent) measures itself based solely on Row content (≈ 40 dp) + vertical padding (14×2 = 28 dp) = ~68 dp. The matchParentSize Box then takes that ~68 dp footprint, and ConcentricRings (also matchParentSize) fills the same. The Canvas's `size.width` and `size.height` equal the banner's rendered dimensions; circles drawn at `Offset(size.width, 0)` (top-right corner) appear correctly anchored. The rounded `clip(shapes.radius16)` on the outer Box continues to clip ring overflow at the corner.

    Imports — `androidx.compose.foundation.layout.Box`, `androidx.compose.ui.Alignment`, and `androidx.compose.ui.Modifier` are all already imported (lines 6, 15, 16). The previous `Alignment.TopEnd` reference is gone, but `Alignment.Center` is still used on the white G circle (line 77), so the `Alignment` import stays. No import changes needed.

    Do NOT change: any other Box/Row/Spacer/Text in the file; the outer Box's modifier chain (fillMaxWidth, googleBannerShadow, clip, background, clickable, padding); the Row's content; or the `colors.accentInk` argument value.

    **Post-edit verification:**

    1. Production code compiles end-to-end:
       ```bash
       ./gradlew :app:compileDebugKotlin
       ```
    2. Existing unit tests still pass — no assertions touch the modified surfaces, so they should be green unchanged:
       ```bash
       ./gradlew :app:testDebugUnitTest --tests 'com.giftregistry.ui.theme.WordmarkTest' --tests 'com.giftregistry.ui.theme.TypographyTest'
       ```
       Expected: WordmarkTest's 5 AnnotatedString assertions stay green (factory unchanged), TypographyTest's 11 tests stay green (file untouched).
    3. Lint sanity check — no unused imports left behind:
       ```bash
       grep -n "import androidx.compose.ui.unit.em" app/src/main/java/com/giftregistry/ui/theme/GiftMaisonWordmark.kt
       grep -n "import androidx.compose.foundation.layout.size" app/src/main/java/com/giftregistry/ui/common/ConcentricRings.kt
       ```
       Both should return nothing (imports successfully removed).
  </action>
  <verify>
    <automated>./gradlew :app:compileDebugKotlin && ./gradlew :app:testDebugUnitTest --tests 'com.giftregistry.ui.theme.WordmarkTest' --tests 'com.giftregistry.ui.theme.TypographyTest'</automated>
  </verify>
  <done>
    - `grep -c "letterSpacing = (-0.4).em" app/src/main/java/com/giftregistry/ui/theme/GiftMaisonWordmark.kt` returns 0.
    - `grep -c "letterSpacing = (-0.4).sp" app/src/main/java/com/giftregistry/ui/theme/GiftMaisonWordmark.kt` returns 1.
    - `grep -c "import androidx.compose.ui.unit.em" app/src/main/java/com/giftregistry/ui/theme/GiftMaisonWordmark.kt` returns 0.
    - `grep -c "modifier.size(70.dp)" app/src/main/java/com/giftregistry/ui/common/ConcentricRings.kt` returns 0.
    - `grep -c "Canvas(modifier = modifier)" app/src/main/java/com/giftregistry/ui/common/ConcentricRings.kt` returns 1.
    - `grep -c "import androidx.compose.foundation.layout.size" app/src/main/java/com/giftregistry/ui/common/ConcentricRings.kt` returns 0.
    - `grep -c "Modifier.matchParentSize()" app/src/main/java/com/giftregistry/ui/auth/GoogleBanner.kt` returns 2 (outer wrapper + inner ConcentricRings modifier).
    - `grep -c "Modifier.align(Alignment.TopEnd)" app/src/main/java/com/giftregistry/ui/auth/GoogleBanner.kt` returns 0 (replaced by matchParentSize pattern).
    - `./gradlew :app:compileDebugKotlin` exits 0.
    - `./gradlew :app:testDebugUnitTest --tests 'com.giftregistry.ui.theme.WordmarkTest' --tests 'com.giftregistry.ui.theme.TypographyTest'` exits 0; WordmarkTest's 5 tests + TypographyTest's 11 tests all green.
  </done>
</task>

<task type="checkpoint:human-verify" gate="blocking">
  <name>Task 2: On-device visual check — wordmark legible + Google banner ~68 dp tall</name>
  <what-built>
    Three surgical fixes applied:
    1. `GiftMaisonWordmark.kt` letterSpacing flipped from `.em` to `.sp` — the wordmark "giftmaison." should now render with gentle −0.4 sp tracking instead of the −8 sp crush that produced the dark smear.
    2. `ConcentricRings.kt` no longer hardcodes a 70 dp Canvas size — caller controls sizing via the modifier chain.
    3. `GoogleBanner.kt` wraps the rings in a `Modifier.matchParentSize()` Box, so the rings overlay the banner without claiming an intrinsic 70 dp footprint that was inflating the banner from ~68 dp to ~98 dp.
  </what-built>
  <how-to-verify>
    1. Rebuild and install on the same device/emulator where the bug was visible:
       ```bash
       ./gradlew :app:installDebug
       ```
    2. Launch the app. If signed in, sign out to reach the Auth screen (SCR-06 in the Phase 10 handoff).
    3. **Wordmark check (top-left of Auth screen):** Look for the brand mark "giftmaison." in italic Instrument Serif at the top of the screen. The 10 letters + period should be clearly distinguishable — letters spaced naturally with the gentle −0.4 sp tightening (subtly tracked-in but readable), accent-coloured period at the end. NOT a solid dark blob. If it still looks crushed, capture a screenshot — likely the install didn't take or there is another `letterSpacing = ...em` override elsewhere (grep confirmed there isn't one, but worth verifying the build cache is fresh).
    4. **Google banner height check:** Locate the "Continue with Google" banner (rounded-16 accent rectangle with white G circle, label, and italic arrow). Measure visually:
       - Expected: ~68 dp tall — Row content (≈ 40 dp from the 20 dp G circle / single-line text) + 14 dp top + 14 dp bottom padding.
       - Compare against the handoff reference at `design_handoff/design_handoff_android_owner_flow/reference/GiftMaison - gift registry mocks.html` (06 · Onboarding + sign up artboard) opened in a browser. The banner should now match the handoff's compact proportion, not feel chunky/oversized.
       - The three concentric rings should still appear in the top-right corner of the banner, anchored to the rounded edge and clipped where they spill past the radius. Same visual decoration, same accent-ink subtle alphas.
    5. **Layout sanity:** The Row contents (G circle + "Continue with Google" label + arrow) should be vertically centred in the banner. The label should not feel pushed up or stretched.
    6. Capture before/after screenshots if you'd like a record — the difference should be obvious without measuring tools.

    If the banner is still ~98 dp, suspect: (a) install cache stale, (b) a parent Surface/Card adding extra padding (none expected — the AuthScreen passes the banner straight into a Column), or (c) the matchParentSize wrapper not behaving as expected (very unlikely for stable Compose).

    If the wordmark is still cramped, suspect: (a) install cache stale, or (b) some screen-level wrapper with a hardcoded letterSpacing override on Display XL (grep across `app/` should find nothing — verified during planning).
  </how-to-verify>
  <resume-signal>Type "approved" if wordmark is legible and Google banner is ~68 dp on the Auth screen. Otherwise describe what looks off, ideally with a screenshot.</resume-signal>
</task>

</tasks>

<verification>
- `./gradlew :app:compileDebugKotlin` — main source compiles after all three edits.
- `./gradlew :app:testDebugUnitTest --tests 'com.giftregistry.ui.theme.WordmarkTest' --tests 'com.giftregistry.ui.theme.TypographyTest'` — existing tests stay green (no assertions on modified surfaces, so no test edits needed).
- Visual on-device check per Task 2 — wordmark legible, banner ~68 dp tall, rings still decorate the corner.
</verification>

<success_criteria>
- `GiftMaisonWordmark.kt` line 56 expresses letter-spacing in `.sp`, not `.em`. The unused `em` import on line 15 is removed.
- `ConcentricRings.kt` Canvas uses `modifier` directly — no internal `.size(70.dp)`. The unused `layout.size` import is removed. KDoc updated to note caller-controlled sizing.
- `GoogleBanner.kt` places ConcentricRings inside a `Modifier.matchParentSize()` Box, replacing the previous `.align(Alignment.TopEnd)` placement.
- Out-of-scope invariants upheld: no other typography roles touched, no fontSize/fontFamily/colour changes, no Row/padding/shadow modifications, no drawing math changes in ConcentricRings, no test files modified.
- User visually confirms (a) the wordmark renders as readable text on the Auth screen, and (b) the Google banner is back to its handoff-intended ~68 dp height with rings still overlaying the corner.
</success_criteria>

<output>
After completion, create `.planning/quick/260427-gld-fix-wordmark-letterspacing-em-to-sp-and-/260427-gld-SUMMARY.md` per the execute-plan template.
</output>
