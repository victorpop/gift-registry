package com.giftregistry.domain.model

/**
 * Metadata scraped from a product page by the fetchOgMetadata Cloud Function.
 *
 * [price] is the pre-formatted display string (e.g. "459,00 RON") and is what
 * the item form's price field auto-fills with. [priceAmount] and [priceCurrency]
 * are the structured equivalents: a numeric string in the site's original locale
 * format and an ISO 4217 code. Both structured fields may be null when the
 * source page doesn't expose enough information to split them.
 */
data class OgMetadata(
    val title: String? = null,
    val imageUrl: String? = null,
    val price: String? = null,
    val priceAmount: String? = null,
    val priceCurrency: String? = null,
)
