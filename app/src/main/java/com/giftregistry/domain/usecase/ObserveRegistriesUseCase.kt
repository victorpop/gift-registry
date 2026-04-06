package com.giftregistry.domain.usecase

import com.giftregistry.domain.model.Registry
import com.giftregistry.domain.registry.RegistryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveRegistriesUseCase @Inject constructor(
    private val repository: RegistryRepository
) {
    operator fun invoke(ownerId: String): Flow<List<Registry>> =
        repository.observeRegistries(ownerId)
}
