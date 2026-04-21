package com.giftregistry.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * Top-level theme composable. Wraps (does not replace) `MaterialTheme` so every
 * existing `MaterialTheme.colorScheme.*` / `.typography.*` call site continues to
 * work while gaining Housewarming re-skin for free.
 *
 * Provides four CompositionLocals for Phase 9+ screens that consume bespoke
 * GiftMaison tokens (e.g. `inkFaint`, `accentSoft`, `monoCaps`). Access via
 * `GiftMaisonTheme.colors.accent` etc.
 *
 * v1.1 forces light mode (Pitfall 4). `darkTheme` is preserved for source-compat
 * with `MainActivity.kt:48` but is ignored.
 */
@Composable
fun GiftRegistryTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = housewarmingColors()
    val typography = giftMaisonTypography()
    val shapes = giftMaisonShapes()
    val spacing = giftMaisonSpacing()

    CompositionLocalProvider(
        LocalGiftMaisonColors provides colors,
        LocalGiftMaisonTypography provides typography,
        LocalGiftMaisonShapes provides shapes,
        LocalGiftMaisonSpacing provides spacing,
    ) {
        MaterialTheme(
            colorScheme = LightColorScheme,
            typography = GiftRegistryTypography,
            content = content,
        )
    }
}

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
