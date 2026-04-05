package com.giftregistry.data.auth

import com.giftregistry.domain.auth.AuthRepository
import com.giftregistry.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAuthRepository : AuthRepository {
    private val _authState = MutableStateFlow<User?>(null)
    override val authState: Flow<User?> = _authState
    override val currentUser: User? get() = _authState.value

    var shouldFail = false
    var failureException: Exception = Exception("Auth failed")

    override suspend fun signUpWithEmail(email: String, password: String): Result<User> =
        fakeResult(User(uid = "fake-uid", email = email, displayName = null, isAnonymous = false))

    override suspend fun signInWithEmail(email: String, password: String): Result<User> =
        fakeResult(User(uid = "fake-uid", email = email, displayName = null, isAnonymous = false))

    override suspend fun signInWithGoogle(idToken: String): Result<User> =
        fakeResult(User(uid = "google-uid", email = "google@test.com", displayName = "Test User", isAnonymous = false))

    override suspend fun signInAnonymously(): Result<User> =
        fakeResult(User(uid = "anon-uid", email = null, displayName = null, isAnonymous = true))

    override suspend fun linkEmailToAnonymous(email: String, password: String): Result<User> =
        fakeResult(User(uid = "anon-uid", email = email, displayName = null, isAnonymous = false))

    override fun signOut() { _authState.value = null }

    fun emitUser(user: User?) { _authState.value = user }

    private fun <T> fakeResult(value: T): Result<T> =
        if (shouldFail) Result.failure(failureException)
        else Result.success(value).also { if (value is User) _authState.value = value }
}
