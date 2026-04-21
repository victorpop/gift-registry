package com.giftregistry.ui.registry.create

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * SCR-09: Occasion catalog — 6 occasions in fixed order + legacy alias map for
 * pre-Phase-11 Firestore registry.occasion values.
 *
 * Fixed order (CONTEXT.md § Create Registry 2×3 occasion tile grid):
 *   Housewarming ⌂, Wedding ♡, Baby ◐, Birthday ✦, Christmas ❅, Custom +
 *
 * Legacy aliases (RESEARCH.md Open Question 1):
 *   "Baby shower" → "Baby"       (handoff shortened the label)
 *   "Anniversary" → "Housewarming" (Anniversary retired; Housewarming is the new v1.1 default)
 *   unknown / null → "Custom"
 *
 * glyphFor() is case-insensitive and falls through to legacy aliases then "+".
 * storageKeyFor() canonicalises legacy input to the v1.1 key so the UI renders
 * the correct tile highlighted when editing an older registry.
 *
 * RED in Wave 0 — flips GREEN when Plan 02 ships OccasionCatalog.kt in
 * com.giftregistry.ui.registry.create.
 */
class OccasionCatalogTest {

    @Test fun entries_hasSixItemsInFixedOrder() {
        val entries = OccasionCatalog.entries
        assertEquals(6, entries.size)
        assertEquals(
            listOf("Housewarming", "Wedding", "Baby", "Birthday", "Christmas", "Custom"),
            entries.map { it.storageKey },
        )
    }

    @Test fun entries_glyphsMatchHandoff() {
        val entries = OccasionCatalog.entries
        assertEquals(
            listOf("⌂", "♡", "◐", "✦", "❅", "+"),
            entries.map { it.glyph },
        )
    }

    @Test fun glyphFor_housewarming_returnsHouseGlyph() =
        assertEquals("⌂", OccasionCatalog.glyphFor("Housewarming"))

    @Test fun glyphFor_isCaseInsensitive() {
        assertEquals("⌂", OccasionCatalog.glyphFor("housewarming"))
        assertEquals("⌂", OccasionCatalog.glyphFor("HOUSEWARMING"))
    }

    @Test fun glyphFor_wedding_returnsHeart() =
        assertEquals("♡", OccasionCatalog.glyphFor("Wedding"))

    @Test fun glyphFor_baby_returnsHalfMoon() =
        assertEquals("◐", OccasionCatalog.glyphFor("Baby"))

    @Test fun glyphFor_birthday_returnsStar() =
        assertEquals("✦", OccasionCatalog.glyphFor("Birthday"))

    @Test fun glyphFor_christmas_returnsSnowflake() =
        assertEquals("❅", OccasionCatalog.glyphFor("Christmas"))

    @Test fun glyphFor_custom_returnsPlus() =
        assertEquals("+", OccasionCatalog.glyphFor("Custom"))

    @Test fun glyphFor_null_returnsPlus() =
        assertEquals("+", OccasionCatalog.glyphFor(null))

    @Test fun glyphFor_unknown_returnsPlus() =
        assertEquals("+", OccasionCatalog.glyphFor("SomeUnknownOccasion"))

    @Test fun glyphFor_legacyBabyShower_returnsHalfMoon() =
        assertEquals(
            "Legacy Firestore 'Baby shower' docs (pre-Phase 11) must render the new Baby glyph ◐",
            "◐", OccasionCatalog.glyphFor("Baby shower"),
        )

    @Test fun glyphFor_legacyAnniversary_returnsHousewarmingGlyph() =
        assertEquals(
            "Legacy Firestore 'Anniversary' docs (pre-Phase 11) must render the new Housewarming glyph ⌂",
            "⌂", OccasionCatalog.glyphFor("Anniversary"),
        )

    @Test fun storageKeyFor_babyShower_returnsBaby() =
        assertEquals("Baby", OccasionCatalog.storageKeyFor("Baby shower"))

    @Test fun storageKeyFor_anniversary_returnsHousewarming() =
        assertEquals("Housewarming", OccasionCatalog.storageKeyFor("Anniversary"))

    @Test fun storageKeyFor_null_returnsCustom() =
        assertEquals("Custom", OccasionCatalog.storageKeyFor(null))

    @Test fun storageKeyFor_passthroughForCanonical() =
        assertEquals("Wedding", OccasionCatalog.storageKeyFor("Wedding"))
}
