package com.giftregistry.fcm

import com.giftregistry.domain.usecase.RegisterFcmTokenUseCase
import com.giftregistry.ui.notifications.NotificationBus
import com.giftregistry.ui.notifications.PurchasePush
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase 6 (D-09, D-11, D-12): plain Kotlin class that handles FCM token and message logic,
 * extracted from GiftRegistryMessagingService so it can be unit-tested without an Android
 * Service context. GiftRegistryMessagingService delegates all logic here.
 */
@Singleton
class MessagingHandler @Inject constructor(
    private val registerFcmTokenUseCase: RegisterFcmTokenUseCase,
    private val notificationBus: NotificationBus,
) {

    fun onNewToken(token: String, scope: CoroutineScope) {
        scope.launch {
            registerFcmTokenUseCase(token)
        }
    }

    /**
     * Convenience overload used in tests: launches in the caller's coroutine scope.
     * Only valid in test context where a scope is already available.
     */
    internal suspend fun onNewToken(token: String) {
        registerFcmTokenUseCase(token)
    }

    fun onMessageReceived(remoteMessage: RemoteMessage, scope: CoroutineScope) {
        val data = remoteMessage.data
        if (data["type"] != "purchase") return
        val registryId = data["registryId"].orEmpty()
        val registryName = data["registryName"] ?: remoteMessage.notification?.body.orEmpty()
        if (registryId.isEmpty()) return
        scope.launch {
            notificationBus.emit(PurchasePush(registryId, registryName))
        }
    }

    /**
     * Convenience overload used in tests: suspends to emit synchronously in the test coroutine.
     */
    internal suspend fun onMessageReceived(remoteMessage: RemoteMessage) {
        val data = remoteMessage.data
        if (data["type"] != "purchase") return
        val registryId = data["registryId"].orEmpty()
        val registryName = data["registryName"] ?: remoteMessage.notification?.body.orEmpty()
        if (registryId.isEmpty()) return
        notificationBus.emit(PurchasePush(registryId, registryName))
    }
}
