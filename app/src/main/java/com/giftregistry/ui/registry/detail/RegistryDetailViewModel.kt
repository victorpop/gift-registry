package com.giftregistry.ui.registry.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giftregistry.R
import com.giftregistry.domain.model.GuestUser
import com.giftregistry.domain.model.Item
import com.giftregistry.domain.model.Registry
import com.giftregistry.domain.preferences.GuestPreferencesRepository
import com.giftregistry.domain.usecase.ConfirmPurchaseUseCase
import com.giftregistry.domain.usecase.DeleteItemUseCase
import com.giftregistry.domain.usecase.DeleteRegistryUseCase
import com.giftregistry.domain.usecase.ObserveItemsUseCase
import com.giftregistry.domain.usecase.ObserveRegistryUseCase
import com.giftregistry.domain.usecase.ReserveItemUseCase
import com.giftregistry.ui.notifications.NotificationBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Phase 6 sealed interface for snackbar messages:
 * - Resource: a string-resource id (confirm-purchase success/error)
 * - Push: a foreground FCM purchase push (shows registryName + "View" action)
 */
sealed interface SnackbarMessage {
    data class Resource(val resId: Int) : SnackbarMessage
    data class Push(val registryName: String, val registryId: String) : SnackbarMessage
}

@HiltViewModel
class RegistryDetailViewModel @Inject constructor(
    private val observeRegistryUseCase: ObserveRegistryUseCase,
    private val observeItemsUseCase: ObserveItemsUseCase,
    private val deleteRegistryUseCase: DeleteRegistryUseCase,
    private val deleteItemUseCase: DeleteItemUseCase,
    private val reserveItemUseCase: ReserveItemUseCase,
    private val guestPreferencesRepository: GuestPreferencesRepository,
    private val deepLinkBus: ReservationDeepLinkBus,
    private val confirmPurchaseUseCase: ConfirmPurchaseUseCase,
    private val notificationBus: NotificationBus,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val registryId: String = checkNotNull(savedStateHandle["registryId"])

    // Phase 6: snackbar event channel (confirm-purchase success/error + foreground FCM push)
    private val _snackbarMessages = MutableSharedFlow<SnackbarMessage>(replay = 0, extraBufferCapacity = 1)
    val snackbarMessages: SharedFlow<SnackbarMessage> = _snackbarMessages.asSharedFlow()

    // Phase 6: loading state while ConfirmPurchaseUseCase is in-flight
    private val _confirmingPurchase = MutableStateFlow(false)
    val confirmingPurchase: StateFlow<Boolean> = _confirmingPurchase.asStateFlow()

    init {
        viewModelScope.launch {
            deepLinkBus.requests.collect { req ->
                if (req.registryId == registryId) {
                    onReserveClicked(req.itemId)
                }
            }
        }
        // Phase 6: collect foreground FCM push events and re-emit as snackbar messages
        viewModelScope.launch {
            notificationBus.events.collect { push ->
                _snackbarMessages.emit(SnackbarMessage.Push(registryName = push.registryName, registryId = push.registryId))
            }
        }
    }

    val registry: StateFlow<Registry?> = observeRegistryUseCase(registryId)
        .catch { emit(null) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val items: StateFlow<List<Item>> = observeItemsUseCase(registryId)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /**
     * Phase 6 (UI-SPEC Contract 1): true when the current giver has a guest identity set
     * (meaning they have previously reserved an item in this session) AND at least one item
     * in the registry is still RESERVED. The banner auto-dismisses when Firestore updates
     * the item to PURCHASED via the real-time listener.
     */
    val hasActiveReservation: StateFlow<Boolean> = combine(
        items,
        guestPreferencesRepository.observeGuestIdentity(),
    ) { itemList, guest ->
        guest != null && itemList.any { it.status == com.giftregistry.domain.model.ItemStatus.RESERVED }
    }
        .catch { emit(false) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _deleteError = MutableStateFlow<String?>(null)
    val deleteError: StateFlow<String?> = _deleteError

    private val _registryDeleted = MutableStateFlow(false)
    val registryDeleted: StateFlow<Boolean> = _registryDeleted

    // Reservation state and events
    sealed interface ReservationEvent {
        data class OpenRetailer(val affiliateUrl: String) : ReservationEvent
        data object ShowGuestSheet : ReservationEvent
        data class ShowConflictError(val code: String) : ReservationEvent
    }

    private val _reservationEvents = Channel<ReservationEvent>(Channel.BUFFERED)
    val reservationEvents: Flow<ReservationEvent> = _reservationEvents.receiveAsFlow()

    private val _isReserving = MutableStateFlow(false)
    val isReserving: StateFlow<Boolean> = _isReserving

    private var pendingReserveItemId: String? = null

    fun onDeleteItem(itemId: String) {
        viewModelScope.launch {
            deleteItemUseCase(registryId, itemId).onFailure { e ->
                _deleteError.value = e.message
            }
        }
    }

    fun onDeleteRegistry() {
        viewModelScope.launch {
            deleteRegistryUseCase(registryId).fold(
                onSuccess = { _registryDeleted.value = true },
                onFailure = { e -> _deleteError.value = e.message }
            )
        }
    }

    fun clearDeleteError() {
        _deleteError.value = null
    }

    fun onReserveClicked(itemId: String) {
        if (_isReserving.value) return
        viewModelScope.launch {
            val guest = guestPreferencesRepository.getGuestIdentity()
            if (guest == null) {
                pendingReserveItemId = itemId
                _reservationEvents.send(ReservationEvent.ShowGuestSheet)
                return@launch
            }
            performReservation(itemId, guest)
        }
    }

    fun onGuestIdentitySubmitted(guest: GuestUser) {
        viewModelScope.launch {
            guestPreferencesRepository.saveGuestIdentity(guest)
            val targetItemId = pendingReserveItemId ?: return@launch
            pendingReserveItemId = null
            performReservation(targetItemId, guest)
        }
    }

    private suspend fun performReservation(itemId: String, guest: GuestUser) {
        _isReserving.value = true
        try {
            reserveItemUseCase(registryId, itemId, guest, giverId = null)
                .onSuccess { result ->
                    _reservationEvents.send(ReservationEvent.OpenRetailer(result.affiliateUrl))
                }
                .onFailure { err ->
                    val code = err.message?.takeIf { it.isNotBlank() } ?: "RESERVATION_FAILED"
                    _reservationEvents.send(ReservationEvent.ShowConflictError(code))
                }
        } finally {
            _isReserving.value = false
        }
    }

    /**
     * Phase 6 (UI-SPEC Contract 1/2): giver taps "I completed the purchase".
     * Calls ConfirmPurchaseUseCase; emits snackbar message on success or failure.
     * The banner auto-dismisses when the reservation transitions to purchased via
     * Firestore real-time listener (no extra UI state needed here).
     */
    fun onConfirmPurchase(reservationId: String) {
        viewModelScope.launch {
            _confirmingPurchase.value = true
            val result = confirmPurchaseUseCase(reservationId)
            _confirmingPurchase.value = false
            result.fold(
                onSuccess = { _snackbarMessages.emit(SnackbarMessage.Resource(R.string.reservation_confirm_purchase_success)) },
                onFailure = { _snackbarMessages.emit(SnackbarMessage.Resource(R.string.reservation_confirm_purchase_error)) },
            )
        }
    }
}
