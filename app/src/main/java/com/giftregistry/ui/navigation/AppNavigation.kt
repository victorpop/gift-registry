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
import com.giftregistry.ui.discover.DiscoverScreen
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

    // Unified loading gate: show the spinner whenever state is still being established OR
    // when the backStack hasn't yet been reconciled with the latest authUiState.
    //
    // The mismatch race (BUG-AUTH-FLASH-260512 follow-up):
    //   backStack is initialised to [AuthKey]. The LaunchedEffect that clears and repopulates
    //   it runs *after* the first composition that uses it. On a heavy cold start (logcat shows
    //   "Skipped 35+ frames"), NavDisplay reads backStack.lastOrNull() == AuthKey and renders
    //   AuthScreen for up to ~1 second before the LaunchedEffect fires to replace it with
    //   HomeKey. Gating on the mismatch holds the spinner until the backStack and authUiState
    //   agree, collapsing that window to zero frames.
    val currentKey = backStack.lastOrNull()
    val isAuthRoot = currentKey is AuthKey || currentKey is OnboardingKey

    val isLoading = authUiState is AuthUiState.Loading ||
        onboardingSeenState is OnboardingSeenState.Loading
    // Only check for mismatch when auth state is settled (not Loading) — while Loading we
    // already show the spinner via isLoading, and we cannot know what root to expect yet.
    val isMidTransition = !isLoading &&
        isAuthRoot != (authUiState is AuthUiState.Unauthenticated)

    if (isLoading || isMidTransition) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val showBottomBar = currentKey.showsBottomNav()

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
                    onDiscover = {
                        // Slot 2 → Discover. Nav root — push only if not already on Discover
                        // (avoids duplicate stack entries on repeated tab taps).
                        if (currentKey !is DiscoverKey) {
                            backStack.add(DiscoverKey)
                        }
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
                onBack = { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) },
                entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator()),
                modifier = Modifier.padding(innerPadding),
                entryProvider = entryProvider {
                    entry<AuthKey> { AuthScreen() }

                    entry<OnboardingKey> { OnboardingScreen() }

                    entry<HomeKey> {
                        RegistryListScreen(
                            onNavigateToDetail = { registryId -> backStack.add(RegistryDetailKey(registryId)) },
                            onNavigateToEdit = { registryId -> backStack.add(EditRegistryKey(registryId)) },
                            onNavigateToNotifications = { backStack.add(NotificationsKey) },
                            onNavigateToSettings = { backStack.add(SettingsKey) },
                        )
                    }

                    entry<DiscoverKey> { DiscoverScreen() }

                    entry<CreateRegistryKey> {
                        CreateRegistryScreen(
                            // Guard: removeLast() is called from a LaunchedEffect after an async
                            // Firestore save. On Android 14+ physical devices the predictive-back
                            // gesture (NavigationBackHandler) and this in-screen lambda can fire in
                            // quick succession. Without the size check an empty backStack triggers
                            // NavDisplay's require(backStack.isNotEmpty()) crash.
                            onBack = { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) },
                            onSaved = { registryId ->
                                if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                                backStack.add(AddItemKey(registryId = registryId))   // Phase 11: Step 1 → Step 2
                            },
                            onSkip = {
                                if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                                // Pop to whatever sits beneath CreateRegistryKey on the stack.
                                // On the standard path (Add-action sheet → New registry) that's HomeKey.
                            },
                        )
                    }

                    entry<EditRegistryKey> { key ->
                        CreateRegistryScreen(
                            registryId = key.registryId,
                            onBack = { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) },
                            onSaved = { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) }
                        )
                    }

                    entry<RegistryDetailKey> { key ->
                        var showInviteSheet by remember { mutableStateOf(false) }

                        RegistryDetailScreen(
                            registryId = key.registryId,
                            // Guard: RegistryDetailScreen stays in composition during NavDisplay's
                            // exit animation. On Android 14+ the back arrow remains tappable during
                            // the predictive-back preview. A second tap (or system-gesture + button
                            // together) fires removeLast() on a 1-element stack → empty → crash.
                            onBack = { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) },
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

                    entry<AddItemKey> { key ->
                        AddItemScreen(
                            registryId = key.registryId,
                            fromAddSheet = key.fromAddSheet,
                            initialUrl = key.initialUrl,
                            initialRegistryId = key.initialRegistryId,
                            onBack = { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) },
                            onNavigateToCreateRegistry = {
                                // quick-260428-iny: zero-registry empty-state link in
                                // the picker routes the user to CreateRegistryKey.
                                backStack.add(CreateRegistryKey)
                            },
                        )
                    }

                    entry<EditItemKey> { key ->
                        EditItemScreen(
                            registryId = key.registryId,
                            itemId = key.itemId,
                            onBack = { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) }
                        )
                    }

                    entry<SettingsKey> {
                        SettingsScreen(onBack = { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) })
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
                            onBack = { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) },
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
        onAddItem = {
            // quick-260428-iny: trimmed sheet — single Add-an-item row routes to
            // AddItemScreen with no preselected registry; the picker rendered as
            // the first field gates Save until the user chooses a registry.
            showAddSheet = false
            backStack.add(AddItemKey(registryId = null, fromAddSheet = true))
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
