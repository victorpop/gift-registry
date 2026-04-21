package com.giftregistry.ui.item.add

/**
 * SCR-10: Affiliate confirmation row visibility predicate.
 *
 * CONTEXT.md § Affiliate confirmation row: "shown only when OG fetch succeeded
 * AND the URL domain matches AffiliateUrlTransformer.merchantRules … Omitted
 * entirely for non-matching URLs — no misleading 'tag applied' for unknown
 * merchants."
 *
 * All three conditions must be true: url non-blank, OG fetch succeeded,
 * and isAffiliateDomain (from AffiliateUrlTransformer.isAffiliateDomain).
 *
 * Unit-tested by AffiliateRowVisibilityTest (Wave 0).
 */
fun shouldShowAffiliateRow(
    url: String?,
    isAffiliateDomain: Boolean,
    ogFetchSucceeded: Boolean,
): Boolean = !url.isNullOrBlank() && ogFetchSucceeded && isAffiliateDomain
