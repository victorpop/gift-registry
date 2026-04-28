package com.giftregistry.ui.registry.cover

/**
 * Phase 12 — Wave 0 STUB.
 *
 * D-12 order gate — the cover-photo picker is disabled until the user
 * has selected an occasion in CreateRegistryScreen.
 *
 * STUB returns true unconditionally on purpose so the Wave 0 RED tests
 * (CoverPhotoPickerEnabledTest) fail on the false-when-blank / null /
 * whitespace cases. Plan 03 replaces the body with `!occasion.isNullOrBlank()`.
 */
fun isCoverPickerEnabled(occasion: String?): Boolean = true
