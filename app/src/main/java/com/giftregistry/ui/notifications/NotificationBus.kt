package com.giftregistry.ui.notifications

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase 6 (UI-SPEC Contract 3): process-scoped bus that carries foreground FCM purchase
 * notifications from GiftRegistryMessagingService to the currently-active screen, which
 * displays them as a Material3 Snackbar. replay=0 means late collectors do NOT receive
 * missed events (by design — stale purchase pushes are irrelevant after a scene change).
 *
 * Mirrors ReservationDeepLinkBus from Phase 4.
 */
@Singleton
class NotificationBus @Inject constructor() {
    private val _events = MutableSharedFlow<PurchasePush>(replay = 0)
    val events: SharedFlow<PurchasePush> = _events.asSharedFlow()

    suspend fun emit(event: PurchasePush) {
        _events.emit(event)
    }
}

/** Payload mirrored from the FCM data map: data.registryId / data.registryName (D-12). */
data class PurchasePush(
    val registryId: String,
    val registryName: String,
)
