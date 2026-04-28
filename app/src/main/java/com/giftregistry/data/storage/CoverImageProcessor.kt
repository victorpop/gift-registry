package com.giftregistry.data.storage

import android.net.Uri

/**
 * Phase 12 — pre-upload bitmap pipeline contract (D-06).
 *
 * Decode + downscale to 1280×720 max (16:9) + JPEG compress at quality 85.
 * Implementation arrives in Plan 02 using the canonical Android two-pass
 * `BitmapFactory.Options.inJustDecodeBounds` + `Bitmap.compress` pattern
 * (RESEARCH.md Pattern 2). The interface lives in the data layer because
 * it depends on `android.net.Uri` (a platform type, not pure Kotlin) and
 * is composed by `StorageRepositoryImpl` before calling
 * `FirebaseStorage.reference.putBytes(...)`.
 *
 * Exposed as an interface (not a top-level fn) so VM tests can mock the
 * pipeline via mockk without touching the real BitmapFactory.
 */
interface CoverImageProcessor {

    /**
     * Decode the image at [uri], downscale to fit within 1280×720 preserving
     * aspect, JPEG-compress at quality 85, and return the resulting bytes.
     *
     * Throws on I/O failure (cannot read URI, decoding error, etc.) — the
     * ViewModel wraps the call in `runCatching` to surface a user-friendly
     * error message (D-07 failure path).
     *
     * Runs on `Dispatchers.IO` per RESEARCH.md anti-pattern #2 (never on the
     * main thread — large images can block for hundreds of ms).
     */
    suspend fun compress(uri: Uri): ByteArray
}
