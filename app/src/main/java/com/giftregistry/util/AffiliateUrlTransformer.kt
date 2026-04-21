package com.giftregistry.util

import java.net.URI
import java.net.URLEncoder

data class TransformResult(
    val originalUrl: String,
    val affiliateUrl: String,
    val merchantName: String?,
    val wasTransformed: Boolean
)

object AffiliateUrlTransformer {

    // Placeholder affiliate IDs — replace with BuildConfig fields sourced from local.properties
    private const val AFFILIATE_UNIQUE_ID = "PLACEHOLDER_UNIQUE_ID"
    private const val AFFILIATE_CODE = "PLACEHOLDER_AFF_CODE"
    private const val EMAG_CAMPAIGN_ID = "PLACEHOLDER_CAMPAIGN_ID"

    private val merchantRules: Map<String, (String) -> String> = mapOf(
        "emag.ro" to ::buildEmagAffiliateUrl
    )

    fun transform(url: String): TransformResult {
        if (url.isBlank()) return noMatch(url)
        val domain = extractDomain(url) ?: return noMatch(url)
        val builder = merchantRules.entries.firstOrNull { domain.endsWith(it.key) }?.value
            ?: return noMatch(url)
        val merchantName = merchantRules.keys.firstOrNull { domain.endsWith(it) }
        return TransformResult(
            originalUrl = url,
            affiliateUrl = builder(url),
            merchantName = merchantName,
            wasTransformed = true
        )
    }

    private fun extractDomain(url: String): String? {
        return try {
            URI(url).host?.lowercase()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Phase 11 SCR-10 accessor: returns true when [url]'s host matches any entry
     * in [merchantRules] (currently `"emag.ro"`). Mirrors the domain check in
     * [transform] without invoking the URL builder — the Add item screen uses this
     * to decide whether to show the affiliate confirmation row before save.
     *
     * Returns false for blank / malformed URLs and for hosts that do not match
     * any merchant rule. Case-insensitive (extractDomain lowercases).
     */
    fun isAffiliateDomain(url: String): Boolean {
        if (url.isBlank()) return false
        val domain = extractDomain(url) ?: return false
        return merchantRules.keys.any { domain.endsWith(it) }
    }

    private fun noMatch(url: String) = TransformResult(url, url, null, false)

    private fun buildEmagAffiliateUrl(productUrl: String): String {
        val encoded = URLEncoder.encode(productUrl, "UTF-8")
        return "https://event.2performant.com/events/click" +
                "?ad_type=product_store" +
                "&unique=$AFFILIATE_UNIQUE_ID" +
                "&aff_code=$AFFILIATE_CODE" +
                "&campaign_unique=$EMAG_CAMPAIGN_ID" +
                "&redirect_to=$encoded"
    }
}
