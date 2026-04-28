package com.giftregistry.ui.registry.cover

/**
 * D-12 order gate — the cover-photo picker is disabled until the user
 * has selected an occasion in CreateRegistryScreen.
 *
 * Returns false for null / blank / whitespace-only `occasion` strings;
 * true for any non-blank occasion. The disabled-state preview surfaces
 * the caption "Pick an occasion to see suggested covers"
 * (`R.string.cover_photo_pick_occasion_first`, wired by Plan 04).
 *
 * Flips Wave 0 RED tests in
 * `app/src/test/java/com/giftregistry/ui/registry/cover/CoverPhotoPickerEnabledTest.kt`
 * GREEN: empty / whitespace / null cases now return false; valid-occasion
 * case still returns true.
 */
fun isCoverPickerEnabled(occasion: String?): Boolean =
    !occasion.isNullOrBlank()
