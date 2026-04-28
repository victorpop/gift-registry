package com.giftregistry.data.storage

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase 12 — real implementation of [CoverImageProcessor] (D-06).
 *
 * Two-pass [BitmapFactory] decode + downscale to 1280×720 (16:9) + JPEG
 * compress at quality 85 per RESEARCH.md Pattern 2:
 *
 *   1. Decode bounds only (`inJustDecodeBounds = true`) to learn the source
 *      dimensions without allocating a Bitmap.
 *   2. Compute the largest power-of-2 `inSampleSize` that keeps the sampled
 *      width/height ≥ the 1280×720 target (overshoots so the final scale-to-fit
 *      pass smooths to the exact aspect).
 *   3. Decode again with that sample size → in-memory Bitmap.
 *   4. `Bitmap.createScaledBitmap` to fit within 1280×720 preserving aspect.
 *   5. `Bitmap.compress(JPEG, 85)` into a `ByteArrayOutputStream`.
 *
 * Runs on `Dispatchers.IO` — RESEARCH.md anti-pattern #2 explicitly forbids
 * decoding on the main thread (4032×3024 phone-camera images can block for
 * hundreds of milliseconds).
 *
 * Pitfall 3 caveat (RESEARCH.md): the caller must not persist [Uri] across
 * process death — the Photo Picker grant is process-scoped. Plan 04's
 * ViewModel uploads the bytes during `onSave()` while the URI is still live.
 */
@Singleton
class CoverImageProcessorImpl @Inject constructor(
    private val contentResolver: ContentResolver,
) : CoverImageProcessor {

    override suspend fun compress(uri: Uri): ByteArray = withContext(Dispatchers.IO) {
        // Pass 1 — decode bounds only.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        } ?: throw IOException("Cannot read URI: $uri")

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IOException("Cannot decode bounds for URI: $uri")
        }

        // Compute power-of-2 inSampleSize that keeps the sampled dimensions
        // at or above the target so the subsequent scale-to-fit pass has
        // enough resolution to produce a smooth result.
        var sample = 1
        while (
            bounds.outWidth / (sample * 2) >= TARGET_WIDTH ||
            bounds.outHeight / (sample * 2) >= TARGET_HEIGHT
        ) {
            sample *= 2
        }

        // Pass 2 — decode at the sampled size.
        val decoded = contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(
                stream,
                null,
                BitmapFactory.Options().apply { inSampleSize = sample },
            )
        } ?: throw IOException("Cannot decode URI: $uri")

        // Scale-to-fit while preserving aspect.
        val scaled = decoded.scaleToFit(TARGET_WIDTH, TARGET_HEIGHT)
        if (scaled !== decoded) decoded.recycle()

        // JPEG compress at quality 85 — produces ~150–300 KB for typical
        // phone-camera input per the D-06 size budget.
        ByteArrayOutputStream().also { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            scaled.recycle()
        }.toByteArray()
    }

    private fun Bitmap.scaleToFit(maxW: Int, maxH: Int): Bitmap {
        if (width <= maxW && height <= maxH) return this
        val scale = minOf(maxW.toFloat() / width, maxH.toFloat() / height)
        val newW = (width * scale).toInt().coerceAtLeast(1)
        val newH = (height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(this, newW, newH, /* filter = */ true)
    }

    companion object {
        internal const val TARGET_WIDTH = 1280
        internal const val TARGET_HEIGHT = 720
        internal const val JPEG_QUALITY = 85
    }
}
