package com.giftregistry.ui.registry.cover

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 12 — Wave 0 RED tests for [isCoverPickerEnabled].
 *
 * Pins: D-12 order gate. The cover-photo picker is disabled until an
 * occasion is selected:
 *   - empty / null / whitespace-only `occasion` -> false (disabled)
 *   - any non-blank `occasion` -> true (enabled)
 *
 * Wave 0 stub returns true unconditionally so the false-when-blank cases
 * fail RED. Plan 03 replaces the body with `!occasion.isNullOrBlank()`.
 */
class CoverPhotoPickerEnabledTest {

    /** D-12: empty occasion disables the picker (caption shows "Pick an occasion to see suggested covers"). */
    @Test
    fun isCoverPickerEnabled_emptyOccasion_returnsFalse() {
        assertFalse(
            "Empty occasion must disable picker per D-12 order gate",
            isCoverPickerEnabled(""),
        )
    }

    /** D-12: whitespace-only counts as blank — disabled. */
    @Test
    fun isCoverPickerEnabled_whitespaceOccasion_returnsFalse() {
        assertFalse(isCoverPickerEnabled("   "))
    }

    /** D-12: null occasion (initial state) keeps picker disabled. */
    @Test
    fun isCoverPickerEnabled_nullOccasion_returnsFalse() {
        assertFalse(isCoverPickerEnabled(null))
    }

    /** D-12: any non-blank occasion enables the picker. */
    @Test
    fun isCoverPickerEnabled_validOccasion_returnsTrue() {
        assertTrue(isCoverPickerEnabled("Wedding"))
    }
}
