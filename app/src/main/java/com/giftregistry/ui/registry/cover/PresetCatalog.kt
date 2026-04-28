package com.giftregistry.ui.registry.cover

import com.giftregistry.R
import com.giftregistry.ui.registry.create.OccasionCatalog

/**
 * Phase 12 — bundled cover-photo preset lookup table (D-02 / D-05 / D-11).
 *
 * 6 presets per occasion × 6 occasions = 36 R.drawable.preset_*_* JPEGs ship
 * in `drawable-xxhdpi/`. The sentinel format `preset:{occasion}:{index1Based}`
 * is persisted on `Registry.imageUrl`; [resolve] turns the sentinel back into
 * a `R.drawable.*` Int that Coil 3 can render via `AsyncImage(model = …)`.
 *
 * Occasion canonicalisation goes through [OccasionCatalog.storageKeyFor] so
 * legacy Firestore docs (Phase 03 era) using `Baby shower` / `Anniversary`
 * resolve to the v1.1 `Baby` / `Housewarming` preset sets without a data
 * migration (RESEARCH.md Pattern 5; Phase 11 alias map).
 */
object PresetCatalog {

    private val byOccasion: Map<String, List<Int>> = mapOf(
        "Housewarming" to listOf(
            R.drawable.preset_housewarming_1, R.drawable.preset_housewarming_2,
            R.drawable.preset_housewarming_3, R.drawable.preset_housewarming_4,
            R.drawable.preset_housewarming_5, R.drawable.preset_housewarming_6,
        ),
        "Wedding" to listOf(
            R.drawable.preset_wedding_1, R.drawable.preset_wedding_2,
            R.drawable.preset_wedding_3, R.drawable.preset_wedding_4,
            R.drawable.preset_wedding_5, R.drawable.preset_wedding_6,
        ),
        "Baby" to listOf(
            R.drawable.preset_baby_1, R.drawable.preset_baby_2,
            R.drawable.preset_baby_3, R.drawable.preset_baby_4,
            R.drawable.preset_baby_5, R.drawable.preset_baby_6,
        ),
        "Birthday" to listOf(
            R.drawable.preset_birthday_1, R.drawable.preset_birthday_2,
            R.drawable.preset_birthday_3, R.drawable.preset_birthday_4,
            R.drawable.preset_birthday_5, R.drawable.preset_birthday_6,
        ),
        "Christmas" to listOf(
            R.drawable.preset_christmas_1, R.drawable.preset_christmas_2,
            R.drawable.preset_christmas_3, R.drawable.preset_christmas_4,
            R.drawable.preset_christmas_5, R.drawable.preset_christmas_6,
        ),
        "Custom" to listOf(
            R.drawable.preset_custom_1, R.drawable.preset_custom_2,
            R.drawable.preset_custom_3, R.drawable.preset_custom_4,
            R.drawable.preset_custom_5, R.drawable.preset_custom_6,
        ),
    )

    /**
     * 6 preset drawable IDs for [occasion] in display order.
     *
     * Null / blank / unknown occasion → empty list (D-12 keeps the picker
     * disabled when no occasion is set so this branch is rarely hit by UI).
     * Legacy aliases (`baby shower` → `Baby`, `Anniversary` → `Housewarming`)
     * canonicalise via [OccasionCatalog.storageKeyFor] (Phase 11).
     */
    fun presetsFor(occasion: String?): List<Int> {
        if (occasion.isNullOrBlank()) return emptyList()
        val canonical = OccasionCatalog.storageKeyFor(occasion)
        return byOccasion[canonical] ?: emptyList()
    }

    /**
     * Encodes a (occasion, 1-based index) preset selection as the canonical
     * sentinel string persisted on `Registry.imageUrl`. (D-02 / D-05)
     *
     * Format: `preset:{occasion}:{index1Based}`. Final-shape — no Wave 1
     * mutation; the unit test for this method PASSES in Wave 0 already.
     */
    fun encode(occasion: String, index1Based: Int): String =
        "preset:$occasion:$index1Based"

    /**
     * Resolves a `preset:{occasion}:{index}` sentinel back to its
     * `R.drawable.*` Int.
     *
     * Returns `null` for any non-sentinel string (HTTP URL, blank, malformed)
     * AND for sentinels whose canonicalised occasion or index falls outside
     * the available presets (e.g., `preset:Wedding:99`). Callers (notably
     * [resolveImageModel]) treat `null` as the placeholder branch.
     */
    fun resolve(sentinel: String): Int? {
        if (!sentinel.startsWith(SENTINEL_PREFIX)) return null
        val payload = sentinel.removePrefix(SENTINEL_PREFIX)
        // Split into exactly two parts: occasion + index.
        // We cannot use `split(":", limit = 2)` and then split again because
        // a malformed input like "preset:Wedding:3:extra" must yield null per
        // the safety contract — accept exactly one ':' inside the payload.
        val parts = payload.split(":")
        if (parts.size != 2) return null
        val (occasion, indexStr) = parts
        val idx1Based = indexStr.toIntOrNull() ?: return null
        if (idx1Based < 1) return null
        // Only canonicalise if the occasion isn't blank — `storageKeyFor` would
        // happily collapse `""` to `Custom` and let an out-of-range Custom
        // sentinel resolve to a Custom drawable, masking the malformed input.
        if (occasion.isBlank()) return null
        val list = byOccasion[OccasionCatalog.storageKeyFor(occasion)] ?: return null
        return list.getOrNull(idx1Based - 1)
    }

    private const val SENTINEL_PREFIX = "preset:"
}
