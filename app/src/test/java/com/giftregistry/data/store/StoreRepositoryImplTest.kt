package com.giftregistry.data.store

import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreRepositoryImplTest {

    private fun mockSnapshotWithStores(stores: List<Map<String, Any?>>?): Pair<FirebaseFirestore, DocumentSnapshot> {
        val firestore = mockk<FirebaseFirestore>()
        val collectionRef = mockk<CollectionReference>()
        val docRef = mockk<DocumentReference>()
        val snapshot = mockk<DocumentSnapshot>()
        every { firestore.collection("config") } returns collectionRef
        every { collectionRef.document("stores") } returns docRef
        every { docRef.get() } returns Tasks.forResult(snapshot)
        every { snapshot.get("stores") } returns stores
        return firestore to snapshot
    }

    @Test
    fun `sorts stores by displayOrder ascending`() = runTest {
        val (firestore, _) = mockSnapshotWithStores(
            listOf(
                mapOf("id" to "c", "name" to "C", "homepageUrl" to "https://c", "displayOrder" to 30L, "logoAsset" to "store_c"),
                mapOf("id" to "a", "name" to "A", "homepageUrl" to "https://a", "displayOrder" to 10L, "logoAsset" to "store_a"),
                mapOf("id" to "b", "name" to "B", "homepageUrl" to "https://b", "displayOrder" to 20L, "logoAsset" to "store_b"),
            )
        )
        val repo = StoreRepositoryImpl(firestore)
        val result = repo.getStores()
        assertTrue(result.isSuccess)
        assertEquals(listOf("a", "b", "c"), result.getOrThrow().map { it.id })
    }

    @Test
    fun `falls back to store_generic when logoAsset missing`() = runTest {
        val (firestore, _) = mockSnapshotWithStores(
            listOf(
                mapOf("id" to "x", "name" to "X", "homepageUrl" to "https://x", "displayOrder" to 10L),
            )
        )
        val repo = StoreRepositoryImpl(firestore)
        val store = repo.getStores().getOrThrow().single()
        assertEquals("store_generic", store.logoAsset)
    }

    @Test
    fun `returns failure when firestore get throws`() = runTest {
        val firestore = mockk<FirebaseFirestore>()
        val collectionRef = mockk<CollectionReference>()
        val docRef = mockk<DocumentReference>()
        every { firestore.collection("config") } returns collectionRef
        every { collectionRef.document("stores") } returns docRef
        every { docRef.get() } returns Tasks.forException(RuntimeException("network"))
        val repo = StoreRepositoryImpl(firestore)
        val result = repo.getStores()
        assertTrue(result.isFailure)
    }

    @Test
    fun `returns empty list when stores array absent`() = runTest {
        val (firestore, _) = mockSnapshotWithStores(null)
        val repo = StoreRepositoryImpl(firestore)
        val result = repo.getStores()
        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().isEmpty())
    }
}
