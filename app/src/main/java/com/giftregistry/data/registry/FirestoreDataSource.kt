package com.giftregistry.data.registry

import com.giftregistry.data.model.ItemDto
import com.giftregistry.data.model.RegistryDto
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    // --- Registries ---

    fun observeRegistries(ownerId: String): Flow<List<RegistryDto>> = callbackFlow {
        val listener = firestore.collection("registries")
            .whereEqualTo("ownerId", ownerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val registries = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(RegistryDto::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(registries)
            }
        awaitClose { listener.remove() }
    }

    fun observeRegistry(registryId: String): Flow<RegistryDto?> = callbackFlow {
        val listener = firestore.collection("registries").document(registryId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val registry = snapshot?.toObject(RegistryDto::class.java)?.copy(id = snapshot.id)
                trySend(registry)
            }
        awaitClose { listener.remove() }
    }

    suspend fun createRegistry(data: Map<String, Any?>): String {
        val ref = firestore.collection("registries").document()
        ref.set(data).await()
        return ref.id
    }

    suspend fun updateRegistry(registryId: String, data: Map<String, Any?>) {
        firestore.collection("registries").document(registryId).update(data).await()
    }

    suspend fun deleteRegistry(registryId: String) {
        firestore.collection("registries").document(registryId).delete().await()
    }

    // --- Items (subcollection per D-01) ---

    fun observeItems(registryId: String): Flow<List<ItemDto>> = callbackFlow {
        val listener = firestore.collection("registries").document(registryId)
            .collection("items")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val items = snapshot?.documents?.mapNotNull { doc ->
                    val expiresAtMs = doc.getTimestamp("expiresAt")?.toDate()?.time
                    doc.toObject(ItemDto::class.java)?.copy(
                        id = doc.id,
                        expiresAt = expiresAtMs,
                    )
                } ?: emptyList()
                trySend(items)
            }
        awaitClose { listener.remove() }
    }

    suspend fun addItem(registryId: String, data: Map<String, Any?>): String {
        val ref = firestore.collection("registries").document(registryId)
            .collection("items").document()
        ref.set(data).await()
        return ref.id
    }

    suspend fun updateItem(registryId: String, itemId: String, data: Map<String, Any?>) {
        firestore.collection("registries").document(registryId)
            .collection("items").document(itemId).update(data).await()
    }

    suspend fun deleteItem(registryId: String, itemId: String) {
        firestore.collection("registries").document(registryId)
            .collection("items").document(itemId).delete().await()
    }
}
