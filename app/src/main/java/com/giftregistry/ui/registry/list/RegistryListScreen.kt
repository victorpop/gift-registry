package com.giftregistry.ui.registry.list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.giftregistry.R
import com.giftregistry.domain.model.Registry
import com.giftregistry.ui.common.toAvatarInitials
import com.giftregistry.ui.theme.GiftMaisonTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RegistryListScreen(
    onNavigateToDetail: (String) -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: RegistryListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val deleteError by viewModel.deleteError.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val colors = GiftMaisonTheme.colors
    val typography = GiftMaisonTheme.typography
    val spacing = GiftMaisonTheme.spacing

    var registryToDelete by remember { mutableStateOf<Registry?>(null) }
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.home_tab_active),
        stringResource(R.string.home_tab_drafts),
        stringResource(R.string.home_tab_past),
    )

    LaunchedEffect(deleteError) {
        deleteError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearDeleteError()
        }
    }

    Scaffold(
        containerColor = colors.paper,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.paper)
                .padding(innerPadding),
        ) {
            HomeTopBar(
                initials = toAvatarInitials(
                    displayName = currentUser?.displayName,
                    email = currentUser?.email,
                ),
                onAvatarClick = onNavigateToSettings,
            )

            when (val state = uiState) {
                RegistryListUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = colors.accent)
                    }
                }
                is RegistryListUiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = state.message,
                            style = typography.bodyM,
                            color = colors.ink,
                        )
                        Spacer(modifier = Modifier.height(spacing.gap8))
                        TextButton(onClick = { /* retry handled by Flow restart */ }) {
                            Text(stringResource(R.string.registry_list_retry), color = colors.accent)
                        }
                    }
                }
                is RegistryListUiState.Success -> {
                    val registries = state.registries
                    val primaryId = remember(registries) { primaryRegistryIdOf(registries) }
                    val todayMs = remember { startOfTodayMs() }
                    val filtered = remember(registries, selectedTabIndex, todayMs) {
                        when (selectedTabIndex) {
                            0 -> registries.filter { it.isActive(todayMs) }
                            1 -> registries.filter { it.isDraft(itemCount = 0) }  // TODO: Phase 10 stats aggregation deferred
                            2 -> registries.filter { it.isPast(todayMs) }
                            else -> registries
                        }
                    }

                    // Headline + stats caption
                    Column(modifier = Modifier.padding(horizontal = spacing.edge)) {
                        Text(
                            text = stringResource(R.string.home_headline),
                            style = typography.displayXL,
                            color = colors.ink,
                        )
                        Spacer(modifier = Modifier.height(spacing.gap4))
                        val activeCount = registries.count { it.isActive(todayMs) }
                        val totalItems = 0  // deferred — see CONTEXT.md
                        Text(
                            text = stringResource(R.string.home_stats_caption, activeCount, totalItems),
                            style = typography.monoCaps,
                            color = colors.inkFaint,
                        )
                    }

                    Spacer(modifier = Modifier.height(spacing.gap16))

                    SegmentedTabs(
                        tabs = tabs,
                        selectedIndex = selectedTabIndex,
                        onTabSelected = { selectedTabIndex = it },
                        modifier = Modifier.padding(horizontal = spacing.edge),
                    )

                    Spacer(modifier = Modifier.height(spacing.gap16))

                    if (filtered.isEmpty()) {
                        val emptyKey = when (selectedTabIndex) {
                            0 -> R.string.home_empty_active
                            1 -> R.string.home_empty_drafts
                            else -> R.string.home_empty_past
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = spacing.gap20),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(emptyKey),
                                style = typography.bodyM,
                                color = colors.inkFaint,
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = spacing.edge,
                                end = spacing.edge,
                                top = 0.dp,
                                bottom = 100.dp,  // clearance for bottom nav + FAB per handoff
                            ),
                            verticalArrangement = Arrangement.spacedBy(spacing.gap10),
                        ) {
                            items(filtered, key = { it.id }) { registry ->
                                var menuExpanded by remember(registry.id) { mutableStateOf(false) }
                                val isPrimary = registry.id == primaryId
                                Box {
                                    if (isPrimary) {
                                        RegistryCardPrimary(
                                            registry = registry,
                                            onClick = { onNavigateToDetail(registry.id) },
                                            onLongClick = { menuExpanded = true },
                                        )
                                    } else {
                                        RegistryCardSecondary(
                                            registry = registry,
                                            onClick = { onNavigateToDetail(registry.id) },
                                            onLongClick = { menuExpanded = true },
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = menuExpanded,
                                        onDismissRequest = { menuExpanded = false },
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.registry_card_menu_edit)) },
                                            onClick = {
                                                menuExpanded = false
                                                onNavigateToEdit(registry.id)
                                            },
                                        )
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = stringResource(R.string.registry_card_menu_delete),
                                                    color = MaterialTheme.colorScheme.error,
                                                )
                                            },
                                            onClick = {
                                                menuExpanded = false
                                                registryToDelete = registry
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    registryToDelete?.let { registry ->
        AlertDialog(
            onDismissRequest = { registryToDelete = null },
            title = { Text(stringResource(R.string.registry_delete_confirm_title)) },
            text = { Text(stringResource(R.string.registry_delete_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onDeleteRegistry(registry.id)
                    registryToDelete = null
                }) {
                    Text(
                        text = stringResource(R.string.common_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { registryToDelete = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}
