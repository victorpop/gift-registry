package com.giftregistry.data.model

import com.giftregistry.domain.model.Store

data class StoreDto(
    val id: String = "",
    val name: String = "",
    val homepageUrl: String = "",
    val displayOrder: Int = 0,
    val logoAsset: String = "store_generic",
) {
    fun toDomain(): Store = Store(
        id = id,
        name = name,
        homepageUrl = homepageUrl,
        displayOrder = displayOrder,
        logoAsset = logoAsset,
    )
}
