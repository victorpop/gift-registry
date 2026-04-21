package com.giftregistry.ui.registry.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giftregistry.domain.auth.AuthRepository
import com.giftregistry.domain.model.Registry
import com.giftregistry.domain.model.User
import com.giftregistry.domain.usecase.DeleteRegistryUseCase
import com.giftregistry.domain.usecase.ObserveRegistriesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface RegistryListUiState {
    data object Loading : RegistryListUiState
    data class Success(val registries: List<Registry>) : RegistryListUiState
    data class Error(val message: String) : RegistryListUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RegistryListViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    observeRegistries: ObserveRegistriesUseCase,
    private val deleteRegistry: DeleteRegistryUseCase
) : ViewModel() {

    private val _deleteError = MutableStateFlow<String?>(null)
    val deleteError: StateFlow<String?> = _deleteError

    val currentUser: StateFlow<User?> = authRepository.authState
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val uiState: StateFlow<RegistryListUiState>

    init {
        uiState = authRepository.authState
            .flatMapLatest { user ->
                if (user == null) {
                    flowOf<RegistryListUiState>(RegistryListUiState.Loading)
                } else {
                    observeRegistries(user.uid)
                        .map { RegistryListUiState.Success(it) as RegistryListUiState }
                }
            }
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

    fun clearDeleteError() {
        _deleteError.value = null
    }
}
