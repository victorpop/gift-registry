package com.giftregistry.data.registry

import com.giftregistry.data.model.RegistryDto
import com.giftregistry.domain.model.Registry
import com.giftregistry.domain.registry.RegistryRepository
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RegistryRepositoryImpl @Inject constructor(
    private val dataSource: FirestoreDataSource,
    private val functions: FirebaseFunctions
) : RegistryRepository {

    override fun observeRegistries(ownerId: String): Flow<List<Registry>> =
        dataSource.observeRegistries(ownerId).map { dtos -> dtos.map { it.toDomain() } }

    override fun observeRegistry(registryId: String): Flow<Registry?> =
        dataSource.observeRegistry(registryId).map { it?.toDomain() }

    override suspend fun createRegistry(registry: Registry): Result<String> =
        runCatching { dataSource.createRegistry(registry.toMap()) }

    override suspend fun updateRegistry(registry: Registry): Result<Unit> =
        runCatching { dataSource.updateRegistry(registry.id, registry.toUpdateMap()) }

    override suspend fun deleteRegistry(registryId: String): Result<Unit> =
        runCatching { dataSource.deleteRegistry(registryId) }

    override suspend fun inviteUser(registryId: String, email: String): Result<Unit> =
        runCatching {
            functions.getHttpsCallable("inviteToRegistry")
                .call(mapOf("registryId" to registryId, "email" to email))
                .await()
            Unit
        }

    private fun RegistryDto.toDomain() = Registry(
        id = id, ownerId = ownerId, title = title, occasion = occasion,
        visibility = visibility, eventDateMs = eventDateMs, eventLocation = eventLocation,
        description = description, locale = locale, notificationsEnabled = notificationsEnabled,
        // Coerce raw Firestore values to Boolean. New invites write `true`, but
        // legacy documents from the pre-FieldPath inviteToRegistry may contain a
        // nested Map at this key — in that case the user was still invited, so
        // treat any non-null value as `true` rather than dropping the entry.
        invitedUsers = invitedUsers.mapValues { (_, value) ->
            when (value) {
                is Boolean -> value
                null -> false
                else -> true
            }
        },
        createdAt = createdAt, updatedAt = updatedAt
    )

    private fun Registry.toMap(): Map<String, Any?> = mapOf(
        "ownerId" to ownerId, "title" to title, "occasion" to occasion,
        "visibility" to visibility, "eventDateMs" to eventDateMs,
        "eventLocation" to eventLocation, "description" to description,
        "locale" to locale, "notificationsEnabled" to notificationsEnabled,
        "invitedUsers" to invitedUsers.ifEmpty { emptyMap<String, Boolean>() },
        "createdAt" to System.currentTimeMillis(), "updatedAt" to System.currentTimeMillis()
    )

    private fun Registry.toUpdateMap(): Map<String, Any?> = mapOf(
        "title" to title, "occasion" to occasion, "visibility" to visibility,
        "eventDateMs" to eventDateMs, "eventLocation" to eventLocation,
        "description" to description, "notificationsEnabled" to notificationsEnabled,
        "updatedAt" to System.currentTimeMillis()
    )
}
