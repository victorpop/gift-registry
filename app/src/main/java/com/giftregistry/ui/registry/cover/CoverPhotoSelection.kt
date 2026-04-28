package com.giftregistry.ui.registry.cover

import android.net.Uri

/**
 * Phase 12 — Wave 0 STUB (final-shape).
 *
 * Locked CoverPhotoSelection contract for the inline cover-photo picker.
 * Sealed interface so [None] is a singleton (`===` reference equality
 * across the app) and [Preset] / [Gallery] use data-class equality.
 *
 * Decision references:
 * - D-09 inline preview block at the top of CreateRegistryScreen
 * - D-10 ModalBottomSheet picker contents
 * - D-11 Preset cleared when occasion changes (handled in VM)
 *
 * This file ships its FINAL shape in Wave 0 — the test asserts equality
 * semantics directly, no further mutation in Plans 02/03/04.
 */
sealed interface CoverPhotoSelection {
    /** No cover photo selected — render the themed placeholder (D-14). */
    data object None : CoverPhotoSelection

    /**
     * Bundled occasion preset selection.
     * @param occasion the canonical [com.giftregistry.ui.registry.create.OccasionCatalog]
     *                 storage key (e.g. "Wedding"). Plan 02 wires the preset catalog.
     * @param index    1-based preset index (1..6) within the occasion's preset list.
     */
    data class Preset(val occasion: String, val index: Int) : CoverPhotoSelection

    /** User-picked gallery image staged for upload during onSave (D-07). */
    data class Gallery(val uri: Uri) : CoverPhotoSelection
}
