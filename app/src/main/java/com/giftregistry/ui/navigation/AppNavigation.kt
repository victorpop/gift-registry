package com.giftregistry.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.giftregistry.domain.usecase.ResolveReservationUseCase
import com.giftregistry.ui.registry.detail.ReservationDeepLinkBus
import kotlinx.coroutines.launch
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.giftregistry.ui.auth.AuthScreen
import com.giftregistry.ui.auth.AuthUiState
import com.giftregistry.ui.auth.AuthViewModel
import com.giftregistry.ui.item.add.AddItemScreen
import com.giftregistry.ui.item.edit.EditItemScreen
import com.giftregistry.ui.registry.create.CreateRegistryScreen
import com.giftregistry.ui.registry.detail.RegistryDetailScreen
import com.giftregistry.ui.registry.invite.InviteBottomSheet
import com.giftregistry.ui.registry.list.RegistryListScreen
import com.giftregistry.ui.settings.SettingsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(deepLinkRegistryId: String? = null) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val authUiState by authViewModel.authState.collectAsStateWithLifecycle()

    val backStack = remember { mutableStateListOf<Any>(AuthKey) }

    LaunchedEffect(authUiState) {
        when (authUiState) {
            is AuthUiState.Authenticated -> {
                if (backStack.lastOrNull() !is HomeKey) {
                    backStack.clear()
                    if (deepLinkRegistryId != null) {
                        backStack.add(HomeKey)
                        backStack.add(RegistryDetailKey(deepLinkRegistryId))
                    } else {
                        backStack.add(HomeKey)
                    }
                }
            }
            is AuthUiState.Unauthenticated -> {
                if (backStack.lastOrNull() !is AuthKey) {
                    backStack.clear()
                    backStack.add(AuthKey)
                }
            }
            is AuthUiState.Loading -> {
                // Do nothing — wait for Firebase session restore
            }
        }
    }

    if (authUiState is AuthUiState.Loading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    NavDisplay(
        backStack = backStack,
        onBack = { if (backStack.size > 1) backStack.removeLast() },
        entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator()),
        entryProvider = entryProvider {
            entry<AuthKey> { AuthScreen() }

            entry<HomeKey> {
                RegistryListScreen(
                    onNavigateToCreate = { backStack.add(CreateRegistryKey) },
                    onNavigateToDetail = { registryId -> backStack.add(RegistryDetailKey(registryId)) },
                    onNavigateToEdit = { registryId -> backStack.add(EditRegistryKey(registryId)) },
                    onNavigateToSettings = { backStack.add(SettingsKey) }
                )
            }

            entry<CreateRegistryKey> {
                CreateRegistryScreen(
                    onBack = { backStack.removeLast() },
                    onSaved = { registryId ->
                        backStack.removeLast()
                        backStack.add(RegistryDetailKey(registryId))
                    }
                )
            }

            entry<EditRegistryKey> { key ->
                CreateRegistryScreen(
                    registryId = key.registryId,
                    onBack = { backStack.removeLast() },
                    onSaved = { backStack.removeLast() }
                )
            }

            entry<RegistryDetailKey> { key ->
                var showInviteSheet by remember { mutableStateOf(false) }

                RegistryDetailScreen(
                    registryId = key.registryId,
                    onBack = { backStack.removeLast() },
                    onNavigateToAddItem = { backStack.add(AddItemKey(key.registryId)) },
                    onNavigateToEditItem = { itemId -> backStack.add(EditItemKey(key.registryId, itemId)) },
                    onNavigateToEditRegistry = { backStack.add(EditRegistryKey(key.registryId)) },
                    onNavigateToInvite = { showInviteSheet = true }
                )

                if (showInviteSheet) {
                    InviteBottomSheet(
                        registryId = key.registryId,
                        onDismiss = { showInviteSheet = false }
                    )
                }
            }

            entry<AddItemKey> { key ->
                AddItemScreen(
                    registryId = key.registryId,
                    onBack = { backStack.removeLast() }
                )
            }

            entry<EditItemKey> { key ->
                EditItemScreen(
                    registryId = key.registryId,
                    itemId = key.itemId,
                    onBack = { backStack.removeLast() }
                )
            }

            entry<SettingsKey> {
                SettingsScreen(onBack = { backStack.removeLast() })
            }

            entry<ReReserveDeepLink> { key ->
                ReReserveResolver(
                    reservationId = key.reservationId,
                    onResolved = { registryId, _ ->
                        backStack.clear()
                        backStack.add(HomeKey)
                        backStack.add(RegistryDetailKey(registryId = registryId))
                    },
                    onError = {
                        backStack.clear()
                        backStack.add(HomeKey)
                    },
                )
            }
        }
    )
}

@Composable
private fun ReReserveResolver(
    reservationId: String,
    onResolved: (registryId: String, itemId: String) -> Unit,
    onError: () -> Unit,
) {
    val viewModel: ReReserveResolverViewModel = hiltViewModel(
        key = "re-reserve-$reservationId"
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(reservationId) {
        viewModel.resolve(reservationId)
    }

    LaunchedEffect(state) {
        when (val s = state) {
            is ReReserveResolverViewModel.State.Resolved ->
                onResolved(s.registryId, s.itemId)
            is ReReserveResolverViewModel.State.Error -> onError()
            else -> Unit
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@dagger.hilt.android.lifecycle.HiltViewModel
class ReReserveResolverViewModel @javax.inject.Inject constructor(
    private val resolveReservationUseCase: ResolveReservationUseCase,
    private val deepLinkBus: ReservationDeepLinkBus,
) : androidx.lifecycle.ViewModel() {

    sealed interface State {
        data object Idle : State
        data object Loading : State
        data class Resolved(val registryId: String, val itemId: String) : State
        data class Error(val code: String) : State
    }

    private val _state = kotlinx.coroutines.flow.MutableStateFlow<State>(State.Idle)
    val state: kotlinx.coroutines.flow.StateFlow<State> = _state

    fun resolve(reservationId: String) {
        if (_state.value is State.Loading || _state.value is State.Resolved) return
        _state.value = State.Loading
        viewModelScope.launch {
            resolveReservationUseCase(reservationId)
                .onSuccess { lookup ->
                    deepLinkBus.request(lookup.registryId, lookup.itemId)
                    _state.value = State.Resolved(lookup.registryId, lookup.itemId)
                }
                .onFailure { err ->
                    _state.value = State.Error(err.message ?: "RESOLVE_FAILED")
                }
        }
    }
}
