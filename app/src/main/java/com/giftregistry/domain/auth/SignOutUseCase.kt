package com.giftregistry.domain.auth

import javax.inject.Inject

class SignOutUseCase @Inject constructor(private val repository: AuthRepository) {
    operator fun invoke() = repository.signOut()
}
