package com.giftregistry.domain.usecase

import com.giftregistry.data.registry.FakeRegistryRepository
import com.giftregistry.domain.model.Registry
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateRegistryUseCaseTest {
    private val fakeRepo = FakeRegistryRepository()
    private val useCase = CreateRegistryUseCase(fakeRepo)

    @Test
    fun `createRegistry returns success with generated ID`() = runTest {
        val registry = Registry(ownerId = "user-1", title = "Test Registry", occasion = "birthday")
        val result = useCase(registry)
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!.startsWith("fake-"))
    }

    @Test
    fun `createRegistry returns failure when repository fails`() = runTest {
        fakeRepo.shouldFail = true
        val registry = Registry(ownerId = "user-1", title = "Test", occasion = "wedding")
        val result = useCase(registry)
        assertTrue(result.isFailure)
    }
}
