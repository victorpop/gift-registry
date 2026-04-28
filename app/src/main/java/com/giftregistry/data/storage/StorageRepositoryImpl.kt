package com.giftregistry.data.storage

import com.giftregistry.domain.storage.StorageRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase 12 — Wave 0 STUB.
 *
 * Empty skeleton so [com.giftregistry.data.storage.StorageRepositoryImplTest]
 * compiles in Wave 0. Plan 02 Task 3 replaces the body with the real
 * Firebase Storage `putBytes(...).await() + downloadUrl.await()` pipeline
 * inside `runCatching { … }` per RESEARCH.md Pattern 3.
 *
 * Wave 0 returns Result.failure(NotImplementedError) so the StorageRepositoryImplTest
 * happy-path assertions fail RED (expected); the failure-path test for `runCatching`
 * also fails RED because we throw NotImplementedError before runCatching wraps the
 * mocked Firebase exception.
 */
@Singleton
class StorageRepositoryImpl @Inject constructor() : StorageRepository {

    override suspend fun uploadCover(
        uid: String,
        registryId: String,
        jpegBytes: ByteArray,
    ): Result<String> = Result.failure(NotImplementedError("Plan 02 wires Firebase Storage"))
}
