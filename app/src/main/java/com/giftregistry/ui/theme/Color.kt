package com.giftregistry.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Light theme (60-30-10 rule per UI-SPEC)
val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6750A4),           // Accent 10%
    onPrimary = Color(0xFFFFFFFF),
    background = Color(0xFFFFFBFE),        // Dominant 60%
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFBFE),
    surfaceVariant = Color(0xFFE7E0EC),    // Secondary 30%
    onSurface = Color(0xFF1C1B1F),
    onSurfaceVariant = Color(0xFF49454F),
    error = Color(0xFFB3261E),             // Destructive
    onError = Color(0xFFFFFFFF)
)

// Dark theme
val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),           // Accent 10%
    onPrimary = Color(0xFF381E72),
    background = Color(0xFF1C1B1F),        // Dominant 60%
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFF49454F),    // Secondary 30%
    onSurface = Color(0xFFE6E1E5),
    onSurfaceVariant = Color(0xFFCAC4D0),
    error = Color(0xFFF2B8B5),             // Destructive
    onError = Color(0xFF601410)
)
