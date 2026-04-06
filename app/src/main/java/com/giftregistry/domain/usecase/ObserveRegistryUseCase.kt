package com.giftregistry.domain.usecase

import com.giftregistry.domain.model.Registry
import com.giftregistry.domain.registry.RegistryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveRegistryUseCase @Inject constructor(
    private val repository: RegistryRepository
) {
    operator fun invoke(registryId: String): Flow<Registry?> =
        repository.observeRegistry(registryId)
}
