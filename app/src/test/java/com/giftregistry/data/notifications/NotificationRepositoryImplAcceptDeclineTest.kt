package com.giftregistry.data.notifications

import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.HttpsCallableReference
import com.google.firebase.functions.HttpsCallableResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals

/**
 * Wave 0 RED test (Plan 16-01) — D-27 contract for the new
 * accept/decline callable wrappers on [NotificationRepositoryImpl].
 *
 * Today, [NotificationRepositoryImpl] only takes [FirebaseFirestore].
 * Plan 16-03 will add a second [FirebaseFunctions] constructor parameter
 * and implement `acceptInvite` / `declineInvite`. Until then this file
 * fails to compile — that IS the RED state.
 *
 * Pattern mirrors [com.giftregistry.data.reservation.ReservationRepositoryImpl]:
 *   `functions.getHttpsCallable("name").call(mapOf(...))` wrapped in runCatching.
 */
class NotificationRepositoryImplAcceptDeclineTest {

    @Test
    fun `acceptInvite calls acceptInvite callable with registryId`() = runTest {
        val firestore = mockk<FirebaseFirestore>(relaxed = true)
        val functions = mockk<FirebaseFunctions>()
        val nameSlot = slot<String>()
        val argSlot = slot<Map<String, Any>>()
        every { functions.getHttpsCallable(capture(nameSlot)) } answers {
            val callable = mockk<HttpsCallableReference>()
            every { callable.call(capture(argSlot)) } returns Tasks.forResult(mockk<HttpsCallableResult>())
            callable
        }
        val repo = NotificationRepositoryImpl(firestore, functions)

        val result = repo.acceptInvite("reg-123")

        assertTrue(result.isSuccess)
        assertEquals("acceptInvite", nameSlot.captured)
        assertEquals(mapOf("registryId" to "reg-123"), argSlot.captured)
    }

    @Test
    fun `declineInvite calls declineInvite callable with registryId`() = runTest {
        val firestore = mockk<FirebaseFirestore>(relaxed = true)
        val functions = mockk<FirebaseFunctions>()
        val nameSlot = slot<String>()
        val argSlot = slot<Map<String, Any>>()
        every { functions.getHttpsCallable(capture(nameSlot)) } answers {
            val callable = mockk<HttpsCallableReference>()
            every { callable.call(capture(argSlot)) } returns Tasks.forResult(mockk<HttpsCallableResult>())
            callable
        }
        val repo = NotificationRepositoryImpl(firestore, functions)

        val result = repo.declineInvite("reg-456")

        assertTrue(result.isSuccess)
        assertEquals("declineInvite", nameSlot.captured)
        assertEquals(mapOf("registryId" to "reg-456"), argSlot.captured)
    }

    @Test
    fun `acceptInvite wraps thrown exception in Result failure`() = runTest {
        val firestore = mockk<FirebaseFirestore>(relaxed = true)
        val functions = mockk<FirebaseFunctions>()
        val callable = mockk<HttpsCallableReference>()
        every { functions.getHttpsCallable("acceptInvite") } returns callable
        every { callable.call(any()) } returns Tasks.forException(RuntimeException("network"))
        val repo = NotificationRepositoryImpl(firestore, functions)

        val result = repo.acceptInvite("reg-x")

        assertTrue(result.isFailure)
    }

    @Test
    fun `declineInvite wraps thrown exception in Result failure`() = runTest {
        val firestore = mockk<FirebaseFirestore>(relaxed = true)
        val functions = mockk<FirebaseFunctions>()
        val callable = mockk<HttpsCallableReference>()
        every { functions.getHttpsCallable("declineInvite") } returns callable
        every { callable.call(any()) } returns Tasks.forException(RuntimeException("network"))
        val repo = NotificationRepositoryImpl(firestore, functions)

        val result = repo.declineInvite("reg-y")

        assertTrue(result.isFailure)
    }
}
