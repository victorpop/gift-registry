package com.giftregistry.data.storage

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 12 — unit tests for [StorageRepositoryImpl] (D-04 / D-07; Phase 02 D-08).
 *
 * Plan 02 GREEN. The repo is now a thin `runCatching` wrapper around
 * [StorageDataSource]; this test mocks the data source so the FirebaseStorage
 * mocking lives in [StorageDataSourceTest] alone (D-05 path schema is asserted
 * there).
 *
 * Pins (D-04 / D-07; Phase 02 D-08):
 *   - Happy path: `uploadCover(...)` returns `Result.success(downloadUrl)`.
 *   - Failure path: data-source exceptions become `Result.failure(...)`
 *     carrying the original throwable — no FirebaseException leaks through.
 */
class StorageRepositoryImplTest {

    /** D-04 / D-07 happy path: `Result.success` carrying the download URL. */
    @Test
    fun uploadCover_happyPath_returnsDownloadUrl() = runTest {
        val expected = "https://firebasestorage.googleapis.com/v0/b/test/o/cover.jpg?token=abc"
        val dataSource = mockk<StorageDataSource>()
        coEvery {
            dataSource.uploadCoverBytes("uid-1", "reg-1", any())
        } returns expected

        val repo = StorageRepositoryImpl(dataSource)

        val result = repo.uploadCover(
            uid = "uid-1",
            registryId = "reg-1",
            jpegBytes = ByteArray(1024),
        )

        assertTrue("happy path must return Result.success", result.isSuccess)
        assertEquals(expected, result.getOrNull())
    }

    /**
     * D-07 / Phase 02 D-08 failure path: exceptions thrown by [StorageDataSource]
     * (which include FirebaseException instances when the real FirebaseStorage
     * SDK throws) become `Result.failure(...)` so the domain never sees them.
     */
    @Test
    fun uploadCover_failure_returnsResultFailure() = runTest {
        val expected = RuntimeException("storage rules denied")
        val dataSource = mockk<StorageDataSource>()
        coEvery {
            dataSource.uploadCoverBytes(any(), any(), any())
        } throws expected

        val repo = StorageRepositoryImpl(dataSource)

        val result = repo.uploadCover(
            uid = "uid-1",
            registryId = "reg-1",
            jpegBytes = ByteArray(1024),
        )

        assertTrue("runCatching must wrap exceptions into Result.failure", result.isFailure)
        assertSame(
            "the original throwable must be preserved so callers can branch on type",
            expected,
            result.exceptionOrNull(),
        )
    }

    /**
     * D-05 — path schema is asserted by [StorageDataSourceTest] which mocks
     * FirebaseStorage directly. The repository layer doesn't construct paths
     * any more, so this test is a forwarding contract: the repo MUST hand
     * (uid, registryId, bytes) verbatim to the data source.
     */
    @Test
    fun uploadCover_pathSchema_forwardsToDataSource() = runTest {
        val dataSource = mockk<StorageDataSource>()
        val capturedUid = io.mockk.slot<String>()
        val capturedRegistryId = io.mockk.slot<String>()
        coEvery {
            dataSource.uploadCoverBytes(capture(capturedUid), capture(capturedRegistryId), any())
        } returns "https://x"

        val repo = StorageRepositoryImpl(dataSource)
        repo.uploadCover(uid = "uid-9", registryId = "reg-9", jpegBytes = ByteArray(0))

        assertEquals("uid-9", capturedUid.captured)
        assertEquals("reg-9", capturedRegistryId.captured)
    }
}
