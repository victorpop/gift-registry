---
phase: quick-260427-gxu
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - app/src/main/res/font/instrument_serif_regular.ttf
  - app/src/main/res/font/instrument_serif_italic.ttf
  - app/src/main/res/font/OFL.txt
  - app/src/main/java/com/giftregistry/ui/theme/GiftMaisonFonts.kt
  - app/src/main/java/com/giftregistry/ui/auth/AuthHeadline.kt
  - app/src/test/java/com/giftregistry/ui/auth/AuthHeadlineTest.kt
autonomous: true
requirements:
  - QUICK-GXU-01  # Bundle Instrument Serif as offline-capable fallback
  - QUICK-GXU-02  # Fix Auth headline colour split (line 2 fully accent)

must_haves:
  truths:
    - "Auth screen line 1 'Start your' renders in solid ink (black)"
    - "Auth screen line 2 'first registry.' renders entirely in accent colour, period included"
    - "InstrumentSerifFamily renders correctly on devices/emulators without Google Play Services"
    - "Existing AuthHeadlineTest suite passes after the colour-split fix"
  artifacts:
    - path: "app/src/main/res/font/instrument_serif_regular.ttf"
      provides: "Bundled offline-capable Instrument Serif Regular fallback"
      min_size_kb: 10
    - path: "app/src/main/res/font/instrument_serif_italic.ttf"
      provides: "Bundled offline-capable Instrument Serif Italic fallback"
      min_size_kb: 10
    - path: "app/src/main/res/font/OFL.txt"
      provides: "SIL OFL-1.1 licence text required by font redistribution terms"
    - path: "app/src/main/java/com/giftregistry/ui/theme/GiftMaisonFonts.kt"
      provides: "InstrumentSerifFamily with GMS-first + bundled-fallback Font entries"
      contains: "R.font.instrument_serif_regular"
    - path: "app/src/main/java/com/giftregistry/ui/auth/AuthHeadline.kt"
      provides: "authHeadlineAnnotatedString factory with 2-span ink+accent structure"
    - path: "app/src/test/java/com/giftregistry/ui/auth/AuthHeadlineTest.kt"
      provides: "Updated test asserting 'first registry.' is fully accent-coloured"
  key_links:
    - from: "GiftMaisonFonts.kt InstrumentSerifFamily"
      to: "R.font.instrument_serif_regular / R.font.instrument_serif_italic"
      via: "androidx.compose.ui.text.font.Font(resId = ...) entries appended after googlefonts.Font entries"
      pattern: "Font\\(resId = R\\.font\\.instrument_serif_(regular|italic)"
    - from: "AuthHeadline.kt authHeadlineAnnotatedString"
      to: "GiftMaisonTheme.colors.accent"
      via: "single SpanStyle wrapping 'first registry.' (period included)"
      pattern: "withStyle\\(SpanStyle\\(color = accentColor\\)\\)"
    - from: "AuthHeadlineTest.kt"
      to: "authHeadlineAnnotatedString"
      via: "assertion that span at index of 'first registry.' has color == accent"
---

<objective>
Two-part Auth screen fix:

1. **Bundle Instrument Serif TTFs as local font fallback** so the brand-critical display serif renders correctly even on devices/emulators where the Google Play Services Fonts API is unavailable or hasn't completed its async download yet.

2. **Fix the Auth headline colour split** — line 2 "first registry." must be entirely accent-coloured (currently incorrectly split: "first registry" in ink + "." in accent, with line 1 in inkSoft instead of solid ink).

Purpose: Restore brand-correct rendering of the SCR-06 Auth screen headline on first launch, on non-GMS devices, and per the design handoff colour spec.

Output:
- 2 bundled `.ttf` files + OFL licence in `app/src/main/res/font/`
- Updated `InstrumentSerifFamily` declaration with GMS-first + bundled-fallback entries
- 2-span `authHeadlineAnnotatedString` (ink for line 1, accent for line 2 including period)
- Updated `AuthHeadlineTest` reflecting the corrected colour contract
</objective>

