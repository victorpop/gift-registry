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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegistryDetailViewModel @Inject constructor(
    private val observeRegistryUseCase: ObserveRegistryUseCase,
    private val observeItemsUseCase: ObserveItemsUseCase,
    private val deleteRegistryUseCase: DeleteRegistryUseCase,
    private val deleteItemUseCase: DeleteItemUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val registryId: String = checkNotNull(savedStateHandle["registryId"])

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
}
