package com.giftregistry.domain.usecase

import com.giftregistry.domain.item.ItemRepository
import com.giftregistry.domain.model.OgMetadata
import javax.inject.Inject

class FetchOgMetadataUseCase @Inject constructor(private val repository: ItemRepository) {
    suspend operator fun invoke(url: String): Result<OgMetadata> =
        repository.fetchOgMetadata(url)
}
