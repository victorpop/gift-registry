package com.giftregistry.data.auth

import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.giftregistry.domain.auth.AuthStateEvent
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthDataSource @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) {
    val authStateFlow: Flow<AuthStateEvent> = callbackFlow {
        var seenFirst = false
        val listener = FirebaseAuth.AuthStateListener { auth ->
            val user = auth.currentUser?.toDomain()
            if (!seenFirst) {
                seenFirst = true
                trySend(AuthStateEvent.Initial(user))
            } else {
                trySend(AuthStateEvent.Changed(user))
            }
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    val currentUser: FirebaseUser? get() = firebaseAuth.currentUser

    suspend fun createUserWithEmail(email: String, password: String): AuthResult =
        firebaseAuth.createUserWithEmailAndPassword(email, password).await()

    suspend fun signInWithEmail(email: String, password: String): AuthResult =
        firebaseAuth.signInWithEmailAndPassword(email, password).await()

    suspend fun signInWithGoogleCredential(idToken: String): AuthResult {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        return firebaseAuth.signInWithCredential(credential).await()
    }

    suspend fun signInAnonymously(): AuthResult =
        firebaseAuth.signInAnonymously().await()

    suspend fun linkWithEmail(email: String, password: String): AuthResult {
        val credential = EmailAuthProvider.getCredential(email, password)
        return firebaseAuth.currentUser!!.linkWithCredential(credential).await()
    }

    fun signOut() = firebaseAuth.signOut()
}
