package com.giftregistry.ui.common.status

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * STAT-04: purchased item row renders at 55% opacity (`alpha(0.55f)`) at the
 * row level. Image grayscale/tint and title strikethrough are applied at
 * element level by the caller (per UI-SPEC split-responsibility note).
 *
 * Regression guard: the row must REMAIN VISIBLE (handoff "trust pattern" —
 * givers who later view the page must still see purchased items so no one
 * double-buys). alpha must be strictly between 0 and 1.
 *
 * RED in Wave 0 — flips GREEN when Plan 02 ships `const val PURCHASED_ROW_ALPHA`
 * and `fun Modifier.purchasedVisualTreatment(): Modifier` in PurchasedRowModifier.kt.
 */
class PurchasedRowModifierTest {
    @Test fun alpha_is055() =
        assertEquals(0.55f, PURCHASED_ROW_ALPHA, 0.0001f)

    @Test fun alpha_isNotFullyHidden() =
        assertTrue("Row must remain visible per STAT-04 trust pattern", PURCHASED_ROW_ALPHA > 0f)

    @Test fun alpha_isNotFullyVisible() =
        assertTrue("Row must be de-emphasised per STAT-04", PURCHASED_ROW_ALPHA < 1f)
}
