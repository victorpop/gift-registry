package com.giftregistry.domain.usecase

import com.giftregistry.domain.notifications.NotificationRepository
import javax.inject.Inject

class MarkNotificationsReadUseCase @Inject constructor(
    private val repository: NotificationRepository
) {
    suspend operator fun invoke(uid: String, notificationIds: List<String>): Result<Unit> =
        repository.markRead(uid, notificationIds)
}
