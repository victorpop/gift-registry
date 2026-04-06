package com.giftregistry.domain.usecase

import com.giftregistry.domain.registry.RegistryRepository
import javax.inject.Inject

class InviteToRegistryUseCase @Inject constructor(private val repository: RegistryRepository) {
    suspend operator fun invoke(registryId: String, email: String): Result<Unit> =
        repository.inviteUser(registryId, email)
}
