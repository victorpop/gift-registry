package com.giftregistry.domain.usecase

import com.giftregistry.domain.item.ItemRepository
import com.giftregistry.domain.model.Item
import javax.inject.Inject

class UpdateItemUseCase @Inject constructor(
    private val repository: ItemRepository
) {
    suspend operator fun invoke(registryId: String, item: Item): Result<Unit> =
        repository.updateItem(registryId, item)
}
