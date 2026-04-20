package com.giftregistry.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * GiftMaison v1.1 colour tokens — Housewarming palette.
 *
 * Source-of-truth: `design_handoff/design_handoff_android_owner_flow/README.md`
 * "Colour tokens (Housewarming)" table. Values are sRGB hex approximations of the
 * oklch source values in `reference/theme.jsx` — per handoff: "On Android, convert
 * each token to sRGB hex in your colour resources — the occasion-switching logic
 * doesn't need oklch at runtime."
 *
 * v1.1 ships Housewarming ONLY. THEME-01/02/03 (Wedding/Baby/Birthday palettes +
 * per-registry runtime cascade) are deferred to v1.2.
 *
 * Do NOT hardcode these Color values on screens. Read via
 * `GiftMaisonTheme.colors.*` (wired up in Plan 05 / Theme.kt).
 */
@Immutable
data class GiftMaisonColors(
    /** Page bg, elevated surfaces, pills-on-dark. Cream white. */
    val paper: Color,
    /** Cards, segmented-track bg, field bg. Warmer card surface. */
    val paperDeep: Color,
    /** Primary text, primary buttons. Soft near-black. */
    val ink: Color,
    /** Body text, secondary labels. */
    val inkSoft: Color,
    /** Placeholders, tertiary text, inactive icons. */
    val inkFaint: Color,
    /** Borders, dividers. */
    val line: Color,
    /** Terracotta — brand accent, selected states, Reserved. */
    val accent: Color,
    /** Text on accent surfaces. */
    val accentInk: Color,
    /** Accent backgrounds, selected-tab pill. */
    val accentSoft: Color,
    /** Olive — Given state, avatar bg. */
    val second: Color,
    /** Olive backgrounds, info pills. */
    val secondSoft: Color,
    /** Success text (e.g. affiliate tag line). */
    val ok: Color,
    /** Warnings. Unused on Housewarming owner flows today; reserved for future. */
    val warn: Color,
)

/**
 * Factory for the Housewarming palette. The 13 sRGB hex values are the
 * contract; see `ColorsTest` for per-token verification.
 */
fun housewarmingColors(): GiftMaisonColors = GiftMaisonColors(
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

/**
 * CompositionLocal for GiftMaison colours. The default is populated with
 * Color.Unspecified to crash-loudly if a consumer reads colours outside of
 * a `GiftRegistryTheme { ... }` scope (rather than silently rendering black).
 *
 * Providers: `GiftRegistryTheme` in Theme.kt (Plan 05).
 * Consumers: `GiftMaisonTheme.colors.*` accessor in Theme.kt (Plan 05).
 */
val LocalGiftMaisonColors = staticCompositionLocalOf {
    GiftMaisonColors(
        paper = Color.Unspecified,
        paperDeep = Color.Unspecified,
        ink = Color.Unspecified,
        inkSoft = Color.Unspecified,
        inkFaint = Color.Unspecified,
        line = Color.Unspecified,
        accent = Color.Unspecified,
        accentInk = Color.Unspecified,
        accentSoft = Color.Unspecified,
        second = Color.Unspecified,
        secondSoft = Color.Unspecified,
        ok = Color.Unspecified,
        warn = Color.Unspecified,
    )
}
