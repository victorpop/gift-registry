---
phase: quick-260421-lwi
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - app/src/main/java/com/giftregistry/ui/theme/GiftMaisonTypography.kt
  - app/src/test/java/com/giftregistry/ui/theme/TypographyTest.kt
autonomous: true
requirements: [DES-02-FIX]
must_haves:
  truths:
    - "Every one of the 10 TextStyle roles in GiftMaisonTypography has letterSpacing expressed in .sp (not .em)"
    - "TypographyTest asserts letter-spacing in .sp and all assertions pass"
    - "Text on Auth screen (and every other screen reading MaterialTheme.typography or GiftMaisonTheme.typography) renders with handoff-intended pixel letter-spacing, no longer cramped"
    - "No lineHeight values are touched — all 10 roles keep their .em multiplier semantics"
    - "No fontSize, fontWeight, fontFamily, platformStyle, or lineHeightStyle values are touched"
  artifacts:
    - path: "app/src/main/java/com/giftregistry/ui/theme/GiftMaisonTypography.kt"
      provides: "10 TextStyle roles with pixel-equivalent letter-spacing matching the JSX source"
      contains: "letterSpacing = (-0.8).sp"
    - path: "app/src/test/java/com/giftregistry/ui/theme/TypographyTest.kt"
      provides: "Updated assertions that pin letter-spacing units to .sp"
      contains: "assertEquals((-0.8).sp, t.displayXL.letterSpacing)"
  key_links:
    - from: "GiftMaisonTypography.kt"
      to: "design_handoff/.../GiftMaison - gift registry mocks.html JSX letterSpacing values"
      via: "Pixel-equivalent mapping (JSX naked numbers → Compose .sp)"
      pattern: "letterSpacing = \\(?-?[0-9.]+\\)?\\.sp"
    - from: "TypographyTest.kt"
      to: "GiftMaisonTypography.kt"
      via: "Test fixture asserts new .sp values across 8 roles"
      pattern: "\\.sp, t\\.(displayXL|displayL|displayM|displayS|bodyL|bodyM|bodyS|bodyXS)\\.letterSpacing"
---

<objective>
Fix the typography letter-spacing unit mismatch that is cramping text across every screen in the app (visible on the Auth screen per user screenshot).

**Root cause:** Phase 8 Plan 03 (DES-02) implemented letter-spacing in `.em` based on 08-RESEARCH.md Pitfall 3, which incorrectly asserted the handoff "CSS convention" was em-based. The handoff's JSX source (`reference/GiftMaison - gift registry mocks.html`) actually uses **naked numeric letter-spacing values**, which in React inline styles default to **pixels** — not em. Examples:

- Line 457: `letterSpacing: -0.2` on the `giftmaison.` wordmark (32 px font) — intended as −0.2 px, rendered by our code as `-0.2 × 32 ≈ -6.4 px` (≈ 32× too tight).
- Line 462: `letterSpacing: 1.5` on the "GIFT REGISTRY" mono tag (10 px font) — intended as 1.5 px, rendered by our code as `1.5 × 10 = 15 px` (10× too wide, which Compose actually ends up collapsing into mashed-together text on some roles because em lineHeight + em letter-spacing interact oddly).
- Line 594: `letterSpacing: 1.3` on the "✓ Given by…" pill (11 px font) — intended as 1.3 px, rendered by our code as `1.3 × 11 ≈ 14.3 px`.

All 10 type roles are affected. The fix is a pure unit swap: `.em` → `.sp` on the `letterSpacing` property only. `lineHeight` values stay in `.em` (they are correct — `1.35.em` on a 13.5 sp font = 1.35× multiplier, matching the JSX which uses CSS-standard unitless line-height).

Purpose: Restore legibility across every screen that reads `GiftMaisonTheme.typography.*` or `MaterialTheme.typography.*` (Plan 05 mapped the GM roles onto M3 slots).

Output: Two files modified — production typography + its unit test. Tests remain GREEN.
</objective>

<execution_context>
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/workflows/execute-plan.md
</execution_context>

<context>
@app/src/main/java/com/giftregistry/ui/theme/GiftMaisonTypography.kt
@app/src/test/java/com/giftregistry/ui/theme/TypographyTest.kt
@design_handoff/design_handoff_android_owner_flow/reference/GiftMaison - gift registry mocks.html

<interfaces>
<!-- Contract the fix must preserve: TypographyTest.kt currently asserts letter-spacing in .em.
     We are flipping BOTH the production values AND the test assertions in lockstep. -->

Current 10-role type scale (GiftMaisonTypography.kt):

