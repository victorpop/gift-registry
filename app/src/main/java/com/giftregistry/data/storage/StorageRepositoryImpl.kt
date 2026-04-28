package com.giftregistry.data.storage

import com.giftregistry.domain.storage.StorageRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase 12 — real implementation of [StorageRepository] (D-04 / D-05 / D-07).
 *
 * Delegates the FirebaseStorage call to [StorageDataSource] (which holds the
 * canonical D-05 path schema) and wraps it in `runCatching` so any
 * StorageException / IOException surfaces as `Result.failure` rather than
 * leaking out of the data layer (Phase 02 D-08 — keep FirebaseExceptions out
 * of the domain).
 *
 * The ViewModel translates the failure into a user-facing error string via
 * its existing `error: StateFlow<String?>` channel (D-07 failure path).
 *
 * RESEARCH.md Pattern 3.
 */
@Singleton
class StorageRepositoryImpl @Inject constructor(
    private val dataSource: StorageDataSource,
) : StorageRepository {

    override suspend fun uploadCover(
        uid: String,
        registryId: String,
        jpegBytes: ByteArray,
    ): Result<String> = runCatching {
        dataSource.uploadCoverBytes(uid, registryId, jpegBytes)
    }
}
