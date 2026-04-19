package com.giftregistry.domain.usecase

import com.giftregistry.domain.model.Store
import com.giftregistry.domain.store.StoreRepository
import javax.inject.Inject

class GetStoresUseCase @Inject constructor(
    private val repository: StoreRepository
) {
    suspend operator fun invoke(): Result<List<Store>> = repository.getStores()
}
