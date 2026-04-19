package com.giftregistry.domain.usecase

import com.giftregistry.domain.model.GuestUser
import com.giftregistry.domain.model.ReservationResult
import com.giftregistry.domain.reservation.ReservationRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReserveItemUseCaseTest {

    private class FakeReservationRepository(
        private val response: Result<ReservationResult>,
    ) : ReservationRepository {
        var lastRegistryId: String? = null
        var lastItemId: String? = null
        var lastGiver: GuestUser? = null
        var lastGiverId: String? = null

        override suspend fun reserve(
            registryId: String,
            itemId: String,
            giver: GuestUser,
            giverId: String?,
        ): Result<ReservationResult> {
            lastRegistryId = registryId
            lastItemId = itemId
            lastGiver = giver
            lastGiverId = giverId
            return response
        }

        override suspend fun resolve(reservationId: String): Result<com.giftregistry.domain.model.ReservationLookup> =
            Result.failure(UnsupportedOperationException("not used in ReserveItemUseCaseTest"))

        override suspend fun confirmPurchase(reservationId: String): Result<Unit> =
            Result.failure(UnsupportedOperationException("not used in ReserveItemUseCaseTest"))
    }

    private val guest = GuestUser(firstName = "Ana", lastName = "Pop", email = "ana@example.com")

    @Test
    fun `reserve success returns ReservationResult`() = runTest {
        val expected = ReservationResult("res-1", "https://aff.example/x", 999L)
        val useCase = ReserveItemUseCase(FakeReservationRepository(Result.success(expected)))

        val result = useCase("reg-1", "item-1", guest, giverId = null)

        assertEquals(Result.success(expected), result)
    }

    @Test
    fun `reserve failure forwards repository error`() = runTest {
        val err = IllegalStateException("ITEM_UNAVAILABLE")
        val useCase = ReserveItemUseCase(FakeReservationRepository(Result.failure(err)))

        val result = useCase("reg-1", "item-1", guest, giverId = null)

        assertTrue(result.isFailure)
        assertEquals("ITEM_UNAVAILABLE", result.exceptionOrNull()?.message)
    }

    @Test
    fun `use case passes guest identity and ids to repository`() = runTest {
        val repo = FakeReservationRepository(
            Result.success(ReservationResult("r", "u", 0L))
        )
        val useCase = ReserveItemUseCase(repo)

        useCase("reg-42", "item-99", guest, giverId = "uid-7")

        assertEquals("reg-42", repo.lastRegistryId)
        assertEquals("item-99", repo.lastItemId)
        assertEquals(guest, repo.lastGiver)
        assertEquals("uid-7", repo.lastGiverId)
    }
}
