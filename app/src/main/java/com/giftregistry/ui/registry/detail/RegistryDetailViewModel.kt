package com.giftregistry.ui.registry.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giftregistry.R
import com.giftregistry.data.storage.CoverImageProcessor
import com.giftregistry.domain.auth.AuthRepository
import com.giftregistry.domain.model.GuestUser
import com.giftregistry.domain.model.Item
import com.giftregistry.domain.model.Registry
import com.giftregistry.domain.preferences.GuestPreferencesRepository
import com.giftregistry.domain.storage.StorageRepository
import com.giftregistry.domain.usecase.ConfirmPurchaseUseCase
import com.giftregistry.domain.usecase.DeleteItemUseCase
import com.giftregistry.domain.usecase.DeleteRegistryUseCase
import com.giftregistry.domain.usecase.ObserveItemsUseCase
import com.giftregistry.domain.usecase.ObserveRegistryUseCase
import com.giftregistry.domain.usecase.ReserveItemUseCase
import com.giftregistry.domain.usecase.UpdateRegistryUseCase
import com.giftregistry.ui.notifications.NotificationBus
import com.giftregistry.ui.registry.cover.CoverPhotoSelection
import com.giftregistry.ui.registry.cover.PresetCatalog
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
    // Phase 12 — cover-photo edit on Detail (D-13 owner-only tap)
    private val authRepository: AuthRepository,
    private val updateRegistryUseCase: UpdateRegistryUseCase,
    private val storageRepository: StorageRepository,
    private val coverImageProcessor: CoverImageProcessor,
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

    /**
     * Phase 12 D-13 — owner taps the hero, picker sheet emits a new selection.
     * Maps the selection to a Firestore-persisted imageUrl following the same
     * D-07 + Pitfall 2 strict ordering as CreateRegistryViewModel: Gallery
     * uploads complete BEFORE the Firestore update; Preset / None branches
     * skip the upload entirely.
     *
     * On Gallery upload failure the selection state is rolled back (None)
     * and an error snackbar is emitted via the existing snackbar channel.
     */
    fun onCoverPhotoSelectionChanged(selection: CoverPhotoSelection) {
        _coverPhotoSelection.value = selection
        val current = registry.value ?: return
        viewModelScope.launch {
            val resolvedImageUrl: String? = when (selection) {
                CoverPhotoSelection.None -> null
                is CoverPhotoSelection.Preset ->
                    PresetCatalog.encode(selection.occasion, selection.index)
                is CoverPhotoSelection.Gallery -> {
                    val ownerId = authRepository.currentUser?.uid ?: run {
                        _snackbarMessages.emit(SnackbarMessage.Resource(R.string.cover_photo_upload_failed))
                        return@launch
                    }
                    val jpeg = runCatching { coverImageProcessor.compress(selection.uri) }
                        .getOrElse {
                            _snackbarMessages.emit(SnackbarMessage.Resource(R.string.cover_photo_processing_failed))
                            _coverPhotoSelection.value = CoverPhotoSelection.None
                            return@launch
                        }
                    storageRepository.uploadCover(ownerId, registryId, jpeg)
                        .getOrElse {
                            _snackbarMessages.emit(SnackbarMessage.Resource(R.string.cover_photo_upload_failed))
                            _coverPhotoSelection.value = CoverPhotoSelection.None
                            return@launch
                        }
                }
            }
            updateRegistryUseCase(current.copy(imageUrl = resolvedImageUrl))
                .onFailure {
                    _snackbarMessages.emit(SnackbarMessage.Resource(R.string.cover_photo_upload_failed))
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

    /**
     * The reservationId returned by `createReservation` is persisted in DataStore on
     * reservation success so it survives process death. Used by ConfirmPurchaseBanner
     * to pass the correct ID to `confirmPurchase` (NOT the item ID — the Cloud Function
     * looks up `reservations/{reservationId}` which is a separate collection from items).
     * Cleared after a successful confirm-purchase call.
     */
    val activeReservationId: StateFlow<String?> = guestPreferencesRepository
        .observeActiveReservationId()
        .catch { emit(null) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /**
     * Phase 12 D-13 — true when the current user owns this registry. Drives
     * the owner-only tap target on the 180 dp hero. Guests / web viewers
     * (`isOwner == false`) see no tap affordance because the screen passes
     * `onCoverTap = null` when this flow emits false.
     */
    val isOwner: StateFlow<Boolean> = combine(
        registry,
        authRepository.authState,
    ) { reg, user ->
        reg != null && user != null && reg.ownerId == user.uid
    }
        .catch { emit(false) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /**
     * Phase 12 D-09 / D-13 — current cover-photo selection on the Detail
     * surface. Initialised from `registry.imageUrl` (preset sentinel decodes
     * back to Preset; URL leaves it as None — the inline preview still shows
     * the existing image via HeroImageOrPlaceholder).
     */
    private val _coverPhotoSelection = MutableStateFlow<CoverPhotoSelection>(CoverPhotoSelection.None)
    val coverPhotoSelection: StateFlow<CoverPhotoSelection> = _coverPhotoSelection.asStateFlow()

    init {
        // Phase 12 — rehydrate cover-photo selection from the observed registry's
        // imageUrl. Preset sentinels decode to Preset(occasion, index); URLs
        // leave selection as None (the picker sheet header / hero still render
        // the existing image via HeroImageOrPlaceholder). This init block runs
        // AFTER `val registry` is initialised — putting it earlier would access
        // a null field because Kotlin runs init blocks in source order.
        viewModelScope.launch {
            registry.collect { reg ->
                if (reg == null) return@collect
                val url = reg.imageUrl
                if (url != null && url.startsWith("preset:") &&
                    _coverPhotoSelection.value is CoverPhotoSelection.None
                ) {
                    val parts = url.removePrefix("preset:").split(":")
                    val occ = parts.getOrNull(0)
                    val idx = parts.getOrNull(1)?.toIntOrNull()
                    if (occ != null && occ.isNotBlank() && idx != null && idx >= 1) {
                        _coverPhotoSelection.value = CoverPhotoSelection.Preset(occ, idx)
                    }
                }
            }
        }
    }

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
                    // Persist reservationId so ConfirmPurchaseBanner can pass the correct
                    // server-side ID to `confirmPurchase` (items/{id} and reservations/{id}
                    // are different Firestore collections). Survives process death.
                    guestPreferencesRepository.setActiveReservationId(result.reservationId)
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
                onSuccess = {
                    guestPreferencesRepository.setActiveReservationId(null)
                    _snackbarMessages.emit(SnackbarMessage.Resource(R.string.reservation_confirm_purchase_success))
                },
                onFailure = { _snackbarMessages.emit(SnackbarMessage.Resource(R.string.reservation_confirm_purchase_error)) },
            )
        }
    }
}
