package com.giftregistry.data.model

data class ItemDto(
    val id: String = "",
    val title: String = "",
    val originalUrl: String = "",
    val affiliateUrl: String = "",
    val imageUrl: String? = null,
    val price: String? = null,
    val notes: String? = null,
    val status: String = "available",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)
