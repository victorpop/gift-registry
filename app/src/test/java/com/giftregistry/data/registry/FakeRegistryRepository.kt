package com.giftregistry.data.registry

import com.giftregistry.domain.model.Registry
import com.giftregistry.domain.registry.RegistryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeRegistryRepository : RegistryRepository {
    private val registries = MutableStateFlow<List<Registry>>(emptyList())
    var shouldFail = false
    var inviteLog = mutableListOf<Pair<String, String>>()

    override fun observeRegistries(ownerId: String): Flow<List<Registry>> =
        registries.map { list -> list.filter { it.ownerId == ownerId } }

    override fun observeRegistry(registryId: String): Flow<Registry?> =
        registries.map { list -> list.firstOrNull { it.id == registryId } }

    override suspend fun createRegistry(registry: Registry): Result<String> {
        if (shouldFail) return Result.failure(RuntimeException("Fake error"))
        val id = "fake-${registries.value.size + 1}"
        registries.value = registries.value + registry.copy(id = id)
        return Result.success(id)
    }

    override suspend fun updateRegistry(registry: Registry): Result<Unit> {
        if (shouldFail) return Result.failure(RuntimeException("Fake error"))
        registries.value = registries.value.map { if (it.id == registry.id) registry else it }
        return Result.success(Unit)
    }

    override suspend fun deleteRegistry(registryId: String): Result<Unit> {
        if (shouldFail) return Result.failure(RuntimeException("Fake error"))
        registries.value = registries.value.filter { it.id != registryId }
        return Result.success(Unit)
    }

    override suspend fun inviteUser(registryId: String, email: String): Result<Unit> {
        if (shouldFail) return Result.failure(RuntimeException("Fake error"))
        inviteLog.add(registryId to email)
        return Result.success(Unit)
    }
}
