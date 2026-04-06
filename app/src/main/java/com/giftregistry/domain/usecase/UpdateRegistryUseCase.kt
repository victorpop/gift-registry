package com.giftregistry.domain.usecase

import com.giftregistry.domain.model.Registry
import com.giftregistry.domain.registry.RegistryRepository
import javax.inject.Inject

class UpdateRegistryUseCase @Inject constructor(
    private val repository: RegistryRepository
) {
    suspend operator fun invoke(registry: Registry): Result<Unit> =
        repository.updateRegistry(registry)
}
