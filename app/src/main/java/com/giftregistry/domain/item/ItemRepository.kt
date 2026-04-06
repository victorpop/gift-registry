package com.giftregistry.domain.item

import com.giftregistry.domain.model.Item
import com.giftregistry.domain.model.OgMetadata
import kotlinx.coroutines.flow.Flow

interface ItemRepository {
    fun observeItems(registryId: String): Flow<List<Item>>
    suspend fun addItem(registryId: String, item: Item): Result<String>
    suspend fun updateItem(registryId: String, item: Item): Result<Unit>
    suspend fun deleteItem(registryId: String, itemId: String): Result<Unit>
    suspend fun fetchOgMetadata(url: String): Result<OgMetadata>
}