| Role          | fontSize  | letterSpacing (BEFORE) | letterSpacing (AFTER) | lineHeight (UNCHANGED) |
|---------------|-----------|------------------------|-----------------------|------------------------|
| displayXL     | 32.sp     | (-0.8).em              | (-0.8).sp             | 1.0.em                 |
| displayL      | 24.sp     | (-0.4).em              | (-0.4).sp             | 1.05.em                |
| displayM      | 22.sp     | (-0.4).em              | (-0.4).sp             | 1.1.em                 |
| displayS      | 18.sp     | (-0.3).em              | (-0.3).sp             | 1.1.em                 |
| bodyL         | 15.sp     | (-0.2).em              | (-0.2).sp             | 1.35.em                |
| bodyM         | 13.5.sp   | (-0.1).em              | (-0.1).sp             | 1.45.em                |
| bodyMEmphasis | 13.5.sp   | (-0.1).em              | (-0.1).sp             | 1.45.em                |
| bodyS         | 12.5.sp   | 0.em                   | 0.sp                  | 1.4.em                 |
| bodyXS        | 11.5.sp   | 0.em                   | 0.sp                  | 1.35.em                |
| monoCaps      | 9.5.sp    | 1.5.em                 | 1.5.sp                | 1.3.em                 |

TypographyTest.kt asserts letterSpacing for 8 roles (monoCaps + bodyMEmphasis don't have letterSpacing assertions — monoCaps was intentionally deferred per Plan 03 because the "0.6–1.5 em" range is variable across call sites, and bodyMEmphasis only asserts weight+size). Eight assertions need their unit flipped.

Both files must be updated in the SAME commit so the test suite stays GREEN before and after.
</interfaces>

<out_of_scope>
The following are explicitly NOT touched by this fix (per constraints):

- `lineHeight` values (all stay as `.em` — correct CSS multiplier semantics; JSX uses unitless line-height which DOES mean multiplier in CSS, unlike letter-spacing).
- `fontSize`, `fontWeight`, `fontFamily`, `platformStyle`, `lineHeightStyle` — no changes.
- The 10 TextStyle role names, order, or structure.
- `GiftMaisonWordmark.kt` line 56 uses `letterSpacing = (-0.4).em` for the wordmark text — this is a separate file with its own letter-spacing choice and is out of scope for this quick fix. (If the wordmark also looks cramped, file a follow-up quick task.)
- `ConfirmPurchaseBanner`, `RegistryDetailScreen`, any screen-level `letterSpacing = ...` overrides — none exist in the codebase (`grep letterSpacing app/` shows only GiftMaisonTypography.kt, TypographyTest.kt, and GiftMaisonWordmark.kt).
- 08-RESEARCH.md Pitfall 3 itself — the research doc's claim "always use .em for letter-spacing" was wrong, but amending historical research is not required for the runtime fix. (Optional follow-up: append a correction note so future planners don't repeat the mistake.)
</out_of_scope>
</context>

<tasks>

