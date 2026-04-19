package com.giftregistry.domain.usecase

import com.giftregistry.domain.fcm.FcmTokenRepository
import javax.inject.Inject

class RegisterFcmTokenUseCase @Inject constructor(
    private val repository: FcmTokenRepository,
) {
    suspend operator fun invoke(token: String): Result<Unit> =
        repository.registerToken(token)
}
