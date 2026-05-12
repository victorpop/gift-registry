package com.giftregistry.domain.auth

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveAuthStateUseCase @Inject constructor(private val repository: AuthRepository) {
    operator fun invoke(): Flow<AuthStateEvent> = repository.authState
}