<task type="auto">
  <name>Task 1: Flip letterSpacing units from .em to .sp across GiftMaisonTypography.kt + TypographyTest.kt</name>
  <files>app/src/main/java/com/giftregistry/ui/theme/GiftMaisonTypography.kt, app/src/test/java/com/giftregistry/ui/theme/TypographyTest.kt</files>
  <action>
    Two edits in lockstep — production + test — to keep the build green at every commit boundary.

    **Edit 1 — `app/src/main/java/com/giftregistry/ui/theme/GiftMaisonTypography.kt`**

    Replace the `letterSpacing` unit for ALL 10 TextStyle roles. Do NOT touch `lineHeight`, `fontSize`, `fontWeight`, `fontFamily`, `platformStyle`, or `lineHeightStyle`. Do NOT rename or reorder the roles. Do NOT remove the `androidx.compose.ui.unit.em` import — it's still used by the 10 `lineHeight` values.

    Line-by-line edits (reference current file content — exact string replacements):

    | Line | Before | After |
    |------|--------|-------|
    | 52   | `letterSpacing = (-0.8).em,` | `letterSpacing = (-0.8).sp,` |
    | 61   | `letterSpacing = (-0.4).em,` | `letterSpacing = (-0.4).sp,` |
    | 70   | `letterSpacing = (-0.4).em,` | `letterSpacing = (-0.4).sp,` |
    | 79   | `letterSpacing = (-0.3).em,` | `letterSpacing = (-0.3).sp,` |
    | 88   | `letterSpacing = (-0.2).em,` | `letterSpacing = (-0.2).sp,` |
    | 97   | `letterSpacing = (-0.1).em,` | `letterSpacing = (-0.1).sp,` |
    | 106  | `letterSpacing = (-0.1).em,` | `letterSpacing = (-0.1).sp,` |
    | 115  | `letterSpacing = 0.em,`       | `letterSpacing = 0.sp,`       |
    | 124  | `letterSpacing = 0.em,`       | `letterSpacing = 0.sp,`       |
    | 133  | `letterSpacing = 1.5.em,`     | `letterSpacing = 1.5.sp,`     |

    Also update the class-level KDoc (lines 12–26) so future readers aren't misled. The current KDoc says:

    ```
    * Source-of-truth: `design_handoff/design_handoff_android_owner_flow/README.md`
    * "Type scale" table. Letter-spacing and line-height values from the handoff are
    * expressed as em multipliers (CSS convention) — see 08-RESEARCH.md Pattern 5 and
    * Pitfall 3.
    ```

    Replace the "Letter-spacing and line-height…" sentence with:

    ```
    * Line-height values from the handoff are CSS unitless multipliers, expressed here
    * as `.em` (per 08-RESEARCH.md Pattern 5). Letter-spacing values in the JSX source
    * (`reference/GiftMaison - gift registry mocks.html`) are naked numerics, which
    * React inline styles treat as **pixels** — NOT em. Mapped here as `.sp` so a "−0.2"
    * handoff value renders as −0.2 scaled-pixels, matching the web prototype.
    * (Corrects the em interpretation in 08-RESEARCH.md Pitfall 3.)
    ```

    **Edit 2 — `app/src/test/java/com/giftregistry/ui/theme/TypographyTest.kt`**

    Flip the 8 letter-spacing assertions from `.em` to `.sp`. The 8 target lines (file content shown above):

    | Line | Before | After |
    |------|--------|-------|
    | 22   | `assertEquals((-0.8).em, t.displayXL.letterSpacing)` | `assertEquals((-0.8).sp, t.displayXL.letterSpacing)` |
    | 29   | `assertEquals((-0.4).em, t.displayL.letterSpacing)`  | `assertEquals((-0.4).sp, t.displayL.letterSpacing)`  |
    | 35   | `assertEquals((-0.4).em, t.displayM.letterSpacing)`  | `assertEquals((-0.4).sp, t.displayM.letterSpacing)`  |
    | 41   | `assertEquals((-0.3).em, t.displayS.letterSpacing)`  | `assertEquals((-0.3).sp, t.displayS.letterSpacing)`  |
    | 48   | `assertEquals((-0.2).em, t.bodyL.letterSpacing)`     | `assertEquals((-0.2).sp, t.bodyL.letterSpacing)`     |
    | 56   | `assertEquals((-0.1).em, t.bodyM.letterSpacing)`     | `assertEquals((-0.1).sp, t.bodyM.letterSpacing)`     |
    | 67   | `assertEquals(0.em, t.bodyS.letterSpacing)`          | `assertEquals(0.sp, t.bodyS.letterSpacing)`          |
    | 73   | `assertEquals(0.em, t.bodyXS.letterSpacing)`         | `assertEquals(0.sp, t.bodyXS.letterSpacing)`         |

    The `import androidx.compose.ui.unit.em` import at line 6 stays (still used by all 8 `lineHeight` assertions: `1.0.em`, `1.05.em`, `1.1.em`, `1.35.em`, `1.45.em`, `1.4.em`, `1.35.em`, `1.3.em`). The `import androidx.compose.ui.unit.sp` import at line 7 also stays (already used by fontSize assertions). No import changes needed.

    The `everyRole_disablesFontPadding_pitfall2` test (lines 84–94) is unaffected — it asserts on `platformStyle`, not letter-spacing.

    **Post-edit verification:** After both files are saved, run the test suite. It should be green end-to-end (the only changes are symmetric across production + test, and no other consumer reads these TextStyle values at test time).

    ```bash
    ./gradlew :app:testDebugUnitTest --tests 'com.giftregistry.ui.theme.TypographyTest'
    # Expect: all 11 tests green, including the pitfall2 guard.
    ```

    Also confirm compileDebugKotlin still succeeds — no other file in `app/src/main/` references these letter-spacing values at compile time:

    ```bash
    ./gradlew :app:compileDebugKotlin
    ```
  </action>
  <verify>
    <automated>./gradlew :app:testDebugUnitTest --tests 'com.giftregistry.ui.theme.TypographyTest' && ./gradlew :app:compileDebugKotlin</automated>
  </verify>
  <done>
    - `grep -c "letterSpacing = .*\.em" app/src/main/java/com/giftregistry/ui/theme/GiftMaisonTypography.kt` returns 0 (zero `.em` letter-spacings remain in production).
    - `grep -c "letterSpacing = .*\.sp" app/src/main/java/com/giftregistry/ui/theme/GiftMaisonTypography.kt` returns 10 (all 10 roles now in `.sp`).
    - `grep -c "lineHeight = .*\.em" app/src/main/java/com/giftregistry/ui/theme/GiftMaisonTypography.kt` returns 10 (lineHeight unchanged — still all `.em`).
    - `grep -c "\.em, t\..*\.letterSpacing" app/src/test/java/com/giftregistry/ui/theme/TypographyTest.kt` returns 0.
    - `grep -c "\.sp, t\..*\.letterSpacing" app/src/test/java/com/giftregistry/ui/theme/TypographyTest.kt` returns 8.
    - `./gradlew :app:testDebugUnitTest --tests 'com.giftregistry.ui.theme.TypographyTest'` exits 0 with all 11 tests green.
    - `./gradlew :app:compileDebugKotlin` exits 0.
  </done>
