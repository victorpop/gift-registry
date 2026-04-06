package com.giftregistry.domain.usecase

import com.giftregistry.data.registry.FakeRegistryRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InviteToRegistryUseCaseTest {
    private val fakeRepo = FakeRegistryRepository()
    private val useCase = InviteToRegistryUseCase(fakeRepo)

    @Test
    fun `inviteUser calls repository with correct args`() = runTest {
        val result = useCase("reg-1", "friend@example.com")
        assertTrue(result.isSuccess)
        assertEquals(1, fakeRepo.inviteLog.size)
        assertEquals("reg-1" to "friend@example.com", fakeRepo.inviteLog[0])
    }

    @Test
    fun `inviteUser returns failure when repository fails`() = runTest {
        fakeRepo.shouldFail = true
        val result = useCase("reg-1", "friend@example.com")
        assertTrue(result.isFailure)
    }
}
