package com.giftregistry.domain.notifications

import com.giftregistry.domain.model.Notification
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun observe(uid: String, limit: Int = 50): Flow<List<Notification>>
    fun observeUnreadCount(uid: String): Flow<Int>
    suspend fun markRead(uid: String, notificationIds: List<String>): Result<Unit>

    /**
     * D-27 — Promote the current user from pendingInvitedUsers into invitedUsers
     * on [registryId]. Server-side transaction guarantees atomicity; this client
     * wrapper just calls the `acceptInvite` callable and surfaces success/failure
     * via Result<Unit>.
     *
     * Returns Result.failure if: NOT signed in, missing registryId, pending entry
     * not found, registry not found, network error, App Check token missing.
     */
    suspend fun acceptInvite(registryId: String): Result<Unit>

    /**
     * D-27 — Remove the current user from pendingInvitedUsers on [registryId].
     * Same failure modes as acceptInvite. Decline is symmetric with accept on
     * the wire; the difference is server-side (no membership promote, no JOINED
     * notification — just owner inbox entry).
     */
    suspend fun declineInvite(registryId: String): Result<Unit>
}
