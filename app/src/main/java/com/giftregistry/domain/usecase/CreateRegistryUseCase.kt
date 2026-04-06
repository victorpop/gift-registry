package com.giftregistry.domain.usecase

import com.giftregistry.domain.model.Registry
import com.giftregistry.domain.registry.RegistryRepository

class CreateRegistryUseCase(private val repository: RegistryRepository) {
    suspend operator fun invoke(registry: Registry): Result<String> =
        repository.createRegistry(registry)
}
