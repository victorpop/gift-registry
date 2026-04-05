package com.giftregistry.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.giftregistry.R
import com.giftregistry.ui.auth.AuthScreen
import com.giftregistry.ui.auth.AuthUiState
import com.giftregistry.ui.auth.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
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
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(stringResource(R.string.app_name)) },
                            actions = {
                                IconButton(onClick = { backStack.add(SettingsKey) }) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = stringResource(R.string.auth_settings_title)
                                    )
                                }
                            }
                        )
                    }
                ) { paddingValues ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Home \u2014 Phase 3")
                    }
                }
            }
            entry<SettingsKey> {
                Text("Settings \u2014 Plan 04")
            }
        }
    )
}
