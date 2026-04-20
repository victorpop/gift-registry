package com.giftregistry.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * DES-04: Spacing, radii (8/10/12/14/16/22/999), and shadow specs.
 * Values per design_handoff/design_handoff_android_owner_flow/README.md "Spacing / radii / shadows" section.
 */
class ShapesAndDimensTest {
    private val s = giftMaisonShapes()
    private val sp = giftMaisonSpacing()

    @Test fun radius8_isThumbnail() = assertEquals(RoundedCornerShape(8.dp), s.radius8)
    @Test fun radius10_isSmallCard() = assertEquals(RoundedCornerShape(10.dp), s.radius10)
    @Test fun radius12_isInput() = assertEquals(RoundedCornerShape(12.dp), s.radius12)
    @Test fun radius14_isTile() = assertEquals(RoundedCornerShape(14.dp), s.radius14)
    @Test fun radius16_isCard() = assertEquals(RoundedCornerShape(16.dp), s.radius16)
    @Test fun radius22_isBottomSheet() = assertEquals(RoundedCornerShape(22.dp), s.radius22)
    @Test fun radius999_isPill() = assertEquals(CircleShape, s.pill)

    @Test fun edgePadding_is16dp() = assertEquals(16.dp, sp.edge)
    @Test fun edgePaddingWide_is20dp() = assertEquals(20.dp, sp.edgeWide)
    @Test fun gap4_exists() = assertEquals(4.dp, sp.gap4)
    @Test fun gap6_exists() = assertEquals(6.dp, sp.gap6)
    @Test fun gap8_exists() = assertEquals(8.dp, sp.gap8)
    @Test fun gap10_exists() = assertEquals(10.dp, sp.gap10)
    @Test fun gap12_exists() = assertEquals(12.dp, sp.gap12)
    @Test fun gap14_exists() = assertEquals(14.dp, sp.gap14)
    @Test fun gap16_exists() = assertEquals(16.dp, sp.gap16)
    @Test fun gap20_exists() = assertEquals(20.dp, sp.gap20)

    @Test fun shadows_fabElevation_is20dp() =
        assertEquals(20.dp, GiftMaisonShadows.fabElevation)
    @Test fun shadows_googleBannerElevation_is24dp() =
        assertEquals(24.dp, GiftMaisonShadows.googleBannerElevation)
    @Test fun shadows_bottomSheetElevation_is40dp() =
        assertEquals(40.dp, GiftMaisonShadows.bottomSheetElevation)
}
