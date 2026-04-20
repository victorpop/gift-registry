package com.giftregistry.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * DES-01: Instrument Serif, Inter, JetBrains Mono available as Compose FontFamily via Google Fonts.
 * Tests verify the shared GoogleFont.Provider values and that each FontFamily is non-null.
 */
class FontsTest {
    @Test fun providerAuthority_isGmsFonts() =
        assertEquals("com.google.android.gms.fonts", giftMaisonFontProvider.providerAuthority)

    @Test fun providerPackage_isGms() =
        assertEquals("com.google.android.gms", giftMaisonFontProvider.providerPackage)

    @Test fun instrumentSerifFamily_exists() = assertNotNull(InstrumentSerifFamily)
    @Test fun interFamily_exists() = assertNotNull(InterFamily)
    @Test fun jetBrainsMonoFamily_exists() = assertNotNull(JetBrainsMonoFamily)
}
