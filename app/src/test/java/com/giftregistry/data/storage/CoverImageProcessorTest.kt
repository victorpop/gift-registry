package com.giftregistry.data.storage

import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test

/**
 * Phase 12 — Wave 0 RED tests for [CoverImageProcessor] (D-06).
 *
 * Pins:
 * - `compress(uri)` returns JPEG bytes ≤ ~300 KB for typical input.
 * - Large 4032×3024 bitmaps are downscaled to fit within 1280×720 (16:9).
 *
 * IGNORED in Wave 0 — Robolectric is NOT on the test classpath, and pure-JVM
 * unit tests cannot exercise BitmapFactory (needs the Android framework).
 * Plan 02 will either:
 *   (a) move these tests to androidTest/ (instrumented, real Android runtime), OR
 *   (b) add Robolectric to the testRuntimeClasspath, OR
 *   (c) introduce a fake Bitmap path keyed off the test environment.
 *
 * The `compress` interface contract is locked here so the production
 * pipeline cannot drift; the runtime assertions return once the impl
 * decision in Plan 02 is final.
 */
@Ignore("Plan 02: needs Robolectric or instrumented variant — D-06 size invariant tests deferred")
class CoverImageProcessorTest {

    /** D-06: typical input compresses to ≤ ~300 KB at quality 85. */
    @Test
    fun compress_smallImage_under300kb() {
        // Plan 02 wires the real BitmapFactory path.
        // Wave 0 placeholder — test is @Ignored at class level.
        assertTrue("placeholder", true)
    }

    /**
     * D-06: 4032×3024 (typical phone camera) downscales to fit 1280×720,
     * outputs ≤ ~300 KB JPEG, decodable as a valid Bitmap on read-back.
     */
    @Test
    fun compress_largeImage_downscalesAndCompresses() {
        // Plan 02 wires the real BitmapFactory path with two-pass inSampleSize +
        // Bitmap.createScaledBitmap + Bitmap.compress(JPEG, 85).
        // Wave 0 placeholder — test is @Ignored at class level.
        assertTrue("placeholder", true)
    }
}
