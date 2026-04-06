package com.giftregistry.data.item

import com.giftregistry.domain.item.ItemRepository
import com.giftregistry.domain.model.Item
import com.giftregistry.domain.model.OgMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeItemRepository : ItemRepository {
    private val items = MutableStateFlow<List<Item>>(emptyList())
    var shouldFail = false
    var ogResponse = OgMetadata(title = "Test Product", imageUrl = "https://img.test/1.jpg", price = "99.99")

    override fun observeItems(registryId: String): Flow<List<Item>> =
        items.map { list -> list.filter { it.registryId == registryId } }

    override suspend fun addItem(registryId: String, item: Item): Result<String> {
        if (shouldFail) return Result.failure(RuntimeException("Fake error"))
        val id = "fake-item-${items.value.size + 1}"
        items.value = items.value + item.copy(id = id, registryId = registryId)
        return Result.success(id)
    }

    override suspend fun updateItem(registryId: String, item: Item): Result<Unit> {
        if (shouldFail) return Result.failure(RuntimeException("Fake error"))
        items.value = items.value.map { if (it.id == item.id) item else it }
        return Result.success(Unit)
    }

    override suspend fun deleteItem(registryId: String, itemId: String): Result<Unit> {
        if (shouldFail) return Result.failure(RuntimeException("Fake error"))
        items.value = items.value.filter { it.id != itemId }
        return Result.success(Unit)
    }

    override suspend fun fetchOgMetadata(url: String): Result<OgMetadata> {
        if (shouldFail) return Result.failure(RuntimeException("Fetch failed"))
        return Result.success(ogResponse)
    }

    // Helper to seed items for observation tests
    fun seedItems(newItems: List<Item>) {
        items.value = newItems
    }
}
