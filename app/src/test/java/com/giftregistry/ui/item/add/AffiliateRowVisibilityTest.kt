package com.giftregistry.ui.item.add

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * SCR-10: Affiliate confirmation row visibility predicate.
 *
 *   shown = !url.isNullOrBlank() && ogFetchSucceeded && isAffiliateDomain
 *
 * CONTEXT.md § Affiliate confirmation row: "shown only when OG fetch succeeds
 * AND the URL domain matches AffiliateUrlTransformer.merchantRules (the Phase 3
 * list: emag.ro + any added merchants). Omitted entirely for non-matching URLs
 * — no misleading 'tag applied' for unknown merchants."
 *
 * `isAffiliateDomain` is passed in as a plain Boolean so the predicate stays
 * Compose/DI-free. The real domain check lives in AffiliateUrlTransformer.
 * isAffiliateDomain(url) (Plan 02 adds this helper).
 *
 * RED in Wave 0 — flips GREEN when Plan 02 ships AffiliateRowVisibility.kt with
 * `fun shouldShowAffiliateRow(url: String?, isAffiliateDomain: Boolean, ogFetchSucceeded: Boolean): Boolean`
 * in com.giftregistry.ui.item.add.
 */
class AffiliateRowVisibilityTest {

    @Test fun hidden_whenUrlIsNull() =
        assertFalse(shouldShowAffiliateRow(url = null, isAffiliateDomain = true, ogFetchSucceeded = true))

    @Test fun hidden_whenUrlIsBlank() {
        assertFalse(shouldShowAffiliateRow(url = "", isAffiliateDomain = true, ogFetchSucceeded = true))
        assertFalse(shouldShowAffiliateRow(url = "   ", isAffiliateDomain = true, ogFetchSucceeded = true))
    }

    @Test fun hidden_whenOgFetchFailed() =
        assertFalse(
            shouldShowAffiliateRow(
                url = "https://emag.ro/product/x",
                isAffiliateDomain = true,
                ogFetchSucceeded = false,
            )
        )

    @Test fun hidden_whenNonAffiliateDomain() =
        assertFalse(
            "Unknown merchant URLs must NOT show the affiliate row (CONTEXT.md § Affiliate confirmation row)",
            shouldShowAffiliateRow(
                url = "https://example.com/product/x",
                isAffiliateDomain = false,
                ogFetchSucceeded = true,
            )
        )

    @Test fun shown_whenAllThreeConditionsTrue() =
        assertTrue(
            shouldShowAffiliateRow(
                url = "https://emag.ro/product/x",
                isAffiliateDomain = true,
                ogFetchSucceeded = true,
            )
        )

    @Test fun hidden_whenBothConditionsFalse() =
        assertFalse(
            shouldShowAffiliateRow(
                url = "https://example.com",
                isAffiliateDomain = false,
                ogFetchSucceeded = false,
            )
        )
}
