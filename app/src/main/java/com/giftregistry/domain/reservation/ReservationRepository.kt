package com.giftregistry.domain.reservation

import com.giftregistry.domain.model.GuestUser
import com.giftregistry.domain.model.ReservationLookup
import com.giftregistry.domain.model.ReservationResult

interface ReservationRepository {
    suspend fun reserve(
        registryId: String,
        itemId: String,
        giver: GuestUser,
        giverId: String?,
    ): Result<ReservationResult>

    suspend fun resolve(reservationId: String): Result<ReservationLookup>
}
