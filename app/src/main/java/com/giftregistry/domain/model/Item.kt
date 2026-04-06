package com.giftregistry.domain.model

data class Item(
    val id: String = "",
    val registryId: String = "",
    val title: String = "",
    val originalUrl: String = "",
    val affiliateUrl: String = "",
    val imageUrl: String? = null,
    val price: String? = null,
    val notes: String? = null,
    val status: ItemStatus = ItemStatus.AVAILABLE,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)
