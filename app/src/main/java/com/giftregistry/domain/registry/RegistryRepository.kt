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

    /**
     * Phase 12 — generates a fresh registry ID without writing a Firestore
     * document. Required by the D-07 upload-then-write flow: the cover-photo
     * upload path is `users/{uid}/registries/{registryId}/cover.jpg`, so the
     * client needs the final registryId BEFORE the registry doc is written
     * (Pitfall 2 mitigation — see RESEARCH.md "Note on targetRegistryId").
     *
     * Plan 02 wires this to `firestore.collection("registries").document().id`
     * (Firestore client-side ID generation — no server roundtrip). Wave 0
     * default returns `""` so the RED test asserts non-blank and fails.
     */
    fun newRegistryId(): String = ""
}
