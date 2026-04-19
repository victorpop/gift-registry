package com.giftregistry.data.fcm

import com.giftregistry.domain.fcm.FcmTokenRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FcmTokenRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) : FcmTokenRepository {

    override suspend fun registerToken(token: String): Result<Unit> = runCatching {
        val uid = firebaseAuth.currentUser?.uid ?: return@runCatching Unit
        if (token.isBlank()) return@runCatching Unit
        firestore.collection("users").document(uid)
            .collection("fcmTokens").document(token)
            .set(
                mapOf(
                    "token" to token,
                    "platform" to "android",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "lastSeenAt" to FieldValue.serverTimestamp(),
                ),
                SetOptions.merge()
            )
            .await()
        Unit
    }

    override suspend fun deleteToken(token: String): Result<Unit> = runCatching {
        val uid = firebaseAuth.currentUser?.uid ?: return@runCatching Unit
        if (token.isBlank()) return@runCatching Unit
        firestore.collection("users").document(uid)
            .collection("fcmTokens").document(token)
            .delete()
            .await()
        Unit
    }
}
