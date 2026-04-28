package com.giftregistry.data.registry

import com.giftregistry.data.model.ItemDto
import com.giftregistry.domain.item.ItemRepository
import com.giftregistry.domain.model.Item
import com.giftregistry.domain.model.ItemStatus
import com.giftregistry.domain.model.OgMetadata
import com.giftregistry.util.AffiliateUrlTransformer
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItemRepositoryImpl @Inject constructor(
    private val dataSource: FirestoreDataSource,
    private val functions: FirebaseFunctions
) : ItemRepository {

    override fun observeItems(registryId: String): Flow<List<Item>> =
        dataSource.observeItems(registryId).map { dtos -> dtos.map { it.toDomain(registryId) } }

    override suspend fun addItem(registryId: String, item: Item): Result<String> =
        runCatching {
            val transformResult = AffiliateUrlTransformer.transform(item.originalUrl)
            if (!transformResult.wasTransformed) {
                // AFF-04: Log unknown merchant for review
                android.util.Log.w("AffiliateUrl", "Unknown merchant URL: ${item.originalUrl}")
            }
            val itemWithAffiliate = item.copy(
                affiliateUrl = transformResult.affiliateUrl
            )
            dataSource.addItem(registryId, itemWithAffiliate.toMap())
        }

    override suspend fun updateItem(registryId: String, item: Item): Result<Unit> =
        runCatching { dataSource.updateItem(registryId, item.id, item.toUpdateMap()) }

    override suspend fun deleteItem(registryId: String, itemId: String): Result<Unit> =
        runCatching { dataSource.deleteItem(registryId, itemId) }

    override suspend fun fetchOgMetadata(url: String): Result<OgMetadata> =
        runCatching {
            val result = functions.getHttpsCallable("fetchOgMetadata")
                .call(mapOf("url" to url))
                .await()
            @Suppress("UNCHECKED_CAST")
            val data = result.data as Map<String, Any?>
            val rawImageUrl = data["imageUrl"] as? String
            OgMetadata(
                title = data["title"] as? String,
                imageUrl = if (rawImageUrl?.startsWith("http://") == true) {
                    "https://" + rawImageUrl.drop(7)
                } else {
                    rawImageUrl
                },
                price = data["price"] as? String,
                priceAmount = data["priceAmount"] as? String,
                priceCurrency = data["priceCurrency"] as? String,
            )
        }

    private fun ItemDto.toDomain(registryId: String) = Item(
        id = id, registryId = registryId, title = title,
        originalUrl = originalUrl, affiliateUrl = affiliateUrl,
        imageUrl = imageUrl, price = price, notes = notes,
        status = ItemStatus.fromString(status),
        createdAt = createdAt, updatedAt = updatedAt,
        expiresAt = expiresAt,
    )

    private fun Item.toMap(): Map<String, Any?> = mapOf(
        "title" to title, "originalUrl" to originalUrl, "affiliateUrl" to affiliateUrl,
        "imageUrl" to imageUrl, "price" to price, "notes" to notes,
        "status" to status.name.lowercase(),
        "createdAt" to System.currentTimeMillis(), "updatedAt" to System.currentTimeMillis()
    )

    private fun Item.toUpdateMap(): Map<String, Any?> = mapOf(
        "title" to title, "originalUrl" to originalUrl, "affiliateUrl" to affiliateUrl,
        "imageUrl" to imageUrl, "price" to price, "notes" to notes,
        "updatedAt" to System.currentTimeMillis()
    )
}
