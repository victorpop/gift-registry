package com.giftregistry.ui.theme

import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * DES-02: Type scale with handoff-specified sizes, weights, letter-spacing, line-heights.
 * Values per design_handoff/design_handoff_android_owner_flow/README.md "Type scale" table.
 */
class TypographyTest {
    private val t = giftMaisonTypography()

    @Test fun displayXL_is32spSerifW400() {
        assertEquals(32.sp, t.displayXL.fontSize)
        assertEquals(FontWeight.Normal, t.displayXL.fontWeight)
        assertEquals((-0.8).em, t.displayXL.letterSpacing)
        assertEquals(1.0.em, t.displayXL.lineHeight)
        assertEquals(InstrumentSerifFamily, t.displayXL.fontFamily)
    }

    @Test fun displayL_is24spSerifW400() {
        assertEquals(24.sp, t.displayL.fontSize)
        assertEquals((-0.4).em, t.displayL.letterSpacing)
        assertEquals(1.05.em, t.displayL.lineHeight)
    }

    @Test fun displayM_is22spSerif() {
        assertEquals(22.sp, t.displayM.fontSize)
        assertEquals((-0.4).em, t.displayM.letterSpacing)
        assertEquals(1.1.em, t.displayM.lineHeight)
    }

    @Test fun displayS_is18spSerif() {
        assertEquals(18.sp, t.displayS.fontSize)
        assertEquals((-0.3).em, t.displayS.letterSpacing)
        assertEquals(1.1.em, t.displayS.lineHeight)
    }

    @Test fun bodyL_is15spInterW500() {
        assertEquals(15.sp, t.bodyL.fontSize)
        assertEquals(FontWeight.Medium, t.bodyL.fontWeight)
        assertEquals((-0.2).em, t.bodyL.letterSpacing)
        assertEquals(1.35.em, t.bodyL.lineHeight)
        assertEquals(InterFamily, t.bodyL.fontFamily)
    }

    @Test fun bodyM_is135spInterW400() {
        assertEquals(13.5.sp, t.bodyM.fontSize)
        assertEquals(FontWeight.Normal, t.bodyM.fontWeight)
        assertEquals((-0.1).em, t.bodyM.letterSpacing)
        assertEquals(1.45.em, t.bodyM.lineHeight)
    }

    @Test fun bodyMEmphasis_isW500() {
        assertEquals(FontWeight.Medium, t.bodyMEmphasis.fontWeight)
        assertEquals(13.5.sp, t.bodyMEmphasis.fontSize)
    }

    @Test fun bodyS_is125sp() {
        assertEquals(12.5.sp, t.bodyS.fontSize)
        assertEquals(0.em, t.bodyS.letterSpacing)
        assertEquals(1.4.em, t.bodyS.lineHeight)
    }

    @Test fun bodyXS_is115sp() {
        assertEquals(11.5.sp, t.bodyXS.fontSize)
        assertEquals(0.em, t.bodyXS.letterSpacing)
        assertEquals(1.35.em, t.bodyXS.lineHeight)
    }

    @Test fun monoCaps_is95spMonoW500() {
        assertEquals(9.5.sp, t.monoCaps.fontSize)
        assertEquals(FontWeight.Medium, t.monoCaps.fontWeight)
        assertEquals(1.3.em, t.monoCaps.lineHeight)
        assertEquals(JetBrainsMonoFamily, t.monoCaps.fontFamily)
    }

    @Test fun everyRole_disablesFontPadding_pitfall2() {
        // Guard against Pitfall 2 (em lineHeight inflated by default font padding).
        val roles = listOf(
            t.displayXL, t.displayL, t.displayM, t.displayS,
            t.bodyL, t.bodyM, t.bodyMEmphasis, t.bodyS, t.bodyXS, t.monoCaps,
        )
        roles.forEach { style ->
            val platform = style.platformStyle
            assertEquals(PlatformTextStyle(includeFontPadding = false), platform)
        }
    }
}
