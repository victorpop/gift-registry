package com.giftregistry.domain.usecase

import com.giftregistry.domain.reservation.ReservationRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfirmPurchaseUseCaseTest {

    @Test
    fun `invoke delegates to repository`() = runTest {
        val repo = mockk<ReservationRepository>()
        coEvery { repo.confirmPurchase("res1") } returns Result.success(Unit)

        val useCase = ConfirmPurchaseUseCase(repo)
        val actual = useCase("res1")

        assertTrue(actual.isSuccess)
        coVerify(exactly = 1) { repo.confirmPurchase("res1") }
    }

    @Test
    fun `invoke propagates failure from repository`() = runTest {
        val repo = mockk<ReservationRepository>()
        val err = RuntimeException("RESERVATION_EXPIRED")
        coEvery { repo.confirmPurchase(any()) } returns Result.failure(err)

        val useCase = ConfirmPurchaseUseCase(repo)
        val actual = useCase("res1")
        assertTrue(actual.isFailure)
        assertEquals(err, actual.exceptionOrNull())
    }
}
