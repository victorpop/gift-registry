package com.giftregistry.data.auth

import com.giftregistry.domain.auth.AuthRepository
import com.giftregistry.domain.model.User
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val dataSource: FirebaseAuthDataSource
) : AuthRepository {

    override val authState: Flow<User?> = dataSource.authStateFlow.map { it?.toDomain() }

    override val currentUser: User? get() = dataSource.currentUser?.toDomain()

    override suspend fun signUpWithEmail(email: String, password: String): Result<User> =
        runCatching { dataSource.createUserWithEmail(email, password).user!!.toDomain() }

    override suspend fun signInWithEmail(email: String, password: String): Result<User> =
        runCatching { dataSource.signInWithEmail(email, password).user!!.toDomain() }

    override suspend fun signInWithGoogle(idToken: String): Result<User> =
        runCatching { dataSource.signInWithGoogleCredential(idToken).user!!.toDomain() }

    override suspend fun signInAnonymously(): Result<User> =
        runCatching { dataSource.signInAnonymously().user!!.toDomain() }

    override suspend fun linkEmailToAnonymous(email: String, password: String): Result<User> =
        runCatching { dataSource.linkWithEmail(email, password).user!!.toDomain() }

    override fun signOut() = dataSource.signOut()

    private fun FirebaseUser.toDomain(): User = User(
        uid = uid,
        email = email,
        displayName = displayName,
        isAnonymous = isAnonymous
    )
}
