package com.giftregistry.ui.registry.detail

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReservationDeepLinkBus @Inject constructor() {
    data class AutoReserve(val registryId: String, val itemId: String)

    private val _requests = MutableSharedFlow<AutoReserve>(
        replay = 1,
        extraBufferCapacity = 1,
    )
    val requests: SharedFlow<AutoReserve> = _requests.asSharedFlow()

    suspend fun request(registryId: String, itemId: String) {
        _requests.emit(AutoReserve(registryId, itemId))
    }
}
