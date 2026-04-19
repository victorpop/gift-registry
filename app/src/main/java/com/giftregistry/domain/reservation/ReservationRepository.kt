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

    // Phase 6 (D-01/D-02): giver confirms they completed the purchase.
    // Implementation calls the Firebase `confirmPurchase` callable which runs a server-side
    // transaction and cancels the Cloud Task.
    suspend fun confirmPurchase(reservationId: String): Result<Unit>
}
