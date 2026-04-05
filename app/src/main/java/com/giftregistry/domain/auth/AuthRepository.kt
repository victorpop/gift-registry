package com.giftregistry.domain.auth

import com.giftregistry.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val authState: Flow<User?>
    val currentUser: User?
    suspend fun signUpWithEmail(email: String, password: String): Result<User>
    suspend fun signInWithEmail(email: String, password: String): Result<User>
    suspend fun signInWithGoogle(idToken: String): Result<User>
    suspend fun signInAnonymously(): Result<User>
    suspend fun linkEmailToAnonymous(email: String, password: String): Result<User>
    fun signOut()
}
