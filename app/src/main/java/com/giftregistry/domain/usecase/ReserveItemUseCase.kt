package com.giftregistry.domain.usecase

import com.giftregistry.domain.model.GuestUser
import com.giftregistry.domain.model.ReservationResult
import com.giftregistry.domain.reservation.ReservationRepository
import javax.inject.Inject

class ReserveItemUseCase @Inject constructor(
    private val repository: ReservationRepository,
) {
    suspend operator fun invoke(
        registryId: String,
        itemId: String,
        giver: GuestUser,
        giverId: String? = null,
    ): Result<ReservationResult> =
        repository.reserve(registryId, itemId, giver, giverId)
}
