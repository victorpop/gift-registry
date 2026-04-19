package com.giftregistry.fcm

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import javax.inject.Inject

/**
 * Phase 6: Foreground FCM receiver + token lifecycle (D-09, D-11, D-12).
 * Background messages auto-render via the default_notification_channel_id declared in
 * AndroidManifest.xml; onMessageReceived here is only invoked when the app is foregrounded.
 *
 * All logic is delegated to [MessagingHandler] so it can be unit-tested without an Android
 * Service context.
 *
 * Uses the @AndroidEntryPoint(BaseClass::class) + Hilt_ClassName pattern that this project
 * adopted due to AGP 9 + KSP — see Phase 2 decision (KSP over KAPT).
 */
@AndroidEntryPoint(FirebaseMessagingService::class)
class GiftRegistryMessagingService : Hilt_GiftRegistryMessagingService() {

    @Inject lateinit var handler: MessagingHandler

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        handler.onNewToken(token, scope)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        handler.onMessageReceived(remoteMessage, scope)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
