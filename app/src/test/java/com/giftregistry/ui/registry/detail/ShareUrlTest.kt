package com.giftregistry.ui.registry.detail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * SCR-08: Share banner URL format.
 *
 *   shareUrlOf(registryId) == "https://gift-registry-ro.web.app/r/${registryId}"
 *
 * CONTEXT.md § Share banner locks the exact host + /r/ path. The host matches
 * Firebase Hosting production target confirmed in the Phase 6 quick-task
 * 260420-nh8 (email invite URL fix).
 *
 * RED in Wave 0 — flips GREEN when Plan 02 ships ShareUrl.kt with
 * `fun shareUrlOf(registryId: String): String`.
 */
class ShareUrlTest {
    @Test fun returnsCorrectHostAndPath() =
        assertEquals("https://gift-registry-ro.web.app/r/abc123", shareUrlOf("abc123"))

    @Test fun usesHttpsScheme() =
        assertTrue(shareUrlOf("abc123").startsWith("https://"))

    @Test fun usesCanonicalHost() =
        assertTrue(shareUrlOf("abc123").contains("gift-registry-ro.web.app"))

    @Test fun placesRegistryIdAfterRSegment() =
        assertTrue(shareUrlOf("my-registry-42").endsWith("/r/my-registry-42"))

    @Test fun doesNotUrlEncodeRegistryId() {
        // Firestore auto-generated IDs never contain characters that need URL encoding
        // (they're base-62). This guards against accidental URLEncoder.encode() usage
        // that would break the web fallback router's /r/:id param match.
        val result = shareUrlOf("abc 123")
        assertTrue(
            "Expected literal 'abc 123' in share URL but got: $result",
            result.contains("abc 123"),
        )
    }
}
