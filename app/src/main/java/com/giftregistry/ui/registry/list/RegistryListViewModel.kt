package com.giftregistry.ui.registry.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giftregistry.domain.auth.AuthRepository
import com.giftregistry.domain.auth.AuthStateEvent
import com.giftregistry.domain.model.ItemStatus
import com.giftregistry.domain.model.Registry
import com.giftregistry.domain.model.User
import com.giftregistry.domain.usecase.DeleteRegistryUseCase
import com.giftregistry.domain.usecase.ObserveItemsUseCase
import com.giftregistry.domain.usecase.ObserveRegistriesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Per-registry item counts derived from the items subcollection.
 * Passed to card composables so they can display real values.
 */
data class RegistryCounts(
    val items: Int = 0,
    val reserved: Int = 0,
    val given: Int = 0,
)

sealed interface RegistryListUiState {
    data object Loading : RegistryListUiState
    data class Success(
        val registries: List<Registry>,
        /** Keyed by registryId; absent entries default to all-zero counts. */
        val counts: Map<String, RegistryCounts> = emptyMap(),
    ) : RegistryListUiState
    data class Error(val message: String) : RegistryListUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RegistryListViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    observeRegistries: ObserveRegistriesUseCase,
    private val observeItems: ObserveItemsUseCase,
    private val deleteRegistry: DeleteRegistryUseCase
) : ViewModel() {

    private val _deleteError = MutableStateFlow<String?>(null)
    val deleteError: StateFlow<String?> = _deleteError

    // Map AuthStateEvent to User? — skip Initial(null) to avoid transient null on cold start
    private val authUserFlow = authRepository.authState
        .filter { event -> event !is AuthStateEvent.Initial || event.user != null }
        .map { event ->
            when (event) {
                is AuthStateEvent.Initial -> event.user
                is AuthStateEvent.Changed -> event.user
            }
        }

    val currentUser: StateFlow<User?> = authUserFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val uiState: StateFlow<RegistryListUiState>

    init {
        uiState = authUserFlow
            .flatMapLatest { user ->
                if (user == null) {
                    flowOf<RegistryListUiState>(RegistryListUiState.Loading)
                } else {
                    observeRegistries(user.uid)
                        .flatMapLatest { registries ->
                            if (registries.isEmpty()) {
                                flowOf(RegistryListUiState.Success(registries, emptyMap()))
                            } else {
                                // Combine one items Flow per registry into a map of counts.
                                // combine(vararg flows) emits whenever any child emits — counts
                                // stay in sync with Firestore real-time updates on items.
                                // Per-registry .catch — if items for one registry fail (e.g.
                                // PERMISSION_DENIED on a registry the user is invited to but
                                // whose items rule denies via get()), that card falls back to
                                // zero counts instead of erroring the whole list.
                                val itemFlows = registries.map { registry ->
                                    observeItems(registry.id)
                                        .map { items ->
                                            registry.id to RegistryCounts(
                                                items = items.size,
                                                reserved = items.count { it.status == ItemStatus.RESERVED },
                                                given = items.count { it.status == ItemStatus.PURCHASED },
                                            )
                                        }
                                        .catch { emit(registry.id to RegistryCounts()) }
                                }
                                combine(itemFlows) { pairs ->
                                    val countsMap = pairs.toMap()
                                    RegistryListUiState.Success(registries, countsMap)
                                        as RegistryListUiState
                                }
                            }
                        }
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
