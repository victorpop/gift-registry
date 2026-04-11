package com.giftregistry.domain.model

data class ReservationResult(
    val reservationId: String,
    val affiliateUrl: String,
    val expiresAtMs: Long,
)
