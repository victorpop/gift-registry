package com.giftregistry.ui.registry.cover

import android.net.Uri
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 12 — Wave 0 tests for [CoverPhotoSelection].
 *
 * The sealed interface is final-shape in Wave 0 (singleton None +
 * data-class Preset/Gallery), so these tests effectively assert the
 * locked contract that Plan 03 + Plan 04 must consume. They serve as
 * regression guards more than RED tests, but they DO fail if anyone
 * later changes None from `data object` to a `class`, breaks data-class
 * equality on Preset, or removes the Gallery URI carrier.
 *
 * Pins: D-09 / D-10 / D-11 (preset cleared on occasion change relies
 * on Preset data-class equality differentiating different occasions).
 */
class CoverPhotoSelectionTest {

    /** D-09: None is a singleton — `===` reference equality across the app. */
    @Test
    fun none_isSingleton() {
        val a = CoverPhotoSelection.None
        val b = CoverPhotoSelection.None
        assertSame("CoverPhotoSelection.None must be a data object singleton", a, b)
    }

    /** D-11: Preset data-class equality on (occasion, index) drives the occasion-change-clears-preset logic. */
    @Test
    fun preset_dataClassEquality_sameOccasionAndIndex() {
        val a = CoverPhotoSelection.Preset("Wedding", 3)
        val b = CoverPhotoSelection.Preset("Wedding", 3)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    /** D-11: differing index makes the two Presets unequal — VM uses this to detect a fresh selection. */
    @Test
    fun preset_dataClassEquality_differentIndex() {
        val a = CoverPhotoSelection.Preset("Wedding", 3)
        val b = CoverPhotoSelection.Preset("Wedding", 4)
        assertNotEquals(a, b)
    }

    /** D-10: Gallery carries the Uri value verbatim so onSave can hand it to the upload pipeline. */
    @Test
    fun gallery_carriesUriValue() {
        // Robolectric is not on the test classpath, so Uri.parse() would NPE.
        // We mock the Uri instance — Wave 0 only needs to assert the value is carried.
        val uri = mockk<Uri>(relaxed = true)
        val selection = CoverPhotoSelection.Gallery(uri)
        assertSame(
            "Gallery must hold the picked Uri reference (D-10 staging for D-07 upload)",
            uri,
            selection.uri,
        )
    }

    /** Sealed interface enforces None / Preset / Gallery as the only branches (compile-time). */
    @Test
    fun sealedInterface_branchTypes() {
        val none: CoverPhotoSelection = CoverPhotoSelection.None
        val preset: CoverPhotoSelection = CoverPhotoSelection.Preset("Birthday", 1)
        val gallery: CoverPhotoSelection = CoverPhotoSelection.Gallery(mockk(relaxed = true))
        assertTrue(none is CoverPhotoSelection.None)
        assertTrue(preset is CoverPhotoSelection.Preset)
        assertTrue(gallery is CoverPhotoSelection.Gallery)
    }
}
