package com.giftregistry.data.notifications

import com.giftregistry.domain.model.Notification
import com.giftregistry.domain.model.NotificationType
import com.giftregistry.domain.notifications.NotificationRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
) : NotificationRepository {

    /**
     * Observes the most recent [limit] notification documents for [uid],
     * ordered by createdAt descending.
     *
     * Uses callbackFlow + awaitClose to mirror FirestoreDataSource pattern —
     * listener is properly cancelled when the coroutine scope is cancelled.
     */
    override fun observe(uid: String, limit: Int): Flow<List<Notification>> = callbackFlow {
        val listener = firestore
            .collection("users").document(uid)
            .collection("notifications")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val notifications = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(NotificationDto::class.java)?.copy(id = doc.id)?.toDomain()
                } ?: emptyList()
                trySend(notifications)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Observes the count of unread notifications (readAt == null) for [uid].
     *
     * Uses a live snapshot listener on null-readAt docs for real-time badge updates.
     * .count() aggregation is a one-shot call and cannot drive a Flow — so we use
     * snapshot?.size() which only counts metadata (no doc bodies transferred).
     */
    override fun observeUnreadCount(uid: String): Flow<Int> = callbackFlow {
        val listener = firestore
            .collection("users").document(uid)
            .collection("notifications")
            .whereEqualTo("readAt", null)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.size() ?: 0)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Marks the given notification IDs as read in a single batched write.
     *
     * Matches the runCatching + Result<Unit> convention in RegistryRepositoryImpl.
     * The Firestore rule allows self-update of readAt only — the batch only updates
     * readAt to prevent rule violation.
     */
    override suspend fun markRead(uid: String, notificationIds: List<String>): Result<Unit> =
        runCatching {
            if (notificationIds.isEmpty()) return@runCatching Unit
            val batch = firestore.batch()
            notificationIds.forEach { id ->
                val ref = firestore
                    .collection("users").document(uid)
                    .collection("notifications").document(id)
                batch.update(ref, "readAt", FieldValue.serverTimestamp())
            }
            batch.commit().await()
            Unit
        }

    // ----- DTO → domain mapper -----

    private fun NotificationDto.toDomain(): Notification {
        // Flatten payload: coerce every value to String? for the domain model
        val flatPayload: Map<String, String?> = payload.mapValues { (_, v) -> v?.toString() }
        return Notification(
            id = id,
            type = NotificationType.fromWire(type),
            titleKey = titleKey,
            bodyKey = bodyKey,
            titleFallback = title,
            bodyFallback = body,
            payload = flatPayload,
            createdAtMs = createdAt?.time ?: 0L,
            readAtMs = readAt?.time,
        )
    }
}
