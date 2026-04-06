package com.giftregistry.domain.usecase

import app.cash.turbine.test
import com.giftregistry.data.item.FakeItemRepository
import com.giftregistry.domain.model.Item
import com.giftregistry.domain.model.ItemStatus
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveItemsUseCaseTest {
    private val fakeRepo = FakeItemRepository()
    private val useCase = ObserveItemsUseCase(fakeRepo)

    @Test
    fun `observeItems emits items for given registryId`() = runTest {
        val items = listOf(
            Item(id = "i1", registryId = "reg-1", title = "Gift 1", status = ItemStatus.AVAILABLE),
            Item(id = "i2", registryId = "reg-1", title = "Gift 2", status = ItemStatus.RESERVED)
        )
        fakeRepo.seedItems(items)

        useCase("reg-1").test {
            val emitted = awaitItem()
            assertEquals(2, emitted.size)
            assertEquals(ItemStatus.AVAILABLE, emitted[0].status)
            assertEquals(ItemStatus.RESERVED, emitted[1].status)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `observeItems filters by registryId`() = runTest {
        fakeRepo.seedItems(listOf(
            Item(id = "i1", registryId = "reg-1", title = "Gift 1"),
            Item(id = "i2", registryId = "reg-2", title = "Gift 2")
        ))

        useCase("reg-1").test {
            val emitted = awaitItem()
            assertEquals(1, emitted.size)
            assertEquals("reg-1", emitted[0].registryId)
            cancelAndConsumeRemainingEvents()
        }
    }
}
