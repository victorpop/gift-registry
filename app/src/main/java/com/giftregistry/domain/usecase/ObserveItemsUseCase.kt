package com.giftregistry.domain.usecase

import com.giftregistry.domain.item.ItemRepository
import com.giftregistry.domain.model.Item
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveItemsUseCase @Inject constructor(private val repository: ItemRepository) {
    operator fun invoke(registryId: String): Flow<List<Item>> =
        repository.observeItems(registryId)
}
