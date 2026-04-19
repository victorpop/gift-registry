package com.giftregistry.domain.usecase

import com.giftregistry.domain.reservation.ReservationRepository
import javax.inject.Inject

class ConfirmPurchaseUseCase @Inject constructor(
    private val repository: ReservationRepository,
) {
    suspend operator fun invoke(reservationId: String): Result<Unit> =
        repository.confirmPurchase(reservationId)
}
