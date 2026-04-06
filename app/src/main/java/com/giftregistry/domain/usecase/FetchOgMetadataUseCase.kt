package com.giftregistry.domain.usecase

import com.giftregistry.domain.item.ItemRepository
import com.giftregistry.domain.model.OgMetadata

class FetchOgMetadataUseCase(private val repository: ItemRepository) {
    suspend operator fun invoke(url: String): Result<OgMetadata> =
        repository.fetchOgMetadata(url)
}
