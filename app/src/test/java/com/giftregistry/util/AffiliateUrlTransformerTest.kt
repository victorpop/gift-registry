package com.giftregistry.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AffiliateUrlTransformerTest {

    @Test
    fun `EMAG URL is transformed to 2Performant tracking URL`() {
        val result = AffiliateUrlTransformer.transform("https://www.emag.ro/product-123")
        assertTrue(result.wasTransformed)
        assertTrue(result.affiliateUrl.contains("event.2performant.com/events/click"))
        assertTrue(result.affiliateUrl.contains("emag.ro%2Fproduct-123"))
        assertEquals("https://www.emag.ro/product-123", result.originalUrl)
        assertEquals("emag.ro", result.merchantName)
    }

    @Test
    fun `EMAG mobile subdomain is also transformed`() {
        val result = AffiliateUrlTransformer.transform("https://m.emag.ro/product")
        assertTrue(result.wasTransformed)
        assertTrue(result.affiliateUrl.contains("event.2performant.com"))
    }

    @Test
    fun `unknown merchant URL passes through unchanged`() {
        val url = "https://www.amazon.com/some-product"
        val result = AffiliateUrlTransformer.transform(url)
        assertFalse(result.wasTransformed)
        assertEquals(url, result.affiliateUrl)
        assertEquals(url, result.originalUrl)
        assertNull(result.merchantName)
    }

    @Test
    fun `empty URL passes through unchanged`() {
        val result = AffiliateUrlTransformer.transform("")
        assertFalse(result.wasTransformed)
        assertEquals("", result.affiliateUrl)
    }

    @Test
    fun `malformed URL without scheme passes through unchanged`() {
        val result = AffiliateUrlTransformer.transform("not-a-url")
        assertFalse(result.wasTransformed)
        assertEquals("not-a-url", result.affiliateUrl)
    }
}
