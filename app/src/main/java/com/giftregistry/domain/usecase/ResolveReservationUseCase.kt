package com.giftregistry.domain.usecase

import com.giftregistry.domain.model.ReservationLookup
import com.giftregistry.domain.reservation.ReservationRepository
import javax.inject.Inject

class ResolveReservationUseCase @Inject constructor(
    private val repository: ReservationRepository,
) {
    suspend operator fun invoke(reservationId: String): Result<ReservationLookup> =
        repository.resolve(reservationId)
}
