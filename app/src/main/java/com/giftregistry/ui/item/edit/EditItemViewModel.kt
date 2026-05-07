package com.giftregistry.ui.item.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giftregistry.domain.auth.AuthRepository
import com.giftregistry.domain.model.Item
import com.giftregistry.domain.model.Registry
import com.giftregistry.domain.preferences.GuestPreferencesRepository
import com.giftregistry.domain.usecase.ConfirmPurchaseUseCase
import com.giftregistry.domain.usecase.FetchOgMetadataUseCase
import com.giftregistry.domain.usecase.ObserveItemsUseCase
import com.giftregistry.domain.usecase.ObserveRegistryUseCase
import com.giftregistry.domain.usecase.ReserveItemUseCase
import com.giftregistry.domain.usecase.UpdateItemUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditItemViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val updateItem: UpdateItemUseCase,
    private val observeItems: ObserveItemsUseCase,
    private val fetchOgMetadata: FetchOgMetadataUseCase,
    // quick-260507-vrp — invitee dual-mode UI dependencies. AuthRepository +
    // ObserveRegistryUseCase drive isOwner; ReserveItemUseCase +
    // ConfirmPurchaseUseCase + GuestPreferencesRepository drive the giver
    // actions on EditItemScreen invitee mode (Task 3). All five are already
    // provided in the existing Hilt graph used by RegistryDetailViewModel —
    // no module changes needed.
    private val authRepository: AuthRepository,
    private val observeRegistry: ObserveRegistryUseCase,
    private val reserveItemUseCase: ReserveItemUseCase,
    private val confirmPurchaseUseCase: ConfirmPurchaseUseCase,
    private val guestPreferencesRepository: GuestPreferencesRepository,
) : ViewModel() {

    val registryId: String = savedStateHandle["registryId"] ?: ""
    val itemId: String = savedStateHandle["itemId"] ?: ""

    // Form fields
    val url = MutableStateFlow("")
    val title = MutableStateFlow("")
    val imageUrl = MutableStateFlow("")
    val price = MutableStateFlow("")
    val notes = MutableStateFlow("")

    /**
     * quick-260507-vrp — observes the registry that owns this item so the
     * UI layer can derive ownership without a separate VM lookup. Mirrors
     * the RegistryDetailViewModel.registry field (line 141-143). Eagerly
     * + initial-null + .catch{emit(null)} keeps the UI safe during load
     * and Firestore errors.
     */
    val registry: StateFlow<Registry?> = observeRegistry(registryId)
        .catch { emit(null) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /**
     * quick-260507-vrp — true when the signed-in user owns the registry
     * that this item belongs to. Drives the dual-mode UI on EditItemScreen:
     * owner mode = full edit (Save / Delete reachable from per-item kebab
     * on the Detail row); invitee mode = read-only fields + Reserve /
     * Mark-as-purchased actions reusing the same use cases the giver flow
     * already uses on RegistryDetailScreen. Mirrors
     * RegistryDetailViewModel.isOwner (RegistryDetailViewModel.kt:182-189)
     * line-for-line so both surfaces use the same ownership predicate as
     * the server (functions/src/registry/inviteToRegistry.ts:50).
     *
     * Eagerly + initial-false + .catch{emit(false)} means non-owner UI is
     * the safe default during load — owner-only edit affordances never
     * flash for an invitee.
     */
    val isOwner: StateFlow<Boolean> = combine(
        registry,
        authRepository.authState,
    ) { reg, user ->
        reg != null && user != null && reg.ownerId == user.uid
    }
        .catch { emit(false) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isFetchingOg = MutableStateFlow(false)
    val isFetchingOg: StateFlow<Boolean> = _isFetchingOg.asStateFlow()

    private val _ogFetchFailed = MutableStateFlow(false)
    val ogFetchFailed: StateFlow<Boolean> = _ogFetchFailed.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _savedSuccessfully = MutableStateFlow(false)
    val savedSuccessfully: StateFlow<Boolean> = _savedSuccessfully.asStateFlow()

    init {
        viewModelScope.launch {
            val items = observeItems(registryId).firstOrNull() ?: emptyList()
            val item = items.firstOrNull { it.id == itemId }
            if (item != null) {
                url.value = item.originalUrl
                title.value = item.title
                imageUrl.value = item.imageUrl ?: ""
                price.value = item.price ?: ""
                notes.value = item.notes ?: ""
            }
            _isLoading.value = false
        }
    }

    fun onFetchMetadata() {
        val currentUrl = url.value.trim()
        if (currentUrl.isBlank()) return

        viewModelScope.launch {
            _isFetchingOg.value = true
            _ogFetchFailed.value = false

            fetchOgMetadata(currentUrl)
                .onSuccess { og ->
                    if (!og.title.isNullOrBlank()) title.value = og.title
                    if (!og.imageUrl.isNullOrBlank()) imageUrl.value = og.imageUrl
                    if (!og.price.isNullOrBlank()) price.value = og.price
                }
                .onFailure {
                    _ogFetchFailed.value = true
                }

            _isFetchingOg.value = false
        }
    }

    fun onSave() {
        if (title.value.isBlank()) {
            _error.value = "Item name is required"
            return
        }

        viewModelScope.launch {
            _isSaving.value = true
            _error.value = null

            val item = Item(
                id = itemId,
                registryId = registryId,
                originalUrl = url.value.trim(),
                title = title.value.trim(),
                imageUrl = imageUrl.value.trim().ifBlank { null },
                price = price.value.trim().ifBlank { null },
                notes = notes.value.trim().ifBlank { null }
            )

            updateItem(registryId, item)
                .onSuccess { _savedSuccessfully.value = true }
                .onFailure { e -> _error.value = e.message ?: "Failed to update item" }

            _isSaving.value = false
        }
    }

    fun clearError() { _error.value = null }
}