</task>

<task type="checkpoint:human-verify" gate="blocking">
  <name>Task 2: On-device visual check — Auth screen no longer shows cramped text</name>
  <what-built>
    GiftMaisonTypography.kt now applies letter-spacing in `.sp` (pixel-equivalent) across all 10 TextStyle roles, matching the JSX source's naked-numeric values (which default to pixels in React inline styles). Every screen that reads `MaterialTheme.typography.*` or `GiftMaisonTheme.typography.*` should now render with handoff-intended letter-spacing instead of em-multiplied tracking.
  </what-built>
  <how-to-verify>
    1. Rebuild and install the app on the same device/emulator where the cramped-text screenshot was captured:
       ```bash
       ./gradlew :app:installDebug
       ```
    2. Launch the app. If signed in, sign out to reach the Auth screen (screen 06 in handoff).
    3. Compare on-screen text against the handoff reference for the Auth screen:
       - **Headline "Start your first registry."** (Display XL, 32 sp, Instrument Serif) — letters should sit comfortably apart; the "−0.8 sp" tracking is a gentle tightening, not the visible squeeze observed before.
       - **Subline "A warm, shareable list — set up in under a minute."** (Body L, 15 sp, Inter W500) — words should feel airy and readable, not mashed.
       - **"Continue with Google"** button label (body M emphasis / Body L) — letters clearly separated.
       - **"or sign up with email"** divider caption — mono caps with 1.5 sp tracking should look spaced, not compressed.
       - **Form field labels / placeholders** — body text should breathe the way the handoff HTML breathes when opened in a browser.
    4. Open `design_handoff/design_handoff_android_owner_flow/reference/GiftMaison - gift registry mocks.html` in a browser and compare the Android "06 · Onboarding + sign up" artboard side-by-side. Letter-spacing should now visually match (or be extremely close — minor rendering differences between Skia and browser text layout are expected).
    5. Sanity-check a second screen — Home (screen 07) and/or a Registry Detail (08) — to confirm the fix applies app-wide, not just on Auth.

    If any text still looks cramped or too spread out, describe which role/screen and capture a screenshot for diagnosis. Likely causes: a hardcoded `letterSpacing = ...` at a screen-level call site (grep confirmed none exist, but a rogue one is the only way the fix could be incomplete) OR a follow-up needed for `GiftMaisonWordmark.kt`.
  </how-to-verify>
  <resume-signal>Type "approved" if text renders correctly across Auth + one other screen, or describe remaining issues.</resume-signal>
</task>

</tasks>

<verification>
- `./gradlew :app:testDebugUnitTest --tests 'com.giftregistry.ui.theme.TypographyTest'` — all tests green.
- `./gradlew :app:compileDebugKotlin` — main source compiles.
- Visual on-device check per Task 2 — text on Auth screen (and at least one other screen) renders with handoff-faithful letter-spacing, no longer cramped.
</verification>

<success_criteria>
- All 10 letter-spacing values in GiftMaisonTypography.kt expressed in `.sp` (pixel-equivalent), not `.em`.
- All 8 letter-spacing assertions in TypographyTest.kt use `.sp` and pass.
- No lineHeight values touched — 10 `.em` lineHeight values remain unchanged in both files.
- User visually confirms Auth screen text is no longer cramped and matches the web prototype.
- Out-of-scope invariants upheld: no changes to fontSize, fontWeight, fontFamily, platformStyle, lineHeightStyle, role names, or role order; GiftMaisonWordmark.kt untouched.
</success_criteria>

<output>
After completion, create `.planning/quick/260421-lwi-fix-typography-letter-spacing-units-em-t/260421-lwi-SUMMARY.md` per the execute-plan template.
</output>
