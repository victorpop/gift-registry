package com.giftregistry.domain.notifications

import com.giftregistry.domain.model.Notification
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun observe(uid: String, limit: Int = 50): Flow<List<Notification>>
    fun observeUnreadCount(uid: String): Flow<Int>
    suspend fun markRead(uid: String, notificationIds: List<String>): Result<Unit>
}
