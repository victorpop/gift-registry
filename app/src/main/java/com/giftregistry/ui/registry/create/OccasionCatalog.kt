package com.giftregistry.ui.registry.create

/**
 * SCR-09: Occasion catalog for the Create Registry 2×3 tile grid + hero
 * placeholder glyph lookup on the Registry Detail screen.
 *
 * The 6 v1.1 occasions in fixed handoff order (Housewarming / Wedding / Baby /
 * Birthday / Christmas / Custom). Each entry exposes the canonical
 * [storageKey] that goes into `Registry.occasion` and the [glyph] char that
 * renders inside the tile + hero placeholder.
 *
 * Legacy alias map for pre-Phase-11 Firestore documents (RESEARCH.md
 * Open Question 1): older docs may contain `"Baby shower"` / `"Anniversary"`
 * strings since the Phase 3 occasion set used different labels. `glyphFor`
 * + `storageKeyFor` surface the right v1.1 tile for those legacy values
 * without requiring a Firestore migration.
 */
data class OccasionEntry(val storageKey: String, val glyph: String)

object OccasionCatalog {

    val entries: List<OccasionEntry> = listOf(
        OccasionEntry(storageKey = "Housewarming", glyph = "⌂"),
        OccasionEntry(storageKey = "Wedding",      glyph = "♡"),
        OccasionEntry(storageKey = "Baby",         glyph = "◐"),
        OccasionEntry(storageKey = "Birthday",     glyph = "✦"),
        OccasionEntry(storageKey = "Christmas",    glyph = "❅"),
        OccasionEntry(storageKey = "Custom",       glyph = "+"),
    )

    /**
     * Legacy value → canonical v1.1 storageKey map.
     * Case-insensitive comparison via lowercase keys.
     */
    private val legacyAliases: Map<String, String> = mapOf(
        "baby shower" to "Baby",
        "anniversary" to "Housewarming",
    )

    /**
     * Returns the tile glyph for [occasion], applying case-insensitive matching
     * and the legacy alias map. Null / unknown → the Custom glyph "+".
     */
    fun glyphFor(occasion: String?): String {
        val canonical = storageKeyFor(occasion)
        return entries.firstOrNull { it.storageKey == canonical }?.glyph ?: "+"
    }

    /**
     * Canonicalises an arbitrary [occasion] string (potentially from a legacy
     * Firestore doc) to the v1.1 storage key. Null → "Custom". Unknown → "Custom".
     * Passes through any already-canonical v1.1 value unchanged.
     */
    fun storageKeyFor(occasion: String?): String {
        if (occasion == null) return "Custom"
        val normalised = occasion.trim()
        if (normalised.isEmpty()) return "Custom"
        val lower = normalised.lowercase()
        legacyAliases[lower]?.let { return it }
        return entries.firstOrNull { it.storageKey.equals(normalised, ignoreCase = true) }?.storageKey
            ?: "Custom"
    }
}
