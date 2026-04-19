package com.giftregistry.data.reservation

import com.google.android.gms.tasks.Tasks
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.HttpsCallableReference
import com.google.firebase.functions.HttpsCallableResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReservationRepositoryConfirmPurchaseTest {

    @Test
    fun `confirmPurchase success returns Result_success_Unit`() = runTest {
        val functions = mockk<FirebaseFunctions>()
        val callable = mockk<HttpsCallableReference>()
        val result = mockk<HttpsCallableResult>(relaxed = true)
        every { functions.getHttpsCallable("confirmPurchase") } returns callable
        every { callable.call(any()) } returns Tasks.forResult(result)

        val repo = ReservationRepositoryImpl(functions)
        val actual = repo.confirmPurchase("res1")
        assertTrue(actual.isSuccess)
    }

    @Test
    fun `confirmPurchase failure propagates error`() = runTest {
        val functions = mockk<FirebaseFunctions>()
        val callable = mockk<HttpsCallableReference>()
        every { functions.getHttpsCallable("confirmPurchase") } returns callable
        every { callable.call(any()) } returns Tasks.forException(RuntimeException("RESERVATION_EXPIRED"))

        val repo = ReservationRepositoryImpl(functions)
        val actual = repo.confirmPurchase("res1")
        assertTrue(actual.isFailure)
        assertEquals("RESERVATION_EXPIRED", actual.exceptionOrNull()?.message)
    }

    @Test
    fun `confirmPurchase passes reservationId in payload`() = runTest {
        val functions = mockk<FirebaseFunctions>()
        val callable = mockk<HttpsCallableReference>()
        val result = mockk<HttpsCallableResult>(relaxed = true)
        val captor = slot<Map<String, Any?>>()
        every { functions.getHttpsCallable("confirmPurchase") } returns callable
        every { callable.call(capture(captor)) } returns Tasks.forResult(result)

        val repo = ReservationRepositoryImpl(functions)
        repo.confirmPurchase("res-abc")
        assertEquals(mapOf("reservationId" to "res-abc"), captor.captured)
    }
}
