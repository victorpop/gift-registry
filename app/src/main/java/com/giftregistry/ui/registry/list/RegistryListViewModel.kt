package com.giftregistry.ui.registry.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giftregistry.domain.auth.AuthRepository
import com.giftregistry.domain.model.Registry
import com.giftregistry.domain.usecase.DeleteRegistryUseCase
import com.giftregistry.domain.usecase.ObserveRegistriesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface RegistryListUiState {
    data object Loading : RegistryListUiState
    data class Success(val registries: List<Registry>) : RegistryListUiState
    data class Error(val message: String) : RegistryListUiState
}

@HiltViewModel
class RegistryListViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    observeRegistries: ObserveRegistriesUseCase,
    private val deleteRegistry: DeleteRegistryUseCase
) : ViewModel() {

    private val _deleteError = MutableStateFlow<String?>(null)
    val deleteError: StateFlow<String?> = _deleteError

    val uiState: StateFlow<RegistryListUiState>

    init {
        val uid = authRepository.currentUser?.uid ?: ""
        uiState = observeRegistries(uid)
            .map { RegistryListUiState.Success(it) as RegistryListUiState }
            .catch { emit(RegistryListUiState.Error(it.message ?: "Unknown error")) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, RegistryListUiState.Loading)
    }

    fun onDeleteRegistry(registryId: String) {
        viewModelScope.launch {
            deleteRegistry(registryId).onFailure { e ->
                _deleteError.value = e.message
            }
        }
    }

    fun clearDeleteError() { _deleteError.value = null }
}
