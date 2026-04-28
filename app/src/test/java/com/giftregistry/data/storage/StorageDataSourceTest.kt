package com.giftregistry.data.storage

import android.net.Uri
import com.google.android.gms.tasks.Tasks
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Phase 12 — unit tests for [StorageDataSource] (D-04 / D-05 / D-07).
 *
 * Mocks the [FirebaseStorage] -> [StorageReference] -> upload+download chain
 * and asserts:
 *
 *   1. The canonical D-05 path schema is constructed as
 *      `users/{uid}/registries/{registryId}/cover.jpg`.
 *   2. The happy path returns the download URL string.
 *   3. Failures from `putBytes(...).await()` propagate to the caller (the
 *      `runCatching` wrapper lives in [StorageRepositoryImpl] — the data
 *      source itself surfaces raw exceptions).
 *
 * NOTE: mockk relaxed mode is required to mock final Firebase classes.
 * We use `Tasks.forResult` / `Tasks.forException` to feed the await()
 * coroutine bridge.
 */
class StorageDataSourceTest {

    private fun mockStorageWithChild(
        pathSlot: io.mockk.CapturingSlot<String>,
        downloadUrl: String? = "https://firebasestorage.googleapis.com/example",
        // Task.getException() returns Exception? (not Throwable?), so the
        // mocked exception must be at least an Exception subtype.
        uploadException: Exception? = null,
    ): FirebaseStorage {
        val storage = mockk<FirebaseStorage>(relaxed = true)
        val rootRef = mockk<StorageReference>(relaxed = true)
        val childRef = mockk<StorageReference>(relaxed = true)
        val uploadTask = mockk<UploadTask>(relaxed = true)

        every { storage.reference } returns rootRef
        every { rootRef.child(capture(pathSlot)) } returns childRef
        every { childRef.putBytes(any()) } returns uploadTask

        // UploadTask is a Task<UploadTask.TaskSnapshot>. await() bridges it
        // via kotlinx-coroutines-play-services. Use Tasks.forResult/
        // forException so the bridge resumes/throws as expected.
        if (uploadException != null) {
            every { uploadTask.isComplete } returns true
            every { uploadTask.isCanceled } returns false
            every { uploadTask.isSuccessful } returns false
            every { uploadTask.exception } returns uploadException
            every { uploadTask.result } throws uploadException
            // The await() extension polls these properties and an
            // OnCompleteListener; relaxed mock returns the same task back.
            every { uploadTask.addOnCompleteListener(any()) } answers {
                @Suppress("UNCHECKED_CAST")
                val listener =
                    arg<com.google.android.gms.tasks.OnCompleteListener<UploadTask.TaskSnapshot>>(0)
                listener.onComplete(uploadTask)
                uploadTask
            }
        } else {
            val snapshot = mockk<UploadTask.TaskSnapshot>(relaxed = true)
            every { uploadTask.isComplete } returns true
            every { uploadTask.isCanceled } returns false
            every { uploadTask.isSuccessful } returns true
            every { uploadTask.exception } returns null
            every { uploadTask.result } returns snapshot
            every { uploadTask.addOnCompleteListener(any()) } answers {
                @Suppress("UNCHECKED_CAST")
                val listener =
                    arg<com.google.android.gms.tasks.OnCompleteListener<UploadTask.TaskSnapshot>>(0)
                listener.onComplete(uploadTask)
                uploadTask
            }
        }

        // downloadUrl returns Task<Uri>; mock to return a URI parseable from
        // the supplied string. Uri.parse is a static call into android.net,
        // so mock the Task itself rather than going through Uri.parse.
        val uri = mockk<Uri>(relaxed = true)
        every { uri.toString() } returns (downloadUrl ?: "")
        every { childRef.downloadUrl } returns Tasks.forResult(uri)

        return storage
    }

    /**
     * D-05 — path schema: re-uploads overwrite cleanly so there are no
     * orphan blobs in Storage.
     */
    @Test
    fun uploadCoverBytes_callsCanonicalPath() = runTest {
        val pathSlot = slot<String>()
        val storage = mockStorageWithChild(pathSlot)
        val dataSource = StorageDataSource(storage)

        dataSource.uploadCoverBytes(
            uid = "uid-1",
            registryId = "reg-1",
            jpegBytes = ByteArray(1024),
        )

        assertEquals(
            "D-05: Storage path MUST be users/{uid}/registries/{registryId}/cover.jpg",
            "users/uid-1/registries/reg-1/cover.jpg",
            pathSlot.captured,
        )
    }

    /**
     * D-04 / D-07 happy path: a successful Storage upload returns the
     * download URL string.
     */
    @Test
    fun uploadCoverBytes_returnsDownloadUrl() = runTest {
        val pathSlot = slot<String>()
        val expectedUrl = "https://firebasestorage.googleapis.com/v0/b/test/o/cover.jpg?token=abc"
        val storage = mockStorageWithChild(pathSlot, downloadUrl = expectedUrl)
        val dataSource = StorageDataSource(storage)

        val url = dataSource.uploadCoverBytes(
            uid = "uid-1",
            registryId = "reg-1",
            jpegBytes = ByteArray(1024),
        )

        assertEquals(expectedUrl, url)
    }

    /**
     * D-07 failure path: putBytes() throwing surfaces to the caller. The
     * `runCatching` wrapper lives one layer up in [StorageRepositoryImpl];
     * the data source itself is exception-transparent.
     */
    @Test(expected = RuntimeException::class)
    fun uploadCoverBytes_propagatesUploadFailure() = runTest {
        val pathSlot = slot<String>()
        val storage = mockStorageWithChild(
            pathSlot,
            uploadException = RuntimeException("network"),
        )
        val dataSource = StorageDataSource(storage)

        dataSource.uploadCoverBytes(
            uid = "uid-1",
            registryId = "reg-1",
            jpegBytes = ByteArray(1024),
        )
    }
}
