package com.giftregistry.data.discover

import com.giftregistry.domain.discover.DiscoverProduct
import com.giftregistry.domain.discover.DiscoverRepository
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase 17 D-40 — wraps the two Discover Cloud Functions Callables.
 *
 * `FirebaseFunctions` is provided by `di/AppModule.kt`
 * (`FirebaseFunctions.getInstance("europe-west3")`) — same provider as
 * ReservationRepositoryImpl / NotificationRepositoryImpl, no per-feature
 * region override needed.
 *
 * Mapping contract (D-49):
 * - Empty `products` array → Result.success(emptyList())
 * - Callable failure (FirebaseFunctionsException, network, etc.) →
 *   Result.failure(<exception>) via runCatching
 * - Search responses (D-31) lack doc IDs — UUIDs are synthesised at
 *   mapping time so LazyColumn keys stay unique.
 */
@Singleton
class DiscoverRepositoryImpl @Inject constructor(
    private val functions: FirebaseFunctions,
) : DiscoverRepository {

    override suspend fun getPopular(): Result<List<DiscoverProduct>> = runCatching {
        val result = functions
            .getHttpsCallable("discoverPopular")
            .call()
            .await()
        mapResponseToProducts(result.getData(), generateMissingIds = false)
    }

    override suspend fun search(query: String): Result<List<DiscoverProduct>> = runCatching {
        val result = functions
            .getHttpsCallable("discoverSearch")
            .call(mapOf("query" to query))
            .await()
        // Search responses don't include doc IDs — generate stable UUIDs at the
        // mapping site so LazyColumn keys are unique and recomposition is stable.
        mapResponseToProducts(result.getData(), generateMissingIds = true)
    }

    private fun mapResponseToProducts(data: Any?, generateMissingIds: Boolean): List<DiscoverProduct> {
        @Suppress("UNCHECKED_CAST")
        val map = data as? Map<String, Any?> ?: return emptyList()
        @Suppress("UNCHECKED_CAST")
        val products = map["products"] as? List<Map<String, Any?>> ?: return emptyList()
        return products.map { item ->
            DiscoverProduct(
                id = (item["id"] as? String)
                    ?: if (generateMissingIds) UUID.randomUUID().toString() else "",
                title = (item["title"] as? String).orEmpty(),
                description = (item["description"] as? String).orEmpty(),
                imageUrl = (item["image_url"] as? String).orEmpty(),
                price = when (val p = item["price"]) {
                    is Number -> p.toDouble()
                    is String -> p.toDoubleOrNull() ?: 0.0
                    else -> 0.0
                },
                currency = (item["currency"] as? String) ?: "RON",
                retailerUrl = (item["retailer_url"] as? String).orEmpty(),
                retailerName = (item["retailer_name"] as? String).orEmpty(),
            )
        }
    }
}
