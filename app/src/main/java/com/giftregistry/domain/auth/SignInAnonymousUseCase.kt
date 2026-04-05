package com.giftregistry.domain.auth

import com.giftregistry.domain.model.User
import javax.inject.Inject

class SignInAnonymousUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(): Result<User> = repository.signInAnonymously()
}
