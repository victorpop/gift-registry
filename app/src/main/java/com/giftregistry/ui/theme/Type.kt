package com.giftregistry.ui.theme

import androidx.compose.material3.Typography

/**
 * Material3 Typography mapped from the GiftMaison type scale.
 *
 * Existing Phase 2-7 screens read `MaterialTheme.typography.headlineLarge` /
 * `.titleLarge` / `.bodyLarge` / `.labelLarge`. This mapping re-skins them onto
 * the Housewarming scale. Phase 9+ screens that need bespoke roles (bodyXS,
 * monoCaps with custom letter-spacing) should read `GiftMaisonTheme.typography.*`
 * directly.
 */
private val gm = giftMaisonTypography()

val GiftRegistryTypography: Typography = Typography(
    displayLarge = gm.displayXL,
    headlineLarge = gm.displayL,
    headlineMedium = gm.displayM,
    titleLarge = gm.displayS,
    bodyLarge = gm.bodyL,
    bodyMedium = gm.bodyM,
    bodySmall = gm.bodyS,
    labelLarge = gm.bodyMEmphasis,
    labelSmall = gm.monoCaps,
)
