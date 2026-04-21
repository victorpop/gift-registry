package com.giftregistry.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme

/**
 * Material3 ColorScheme mapped from the GiftMaison Housewarming palette.
 *
 * The 42+ existing call sites (Phase 2-7) read through `MaterialTheme.colorScheme.*`
 * — this mapping re-skins them onto Housewarming tokens without touching screens.
 * v1.1 forces light mode (Pitfall 4); DarkColorScheme retained structurally.
 */

private val gm = housewarmingColors()

val LightColorScheme = lightColorScheme(
    primary = gm.accent,
    onPrimary = gm.accentInk,
    primaryContainer = gm.accentSoft,
    onPrimaryContainer = gm.ink,
    secondary = gm.second,
    onSecondary = gm.accentInk,
    secondaryContainer = gm.secondSoft,
    onSecondaryContainer = gm.ink,
    background = gm.paper,
    onBackground = gm.ink,
    surface = gm.paper,
    surfaceVariant = gm.paperDeep,
    onSurface = gm.ink,
    onSurfaceVariant = gm.inkSoft,
    outline = gm.line,
    outlineVariant = gm.line,
    error = gm.warn,
    onError = gm.accentInk,
)

val DarkColorScheme = darkColorScheme(
    primary = gm.accent,
    onPrimary = gm.accentInk,
    primaryContainer = gm.accentSoft,
    onPrimaryContainer = gm.ink,
    secondary = gm.second,
    onSecondary = gm.accentInk,
    secondaryContainer = gm.secondSoft,
    onSecondaryContainer = gm.ink,
    background = gm.paper,
    onBackground = gm.ink,
    surface = gm.paper,
    surfaceVariant = gm.paperDeep,
    onSurface = gm.ink,
    onSurfaceVariant = gm.inkSoft,
    outline = gm.line,
    outlineVariant = gm.line,
    error = gm.warn,
    onError = gm.accentInk,
)
