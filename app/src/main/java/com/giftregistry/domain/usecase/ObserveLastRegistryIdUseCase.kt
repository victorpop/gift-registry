package com.giftregistry.domain.usecase

import com.giftregistry.domain.preferences.LastRegistryPreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveLastRegistryIdUseCase @Inject constructor(
    private val repository: LastRegistryPreferencesRepository
) {
    operator fun invoke(): Flow<String?> = repository.observeLastRegistryId()
}
