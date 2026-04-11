package com.giftregistry.domain.model

data class ReservationLookup(
    val registryId: String,
    val itemId: String,
    val status: String,
)
