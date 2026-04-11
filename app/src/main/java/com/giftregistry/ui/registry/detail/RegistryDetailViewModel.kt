package com.giftregistry.ui.registry.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giftregistry.domain.model.GuestUser
import com.giftregistry.domain.model.Item
import com.giftregistry.domain.model.Registry
import com.giftregistry.domain.preferences.GuestPreferencesRepository
import com.giftregistry.domain.usecase.DeleteItemUseCase
import com.giftregistry.domain.usecase.DeleteRegistryUseCase
import com.giftregistry.domain.usecase.ObserveItemsUseCase
import com.giftregistry.domain.usecase.ObserveRegistryUseCase
import com.giftregistry.domain.usecase.ReserveItemUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegistryDetailViewModel @Inject constructor(
    private val observeRegistryUseCase: ObserveRegistryUseCase,
    private val observeItemsUseCase: ObserveItemsUseCase,
    private val deleteRegistryUseCase: DeleteRegistryUseCase,
    private val deleteItemUseCase: DeleteItemUseCase,
    private val reserveItemUseCase: ReserveItemUseCase,
    private val guestPreferencesRepository: GuestPreferencesRepository,
    private val deepLinkBus: ReservationDeepLinkBus,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val registryId: String = checkNotNull(savedStateHandle["registryId"])

    init {
        viewModelScope.launch {
            deepLinkBus.requests.collect { req ->
                if (req.registryId == registryId) {
                    onReserveClicked(req.itemId)
                }
            }
        }
    }

    val registry: StateFlow<Registry?> = observeRegistryUseCase(registryId)
        .catch { emit(null) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val items: StateFlow<List<Item>> = observeItemsUseCase(registryId)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

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
}
