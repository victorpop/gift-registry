package com.giftregistry.domain.usecase

import com.giftregistry.domain.registry.RegistryRepository
import javax.inject.Inject

class DeleteRegistryUseCase @Inject constructor(
    private val repository: RegistryRepository
) {
    suspend operator fun invoke(registryId: String): Result<Unit> =
        repository.deleteRegistry(registryId)
}
