package com.giftregistry.ui.registry.cover

/**
 * Phase 12 — Wave 0 STUB.
 *
 * `presetsFor` and `resolve` return empty/null on purpose so the Wave 0
 * RED tests fail on assertion mismatches (NOT compile errors). Plan 02
 * replaces the stub bodies with the real `R.drawable.preset_*_*` lookup
 * table per RESEARCH.md Pattern 5.
 *
 * `encode` is final-shape because it has no Android dependencies — its
 * sentinel format `preset:{occasion}:{index}` is the locked encoding for
 * `Registry.imageUrl` per D-02 / D-05.
 */
object PresetCatalog {

    /**
     * Returns the per-occasion preset drawable IDs.
     *
     * STUB — Plan 02 wires the 6×6 R.drawable lookup table. Wave 0 returns
     * empty so PresetCatalogTest.presetsFor_* fails on size assertion.
     */
    fun presetsFor(occasion: String?): List<Int> = emptyList()

    /**
     * Encodes a (occasion, 1-based index) preset selection as the canonical
     * sentinel string persisted on `Registry.imageUrl`. (D-02 / D-05)
     *
     * Format: `preset:{occasion}:{index1Based}`. Final-shape — no Wave 1
     * mutation; the unit test for this method PASSES in Wave 0.
     */
    fun encode(occasion: String, index1Based: Int): String =
        "preset:$occasion:$index1Based"

    /**
     * Resolves a sentinel string back to a `R.drawable.*` Int.
     * Returns `null` for non-sentinel strings (URLs, blank, malformed).
     *
     * STUB — Plan 02 inverts the [presetsFor] table. Wave 0 returns null
     * so ResolveImageModelTest + PresetCatalogTest.resolve_* fail RED.
     */
    fun resolve(sentinel: String): Int? = null
}
