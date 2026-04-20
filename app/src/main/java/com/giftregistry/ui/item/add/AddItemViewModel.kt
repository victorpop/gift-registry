package com.giftregistry.ui.item.add

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giftregistry.domain.model.Item
import com.giftregistry.domain.usecase.AddItemUseCase
import com.giftregistry.domain.usecase.FetchOgMetadataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddItemViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val addItem: AddItemUseCase,
    private val fetchOgMetadata: FetchOgMetadataUseCase
) : ViewModel() {

    val registryId: String = savedStateHandle["registryId"] ?: ""

    // Phase 7: Plan 03 — pre-fill support from Store Browser
    val initialUrl: String = savedStateHandle["initialUrl"] ?: ""
    val initialRegistryId: String = savedStateHandle["initialRegistryId"] ?: ""
    // TODO(D-10 follow-up): When multi-registry picker ships, use initialRegistryId
    // as the default selection. Currently unused because registry picker is
    // deferred — the add is always committed to `registryId`.

    // Form fields
    val url = MutableStateFlow("")
    val title = MutableStateFlow("")
    val imageUrl = MutableStateFlow("")
    val price = MutableStateFlow("")
    val notes = MutableStateFlow("")

    private val _isFetchingOg = MutableStateFlow(false)
    val isFetchingOg: StateFlow<Boolean> = _isFetchingOg.asStateFlow()

    private val _ogFetchFailed = MutableStateFlow(false)
    val ogFetchFailed: StateFlow<Boolean> = _ogFetchFailed.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _savedItemId = MutableStateFlow<String?>(null)
    val savedItemId: StateFlow<String?> = _savedItemId.asStateFlow()

    init {
        if (initialUrl.isNotBlank()) {
            url.value = initialUrl
            // Fire OG fetch automatically — user can still edit before saving. The
            // existing affiliate transform (ItemRepositoryImpl) runs on save; no
            // changes needed to the affiliate pipeline for Phase 7.
            onFetchMetadata()
        }
    }

    fun onUrlChanged(newUrl: String) {
        url.value = newUrl
    }

    // Called when user finishes entering/pasting URL — fetches OG metadata
    fun onFetchMetadata() {
        val currentUrl = url.value.trim()
        if (currentUrl.isBlank()) return

        viewModelScope.launch {
            _isFetchingOg.value = true
            _ogFetchFailed.value = false

            fetchOgMetadata(currentUrl)
                .onSuccess { og ->
                    Log.d(
                        "AddItemVM",
                        "fetchOgMetadata OK url=$currentUrl title=${og.title} image=${og.imageUrl} " +
                            "price=${og.price} priceAmount=${og.priceAmount} priceCurrency=${og.priceCurrency}"
                    )
                    // Auto-fill form fields — user can edit before saving
                    if (!og.title.isNullOrBlank()) title.value = og.title
                    if (!og.imageUrl.isNullOrBlank()) imageUrl.value = og.imageUrl
                    if (!og.price.isNullOrBlank()) price.value = og.price
                }
                .onFailure { e ->
                    // Fallback to manual entry
                    Log.e("AddItemVM", "fetchOgMetadata failed for url=$currentUrl", e)
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
                originalUrl = url.value.trim(),
                title = title.value.trim(),
                imageUrl = imageUrl.value.trim().ifBlank { null },
                price = price.value.trim().ifBlank { null },
                notes = notes.value.trim().ifBlank { null }
            )

            addItem(registryId, item)
                .onSuccess { itemId -> _savedItemId.value = itemId }
                .onFailure { e -> _error.value = e.message ?: "Failed to save item" }

            _isSaving.value = false
        }
    }

    fun clearError() { _error.value = null }
}