<execution_context>
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/workflows/execute-plan.md
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/STATE.md
@.planning/quick/260427-gxu-fix-auth-headline-colors-line-2-should-b/260427-gxu-RESEARCH.md
@app/src/main/java/com/giftregistry/ui/theme/GiftMaisonFonts.kt
@app/src/main/java/com/giftregistry/ui/auth/AuthHeadline.kt
@app/src/test/java/com/giftregistry/ui/auth/AuthHeadlineTest.kt

<interfaces>
<!-- Current code surfaces the executor must edit. Do not explore — use these directly. -->

From `GiftMaisonFonts.kt` (will be modified):
```kotlin
val InstrumentSerifFamily: FontFamily = FontFamily(
    Font(googleFont = instrumentSerif, fontProvider = giftMaisonFontProvider, weight = FontWeight.Normal),
    Font(googleFont = instrumentSerif, fontProvider = giftMaisonFontProvider, weight = FontWeight.Normal, style = FontStyle.Italic),
)
```
The existing `Font` import is `androidx.compose.ui.text.googlefonts.Font`. The new fallback entries require the resource-Font overload `androidx.compose.ui.text.font.Font` — both imports coexist (different overloads of `Font(...)`) and Compose resolves by argument type.

From `AuthHeadline.kt` (will be modified):
```kotlin
fun authHeadlineAnnotatedString(
    prefix: String,
    accent: String,
    ink: Color,
    accentColor: Color,
    inkSoft: Color,
): AnnotatedString
```
Call site inside `@Composable AuthHeadline()` passes `colors.ink`, `colors.accent`, `colors.inkSoft` and `remember(...)` keys include all three colours.

From `AuthHeadlineTest.kt` (will be modified):
- `firstRegistryWords_areInkColored` asserts ink colour on "first registry" — WRONG, must flip to assert accent on "first registry."
- `startYourLine_isInkSoftColored` asserts inkSoft on "Start your" — WRONG per user constraint (line 1 must be solid `ink`, not `inkSoft`)
- `period_isAccentColored` remains valid (accent on the trailing period)
- `text_containsBothLines`, `text_hasNewlineBetweenLines`, `periodIsLastCharacter` all remain valid
</interfaces>

<constraint_overrides>
<!-- Per user constraints in the planning prompt: line 1 = solid `ink` (NOT `inkSoft` as RESEARCH.md suggests). -->
<!-- Image #7 in the design handoff shows solid black on line 1, not soft gray. -->
<!-- This overrides the RESEARCH.md "inkSoft for prefix" example — the user has the final say. -->

Final colour contract:
- Line 1 "Start your\n"   → `ink` (solid black)
- Line 2 "first registry." → `accent` (orange, period included)
- `inkSoft` parameter no longer used; keep parameter on factory for API stability OR drop it — executor's choice (constraints say either is acceptable).
</constraint_overrides>
</context>

<tasks>

<task type="auto" tdd="false">
  <name>Task 1: Bundle Instrument Serif TTFs + wire fallback into InstrumentSerifFamily</name>
  <files>
    app/src/main/res/font/instrument_serif_regular.ttf,
    app/src/main/res/font/instrument_serif_italic.ttf,
    app/src/main/res/font/OFL.txt,
    app/src/main/java/com/giftregistry/ui/theme/GiftMaisonFonts.kt
  </files>
  <action>
Implements requirement QUICK-GXU-01.

**Step 1 — Create the font directory and download TTFs:**

```bash
mkdir -p app/src/main/res/font

# Regular (verified URL from RESEARCH.md, live curl 2026-04-27)
curl -L -o app/src/main/res/font/instrument_serif_regular.ttf \
  "https://fonts.gstatic.com/s/instrumentserif/v5/jizBRFtNs2ka5fXjeivQ4LroWlx-2zI.ttf"

# Italic (verified URL from RESEARCH.md, live curl 2026-04-27)
curl -L -o app/src/main/res/font/instrument_serif_italic.ttf \
  "https://fonts.gstatic.com/s/instrumentserif/v5/jizHRFtNs2ka5fXjeivQ4LroWlx-6zATiw.ttf"

# OFL licence (required by SIL OFL-1.1 redistribution terms)
curl -L -o app/src/main/res/font/OFL.txt \
  "https://raw.githubusercontent.com/google/fonts/main/ofl/instrumentserif/OFL.txt"
```

