# Phase 8: GiftMaison Design Foundation — Research

**Researched:** 2026-04-20
**Domain:** Jetpack Compose theming, downloadable fonts, design tokens
**Confidence:** HIGH

## Summary

Phase 8 is a re-skin foundation milestone: ship the GiftMaison design primitives (fonts, type scale, colour tokens, spacing/radii/shadows, wordmark) as Compose-native values that every v1.1 screen consumes. The safest path is to **extend — not replace — the existing MaterialTheme** by wrapping it with a `GiftMaisonTheme` that exposes tokens through `CompositionLocal`s, then pipes Housewarming colours into the Material3 `ColorScheme` mapping so all existing screens (which already read `MaterialTheme.colorScheme.*`) get the new palette for free.

Instrument Serif, Inter, and JetBrains Mono are all on Google Fonts and load via the `androidx.compose.ui:ui-text-google-fonts` downloadable-fonts API — no font binaries need to be bundled. sRGB hex conversion of the oklch source values is already provided in the handoff; use those verbatim (oklch-native `Color` construction via `ColorSpaces.Oklab` is technically possible but adds complexity with no colour-quality benefit for a single fixed palette). The wordmark is a trivial `buildAnnotatedString` — one `SpanStyle` for ink + italic display serif, a second for the terracotta accent on the period.

**Primary recommendation:** Build `GiftMaisonTheme` as a wrapper around the existing `GiftRegistryTheme` composable. Expose colour tokens (`GiftMaisonColors`), typography (`GiftMaisonTypography`), shapes (`GiftMaisonShapes`) via `staticCompositionLocalOf`. Map the 13 Housewarming tokens onto Material3 `ColorScheme` slots so existing `MaterialTheme.colorScheme` call sites don't need to change until Phases 9–11 rewrite each screen. Ship a `@Preview`-only style-guide composable (not a real nav destination) as the verification harness for success criteria 2–4.

<user_constraints>
## User Constraints

**Source:** No CONTEXT.md exists for Phase 8 — this is research ahead of `/gsd:discuss-phase`. Constraints below come from ROADMAP.md, REQUIREMENTS.md, the design handoff README, and CLAUDE.md.

### Locked Decisions (from roadmap + handoff + CLAUDE.md)

