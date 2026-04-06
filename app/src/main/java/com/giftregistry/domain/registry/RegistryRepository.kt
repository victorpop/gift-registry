package com.giftregistry.domain.registry

import com.giftregistry.domain.model.Registry
import kotlinx.coroutines.flow.Flow

interface RegistryRepository {
    fun observeRegistries(ownerId: String): Flow<List<Registry>>
    fun observeRegistry(registryId: String): Flow<Registry?>
    suspend fun createRegistry(registry: Registry): Result<String>
    suspend fun updateRegistry(registry: Registry): Result<Unit>
    suspend fun deleteRegistry(registryId: String): Result<Unit>
    suspend fun inviteUser(registryId: String, email: String): Result<Unit>
}
