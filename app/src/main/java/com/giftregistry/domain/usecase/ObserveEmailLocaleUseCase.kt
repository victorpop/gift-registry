package com.giftregistry.domain.usecase

import com.giftregistry.domain.preferences.EmailLocaleRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveEmailLocaleUseCase @Inject constructor(
    private val repository: EmailLocaleRepository,
) {
    operator fun invoke(): Flow<String?> = repository.observeEmailLocale()
}
