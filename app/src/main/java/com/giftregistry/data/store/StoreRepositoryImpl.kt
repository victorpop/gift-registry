package com.giftregistry.data.store

import com.giftregistry.data.model.StoreDto
import com.giftregistry.domain.model.Store
import com.giftregistry.domain.store.StoreRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StoreRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
) : StoreRepository {

    override suspend fun getStores(): Result<List<Store>> = runCatching {
        val snapshot = firestore.collection("config").document("stores").get().await()
        @Suppress("UNCHECKED_CAST")
        val rawList = snapshot.get("stores") as? List<Map<String, Any?>> ?: emptyList()
        rawList.map { map -> mapToDto(map) }
            .sortedBy { it.displayOrder }
            .map { it.toDomain() }
    }

    private fun mapToDto(map: Map<String, Any?>): StoreDto = StoreDto(
        id = map["id"] as? String ?: "",
        name = map["name"] as? String ?: "",
        homepageUrl = map["homepageUrl"] as? String ?: "",
        displayOrder = (map["displayOrder"] as? Number)?.toInt() ?: 0,
        logoAsset = map["logoAsset"] as? String ?: "store_generic",
    )
}
