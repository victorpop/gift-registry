package com.giftregistry.domain.preferences

import kotlinx.coroutines.flow.Flow

interface LastRegistryPreferencesRepository {
    fun observeLastRegistryId(): Flow<String?>
    suspend fun getLastRegistryId(): String?
    suspend fun setLastRegistryId(registryId: String)
    suspend fun clearLastRegistryId()
}
