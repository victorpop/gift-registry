package com.giftregistry.domain.usecase

import com.giftregistry.domain.model.Registry
import com.giftregistry.domain.registry.RegistryRepository
import javax.inject.Inject

class CreateRegistryUseCase @Inject constructor(private val repository: RegistryRepository) {
    suspend operator fun invoke(registry: Registry): Result<String> =
        repository.createRegistry(registry)
}
