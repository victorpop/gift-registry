package com.giftregistry.ui.registry.cover

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 12 — Wave 0 RED tests for [resolveImageModel].
 *
 * Pins (D-05 / D-14 / D-16): the function returns the right Coil 3 model
 * type based on the `imageUrl` shape:
 *   - null -> null (placeholder branch in HeroImageOrPlaceholder)
 *   - http(s) URL -> the String unchanged (Coil OkHttp fetcher)
 *   - `preset:...` sentinel -> Int? from PresetCatalog.resolve (R.drawable.*)
 *   - malformed sentinel -> null (NOT the raw string — fail-safe to placeholder)
 *
 * Wave 0 stub returns null unconditionally; the URL + sentinel branches fail
 * RED. Plan 03 ships the real `when` block.
 */
class ResolveImageModelTest {

    /** D-14: null imageUrl renders the gradient + glyph placeholder. */
    @Test
    fun resolveImageModel_null_returnsNull() {
        assertNull(resolveImageModel(null))
    }

    /** D-05: HTTP URL passes through as String for Coil OkHttp fetcher. */
    @Test
    fun resolveImageModel_httpUrl_returnsString() {
        val url = "https://example.com/cover.jpg"
        val model = resolveImageModel(url)
        assertTrue(
            "HTTP URL must remain a String so Coil routes through OkHttp",
            model is String,
        )
        assertEquals(url, model)
    }

    /** D-05: preset sentinel resolves to the matching `R.drawable.*` Int via PresetCatalog. */
    @Test
    fun resolveImageModel_validPresetSentinel_returnsInt() {
        val sentinel = "preset:Wedding:3"
        val model = resolveImageModel(sentinel)
        assertTrue(
            "Preset sentinel must resolve to Int (R.drawable.*) via PresetCatalog.resolve (D-05)",
            model is Int,
        )
        assertEquals(
            "resolveImageModel must delegate to PresetCatalog.resolve",
            PresetCatalog.resolve(sentinel),
            model,
        )
    }

    /** D-05: malformed sentinel falls through to null (placeholder), NOT the raw string. */
    @Test
    fun resolveImageModel_malformedSentinel_returnsNull_notRawString() {
        // "preset:Bogus:9" starts with "preset:" but Bogus is not a known occasion
        // AND 9 is out of range. resolveImageModel must return null (placeholder),
        // NOT the literal string "preset:Bogus:9" (which would let Coil 3 try to
        // load it as a URL and break).
        assertNull(
            "Malformed sentinel must NOT fall through to String — UI would render a broken image otherwise",
            resolveImageModel("preset:Bogus:9"),
        )
    }
}
