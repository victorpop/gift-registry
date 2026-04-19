package com.giftregistry.domain.usecase

import com.giftregistry.domain.preferences.LastRegistryPreferencesRepository
import javax.inject.Inject

class SetLastRegistryIdUseCase @Inject constructor(
    private val repository: LastRegistryPreferencesRepository
) {
    suspend operator fun invoke(registryId: String) {
        repository.setLastRegistryId(registryId)
    }
}