**Verify each download is non-empty (>10 KB) before continuing:**

```bash
ls -la app/src/main/res/font/
# Each .ttf must be > 10000 bytes. If a curl produced a 0-byte or HTML error file, abort and retry.
```

If any file is < 10 KB or contains HTML, the download failed (likely a redirect not followed) — re-run with explicit `-L` and `-f` flags and retry. Do not proceed until all three files are verified.

**Step 2 — Edit `GiftMaisonFonts.kt`:**

Add the Compose resource-Font import at the top of the file (alongside the existing `googlefonts.Font` import):

```kotlin
import androidx.compose.ui.text.font.Font
```

Both imports must coexist — one is `androidx.compose.ui.text.googlefonts.Font` (existing) and the new one is `androidx.compose.ui.text.font.Font`. They are overloads, distinguished by argument signature (`googleFont = ...` vs `resId = ...`).

Replace the `InstrumentSerifFamily` declaration with this exact 4-entry shape (GMS-first, bundled-fallback after — Compose picks first matching, so GMS still takes priority when available, and bundled TTF is used only when GMS download fails or hasn't completed):

```kotlin
val InstrumentSerifFamily: FontFamily = FontFamily(
    Font(googleFont = instrumentSerif, fontProvider = giftMaisonFontProvider, weight = FontWeight.Normal),
    Font(resId = R.font.instrument_serif_regular, weight = FontWeight.Normal),
    Font(googleFont = instrumentSerif, fontProvider = giftMaisonFontProvider, weight = FontWeight.Normal, style = FontStyle.Italic),
    Font(resId = R.font.instrument_serif_italic, weight = FontWeight.Normal, style = FontStyle.Italic),
)
```

**Do NOT modify `InterFamily` or `JetBrainsMonoFamily`** — RESEARCH.md decision: only Instrument Serif gets bundled (brand-critical display serif). Inter/JetBrains Mono fallback to system sans/mono is acceptable.

**Step 3 — Verify the new resource references resolve at compile time:**

```bash
./gradlew :app:compileDebugKotlin
```

The `R.font.instrument_serif_regular` and `R.font.instrument_serif_italic` references must resolve. Resource IDs in `R.font` are auto-generated from filenames (snake_case, lowercase, no hyphens) — the executor's filenames must match exactly.
  </action>
  <verify>
    <automated>
ls -la app/src/main/res/font/instrument_serif_regular.ttf app/src/main/res/font/instrument_serif_italic.ttf app/src/main/res/font/OFL.txt &amp;&amp; \
test $(stat -f%z app/src/main/res/font/instrument_serif_regular.ttf 2>/dev/null || stat -c%s app/src/main/res/font/instrument_serif_regular.ttf) -gt 10000 &amp;&amp; \
test $(stat -f%z app/src/main/res/font/instrument_serif_italic.ttf 2>/dev/null || stat -c%s app/src/main/res/font/instrument_serif_italic.ttf) -gt 10000 &amp;&amp; \
./gradlew :app:compileDebugKotlin
    </automated>
  </verify>
  <done>
    - `app/src/main/res/font/instrument_serif_regular.ttf` exists, > 10 KB, valid TTF (file command reports TrueType)
    - `app/src/main/res/font/instrument_serif_italic.ttf` exists, > 10 KB, valid TTF
    - `app/src/main/res/font/OFL.txt` exists, contains "SIL OPEN FONT LICENSE"
    - `GiftMaisonFonts.kt` imports `androidx.compose.ui.text.font.Font` (in addition to existing `googlefonts.Font`)
    - `InstrumentSerifFamily` has 4 `Font(...)` entries: GMS Regular, resId Regular, GMS Italic, resId Italic (in that order)
    - `InterFamily` and `JetBrainsMonoFamily` declarations are unchanged
    - `./gradlew :app:compileDebugKotlin` succeeds
  </done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Fix Auth headline colour split (line 1 = ink, line 2 = accent including period)</name>
  <files>
    app/src/main/java/com/giftregistry/ui/auth/AuthHeadline.kt,
    app/src/test/java/com/giftregistry/ui/auth/AuthHeadlineTest.kt
  </files>
  <behavior>
Updated `authHeadlineAnnotatedString` contract (RED→GREEN in same commit; tests must remain green after the implementation change):

- Test 1 — `text_containsBothLines`: text contains both "Start your" and "first registry." (UNCHANGED, still passes)
- Test 2 — `text_hasNewlineBetweenLines`: text contains "Start your\nfirst registry." (UNCHANGED, still passes)
- Test 3 — `periodIsLastCharacter`: last char of text is '.' (UNCHANGED, still passes)
- Test 4 — `period_isAccentColored`: span at index of last char has `color == accent` (UNCHANGED contract, but now satisfied by the merged line-2 span instead of a dedicated period span)
- Test 5 — REPLACED: `firstRegistryWords_areInkColored` (asserts ink) → `firstRegistryLine_isAccentColored` (asserts accent on the entire "first registry." run including period)
- Test 6 — REPLACED: `startYourLine_isInkSoftColored` (asserts inkSoft) → `startYourLine_isInkColored` (asserts solid `ink` on "Start your", per user constraint that line 1 is solid black not soft gray)
  </behavior>
  <action>
Implements requirement QUICK-GXU-02.

**IMPORTANT — User constraint override:** The RESEARCH.md example keeps `inkSoft` for line 1. The user constraint in the planning prompt explicitly overrides this: line 1 must be `ink` (solid black) per Image #7 in the design handoff. Honour the user constraint.

**Step 1 — Update the test file FIRST** (`app/src/test/java/com/giftregistry/ui/auth/AuthHeadlineTest.kt`):

Update the `build()` helper signature to match the new factory (drop `inkSoft` if removing the parameter — see Step 2 — OR keep all 5 parameters if leaving the factory signature stable). Recommended: keep all 5 parameters for API stability, just stop using `inkSoft` and `ink` doesn't go where the test currently expects.

Replace `firstRegistryWords_areInkColored` with:

```kotlin
@Test fun firstRegistryLine_isAccentColored() {
    val result = build()
    val startIdx = result.text.indexOf("first registry.")
    val span = result.spanStyles.firstOrNull { span ->
        startIdx in span.start until span.end
    }
    assertEquals(
        "'first registry.' run must be entirely accent-coloured (period included)",
        accent, span?.item?.color
    )
}
```

Replace `startYourLine_isInkSoftColored` with:

```kotlin
@Test fun startYourLine_isInkColored() {
    val result = build()
    val startIdx = result.text.indexOf("Start your")
    val span = result.spanStyles.firstOrNull { span ->
        startIdx in span.start until span.end
    }
    assertEquals(
        "'Start your' must be solid ink-coloured (per design Image #7, not inkSoft)",
        ink, span?.item?.color
    )
}
```

`period_isAccentColored`, `text_containsBothLines`, `text_hasNewlineBetweenLines`, `periodIsLastCharacter` stay as-is.

Update the file's KDoc/contract comment block (lines 8-22) to reflect the new contract:
```
 *   Line 1 "Start your"        → SpanStyle(color = ink)
 *   \n
 *   Line 2 "first registry."   → SpanStyle(color = accent)   ← entire line including period
```

**Step 2 — Update `AuthHeadline.kt`:**

Recommended path (preserves API stability — easier diff): keep the 5-parameter factory signature but rewire the body to ignore `inkSoft` and use `ink` for line 1, `accentColor` for line 2 (period included):

```kotlin
fun authHeadlineAnnotatedString(
    prefix: String,
    accent: String,
    ink: Color,
    accentColor: Color,
    @Suppress("UNUSED_PARAMETER") inkSoft: Color,  // retained for API stability; no longer used
): AnnotatedString = buildAnnotatedString {
    withStyle(SpanStyle(color = ink)) {
        append(prefix)
    }
    append("\n")
    withStyle(SpanStyle(color = accentColor)) {
        append(accent)
        append(".")
    }
}
```

Update the file's KDoc (lines 17-27) to reflect the new colour split:
```
 *   Line 1 "Start your"                        → ink (solid black per Image #7)
 *   \n
 *   Line 2 "first registry."                   → accent (entire line incl. period)
```

The `@Composable AuthHeadline()` wrapper does NOT need signature changes — it still passes `ink = colors.ink` and `inkSoft = colors.inkSoft`. The `remember(...)` key list can keep `colors.inkSoft` (harmless — just an extra recomposition key that never changes per theme).

**(Alternative — only if you prefer a cleaner signature):** Drop `inkSoft` from the factory signature AND from the @Composable call site AND from the `remember(...)` key list. Both paths satisfy the constraint; pick whichever produces the cleaner diff.

**Step 3 — Run tests:**

```bash
./gradlew :app:testDebugUnitTest --tests "com.giftregistry.ui.auth.AuthHeadlineTest"
```

All 6 tests must pass.

**Step 4 — Sanity build:**

```bash
./gradlew :app:compileDebugKotlin
```
  </action>
  <verify>
    <automated>./gradlew :app:testDebugUnitTest --tests "com.giftregistry.ui.auth.AuthHeadlineTest" &amp;&amp; ./gradlew :app:compileDebugKotlin</automated>
  </verify>
  <done>
    - `authHeadlineAnnotatedString` produces 2 SpanStyle spans: ink covering "Start your", accent covering "first registry."
    - The trailing period is part of the accent span (not its own span)
    - `inkSoft` is no longer applied to any span (parameter may remain on signature for stability or be removed — both acceptable)
    - `firstRegistryLine_isAccentColored` test exists and asserts accent on "first registry."
    - `startYourLine_isInkColored` test exists and asserts ink on "Start your"
    - `period_isAccentColored` test still passes (now satisfied by the merged line-2 span)
    - All 6 tests in `AuthHeadlineTest` pass
    - `./gradlew :app:compileDebugKotlin` succeeds
  </done>
</task>

</tasks>

<verification>
**Compile gate:**
```bash
./gradlew :app:compileDebugKotlin
```

**Unit test gate:**
```bash
./gradlew :app:testDebugUnitTest --tests "com.giftregistry.ui.auth.AuthHeadlineTest"
```

**File-presence gate:**
```bash
ls -la app/src/main/res/font/instrument_serif_regular.ttf \
       app/src/main/res/font/instrument_serif_italic.ttf \
       app/src/main/res/font/OFL.txt
```

**Manual visual sanity (optional, recommended):**
- Open `StyleGuidePreview` in Android Studio Compose Preview — Instrument Serif renders correctly (look at any display heading)
- Run app on emulator without Google Play Services (or with offline mode toggled) — Auth screen still renders Instrument Serif on the headline (no system serif fallback)
- Auth screen visual: line 1 "Start your" in solid black, line 2 "first registry." entirely in accent orange (period included)
</verification>

<success_criteria>
- All 6 `AuthHeadlineTest` tests pass with the new colour contract
- 3 new files exist in `app/src/main/res/font/` (2 TTFs > 10 KB each + OFL.txt)
- `InstrumentSerifFamily` has 4 Font entries (GMS-first, bundled fallback after) — `Inter` and `JetBrainsMono` families unchanged
- `authHeadlineAnnotatedString` produces 2 spans: ink for line 1, accent for line 2 (period included)
- `./gradlew :app:compileDebugKotlin` succeeds
- No changes outside the listed `files_modified`
</success_criteria>

<output>
After completion, create `.planning/quick/260427-gxu-fix-auth-headline-colors-line-2-should-b/260427-gxu-SUMMARY.md` summarising:
- Files created/modified
- Final InstrumentSerifFamily structure (4 entries)
- Final authHeadlineAnnotatedString colour contract (2 spans)
- Test outcome (6/6 passing)
</output>
