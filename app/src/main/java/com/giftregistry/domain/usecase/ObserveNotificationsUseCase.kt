package com.giftregistry.domain.usecase

import com.giftregistry.domain.model.Notification
import com.giftregistry.domain.notifications.NotificationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveNotificationsUseCase @Inject constructor(
    private val repository: NotificationRepository
) {
    operator fun invoke(uid: String, limit: Int = 50): Flow<List<Notification>> =
        repository.observe(uid, limit)
}
