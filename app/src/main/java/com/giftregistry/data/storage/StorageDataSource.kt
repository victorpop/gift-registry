package com.giftregistry.data.storage

import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase 12 — thin wrapper around [FirebaseStorage] so [StorageRepositoryImpl]
 * is unit-testable without mocking the final `StorageReference` chain inside
 * the repository layer (RESEARCH.md Pattern 3).
 *
 * The data-source split keeps the canonical path schema (D-05) in one place
 * and lets [StorageRepositoryImpl] focus on the runCatching wrapper that
 * converts Firebase exceptions to `Result.failure` (Phase 02 D-08 invariant).
 *
 * NOT intended to be called from outside the data layer — the domain
 * [com.giftregistry.domain.storage.StorageRepository] is the public surface.
 */
@Singleton
class StorageDataSource @Inject constructor(
    private val storage: FirebaseStorage,
) {

    /**
     * Upload [jpegBytes] to the canonical D-05 path
     * `users/{uid}/registries/{registryId}/cover.jpg` and return the public
     * download URL (the same URL Coil 3 will fetch from).
     *
     * Throws on failure (network error, auth error, storage rules denial,
     * etc.) — [StorageRepositoryImpl] wraps the call in `runCatching` so the
     * exception surfaces as `Result.failure` to the ViewModel.
     */
    suspend fun uploadCoverBytes(
        uid: String,
        registryId: String,
        jpegBytes: ByteArray,
    ): String {
        val ref = storage.reference.child("users/$uid/registries/$registryId/cover.jpg")
        ref.putBytes(jpegBytes).await()
        return ref.downloadUrl.await().toString()
    }
}
