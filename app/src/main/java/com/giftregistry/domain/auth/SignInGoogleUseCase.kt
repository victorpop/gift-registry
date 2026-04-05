package com.giftregistry.domain.auth

import com.giftregistry.domain.model.User
import javax.inject.Inject

class SignInGoogleUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(idToken: String): Result<User> =
        repository.signInWithGoogle(idToken)
}
