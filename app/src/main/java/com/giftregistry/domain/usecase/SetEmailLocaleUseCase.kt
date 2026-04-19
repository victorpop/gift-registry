package com.giftregistry.domain.usecase

import com.giftregistry.domain.preferences.EmailLocaleRepository
import javax.inject.Inject

class SetEmailLocaleUseCase @Inject constructor(
    private val repository: EmailLocaleRepository,
) {
    suspend operator fun invoke(locale: String): Result<Unit> =
        repository.setEmailLocale(locale)
}
