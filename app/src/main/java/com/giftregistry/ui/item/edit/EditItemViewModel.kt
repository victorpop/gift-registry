package com.giftregistry.ui.item.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giftregistry.domain.model.Item
import com.giftregistry.domain.usecase.FetchOgMetadataUseCase
import com.giftregistry.domain.usecase.ObserveItemsUseCase
import com.giftregistry.domain.usecase.UpdateItemUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditItemViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val updateItem: UpdateItemUseCase,
    private val observeItems: ObserveItemsUseCase,
    private val fetchOgMetadata: FetchOgMetadataUseCase
) : ViewModel() {

    val registryId: String = savedStateHandle["registryId"] ?: ""
    val itemId: String = savedStateHandle["itemId"] ?: ""

    // Form fields
    val url = MutableStateFlow("")
    val title = MutableStateFlow("")
    val imageUrl = MutableStateFlow("")
    val price = MutableStateFlow("")
    val notes = MutableStateFlow("")

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
