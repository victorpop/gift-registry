package com.giftregistry.domain.usecase

import com.giftregistry.domain.notifications.NotificationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveUnreadNotificationCountUseCase @Inject constructor(
    private val repository: NotificationRepository
) {
    operator fun invoke(uid: String): Flow<Int> = repository.observeUnreadCount(uid)
}
