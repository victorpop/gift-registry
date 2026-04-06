package com.giftregistry.domain.usecase

import com.giftregistry.data.item.FakeItemRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FetchOgMetadataUseCaseTest {
    private val fakeRepo = FakeItemRepository()
    private val useCase = FetchOgMetadataUseCase(fakeRepo)

    @Test
    fun `fetchOgMetadata returns metadata on success`() = runTest {
        val result = useCase("https://www.emag.ro/product-123")
        assertTrue(result.isSuccess)
        assertEquals("Test Product", result.getOrNull()?.title)
    }

    @Test
    fun `fetchOgMetadata returns failure when fetch fails`() = runTest {
        fakeRepo.shouldFail = true
        val result = useCase("https://www.emag.ro/product-123")
        assertTrue(result.isFailure)
    }
}
