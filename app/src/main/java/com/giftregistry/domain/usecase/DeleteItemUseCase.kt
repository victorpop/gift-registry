package com.giftregistry.domain.usecase

import com.giftregistry.domain.item.ItemRepository
import javax.inject.Inject

class DeleteItemUseCase @Inject constructor(
    private val repository: ItemRepository
) {
    suspend operator fun invoke(registryId: String, itemId: String): Result<Unit> =
        repository.deleteItem(registryId, itemId)
}
