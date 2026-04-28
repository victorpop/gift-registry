package com.giftregistry.domain.storage

/**
 * Phase 12 — domain interface for cover-photo upload (D-04, D-05, D-07).
 *
 * MUST contain ZERO Firebase imports per Phase 02 D-08 architecture rule
 * ("Zero Firebase imports in domain layer — pure Kotlin interfaces only").
 * The implementation in [com.giftregistry.data.storage.StorageRepositoryImpl]
 * wraps Firebase Storage's `putBytes().await()` + `downloadUrl.await()` and
 * converts exceptions to `Result.failure` via `runCatching` (Phase 02 D-08
 * "runCatching wraps Firebase suspend calls" — keeps FirebaseExceptions out
 * of the domain).
 */
interface StorageRepository {

    /**
     * Upload a JPEG cover photo to `/users/{uid}/registries/{registryId}/cover.jpg`
     * and return the public download URL string.
     *
     * Failures (network, auth, storage rules denial, etc.) are wrapped in
     * `Result.failure(...)` so the ViewModel can surface the error in its
     * existing `error: StateFlow<String?>` channel without leaking
     * FirebaseExceptions into the UI layer (D-04, D-05, D-07).
     *
     * Single canonical filename `cover.jpg` per D-05 — re-uploads overwrite
     * the prior cover so there are no orphaned blobs in Storage.
     */
    suspend fun uploadCover(
        uid: String,
        registryId: String,
        jpegBytes: ByteArray,
    ): Result<String>
}