- **Housewarming only.** v1.1 ships the Housewarming palette only. THEME-01/02/03 (other occasion themes + runtime cascade) are deferred to v1.2. Don't build a theme-switching mechanism; don't generate Wedding/Baby/Birthday palettes.
- **Jetpack Compose, Material3 as baseline.** Handoff explicitly says "Jetpack Compose preferred; Material 3 as baseline." Project already uses both.
- **Android minSdk 23.** (Confirmed in `app/build.gradle.kts:17`. CLAUDE.md's "minSdk 21" reference is stale.)
- **No KTX Firebase modules** (CLAUDE.md + codebase pattern) — irrelevant for Phase 8 but reinforces "use main Compose modules too."
- **Instrument Serif, Inter, JetBrains Mono via Google Fonts** (handoff: "Android: pull Instrument Serif, Inter, and JetBrains Mono from Google Fonts. All three are available on Google Fonts.")
- **oklch → sRGB hex at build time** (handoff: "On Android, convert each token to sRGB hex in your colour resources — the occasion-switching logic doesn't need oklch at runtime.")
- **Wordmark is text, not an asset** (handoff: "The GiftMaison wordmark is text: Instrument Serif italic, with a single accent-coloured period. No logo file needed.")
- **No screens change in Phase 8.** Every phase goal and success criterion is structural/foundational. Screens 06–10 ship in Phases 10 and 11.

### Claude's Discretion

- Whether the new theme **wraps** MaterialTheme, **replaces** it, or sits alongside as a parallel `CompositionLocal` tree.
- How oklch → sRGB conversion is performed (hex constants vs. `Color(..., ColorSpace.Oklab)` runtime conversion). Handoff provides hex approximations but flags: *"If your colour pipeline supports wide-gamut, prefer converting oklch directly — the oklch values are authoritative."*
- Token-container naming (`GiftMaisonColors` vs `GmColors` vs `Tokens`), package location (`ui/theme/` vs `ui/designsystem/`), and how tokens are grouped.
- Whether the verification harness is a `@Preview`-only file, a hidden debug nav destination, or a gallery Activity (BuildConfig-gated).
- Whether spacing scale is exposed as `Dp` constants, as a wrapper object, or left as call-site magic numbers.
- Whether to adopt Compose M3's `ShapeDefaults` mapping or define bespoke `Shapes` slots.
- Exact font fallback strategy (sans-serif / serif / monospace system fallbacks on download failure).

### Deferred Ideas (OUT OF SCOPE for Phase 8)

- **Occasion theme switching** (Wedding/Baby/Birthday palettes + per-registry runtime cascade) → v1.2.
- **Screen redesigns** (Onboarding, Home, Registry Detail, Create, Add Item) → Phases 10, 11.
- **Status chip animation / pulsing dot** (Reserved 1.4 s pulse, Fetching 1.0 s pulse) → Phase 9 (STAT-01).
- **Bottom nav / centre FAB / Add-action sheet** → Phase 9 (CHROME-01/02/03).
- **Dark mode** — explicitly out of scope for v1.1.
- **Empty states** — explicitly out of scope for v1.1.
- **Web fallback visual refresh** — explicitly out of scope for v1.1 (owner-facing Android only).
- **Icon library redesign** — stroked nav icons are Phase 9's concern, not Phase 8's.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| DES-01 | Instrument Serif, Inter, JetBrains Mono available as Compose `FontFamily` via Google Fonts | Downloadable Fonts API section; provider + font_certs + `ui-text-google-fonts` dependency; italic support verified |
| DES-02 | Type scale (Display XL/L/M/S, Body L/M/S/XS, Mono caps) with handoff sizes, weights, letter-spacing, line-heights | Typography section; em-based `lineHeight` conversion for 1.0/1.05/1.1/1.35/1.4/1.45 multipliers; negative `letterSpacing` in `em` |
| DES-03 | Housewarming colour palette (13 tokens) as sRGB hex matching handoff table | Colour Tokens section; `#F7F2E9`..`#D29447` verbatim from handoff README; Material3 `ColorScheme` mapping |
| DES-04 | Spacing, radii (8/10/12/14/16/22/999), shadows (FAB / Google banner / bottom sheet) | Spacing & Radii section; Compose `RoundedCornerShape` + `Modifier.shadow` patterns; `999.dp` → `CircleShape` idiom |
| DES-05 | "GiftMaison" wordmark — Instrument Serif italic with terracotta period — reusable composable | Wordmark section; `AnnotatedString` + `SpanStyle` pattern preserves kerning within a single `Text` |
</phase_requirements>

## Project Constraints (from CLAUDE.md)

CLAUDE.md is the enforcement source for project-wide rules. Phase 8 must comply with:

- **Compose BOM**: use platform(libs.compose.bom) — BOM `2026.03.00` already pinned (`gradle/libs.versions.toml:5`). Do NOT override individual Compose module versions. ✓ No change needed.
- **Material3 via BOM**: the project already uses `androidx.compose.material3:material3`. Phase 8 continues to use M3 as the baseline, wrapped by GiftMaison tokens.
- **No KTX Firebase modules**: irrelevant for Phase 8 (no Firebase deps needed), but reinforces the "use main-module Compose UI too" convention.
- **Localization**: all UI strings in `strings.xml` / `values-ro/strings.xml`. Phase 8 ships zero user-facing strings (it's a foundation phase), but the wordmark literal "giftmaison." is a brand mark, not a translatable label — document this explicitly in the wordmark composable.
- **GSD workflow**: start work through `/gsd:plan-phase` / `/gsd:execute-phase`. Phase 8 is a planned phase — this research is stage 1 of that workflow.
- **Kotlin 2.3.x + Compose compiler plugin**: plugins already applied (`app/build.gradle.kts:3`). No change.
- **AGP 9.x claim in CLAUDE.md is stale** — actual `libs.versions.toml:2` pins AGP `8.13.0`. Phase 8 does not upgrade AGP. Note for planner: if AGP 8.13 lacks `ui-text-google-fonts` compatibility, plan accordingly (spoiler: it's compatible).

## Existing Code Audit

Current `ui/theme/` module state (Phase 2 scaffold — to be extended, not replaced):

| File | Current content | Phase 8 treatment |
|------|-----------------|-------------------|
| `app/src/main/java/com/giftregistry/ui/theme/Color.kt` | `LightColorScheme` / `DarkColorScheme` — Material3 `ColorScheme` with seed `#6750A4` purple and 10 semantic slots | REPLACE the literal Color values with Housewarming sRGB hex mappings. Keep the `lightColorScheme(...)` / `darkColorScheme(...)` builder calls so `MaterialTheme.colorScheme.*` keeps working. Add a `GiftMaisonColors` data class + `LocalGiftMaisonColors` CompositionLocal beside it for the 13 Housewarming tokens that don't cleanly map to M3 slots (e.g. `accentSoft`, `inkFaint`, `second`, `secondSoft`, `ok`, `warn`, `line`, `paperDeep`). |
| `app/src/main/java/com/giftregistry/ui/theme/Type.kt` | `GiftRegistryTypography` — Material3 `Typography` with `FontFamily.Default`, 4 slots filled (headlineLarge, titleLarge, bodyLarge, labelLarge) | EXTEND: swap `FontFamily.Default` for Google-Fonts-backed `FontFamily`s (Instrument Serif, Inter, JetBrains Mono). Map the 8 handoff type roles onto M3 slots where reasonable; expose bespoke GiftMaison roles (`displayXL`, `monoCaps`, etc.) via a `GiftMaisonTypography` data class + `LocalGiftMaisonTypography` for roles M3 doesn't have (italic display accent, mono caps at 9.5 sp). |
| `app/src/main/java/com/giftregistry/ui/theme/Theme.kt` | `GiftRegistryTheme(darkTheme, content)` — thin wrapper calling `MaterialTheme(colorScheme, typography, content)` | EXTEND: rename contents (keep the composable name `GiftRegistryTheme` for backward compatibility with `MainActivity.kt:48`), but internally provide `LocalGiftMaisonColors`, `LocalGiftMaisonTypography`, `LocalGiftMaisonShapes`, `LocalGiftMaisonSpacing` via `CompositionLocalProvider` around the existing `MaterialTheme(...)`. Ignore `darkTheme` for v1.1 (force light Housewarming — dark mode is out of scope). |

Call-site audit (how many places already read MaterialTheme values):

| Reader | Location | Impact of swapping colour seed |
|--------|----------|---------------------------------|
| `MaterialTheme.colorScheme.*` | 42+ locations across `ui/registry/`, `ui/auth/`, `ui/store/`, etc. (grep confirmed on `RegistryDetailScreen.kt` alone shows 15 reads) | HIGH — every screen's accent colour changes the moment Phase 8 merges. Existing screens keep working (they read through M3 slots) but the purple `#6750A4` becomes terracotta `#C8623A`. **This is desired** — it's the visual refresh baseline. Document for QA that Phase 8 intentionally re-skins existing screens to terracotta; pixel-perfect redesign is Phases 10/11. |
| `MaterialTheme.typography.*` | 28+ locations | MEDIUM — screens will adopt Instrument Serif for `headlineLarge`, Inter for `bodyLarge` / `labelLarge`. Existing layouts continue to work; line-heights shift slightly. |
| `@Preview` composables | 1 (`ConfirmPurchaseBanner.kt:100-110`) | LOW — preview continues to render through the new theme automatically once it's wrapped in `GiftRegistryTheme`. |
| `GiftRegistryTheme { ... }` wrapper | `MainActivity.kt:48` | LOW — single entry point, composable name unchanged. |

**Naming collisions to avoid:** existing `LightColorScheme`, `DarkColorScheme`, `GiftRegistryTypography`. Use fresh names for GiftMaison-specific containers: `GiftMaisonColors`, `GiftMaisonTypography`, `GiftMaisonShapes`, `GiftMaisonSpacing`, `LocalGiftMaisonColors`, etc. Consider a new sub-package `ui/theme/tokens/` or keep everything in `ui/theme/` — the planner's call.

## Standard Stack

### Core (already in project — no new versions needed)

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `androidx.compose.ui:ui` | via BOM `2026.03.00` | Core Compose primitives (`Color`, `TextStyle`, `Modifier`) | Already pinned in `libs.versions.toml:5` |
| `androidx.compose.material3:material3` | via BOM | Material3 `Typography`, `Shapes`, `ColorScheme` slots | Existing baseline; every screen reads `MaterialTheme.*` |
| `androidx.compose.ui:ui-text` | via BOM | `FontFamily`, `FontWeight`, `FontStyle`, `TextUnit.em` | Transitive; no explicit declaration needed |
| `androidx.compose.ui:ui-tooling-preview` | via BOM | `@Preview` annotation | Already declared in `app/build.gradle.kts:60` |

### New for Phase 8

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `androidx.compose.ui:ui-text-google-fonts` | via BOM (latest aligned with `compose-ui:1.10.x`) | `GoogleFont.Provider`, `GoogleFont`, `Font(googleFont=...)` | Required for DES-01. Declares `FontFamily` entries that resolve from Google Fonts via Google Play Services at runtime. |

**Version verification:**
```bash
# Confirm latest aligned version
# The ui-text-google-fonts module is released in lockstep with androidx.compose.ui
# BOM 2026.03.00 maps to compose-ui 1.10.6 (released March 2026, verified above)
# Declaration should be:
#   implementation("androidx.compose.ui:ui-text-google-fonts")
# The BOM pins the version automatically — do NOT hard-code 1.10.x
```

### Supporting (existing, relevant)

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `androidx.compose.material:material-icons-extended` | via BOM | Material Symbols as `ImageVector` | Already declared (`app/build.gradle.kts:62`); Phase 8 doesn't add icons but Phase 9 nav icons depend on it |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Downloadable Google Fonts | Bundled `.ttf` in `res/font/` | Bundled: 100 % deterministic load, +300-700 KB APK, no Play Services dep. Downloadable: ~0 KB APK, needs Play Services, async first-load (flashes system font briefly). Handoff explicitly says "pull from Google Fonts" — downloadable is the intended path. Bundle only if a specific device class (e.g. non-GMS) becomes a target. |
| sRGB hex constants | `Color(L, a, b, alpha, ColorSpaces.Oklab)` runtime conversion from oklch | `ColorSpaces.Oklab` exists in Compose (verified — used internally for gradient interpolation) but storing runtime `Color` objects with wide-gamut spaces has no rendering benefit on the vast majority of Android displays (sRGB panels). Hex is simpler, deterministic, trivially previewed, and matches handoff's explicit guidance. Only switch to oklch-native if the app targets wide-gamut HDR panels (not in scope). |
| Fully-custom `GiftMaisonTheme` that REPLACES `MaterialTheme` | Continue wrapping `MaterialTheme` | Replacing would force rewrites of every existing screen (42+ `MaterialTheme.colorScheme.*` reads) *in Phase 8*. Wrapping lets Phases 10/11 migrate screens incrementally to `GiftMaisonTheme.colors.*`. |
| Separate `@Composable` for wordmark | Text composable with hardcoded `AnnotatedString` | A shared composable is required by DES-05 ("reusable across top bars"). Hardcoding a literal inline per top bar would duplicate the accent-period logic. |
| `androidx.compose.material3:material3-window-size-class` | N/A | Not needed for Phase 8; responsive layout is not in scope. |

**Installation:**
```kotlin
// gradle/libs.versions.toml — add:
// [libraries]
// compose-ui-text-google-fonts = { group = "androidx.compose.ui", name = "ui-text-google-fonts" }

// app/build.gradle.kts — add inside dependencies { }:
implementation(libs.compose.ui.text.google.fonts)
```

## Architecture Patterns

### Recommended file structure

```
app/src/main/java/com/giftregistry/ui/theme/
├── Color.kt              # (existing) Material3 ColorScheme mapped to Housewarming hex — keep name, replace values
├── Type.kt               # (existing) Material3 Typography mapped to Instrument/Inter/Mono — keep name, replace values
├── Theme.kt              # (existing) GiftRegistryTheme wrapper — keep name, add CompositionLocalProvider inside
├── GiftMaisonColors.kt   # NEW  — @Immutable data class + LocalGiftMaisonColors + housewarmingColors() factory
├── GiftMaisonTypography.kt # NEW — @Immutable data class + LocalGiftMaisonTypography + giftMaisonTypography() factory (8 roles)
├── GiftMaisonShapes.kt   # NEW  — @Immutable data class + LocalGiftMaisonShapes (radii 8/10/12/14/16/22/999)
├── GiftMaisonSpacing.kt  # NEW  — @Immutable data class + LocalGiftMaisonSpacing (edge 16/20, gaps 4/6/8/10/12/14/16/18/20, element heights 30/32/36/40/48/54/58/80 dp)
├── GiftMaisonShadows.kt  # NEW  — named Modifier.shadow() helpers (fabShadow, googleBannerShadow, bottomSheetShadow)
├── GiftMaisonFonts.kt    # NEW  — GoogleFont.Provider + Instrument Serif / Inter / JetBrains Mono FontFamilys
├── GiftMaisonWordmark.kt # NEW  — @Composable GiftMaisonWordmark(modifier, style)
└── preview/
    └── StyleGuidePreview.kt # NEW — @Preview-only file exercising every token (success criteria 2, 3, 4, 5)

app/src/main/res/values/
├── font_certs.xml        # NEW — Google Fonts provider certificate hashes (copy from Jetchat sample)
```

### Pattern 1: GoogleFont Provider + FontFamily

**What:** Declare a single shared `GoogleFont.Provider` in `GiftMaisonFonts.kt`, then build one `FontFamily` per typeface with explicit weights + styles.
**When to use:** Every typography token (DES-01, DES-02).
**Example:**
```kotlin
// Source: https://developer.android.com/develop/ui/compose/text/fonts
package com.giftregistry.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.giftregistry.R

internal val giftMaisonFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private val instrumentSerif = GoogleFont("Instrument Serif")
private val inter = GoogleFont("Inter")
private val jetBrainsMono = GoogleFont("JetBrains Mono")

val InstrumentSerifFamily = FontFamily(
    Font(googleFont = instrumentSerif, fontProvider = giftMaisonFontProvider, weight = FontWeight.Normal),
    Font(googleFont = instrumentSerif, fontProvider = giftMaisonFontProvider, weight = FontWeight.Normal, style = FontStyle.Italic),
    // Fallback: system serif if Google Play Services unavailable (e.g. on non-GMS devices during dev)
    Font(resId = R.font.serif_fallback, weight = FontWeight.Normal), // optional: bundle a thin fallback if desired
)

val InterFamily = FontFamily(
    Font(googleFont = inter, fontProvider = giftMaisonFontProvider, weight = FontWeight.Normal),
    Font(googleFont = inter, fontProvider = giftMaisonFontProvider, weight = FontWeight.Medium),
    Font(googleFont = inter, fontProvider = giftMaisonFontProvider, weight = FontWeight.SemiBold),
)

val JetBrainsMonoFamily = FontFamily(
    Font(googleFont = jetBrainsMono, fontProvider = giftMaisonFontProvider, weight = FontWeight.Medium),
    Font(googleFont = jetBrainsMono, fontProvider = giftMaisonFontProvider, weight = FontWeight.SemiBold),
)
```

Note on fallback: bundled fallback fonts are optional — Compose's `FontFamily` degrades gracefully to system fonts if all downloadable Font entries fail. For simpler setup, omit `res/font/` files and rely on system fallback. Verify on an AVD without Play Services during development.

### Pattern 2: CompositionLocal-backed design tokens

**What:** Define `@Immutable data class` for each token group; expose via `staticCompositionLocalOf` with `Color.Unspecified` / `TextStyle.Default` defaults; consume via a `GiftMaisonTheme` object with `@Composable` getters.
**When to use:** Every token group that doesn't cleanly map onto an existing Material3 slot — `accentSoft`, `inkFaint`, `secondSoft`, `ok`, the full type scale's bespoke `displayXL` + `monoCaps` roles, spacing, shapes, shadows.
**Example:**
```kotlin
// Source: https://developer.android.com/develop/ui/compose/designsystems/custom
@Immutable
data class GiftMaisonColors(
    val paper: Color,
    val paperDeep: Color,
    val ink: Color,
    val inkSoft: Color,
    val inkFaint: Color,
    val line: Color,
    val accent: Color,
    val accentInk: Color,
    val accentSoft: Color,
    val second: Color,
    val secondSoft: Color,
    val ok: Color,
    val warn: Color,
)

val LocalGiftMaisonColors = staticCompositionLocalOf {
    GiftMaisonColors(
        paper = Color.Unspecified, paperDeep = Color.Unspecified, ink = Color.Unspecified,
        inkSoft = Color.Unspecified, inkFaint = Color.Unspecified, line = Color.Unspecified,
        accent = Color.Unspecified, accentInk = Color.Unspecified, accentSoft = Color.Unspecified,
        second = Color.Unspecified, secondSoft = Color.Unspecified,
        ok = Color.Unspecified, warn = Color.Unspecified,
    )
}

fun housewarmingColors() = GiftMaisonColors(
    paper = Color(0xFFF7F2E9),
    paperDeep = Color(0xFFEDE5D5),
    ink = Color(0xFF2A2420),
    inkSoft = Color(0xFF6A5E52),
    inkFaint = Color(0xFF9C8E7F),
    line = Color(0xFFDDD4C4),
    accent = Color(0xFFC8623A),
    accentInk = Color(0xFFFCF8EF),
    accentSoft = Color(0xFFF3DED0),
    second = Color(0xFF4F7050),
    secondSoft = Color(0xFFD7E2CE),
    ok = Color(0xFF4F9668),
    warn = Color(0xFFD29447),
)

object GiftMaisonTheme {
    val colors: GiftMaisonColors
        @Composable get() = LocalGiftMaisonColors.current
    val typography: GiftMaisonTypography
        @Composable get() = LocalGiftMaisonTypography.current
    val shapes: GiftMaisonShapes
        @Composable get() = LocalGiftMaisonShapes.current
    val spacing: GiftMaisonSpacing
        @Composable get() = LocalGiftMaisonSpacing.current
}
```

### Pattern 3: Theme composable wraps MaterialTheme (not replaces)

**What:** `GiftRegistryTheme` (keep existing name for call-site compatibility) provides GiftMaison CompositionLocals AND drives Material3 `ColorScheme` / `Typography` from the same source values.
**When to use:** Top-level theme wrapper (`MainActivity.kt:48` entry point).
**Example:**
```kotlin
@Composable
fun GiftRegistryTheme(
    darkTheme: Boolean = false, // v1.1 locks light — dark mode out of scope
    content: @Composable () -> Unit,
) {
    val gmColors = housewarmingColors()
    val gmTypography = giftMaisonTypography() // InstrumentSerifFamily + InterFamily + JetBrainsMonoFamily
    val gmShapes = giftMaisonShapes()
    val gmSpacing = giftMaisonSpacing()

    // Map Housewarming tokens onto M3 ColorScheme slots so existing MaterialTheme.colorScheme.* reads
    // automatically pick up the new palette.
    val m3ColorScheme = lightColorScheme(
        primary = gmColors.accent,
        onPrimary = gmColors.accentInk,
        primaryContainer = gmColors.accentSoft,
        onPrimaryContainer = gmColors.ink,
        secondary = gmColors.second,
        onSecondary = gmColors.accentInk,
        secondaryContainer = gmColors.secondSoft,
        onSecondaryContainer = gmColors.ink,
        background = gmColors.paper,
        onBackground = gmColors.ink,
        surface = gmColors.paper,
        surfaceVariant = gmColors.paperDeep,
        onSurface = gmColors.ink,
        onSurfaceVariant = gmColors.inkSoft,
        outline = gmColors.line,
        outlineVariant = gmColors.line,
        error = gmColors.warn,
        onError = gmColors.accentInk,
    )

    val m3Typography = Typography( // minimal M3 mapping; screens can read GiftMaisonTheme.typography directly for bespoke roles
        displayLarge = gmTypography.displayXL,
        headlineLarge = gmTypography.displayL,
        headlineMedium = gmTypography.displayM,
        titleLarge = gmTypography.displayS,
        bodyLarge = gmTypography.bodyL,
        bodyMedium = gmTypography.bodyM,
        bodySmall = gmTypography.bodyS,
        labelSmall = gmTypography.monoCaps,
    )

    CompositionLocalProvider(
        LocalGiftMaisonColors provides gmColors,
        LocalGiftMaisonTypography provides gmTypography,
        LocalGiftMaisonShapes provides gmShapes,
        LocalGiftMaisonSpacing provides gmSpacing,
    ) {
        MaterialTheme(
            colorScheme = m3ColorScheme,
            typography = m3Typography,
            content = content,
        )
    }
}
```

### Pattern 4: Wordmark via AnnotatedString (DES-05)

**What:** Single `Text(AnnotatedString)` preserves kerning between the "giftmaison" body and the accented period.
**When to use:** Every top-bar using the wordmark.
**Example:**
```kotlin
@Composable
fun GiftMaisonWordmark(
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 20.sp, // handoff doesn't pin a wordmark size — caller can override per top-bar
) {
    val ink = GiftMaisonTheme.colors.ink
    val accent = GiftMaisonTheme.colors.accent
    val text = remember(ink, accent) {
        buildAnnotatedString {
            withStyle(SpanStyle(color = ink)) {
                append("giftmaison")
            }
            withStyle(SpanStyle(color = accent)) {
                append(".")
            }
        }
    }
    Text(
        text = text,
        modifier = modifier,
        fontFamily = InstrumentSerifFamily,
        fontStyle = FontStyle.Italic,
        fontWeight = FontWeight.Normal,
        fontSize = fontSize,
        letterSpacing = (-0.4).em, // Display L cadence from handoff type scale
    )
}
```
Single `Text` with `AnnotatedString` preserves glyph-to-glyph kerning (as opposed to two adjacent `Text` composables, which break kerning across the baseline boundary). **Do not** split into two Text composables.

### Pattern 5: Unitless line-heights via `em`

**What:** Compose's `TextUnit.em` expresses line-height as a multiplier of font size. `1.35.em` on a 13.5 sp font is equivalent to CSS `line-height: 1.35`.
**When to use:** Every type role in the handoff scale.
**Example:**
```kotlin
// Body M — handoff: Inter 400-500, 13.5 sp, -0.1 letter-spacing, 1.45 line-height
val bodyM = TextStyle(
    fontFamily = InterFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 13.5.sp,
    letterSpacing = (-0.1).em, // NOTE: handoff value is "−0.1" — treat as em, not sp, per Compose convention
    lineHeight = 1.45.em,
    platformStyle = PlatformTextStyle(includeFontPadding = false), // critical when using em lineHeight — see Pitfall 3
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.None,
    ),
)
```

**Letter-spacing unit convention:** Handoff gives negative numbers (`−0.8`, `−0.4`, `−0.3`, `−0.2`, `−0.1`) with no unit. In Compose these are **em** (matching CSS convention for negative tracking). Cross-verified against LogRocket / Android Developers typography guidance.

**Conversion table (handoff → Compose):**

| Role | Font | Weight | `fontSize` | `letterSpacing` | `lineHeight` |
|------|------|--------|-----------|-----------------|--------------|
| Display XL (h1) | InstrumentSerifFamily | W400 | `32.sp` | `(-0.8).em` | `1.0.em` |
| Display L (hero) | InstrumentSerifFamily | W400 | `24.sp` | `(-0.4).em` | `1.05.em` |
| Display M | InstrumentSerifFamily | W400 | `22.sp` | `(-0.4).em` | `1.1.em` |
| Display S (card) | InstrumentSerifFamily | W400 | `18.sp` | `(-0.3).em` | `1.1.em` |
| Body L | InterFamily | W500 | `15.sp` | `(-0.2).em` | `1.35.em` |
| Body M (default) | InterFamily | W400 (also W500 variant) | `13.5.sp` | `(-0.1).em` | `1.45.em` |
| Body S | InterFamily | W400 (also W500) | `12.5.sp` | `0.em` | `1.4.em` |
| Body XS | InterFamily | W400 | `11.5.sp` | `0.em` | `1.35.em` |
| Mono caps | JetBrainsMonoFamily | W500 (also W600) | `9.5.sp` (label: up to `11.sp`) | `0.6.em` to `1.5.em` | `1.3.em` |

### Anti-Patterns to Avoid

- **Splitting the wordmark into two adjacent `Text` composables.** Breaks kerning; "giftmaison" and "." render with visible whitespace. Use one `Text(AnnotatedString)`.
- **Hardcoding Housewarming colours inline on individual screens.** Phases 10/11 must read from `GiftMaisonTheme.colors.*` or `MaterialTheme.colorScheme.*` — never `Color(0xFFC8623A)` in a screen file.
- **Bundling `.ttf` for Inter + JetBrains Mono.** Adds ~400 KB APK for fonts that download free via Google Fonts. Handoff is explicit: pull from Google Fonts.
- **Using `sp` for letter-spacing.** `(-0.1).sp` is a 0.1-scaled-pixel adjustment, not a 10% em tracking. Compose convention is `em` for tracking.
- **Forgetting `includeFontPadding = false`.** Default Android font padding inflates `em`-based line-heights by ~20 %, making lines look too airy. Use `PlatformTextStyle(includeFontPadding = false)` + `LineHeightStyle` for handoff-accurate vertical rhythm.
- **Defining separate "dark-mode Housewarming" colours.** Dark mode is out of scope. Force light scheme; leave `darkColorScheme(...)` definition trivial (e.g. identical to light) or delete the parameter.
- **Replacing MaterialTheme entirely in Phase 8.** Phase 8 is a foundation phase — screens haven't been redesigned yet. Wrapping MaterialTheme lets Phases 10/11 migrate incrementally. Full replacement is viable only once every screen has been rewritten.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Colour-space conversion from oklch → sRGB | Custom Kotlin oklch-to-RGB math | Use the hex values already provided in handoff README | Handoff explicitly endorses this path; the hex values ARE the contract |
| Font asynchronous loading and fallback | Manual `TypefaceCompat.createFromResourcesFontFile` or `FontProvider` plumbing | `androidx.compose.ui:ui-text-google-fonts` `Font(googleFont=...)` | First-party Compose API; handles async + fallback automatically |
| Shape scale | Custom `Shape` subclasses | `RoundedCornerShape(8.dp)` for radii 8/10/12/14/16/22; `CircleShape` for 999 | Standard Compose; `999.dp` radius = `CircleShape` semantically |
| Shadow recipes | Custom draw layer | `Modifier.shadow(elevation, shape, ambientColor, spotColor, clip)` | API 28+ supports coloured shadows; for minSdk 23 pre-API-28 falls back to neutral shadow with no visual degradation on handoff screens |
| Elevation tokens | Separate elevation scale | Use shadow specs directly — handoff only names 3 shadows (FAB, Google banner, bottom sheet) | Over-engineering if the whole app uses 3 shadows |
| Preview scaffold | Custom harness Activity | `@Preview(showBackground = true)` composables grouped in a `preview/` package | Compose Studio ships with this; no setup needed |
| Wordmark as SVG/vector asset | Custom ImageVector path | `Text(AnnotatedString)` with Instrument Serif italic | Handoff: "The GiftMaison wordmark is text … No logo file needed." |

**Key insight:** every subsystem Phase 8 ships has a canonical Compose API. There is zero invention needed — this is a configuration phase.

## Common Pitfalls

### Pitfall 1: Google Fonts provider not verified before first render
**What goes wrong:** On devices without Google Play Services (emulator images, Huawei devices, air-gapped test rigs), `Font(googleFont=...)` silently fails — the resolved font is `null`, falling back to Compose's default. Text renders, but in the wrong typeface, and it's easy to miss in QA because there's no error thrown.
**Why it happens:** Downloadable fonts are network-sourced; the provider isn't guaranteed available.
**How to avoid:**
1. Copy the `font_certs.xml` hashes from the Jetchat sample (URL in Sources).
2. In a debug build, call `provider.isAvailableOnDevice(context)` on app start and log a warning if false.
3. Use AVDs with Google Play Services for visual QA of the type scale.
4. Optionally bundle a single fallback `.ttf` per family (minor APK cost).
**Warning signs:** Previews in Android Studio render text in default system font even after theme is applied.

### Pitfall 2: `em`-based `lineHeight` inflated by default font padding
**What goes wrong:** `lineHeight = 1.35.em` on Inter 13.5 sp should yield ~18.2 sp line box. Default Compose text inherits `includeFontPadding = true`, adding ~3-4 sp of padding above and below. Result: visible spacing between lines is 50% more than the handoff mocks.
**Why it happens:** Legacy Android `TextView` behaviour persisted in Compose's default `PlatformTextStyle`.
**How to avoid:** Set `platformStyle = PlatformTextStyle(includeFontPadding = false)` on every `TextStyle` in the scale, plus `LineHeightStyle(alignment = Center, trim = None)` for distribution.
**Warning signs:** Headlines sit with visible gaps above; multi-line body copy feels airier than the mock.

### Pitfall 3: Letter-spacing unit confusion (sp vs em)
**What goes wrong:** Author writes `letterSpacing = (-0.8).sp` interpreting handoff's "−0.8" as pixels. Compose applies a 0.8-scaled-pixel negative tracking — essentially invisible at 32 sp. Display XL headlines render with CSS-standard tracking instead of tight handoff tracking.
**Why it happens:** Compose `letterSpacing` accepts both `sp` and `em`; CSS convention (what the handoff reference uses) is em.
**How to avoid:** Always use `.em` for letter-spacing unless the handoff explicitly specifies a unit. Add a unit test that asserts `bodyM.letterSpacing == (-0.1).em`.
**Warning signs:** Mocks look tighter than the rendered app; no visible change when you adjust the letter-spacing number.

### Pitfall 4: Swapping MaterialTheme seed colour breaks dark-mode contrast assumptions
**What goes wrong:** `DarkColorScheme` was tuned for purple `#6750A4`. Mapping Housewarming `ink → onBackground` in dark mode produces a dark-on-dark unreadable theme.
**Why it happens:** Dark mode wasn't in the handoff scope; Phase 2 `DarkColorScheme` was a reasonable default for purple.
**How to avoid:** Force light mode for v1.1 by ignoring the `darkTheme` parameter in `GiftRegistryTheme`. Leave `DarkColorScheme` as a placeholder (or delete entirely). Document that v1.1 is light-only in CLAUDE.md's conventions.
**Warning signs:** Running AVD with "Dark theme" system setting renders unreadable screens.

### Pitfall 5: Static seed colour hit — `#6750A4` purple has 42+ call sites
**What goes wrong:** Phase 2's "Static seed color #6750A4, no dynamic color" decision was pragmatic for v1.0. Phase 8 changes that seed to `#C8623A` terracotta. Every existing screen re-skins itself automatically — which is desired — but unreviewed QA assumes v1.0 screens still look v1.0.
**Why it happens:** Material3 `ColorScheme` is transparent to the reader; screens don't explicitly ask for purple.
**How to avoid:** Document Phase 8's side effect explicitly in the phase plan — "All existing screens re-skin to Housewarming palette as a side effect; pixel-perfect screen redesign is Phases 10–11. QA baseline shifts from purple to terracotta." Add a screenshot test harness if available (Paparazzi or Compose Screenshot Testing) so diffs are tracked.
**Warning signs:** Stakeholder says "the existing screens changed colour, we didn't agree to that" — communicate proactively.

### Pitfall 6: `remember { buildAnnotatedString { … } }` captures stale colours when theme changes
**What goes wrong:** Wordmark composable calls `remember { … }` without a key. When the user toggles theme (e.g. v1.2 adds occasion themes), the cached `AnnotatedString` still uses the old accent colour.
**Why it happens:** `remember` without keys is called exactly once per composition.
**How to avoid:** `remember(ink, accent) { buildAnnotatedString { … } }` — v1.1 won't trip on this (static Housewarming) but v1.2 will. Future-proof now.
**Warning signs:** None in v1.1; latent for v1.2.

### Pitfall 7: Google Fonts name mismatch
**What goes wrong:** `GoogleFont("Instrument Serif")` expects the exact name from fonts.google.com. Typos (`"InstrumentSerif"`, `"Instrument Serif "`) silently resolve to `null`.
**Why it happens:** The provider is string-matched against the Google Fonts catalog.
**How to avoid:** Paste names verbatim from the URLs: `fonts.google.com/specimen/Instrument+Serif`, `fonts.google.com/specimen/Inter`, `fonts.google.com/specimen/JetBrains+Mono`. Add an instrumented test that inspects the rendered font family name post-load.
**Warning signs:** Text renders as system serif / sans-serif instead of the intended family.

## Code Examples

### Style-guide preview harness (verifies criteria 2-5)

```kotlin
// Source: Compose preview best practice — multiple @Preview composables in one file
// File: app/src/main/java/com/giftregistry/ui/theme/preview/StyleGuidePreview.kt

@Preview(name = "Type scale", showBackground = true, backgroundColor = 0xFFF7F2E9, widthDp = 360, heightDp = 800)
@Composable
private fun TypeScalePreview() {
    GiftRegistryTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Display XL 32", style = GiftMaisonTheme.typography.displayXL)
            Text("Display L 24", style = GiftMaisonTheme.typography.displayL)
            Text("Display M 22", style = GiftMaisonTheme.typography.displayM)
            Text("Display S 18", style = GiftMaisonTheme.typography.displayS)
            Text("Body L 15 500", style = GiftMaisonTheme.typography.bodyL)
            Text("Body M 13.5 default", style = GiftMaisonTheme.typography.bodyM)
            Text("Body S 12.5", style = GiftMaisonTheme.typography.bodyS)
            Text("Body XS 11.5", style = GiftMaisonTheme.typography.bodyXS)
            Text("MONO CAPS 9.5", style = GiftMaisonTheme.typography.monoCaps)
        }
    }
}

@Preview(name = "Colour palette", showBackground = true, widthDp = 360, heightDp = 520)
@Composable
private fun PalettePreview() {
    GiftRegistryTheme {
        val c = GiftMaisonTheme.colors
        val swatches = listOf(
            "paper" to c.paper, "paperDeep" to c.paperDeep, "ink" to c.ink,
            "inkSoft" to c.inkSoft, "inkFaint" to c.inkFaint, "line" to c.line,
            "accent" to c.accent, "accentInk" to c.accentInk, "accentSoft" to c.accentSoft,
            "second" to c.second, "secondSoft" to c.secondSoft, "ok" to c.ok, "warn" to c.warn,
        )
        LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(swatches) { (name, color) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                            .background(color).border(1.dp, c.line, RoundedCornerShape(8.dp)),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(name, style = GiftMaisonTheme.typography.bodyM)
                }
            }
        }
    }
}

@Preview(name = "Radii + spacing + wordmark", showBackground = true, widthDp = 360, heightDp = 360)
@Composable
private fun RadiiShadowsWordmarkPreview() {
    GiftRegistryTheme {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            GiftMaisonWordmark()
            listOf(8, 10, 12, 14, 16, 22).forEach { r ->
                Box(
                    Modifier.fillMaxWidth().height(40.dp)
                        .clip(RoundedCornerShape(r.dp))
                        .background(GiftMaisonTheme.colors.paperDeep)
                        .border(1.dp, GiftMaisonTheme.colors.line, RoundedCornerShape(r.dp))
                )
            }
            // 999 radius = pill
            Box(
                Modifier.width(120.dp).height(40.dp).clip(CircleShape)
                    .background(GiftMaisonTheme.colors.ink),
                contentAlignment = Alignment.Center,
            ) { Text("Pill 999", color = GiftMaisonTheme.colors.paper) }
            // FAB shadow sample
            Box(
                Modifier.size(54.dp)
                    .shadow(elevation = 20.dp, shape = CircleShape, spotColor = GiftMaisonTheme.colors.accent)
                    .clip(CircleShape)
                    .background(GiftMaisonTheme.colors.accent)
            )
        }
    }
}
```

### Wordmark invocation in a top bar

```kotlin
// Example usage Phase 9/10 will consume:
TopAppBar(
    title = { GiftMaisonWordmark(fontSize = 22.sp) },
    actions = { TextButton(onClick = {}) { Text("Need help?", color = GiftMaisonTheme.colors.inkSoft) } },
    colors = TopAppBarDefaults.topAppBarColors(containerColor = GiftMaisonTheme.colors.paper),
)
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Android resource `fonts.xml` + `FontProvider` in AndroidManifest | Compose `ui-text-google-fonts` `Font(googleFont=...)` | Compose 1.2+ (2022) | Simpler API; works entirely in Kotlin; no manifest entries needed |
| `TextUnit.TextUnitType.Sp` for line-height multipliers | `TextUnit.TextUnitType.Em` via `.em` extension | Compose 1.0+ | `1.35.em` is a font-size-relative multiplier; self-documenting |
| Bundled OTF/TTF in `res/font/` | Downloadable Google Fonts | ui-text-google-fonts GA | ~0 KB APK vs 300-700 KB per bundled family; async-load cost negligible after first launch |
| M2 `MaterialTheme.colors.primary` | M3 `MaterialTheme.colorScheme.primary` | M3 GA, mid-2022 | Already on M3 (verified `app/src/main/java/com/giftregistry/ui/theme/Color.kt`) |
| `LocalContentColor` alpha-based emphasis | Explicit `inkFaint` / `inkSoft` semantic tokens | Design-system maturity pattern | Handoff follows this; avoids alpha-opacity guessing |
| `includeFontPadding = true` (legacy default) | `includeFontPadding = false` + `LineHeightStyle` | Compose 1.4+ | Handoff-accurate vertical rhythm; removes ~4 sp of invisible padding |

**Deprecated/outdated:**
- The Phase-2 stock `Typography()` Material3 slot assignments (purple, `FontFamily.Default`) are fully superseded by the GiftMaison scale. Not removed — mapped through so nothing breaks.
- `darkColorScheme(...)` with purple seed — retain the symbol to avoid diff noise, but it's unreachable in v1.1 (always light).

## Open Questions

1. **Wordmark default size**
   - What we know: handoff says Instrument Serif italic + accent period. No pixel size specified; each top bar may size it differently.
   - What's unclear: default `fontSize` for the wordmark composable.
   - Recommendation: default to `20.sp` (typical app-bar title size), allow caller override. Revisit when Phase 10 consumes it on Screen 06 (Onboarding).

2. **Font fallback strategy**
   - What we know: Downloadable fonts can fail on non-GMS devices; system fallback chain kicks in automatically.
   - What's unclear: whether the product team considers system-font fallback acceptable for owner demos on non-GMS emulators.
   - Recommendation: no bundled fallbacks in Phase 8 (keep scope tight); add a debug-only warning log if `provider.isAvailableOnDevice(context)` returns false. Revisit if a demo device fails.

3. **Spacing scale canonicalisation**
   - What we know: handoff lists spacing values ad-hoc (14, 16, 20, 6 gap, 10 gap). No compact "4-step" / "8-step" spacing scale is given.
   - What's unclear: whether `GiftMaisonSpacing` should enumerate every value seen or adopt a canonical 4 dp step scale (4/8/12/16/20/24).
   - Recommendation: enumerate the 8 distinct gaps the handoff references explicitly (`4, 6, 8, 10, 12, 14, 16, 20`) as named fields (`xxs, xs, s, m, md, l, xl, xxl`). Avoid imposing a 4-step scale that fights the handoff values. Planner's call.

4. **Shadow token shape**
   - What we know: handoff names 3 shadows (FAB, Google banner, bottom sheet) with CSS shorthand (`0 8 20 {accent}55`).
   - What's unclear: whether to expose as `Modifier.fabShadow()` extension or as `GiftMaisonShadows.fab` config + applied via `Modifier.shadow(elevation = ..., ambientColor = ..., spotColor = ...)`.
   - Recommendation: `Modifier` extensions — ergonomic at call sites, encapsulates the coloured-shadow API-28 fallback. Planner's call.

5. **Body M's 400–500 weight range**
   - What we know: handoff gives "Body M" as 400–500 weight. That's two weights, not a range.
   - What's unclear: whether Body M has two variants (`bodyM_Regular`, `bodyM_Medium`) or a single default.
   - Recommendation: ship one `bodyM` (W400) and a `bodyMEmphasis` (W500) so call sites have a clean choice. Same for Body S.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|-------------|-----------|---------|----------|
| Android Studio (preview rendering) | Verification of criteria 2-5 | Assumed | Meerkat / Ladybug+ | — |
| Google Play Services on AVD / device | DES-01 (Google Fonts download) | Conditional | — | System fonts (graceful degradation; logged) |
| `androidx.compose.ui:ui-text-google-fonts` module | DES-01 | Not yet declared | via BOM 2026.03.00 | None required |
| JDK 17 | Compose compilation | ✓ (confirmed `app/build.gradle.kts:30-31`) | 17 | — |
| AGP | Build | ✓ | 8.13.0 (`libs.versions.toml:2`) | — |
| Compose compiler plugin | Compose build | ✓ | matches Kotlin 2.3.20 | — |

**Missing dependencies with no fallback:** none.
**Missing dependencies with fallback:** Google Play Services — if absent, downloadable fonts fail gracefully to system fonts. Acceptable for dev AVDs without GMS; flag in QA if encountered on production-target devices.

## Validation Architecture

Phase 8 is a foundation phase — no user-facing behaviour, so most requirements validate **structurally** (the tokens exist, the composable compiles, the values match handoff) rather than **behaviourally**. The Nyquist sampling strategy is unit-test-per-requirement at the data level, plus preview-based visual confirmation at the rendering level.

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit 4 (project baseline — see `app/src/test/java/com/giftregistry/...`) |
| Config file | `app/build.gradle.kts:102-105` (testImplementation entries: junit 4.13.2, coroutines-test, mockk, turbine) |
| Quick run command | `./gradlew :app:testDebugUnitTest --tests "com.giftregistry.ui.theme.*"` |
| Full suite command | `./gradlew :app:testDebugUnitTest` |
| Preview rendering | Android Studio Preview pane (manual, visual) — satisfies "preview or debug harness" clause in criteria 2-4 |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| DES-01 | InstrumentSerifFamily, InterFamily, JetBrainsMonoFamily are FontFamily instances backed by GoogleFont | unit | `./gradlew :app:testDebugUnitTest --tests com.giftregistry.ui.theme.GiftMaisonFontsTest` | ❌ Wave 0 — `app/src/test/java/com/giftregistry/ui/theme/GiftMaisonFontsTest.kt` |
| DES-01 | `giftMaisonFontProvider` uses correct provider authority and package | unit | same target | ❌ Wave 0 |
| DES-01 | font_certs.xml resource declares the array `com_google_android_gms_fonts_certs` | unit (resource assertion) | same target | ❌ Wave 0 — tests that `R.array.com_google_android_gms_fonts_certs` resolves |
| DES-02 | Each of 9 type roles (`displayXL`..`monoCaps`) has exact fontSize / letterSpacing / lineHeight per handoff | unit (data-equality table test) | `./gradlew :app:testDebugUnitTest --tests com.giftregistry.ui.theme.GiftMaisonTypographyTest` | ❌ Wave 0 — `app/src/test/java/com/giftregistry/ui/theme/GiftMaisonTypographyTest.kt` |
| DES-02 | Each type role uses the correct FontFamily | unit | same target | ❌ Wave 0 |
| DES-02 | Every TextStyle sets `includeFontPadding = false` (Pitfall 2 guard) | unit | same target | ❌ Wave 0 |
| DES-02 | Preview harness renders type scale without crash | manual (Android Studio preview) | — | ❌ Wave 0 — `app/src/main/java/com/giftregistry/ui/theme/preview/StyleGuidePreview.kt` |
| DES-03 | `housewarmingColors()` returns 13 tokens with exact sRGB hex matching handoff | unit (data-equality table test) | `./gradlew :app:testDebugUnitTest --tests com.giftregistry.ui.theme.GiftMaisonColorsTest` | ❌ Wave 0 — `app/src/test/java/com/giftregistry/ui/theme/GiftMaisonColorsTest.kt` |
| DES-03 | M3 `ColorScheme` slot mapping: `primary == accent`, `background == paper`, `error == warn`, etc. | unit | same target | ❌ Wave 0 |
| DES-03 | Preview harness renders palette swatches | manual (Android Studio preview) | — | ❌ Wave 0 — same style-guide file |
| DES-04 | `GiftMaisonShapes` exposes radii 8/10/12/14/16/22/999 as `RoundedCornerShape` / `CircleShape` | unit | `./gradlew :app:testDebugUnitTest --tests com.giftregistry.ui.theme.GiftMaisonShapesTest` | ❌ Wave 0 — `app/src/test/java/com/giftregistry/ui/theme/GiftMaisonShapesTest.kt` |
| DES-04 | `GiftMaisonSpacing` exposes named spacing values matching handoff (edge 16/20, gap 4-20) | unit | same target | ❌ Wave 0 |
| DES-04 | `fabShadow()`, `googleBannerShadow()`, `bottomSheetShadow()` Modifiers exist and accept shape argument | unit (API-shape test) | same target | ❌ Wave 0 |
| DES-04 | Preview harness renders radii + shadow samples | manual | — | ❌ Wave 0 |
| DES-05 | `GiftMaisonWordmark` composable is `@Composable`, accepts `Modifier` and `fontSize: TextUnit` | unit (reflection on function signature) | `./gradlew :app:testDebugUnitTest --tests com.giftregistry.ui.theme.GiftMaisonWordmarkTest` | ❌ Wave 0 — `app/src/test/java/com/giftregistry/ui/theme/GiftMaisonWordmarkTest.kt` |
| DES-05 | Wordmark builds a single `AnnotatedString` with two `SpanStyle`s (ink base + accent period) | unit (capture composition via Paparazzi or ComposeTestRule) — OR unit-test the `buildAnnotatedString` factory separately | same target | ❌ Wave 0 — factor the AnnotatedString builder into a `pure Kotlin fun wordmarkAnnotatedString(ink: Color, accent: Color): AnnotatedString` so it's unit-testable without compose-ui test runtime |
| DES-05 | Preview harness renders wordmark in a top bar context | manual | — | ❌ Wave 0 |

### Sampling Rate

- **Per task commit:** `./gradlew :app:testDebugUnitTest --tests "com.giftregistry.ui.theme.*"` (fast — pure unit tests, no Android framework)
- **Per wave merge:** `./gradlew :app:testDebugUnitTest` (full app test suite — ensures existing Phase 2-7 tests still pass after theme swap)
- **Phase gate:** Full suite green + manual Android Studio preview review of `StyleGuidePreview.kt` before `/gsd:verify-work`.

### Wave 0 Gaps

- [ ] `app/src/test/java/com/giftregistry/ui/theme/GiftMaisonFontsTest.kt` — covers DES-01
- [ ] `app/src/test/java/com/giftregistry/ui/theme/GiftMaisonTypographyTest.kt` — covers DES-02
- [ ] `app/src/test/java/com/giftregistry/ui/theme/GiftMaisonColorsTest.kt` — covers DES-03
- [ ] `app/src/test/java/com/giftregistry/ui/theme/GiftMaisonShapesTest.kt` — covers DES-04 (shapes + spacing + shadows)
- [ ] `app/src/test/java/com/giftregistry/ui/theme/GiftMaisonWordmarkTest.kt` — covers DES-05 (AnnotatedString shape)
- [ ] `app/src/main/java/com/giftregistry/ui/theme/preview/StyleGuidePreview.kt` — visual confirmation for criteria 2-4, and wordmark for criterion 5
- [ ] `app/src/main/res/values/font_certs.xml` — Google Fonts provider certificate hashes

**Manual validation checklist (cannot be automated):**
- [ ] Open `StyleGuidePreview.kt` in Android Studio — all 3 previews render without a "Problem" banner
- [ ] Type-scale preview: 9 rows render with expected families (serif for display, sans for body, mono caps)
- [ ] Palette preview: 13 swatches render with visibly distinct colours matching handoff screenshots
- [ ] Radii preview: 6 rounded boxes + 1 pill + 1 shadow-lifted circle render
- [ ] Run on AVD with Google Play Services — verify Instrument Serif renders as designed (curvy serif, not system sans fallback)

## Sources

### Primary (HIGH confidence)

- [Using fonts in Compose — Android Developers](https://developer.android.com/develop/ui/compose/text/fonts) — GoogleFont API, provider setup, font_certs, async pattern, FontFamily fallback, letter-spacing + em support
- [Custom design systems in Compose — Android Developers](https://developer.android.com/develop/ui/compose/designsystems/custom) — CompositionLocal + Immutable data class + Theme object pattern; guidance on wrapping vs replacing MaterialTheme
- [Anatomy of a theme in Compose — Android Developers](https://developer.android.com/develop/ui/compose/designsystems/anatomy) — cross-reference for Theme composable structure
- [Style paragraph | Jetpack Compose — Android Developers](https://developer.android.com/develop/ui/compose/text/style-paragraph) — em-based lineHeight, `LineHeightStyle`, `includeFontPadding`
- [Style text | Jetpack Compose — Android Developers](https://developer.android.com/develop/ui/compose/text/style-text) — AnnotatedString + SpanStyle for inline styled spans
- [Compose UI 1.10.6 release notes — Android Developers](https://developer.android.com/jetpack/androidx/releases/compose-ui) — confirmed 1.10.6 released March 2026, aligned with BOM 2026.03.00
- Local file: `design_handoff/design_handoff_android_owner_flow/README.md` — authoritative tokens, type scale, and wordmark spec
- Local file: `design_handoff/design_handoff_android_owner_flow/reference/theme.jsx` — source-of-truth oklch values for Housewarming

### Secondary (MEDIUM confidence)

- [Jetchat sample — font_certs.xml reference](https://github.com/android/compose-samples/blob/main/Jetchat/app/src/main/res/values-v23/font_certs.xml) — referenced by official docs for Google Fonts cert hashes
- [Instrument Serif on Google Fonts](https://fonts.google.com/specimen/Instrument+Serif) — availability confirmed; Regular + Italic styles available (single weight 400)
- [Jetpack Compose Theming — Stefano Natali (ProAndroidDev)](https://proandroiddev.com/mastering-color-theming-in-jetpack-compose-a4ac53b9b7b5) — extendedColors CompositionLocal pattern
- [Compose Multiplatform custom design system — Kashif Mehmood (ProAndroidDev)](https://proandroiddev.com/building-a-custom-design-system-in-compose-multiplatform-6f5f42f06fa0) — wraps-MaterialTheme pattern

### Tertiary (LOW confidence — kept for reference)

- [OKLCH in CSS: why we moved — Evil Martians](https://evilmartians.com/chronicles/oklch-in-css-why-quit-rgb-hsl) — context on oklch vs hex; the hex fallback choice is not controversial
- [Oklab color space — Wikipedia](https://en.wikipedia.org/wiki/Oklab_color_space) — confirms Oklab is cartesian L/a/b; Oklch is the polar form; Compose's `ColorSpaces.Oklab` is the cartesian form, requires manual polar-to-cartesian conversion for oklch source values

## Metadata

**Confidence breakdown:**

- Standard stack: HIGH — every library already in project or is a BOM-tracked sibling
- Architecture (wrap MaterialTheme + CompositionLocal tokens): HIGH — canonical pattern, documented by Android Developers, proven across industry Compose apps
- Pitfalls: HIGH — Pitfalls 2, 3, 5, 7 are known industry gotchas with documented fixes; Pitfalls 1, 4, 6 are specific-to-this-project traps surfaced by the existing-code audit
- Validation architecture: HIGH — no framework research needed (JUnit4 already in place); just wiring the token-level assertions

**Research date:** 2026-04-20
**Valid until:** 2026-07-20 (3 months — fonts + colour pipeline is stable; only Compose BOM bump would warrant a refresh)
