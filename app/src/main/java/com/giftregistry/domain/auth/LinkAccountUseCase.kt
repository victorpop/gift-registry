package com.giftregistry.domain.auth

import com.giftregistry.domain.model.User
import javax.inject.Inject

class LinkAccountUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): Result<User> =
        repository.linkEmailToAnonymous(email, password)
}
