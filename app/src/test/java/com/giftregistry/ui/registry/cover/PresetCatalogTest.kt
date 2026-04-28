package com.giftregistry.ui.registry.cover

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 12 — Wave 0 RED tests for [PresetCatalog].
 *
 * Pins:
 * - D-02: 6 presets per occasion × 6 occasions in OccasionCatalog order.
 * - D-05: Sentinel encoding `preset:{occasion}:{index}` persisted on
 *         `Registry.imageUrl`; resolve() roundtrips back to a R.drawable Int.
 *
 * Wave 0 stub returns `emptyList()` from presetsFor and `null` from resolve,
 * so these tests fail RED on the size and non-null assertions. Plan 02
 * wires the real R.drawable.preset_*_* table, flipping these GREEN.
 *
 * `encode` is final-shape in Wave 0 — those assertions PASS immediately.
 *
 * Imports the existing `OccasionCatalog` (Phase 11) so the legacy alias
 * test (`baby shower` -> `Baby`) wires through `storageKeyFor` automatically.
 */
class PresetCatalogTest {

    /** D-02: each occasion exposes exactly 6 preset drawables. */
    @Test
    fun presetsFor_wedding_returnsSixDrawables() {
        val list = PresetCatalog.presetsFor("Wedding")
        assertEquals("Wedding occasion must expose 6 presets per D-02", 6, list.size)
    }

    /** D-02 + Phase 11 OccasionCatalog legacy alias map: `baby shower` canonicalises to `Baby`. */
    @Test
    fun presetsFor_legacyBabyShowerAlias_resolvesToBabyPresets() {
        val baby = PresetCatalog.presetsFor("Baby")
        val legacy = PresetCatalog.presetsFor("baby shower")
        assertEquals(
            "Legacy 'baby shower' must canonicalise via OccasionCatalog.storageKeyFor to 'Baby'",
            baby,
            legacy,
        )
        assertEquals("Baby presets must also be 6 entries per D-02", 6, baby.size)
    }

    /** D-02: null occasion yields no presets (picker stays disabled per D-12). */
    @Test
    fun presetsFor_nullOccasion_returnsEmptyList() {
        assertTrue(
            "null occasion must not expose presets — guards D-12 order gate",
            PresetCatalog.presetsFor(null).isEmpty(),
        )
    }

    /** D-02 + D-05: encode produces the canonical `preset:{occasion}:{index}` sentinel. */
    @Test
    fun encode_weddingIndex3_producesCanonicalSentinel() {
        assertEquals("preset:Wedding:3", PresetCatalog.encode("Wedding", 3))
    }

    /** D-05: resolve roundtrips a 1-based index to the 0-based drawable list lookup. */
    @Test
    fun resolve_validSentinel_roundtripsToPresetsForIndex() {
        val all = PresetCatalog.presetsFor("Wedding")
        val resolved = PresetCatalog.resolve("preset:Wedding:3")
        assertNotNull("resolve('preset:Wedding:3') must return a non-null Int (D-05)", resolved)
        assertEquals(
            "resolve must return presetsFor('Wedding')[3 - 1]",
            all[2],
            resolved,
        )
    }

    /** D-05: unknown occasion canonicalises to Custom; if Custom presets exist this still resolves; the contract here is malformed-occasion safety. */
    @Test
    fun resolve_unknownOccasion_returnsNull() {
        // OccasionCatalog.storageKeyFor("NotARealOccasion") returns "Custom".
        // Plan 02 must guarantee resolve() returns null here UNLESS the canonicalised key
        // (Custom) has presets — and it does. So this test specifically uses an index
        // beyond the Custom preset count (99) to lock the safety contract.
        assertNull(
            "Out-of-range index must yield null even for canonicalised occasion (D-05 safety)",
            PresetCatalog.resolve("preset:NotARealOccasion:99"),
        )
    }

    /** D-05: out-of-range index returns null (e.g., 99 when only 6 presets exist). */
    @Test
    fun resolve_outOfRangeIndex_returnsNull() {
        assertNull(PresetCatalog.resolve("preset:Wedding:99"))
    }

    /** D-05: HTTP URL is not a sentinel — resolve returns null so resolveImageModel can route via String passthrough. */
    @Test
    fun resolve_httpUrl_returnsNull() {
        assertNull(PresetCatalog.resolve("https://example.com/x.jpg"))
    }

    /** D-05: empty string is not a sentinel. */
    @Test
    fun resolve_emptyString_returnsNull() {
        assertNull(PresetCatalog.resolve(""))
    }
}
