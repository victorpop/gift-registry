package com.giftregistry.ui.registry.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giftregistry.domain.model.Item
import com.giftregistry.domain.model.Registry
import com.giftregistry.domain.usecase.DeleteItemUseCase
import com.giftregistry.domain.usecase.DeleteRegistryUseCase
import com.giftregistry.domain.usecase.ObserveItemsUseCase
import com.giftregistry.domain.usecase.ObserveRegistryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegistryDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeRegistry: ObserveRegistryUseCase,
    observeItems: ObserveItemsUseCase,
    private val deleteRegistry: DeleteRegistryUseCase,
    private val deleteItem: DeleteItemUseCase
) : ViewModel() {

    val registryId: String = savedStateHandle["registryId"] ?: ""

    val registry: StateFlow<Registry?> = observeRegistry(registryId)
        .catch { emit(null) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val items: StateFlow<List<Item>> = observeItems(registryId)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _registryDeleted = MutableStateFlow(false)
    val registryDeleted: StateFlow<Boolean> = _registryDeleted.asStateFlow()

    fun onDeleteItem(itemId: String) {
        viewModelScope.launch {
            deleteItem(registryId, itemId).onFailure { e ->
                _error.value = e.message ?: "Failed to delete item"
            }
        }
    }

    fun onDeleteRegistry() {
        viewModelScope.launch {
            deleteRegistry(registryId)
                .onSuccess { _registryDeleted.value = true }
                .onFailure { e -> _error.value = e.message ?: "Failed to delete registry" }
        }
    }

    fun clearError() { _error.value = null }
}
