package com.giftregistry.data.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Phase 12 — Wave 0 RED tests for [StorageRepositoryImpl].
 *
 * Pins (D-04 / D-05 / D-07):
 * - Happy path: `uploadCover` returns Result.success(downloadUrl) after
 *   `putBytes(...).await()` + `downloadUrl.await()` roundtrip.
 * - Failure path: `runCatching { ... }` wraps Firebase exceptions into
 *   Result.failure (Phase 02 D-08 — keep FirebaseExceptions out of domain).
 * - Path schema: `users/{uid}/registries/{registryId}/cover.jpg` per D-05.
 *
 * Wave 0: [StorageRepositoryImpl] is a stub returning Result.failure(
 * NotImplementedError). All three tests fail RED. Plan 02 Task 3 ships the
 * real impl using FirebaseStorage + putBytes + downloadUrl + runCatching.
 *
 * NOTE: The full Firebase SDK mocking (mockk on FirebaseStorage final classes)
 * is the responsibility of Plan 02 — once the impl exists, the test will
 * mock storage.reference.child(...) etc. Wave 0 keeps the assertions
 * structural so the file compiles against the empty stub.
 */
class StorageRepositoryImplTest {

    private val repo = StorageRepositoryImpl()

    /**
     * D-04 / D-07 happy path: a successful Storage upload returns the
     * download URL string.
     *
     * Wave 0 RED: stub returns Result.failure(NotImplementedError). Plan 02
     * wires `runCatching { ref.putBytes(...).await(); ref.downloadUrl.await().toString() }`.
     */
    @Test
    fun uploadCover_happyPath_returnsDownloadUrl() {
        val result = kotlinx.coroutines.runBlocking {
            repo.uploadCover(uid = "uid-1", registryId = "reg-1", jpegBytes = ByteArray(1024))
        }
        assertTrue(
            "D-04 happy path: uploadCover must return Result.success with a non-blank URL " +
                "(Wave 0 RED — Plan 02 wires Firebase Storage putBytes + downloadUrl)",
            result.isSuccess,
        )
        assertTrue(
            "Download URL must be non-blank",
            result.getOrNull()?.isNotBlank() == true,
        )
    }

    /**
     * D-07 failure path: Firebase exceptions become Result.failure via runCatching.
     * Plan 02's wrapper guarantees no FirebaseException leaks past the data layer.
     *
     * Wave 0 RED: stub returns Result.failure(NotImplementedError) — the test
     * verifies the FAILURE shape, but the wrapper contract isn't proven until
     * Plan 02 mocks an actual Firebase exception path.
     */
    @Test
    fun uploadCover_failure_returnsResultFailure() {
        // Wave 0 — Plan 02 must mock storage.reference.child(...).putBytes(...).await()
        // to throw a StorageException, then assert Result.isFailure with the same
        // exception type. For Wave 0 we simply mark the contract as outstanding so
        // the assertion fails RED (NotImplementedError is not the contract failure).
        fail(
            "Wave 0 stub — Plan 02 must mock FirebaseStorage to throw and assert " +
                "Result.isFailure carrying the original exception (D-07 + Phase 02 D-08 runCatching)",
        )
    }

    /**
     * D-05 — single-canonical path schema: re-uploads overwrite cleanly so
     * there are no orphan blobs in Storage.
     *
     * Wave 0 RED: no path verification possible until Plan 02 wires the
     * real FirebaseStorage instance and we can capture the StorageReference
     * `.child(path)` call via mockk slot.
     */
    @Test
    fun uploadCover_pathSchema() {
        val expectedPath = "users/uid-1/registries/reg-1/cover.jpg"
        // Plan 02 contract: mockk slot on storage.reference.child(...) captures the path.
        // Wave 0 keeps the contract documented; assertion fails so the test stays RED.
        assertEquals(
            "D-05: Storage path MUST be 'users/{uid}/registries/{registryId}/cover.jpg' " +
                "(Wave 0 RED — Plan 02 wires the StorageReference.child(...) call)",
            expectedPath,
            "WAVE-0-NOT-WIRED-YET",
        )
    }
}
