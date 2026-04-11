package com.giftregistry.data.reservation

import com.giftregistry.domain.model.GuestUser
import com.giftregistry.domain.model.ReservationLookup
import com.giftregistry.domain.model.ReservationResult
import com.giftregistry.domain.reservation.ReservationRepository
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReservationRepositoryImpl @Inject constructor(
    private val functions: FirebaseFunctions,
) : ReservationRepository {

    override suspend fun reserve(
        registryId: String,
        itemId: String,
        giver: GuestUser,
        giverId: String?,
    ): Result<ReservationResult> = runCatching {
        val payload = mapOf(
            "registryId" to registryId,
            "itemId" to itemId,
            "giverName" to "${giver.firstName} ${giver.lastName}".trim(),
            "giverEmail" to giver.email,
            "giverId" to giverId,
        )
        val result = functions
            .getHttpsCallable("createReservation")
            .call(payload)
            .await()

        @Suppress("UNCHECKED_CAST")
        val data = result.data as Map<String, Any?>
        ReservationResult(
            reservationId = data["reservationId"] as String,
            affiliateUrl = data["affiliateUrl"] as String,
            expiresAtMs = (data["expiresAtMs"] as Number).toLong(),
        )
    }

    override suspend fun resolve(reservationId: String): Result<ReservationLookup> = runCatching {
        val result = functions
            .getHttpsCallable("resolveReservation")
            .call(mapOf("reservationId" to reservationId))
            .await()

        @Suppress("UNCHECKED_CAST")
        val data = result.data as Map<String, Any?>
        ReservationLookup(
            registryId = data["registryId"] as String,
            itemId = data["itemId"] as String,
            status = (data["status"] as? String) ?: "unknown",
        )
    }
}
