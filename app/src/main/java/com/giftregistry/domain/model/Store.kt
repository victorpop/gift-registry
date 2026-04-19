package com.giftregistry.domain.model

data class Store(
    val id: String,
    val name: String,
    val homepageUrl: String,
    val displayOrder: Int,
    val logoAsset: String,
)
