package com.giftregistry.data.auth

import com.giftregistry.domain.auth.AuthRepository
import com.giftregistry.domain.auth.AuthStateEvent
import com.giftregistry.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class FakeAuthRepository : AuthRepository {
    private val _authState = MutableSharedFlow<AuthStateEvent>(replay = 1)
    override val authState: Flow<AuthStateEvent> = _authState
    private var _currentUser: User? = null
    override val currentUser: User? get() = _currentUser

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

    override fun signOut() {
        _currentUser = null
        _authState.tryEmit(AuthStateEvent.Changed(null))
    }

    /** Emit the synchronous-attach event (cold-start simulation). */
    fun emitInitial(user: User?) {
        _currentUser = user
        _authState.tryEmit(AuthStateEvent.Initial(user))
    }

    /** Emit a post-attach change (sign-in/sign-out/restoration). */
    fun emitChanged(user: User?) {
        _currentUser = user
        _authState.tryEmit(AuthStateEvent.Changed(user))
    }

    /** Legacy helper for existing tests that don't care about Initial/Changed distinction. */
    fun emitUser(user: User?) {
        emitChanged(user)
    }

    private fun <T> fakeResult(value: T): Result<T> =
        if (shouldFail) Result.failure(failureException)
        else Result.success(value).also {
            if (value is User) {
                _currentUser = value
                _authState.tryEmit(AuthStateEvent.Changed(value))
            }
        }
}
