package com.giftregistry.data.preferences

import com.giftregistry.domain.preferences.EmailLocaleRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private val ALLOWED_LOCALES = setOf("en", "ro")

@Singleton
class EmailLocaleRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) : EmailLocaleRepository {

    override fun observeEmailLocale(): Flow<String?> {
        val uid = firebaseAuth.currentUser?.uid ?: return flowOf(null)
        return callbackFlow {
            val registration = firestore.collection("users").document(uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(null)
                        return@addSnapshotListener
                    }
                    val locale = snapshot?.getString("preferredLocale")
                    val normalized = if (locale in ALLOWED_LOCALES) locale else null
                    trySend(normalized)
                }
            awaitClose { registration.remove() }
        }
    }

    override suspend fun getEmailLocale(): String? {
        val uid = firebaseAuth.currentUser?.uid ?: return null
        val snap = firestore.collection("users").document(uid).get().await()
        val value = snap.getString("preferredLocale")
        return if (value in ALLOWED_LOCALES) value else null
    }

    override suspend fun setEmailLocale(locale: String): Result<Unit> = runCatching {
        require(locale in ALLOWED_LOCALES) { "Invalid locale: $locale (must be 'en' or 'ro')" }
        val uid = firebaseAuth.currentUser?.uid ?: error("User not signed in")
        firestore.collection("users").document(uid)
            .set(mapOf("preferredLocale" to locale), SetOptions.merge())
            .await()
        Unit
    }
}
