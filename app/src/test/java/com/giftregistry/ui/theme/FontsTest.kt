package com.giftregistry.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * DES-01: Instrument Serif, Inter, JetBrains Mono available as Compose FontFamily via Google Fonts.
 *
 * GoogleFont.Provider's `providerAuthority` / `providerPackage` properties are `internal` in
 * androidx.compose.ui:ui-text-google-fonts 1.10.5, so tests assert against the module-level
 * constants the provider reads from (GMS_FONTS_AUTHORITY / GMS_FONTS_PACKAGE in
 * GiftMaisonFonts.kt) plus existence of the provider + families.
 */
class FontsTest {
    @Test fun providerAuthority_isGmsFonts() =
        assertEquals("com.google.android.gms.fonts", GMS_FONTS_AUTHORITY)

    @Test fun providerPackage_isGms() =
        assertEquals("com.google.android.gms", GMS_FONTS_PACKAGE)

    @Test fun fontProvider_exists() = assertNotNull(giftMaisonFontProvider)
    @Test fun instrumentSerifFamily_exists() = assertNotNull(InstrumentSerifFamily)
    @Test fun interFamily_exists() = assertNotNull(InterFamily)
    @Test fun jetBrainsMonoFamily_exists() = assertNotNull(JetBrainsMonoFamily)
}
