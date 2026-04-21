package com.giftregistry.ui.navigation

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.giftregistry.R
import com.giftregistry.domain.usecase.ResolveReservationUseCase
import com.giftregistry.ui.common.chrome.AddActionSheet
import com.giftregistry.ui.common.chrome.GiftMaisonBottomNav
import com.giftregistry.ui.common.chrome.showsBottomNav
import com.giftregistry.ui.registry.detail.ReservationDeepLinkBus
import com.giftregistry.ui.registry.list.RegistryListUiState
import com.giftregistry.ui.registry.list.RegistryListViewModel
import kotlinx.coroutines.launch
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.giftregistry.ui.auth.AuthScreen
import com.giftregistry.ui.auth.AuthUiState
import com.giftregistry.ui.auth.AuthViewModel
import com.giftregistry.ui.item.add.AddItemScreen
import com.giftregistry.ui.item.edit.EditItemScreen
import com.giftregistry.ui.onboarding.OnboardingScreen
import com.giftregistry.ui.onboarding.OnboardingSeenState
import com.giftregistry.ui.onboarding.OnboardingViewModel
import com.giftregistry.ui.registry.create.CreateRegistryScreen
import com.giftregistry.ui.registry.detail.RegistryDetailScreen
import com.giftregistry.ui.registry.invite.InviteBottomSheet
import com.giftregistry.ui.notifications.NotificationsScreen
import com.giftregistry.ui.registry.list.RegistryListScreen
import com.giftregistry.ui.settings.SettingsScreen
import com.giftregistry.ui.store.browser.StoreBrowserScreen
import com.giftregistry.ui.store.list.StoreListScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(deepLinkRegistryId: String? = null) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val authUiState by authViewModel.authState.collectAsStateWithLifecycle()

    val onboardingViewModel: OnboardingViewModel = hiltViewModel()
    val onboardingSeenState by onboardingViewModel.state.collectAsStateWithLifecycle()

    val backStack = remember { mutableStateListOf<Any>(AuthKey) }

    // RegistryListViewModel at nav scope for Lists-tab isPrimary resolver.
    // Per Phase 2 decision log: hiltViewModel() uses Activity ViewModelStoreOwner in Nav3,
    // so this shares the same instance as the one inside entry<HomeKey>.
    val registryListViewModel: RegistryListViewModel = hiltViewModel()
    val registryListState by registryListViewModel.uiState.collectAsStateWithLifecycle()

    var showAddSheet by remember { mutableStateOf(false) }

    // isPrimary resolver (Phase 9): most-recently-updated registry; null if zero.
    // Phase 10 refines this with the real isPrimary field once Home redesign lands.
    val primaryRegistryId: String? = when (val s = registryListState) {
        is RegistryListUiState.Success -> s.registries.maxByOrNull { it.updatedAt }?.id
        else -> null
    }
    val hasRegistries = primaryRegistryId != null

    LaunchedEffect(authUiState, onboardingSeenState) {
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
                // Route through onboarding when the flag is not yet set; otherwise go
                // straight to Auth. Signing out lands on Auth (flag persists in DataStore).
                val entryKey: Any = if (onboardingSeenState is OnboardingSeenState.NotSeen) {
                    OnboardingKey
                } else {
                    AuthKey
                }
                val current = backStack.lastOrNull()
                val alreadyOnCorrect =
                    (entryKey is OnboardingKey && current is OnboardingKey) ||
                        (entryKey == AuthKey && current is AuthKey)
                if (!alreadyOnCorrect) {
                    backStack.clear()
                    backStack.add(entryKey)
                }
            }
            is AuthUiState.Loading -> {
                // Do nothing — wait for Firebase session restore
            }
        }
    }

    if (authUiState is AuthUiState.Loading || onboardingSeenState is OnboardingSeenState.Loading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val currentKey = backStack.lastOrNull()
    val showBottomBar = currentKey.showsBottomNav()

    // Derived: when on RegistryDetailKey, pre-select that registry for sheet actions.
    // When on HomeKey (or elsewhere), fall back to the isPrimary registry.
    val sheetContextRegistryId: String? = when (val currentKeyLocal = currentKey) {
        is RegistryDetailKey -> currentKeyLocal.registryId
        else -> primaryRegistryId
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                GiftMaisonBottomNav(
                    currentKey = currentKey,
                    onHome = {
                        if (currentKey !is HomeKey) {
                            backStack.clear()
                            backStack.add(HomeKey)
                        }
                    },
                    onStores = {
                        // Push StoreListKey with current registry context if on
                        // RegistryDetailKey, else null (Home-FAB pathway).
                        val preSelected = (currentKey as? RegistryDetailKey)?.registryId
                        backStack.add(StoreListKey(preSelectedRegistryId = preSelected))
                    },
                    onFab = { showAddSheet = true },
                    onLists = {
                        // UI-SPEC Interaction table: no-op if already on RegistryDetail.
                        if (currentKey is RegistryDetailKey) {
                            // no-op — user already on a detail screen
                        } else if (hasRegistries) {
                            backStack.add(RegistryDetailKey(registryId = primaryRegistryId))
                        } else {
                            // Zero-registry: open the sheet pre-focused on New registry.
                            showAddSheet = true
                        }
                    },
                    onYou = {
                        if (currentKey !is SettingsKey) {
                            backStack.add(SettingsKey)
                        }
                    },
                )
            }
        }
    ) { innerPadding ->
        // Blur underneath the AddActionSheet on API 31+; plain scrim only on older APIs.
        val contentBlur = if (showAddSheet && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Modifier.blur(1.dp)
        } else {
            Modifier
        }
        Box(modifier = Modifier.fillMaxSize().then(contentBlur)) {
            NavDisplay(
                backStack = backStack,
                onBack = { if (backStack.size > 1) backStack.removeLast() },
                entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator()),
                modifier = Modifier.padding(innerPadding),
                entryProvider = entryProvider {
                    entry<AuthKey> { AuthScreen() }

                    entry<OnboardingKey> { OnboardingScreen() }

                    entry<HomeKey> {
                        RegistryListScreen(
                            onNavigateToCreate = { backStack.add(CreateRegistryKey) },
                            onNavigateToDetail = { registryId -> backStack.add(RegistryDetailKey(registryId)) },
                            onNavigateToEdit = { registryId -> backStack.add(EditRegistryKey(registryId)) },
                            onNavigateToNotifications = { backStack.add(NotificationsKey) },
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
                            onNavigateToInvite = { showInviteSheet = true },
                        )

                        if (showInviteSheet) {
                            InviteBottomSheet(
                                registryId = key.registryId,
                                onDismiss = { showInviteSheet = false }
                            )
                        }
                    }

                    entry<StoreListKey> { key ->
                        StoreListScreen(
                            onBack = { backStack.removeLast() },
                            onStoreSelected = { storeId ->
                                backStack.add(StoreBrowserKey(storeId = storeId, registryId = key.preSelectedRegistryId))
                            },
                        )
                    }

                    entry<StoreBrowserKey> { key ->
                        StoreBrowserScreen(
                            onBack = { backStack.removeLast() },
                            onAddToList = { url, registryId ->
                                val target = registryId ?: return@StoreBrowserScreen
                                // registryId is nullable at the nav-key level; when null (entered from
                                // Home FAB) we cannot dispatch to AddItemKey which requires a concrete
                                // registryId. The Add-to-list button is already disabled when registryId
                                // is null (guarded in StoreBrowserScreen with viewModel.registryId != null),
                                // so this branch is defensive only. Follow-up: D-10 picker + auto-pick
                                // last-used registry for Home-FAB entry path.
                                backStack.add(
                                    AddItemKey(
                                        registryId = target,
                                        initialUrl = url,
                                        initialRegistryId = target,
                                    )
                                )
                            },
                        )
                    }

                    entry<AddItemKey> { key ->
                        AddItemScreen(
                            registryId = key.registryId,
                            initialUrl = key.initialUrl,
                            initialRegistryId = key.initialRegistryId,
                            onBack = { backStack.removeLast() },
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

                    entry<NotificationsKey> {
                        NotificationsScreen(
                            onBack = { backStack.removeLast() },
                            onNavigateToRegistry = { registryId ->
                                backStack.add(RegistryDetailKey(registryId))
                            },
                        )
                    }
                }
            )
        }
    }

    // Hoisted above the Scaffold's NavDisplay so the sheet's scrim covers the nav bar.
    AddActionSheet(
        visible = showAddSheet,
        onDismiss = { showAddSheet = false },
        onNewRegistry = {
            showAddSheet = false
            backStack.add(CreateRegistryKey)
        },
        onItemFromUrl = {
            showAddSheet = false
            if (sheetContextRegistryId != null) {
                backStack.add(AddItemKey(registryId = sheetContextRegistryId))
            }
            // Zero-registry: row is visually disabled inside the sheet; this branch
            // is a defensive no-op. Phase 10 adds the inline helper/picker polish.
        },
        onBrowseStores = {
            showAddSheet = false
            backStack.add(StoreListKey(preSelectedRegistryId = sheetContextRegistryId))
        },
        onAddManually = {
            showAddSheet = false
            if (sheetContextRegistryId != null) {
                backStack.add(
                    AddItemKey(
                        registryId = sheetContextRegistryId,
                        initialUrl = "",
                    )
                )
            }
        },
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
