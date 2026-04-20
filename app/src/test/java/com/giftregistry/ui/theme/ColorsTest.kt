package com.giftregistry.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * DES-03: Housewarming colour palette (13 tokens) as sRGB hex matching handoff.
 * Values per design_handoff/design_handoff_android_owner_flow/README.md "Colour tokens" table.
 */
class ColorsTest {
    private val c = housewarmingColors()

    @Test fun paper_isCreamWhite() = assertEquals(Color(0xFFF7F2E9), c.paper)
    @Test fun paperDeep_isWarmerCard() = assertEquals(Color(0xFFEDE5D5), c.paperDeep)
    @Test fun ink_isSoftNearBlack() = assertEquals(Color(0xFF2A2420), c.ink)
    @Test fun inkSoft_isBodyText() = assertEquals(Color(0xFF6A5E52), c.inkSoft)
    @Test fun inkFaint_isPlaceholder() = assertEquals(Color(0xFF9C8E7F), c.inkFaint)
    @Test fun line_isBorderColor() = assertEquals(Color(0xFFDDD4C4), c.line)
    @Test fun accent_isTerracotta() = assertEquals(Color(0xFFC8623A), c.accent)
    @Test fun accentInk_isTextOnAccent() = assertEquals(Color(0xFFFCF8EF), c.accentInk)
    @Test fun accentSoft_isAccentBg() = assertEquals(Color(0xFFF3DED0), c.accentSoft)
    @Test fun second_isDeepOlive() = assertEquals(Color(0xFF4F7050), c.second)
    @Test fun secondSoft_isOliveBg() = assertEquals(Color(0xFFD7E2CE), c.secondSoft)
    @Test fun ok_isSuccessGreen() = assertEquals(Color(0xFF4F9668), c.ok)
    @Test fun warn_isWarningAmber() = assertEquals(Color(0xFFD29447), c.warn)
}
