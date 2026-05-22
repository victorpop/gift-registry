package com.giftregistry.data.auth

import android.util.Log
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.giftregistry.domain.auth.AuthStateEvent
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "FirebaseAuthDS"

@Singleton
class FirebaseAuthDataSource @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) {
    val authStateFlow: Flow<AuthStateEvent> = callbackFlow {
        var seenFirst = false
        val listener = FirebaseAuth.AuthStateListener { auth ->
            val user = auth.currentUser
            if (!seenFirst) {
                seenFirst = true
                if (user != null) {
                    // Validate the cached session before announcing it to the rest of the
                    // app. Firebase Auth restores a cached user from disk on cold start
                    // without contacting the auth server — the refresh token may be stale
                    // (e.g. the emulator was reset, or the server invalidated the session).
                    // A stale token causes every Firebase API call (Functions callable,
                    // Firestore streams) to fail with INVALID_REFRESH_TOKEN after the user
                    // is already in "Authenticated" state, with no UI recovery path.
                    //
                    // Fix: force-refresh the ID token synchronously in the channel's
                    // coroutine scope. If getIdToken(true) fails, sign out before emitting
                    // — this drives AuthViewModel to Unauthenticated and the nav gate to
                    // AuthScreen, which is the correct recovery UX.
                    launch {
                        // 15-second deadline on the token refresh. Without a timeout,
                        // getIdToken(true) will hang indefinitely when Firebase Auth is
                        // unreachable (emulator not running, no network, firewall). This
                        // causes AuthViewModel to stay in AuthUiState.Loading forever,
                        // showing a permanent loading spinner. On timeout we treat it
                        // as a failure and sign out — same recovery path as a real error.
                        val tokenResult = runCatching {
                            withTimeout(15_000L) { user.getIdToken(true).await() }
                        }
                        if (tokenResult.isFailure) {
                            val e = tokenResult.exceptionOrNull()
                            val isTimeout = e is TimeoutCancellationException
                            Log.w(
                                TAG,
                                if (isTimeout) "Cached user token refresh timed out (15s); signing out to force re-auth"
                                else "Cached user token refresh failed (${e?.message}); signing out to force re-auth",
                                e,
                            )
                            auth.signOut()
                            // signOut() triggers another AuthStateListener callback which
                            // will emit AuthStateEvent.Changed(null). We do NOT emit
                            // AuthStateEvent.Initial(null) here — the Changed emission
                            // from signOut is sufficient for AuthViewModel to react.
                        } else {
                            trySend(AuthStateEvent.Initial(user.toDomain()))
                        }
                    }
                } else {
                    trySend(AuthStateEvent.Initial(null))
                }
            } else {
                trySend(AuthStateEvent.Changed(user?.toDomain()))
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
