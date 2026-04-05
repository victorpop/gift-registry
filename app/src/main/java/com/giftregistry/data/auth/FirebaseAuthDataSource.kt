package com.giftregistry.data.auth

import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
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
    val authStateFlow: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
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
