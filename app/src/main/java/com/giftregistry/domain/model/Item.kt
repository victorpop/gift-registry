package com.giftregistry.domain.model

data class Item(
    val id: String = "",
    val registryId: String = "",
    val title: String = "",
    val description: String = "",
    val originalUrl: String = "",
    val affiliateUrl: String = "",
    val imageUrl: String = "",
    val price: String = "",
    val status: ItemStatus = ItemStatus.AVAILABLE,
    val addedAt: Long = 0L
)
