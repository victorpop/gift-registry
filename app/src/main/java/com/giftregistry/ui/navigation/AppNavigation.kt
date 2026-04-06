package com.giftregistry.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.giftregistry.ui.auth.AuthScreen
import com.giftregistry.ui.auth.AuthUiState
import com.giftregistry.ui.auth.AuthViewModel
import com.giftregistry.ui.registry.create.CreateRegistryScreen
import com.giftregistry.ui.registry.detail.RegistryDetailScreen
import com.giftregistry.ui.registry.list.RegistryListScreen
import com.giftregistry.ui.settings.SettingsScreen

@Composable
fun AppNavigation() {
    val authViewModel: AuthViewModel = hiltViewModel()
    val authUiState by authViewModel.authState.collectAsStateWithLifecycle()

    val backStack = remember { mutableStateListOf<Any>(AuthKey) }

    LaunchedEffect(authUiState) {
        when (authUiState) {
            is AuthUiState.Authenticated -> {
                if (backStack.lastOrNull() !is HomeKey) {
                    backStack.clear()
                    backStack.add(HomeKey)
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
                RegistryDetailScreen(
                    registryId = key.registryId,
                    onBack = { backStack.removeLast() },
                    onNavigateToAddItem = { backStack.add(AddItemKey(key.registryId)) },
                    onNavigateToEditItem = { itemId -> backStack.add(EditItemKey(key.registryId, itemId)) },
                    onNavigateToEditRegistry = { backStack.add(EditRegistryKey(key.registryId)) },
                    onNavigateToInvite = { /* Plan 05 Task 2 wires InviteBottomSheet */ }
                )
            }

            entry<AddItemKey> { key ->
                // Placeholder — Plan 05 replaces with AddItemScreen
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Add Item — Plan 05")
                }
            }

            entry<EditItemKey> { key ->
                // Placeholder — Plan 05 replaces with EditItemScreen
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Edit Item — Plan 05")
                }
            }

            entry<SettingsKey> {
                SettingsScreen(onBack = { backStack.removeLast() })
            }
        }
    )
}
