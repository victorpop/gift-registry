package com.giftregistry.ui.registry.detail

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.giftregistry.R
import com.giftregistry.domain.model.Item
import com.giftregistry.ui.navigation.hiltViewModelWithNavArgs
import com.giftregistry.ui.theme.GiftMaisonTheme
import kotlinx.coroutines.launch

@Composable
fun RegistryDetailScreen(
    registryId: String,
    onBack: () -> Unit,
    // onNavigateToAddItem retained for future inline "add first item" affordance;
    // Phase 9 global FAB handles current add-item nav flow.
    onNavigateToAddItem: () -> Unit,
    onNavigateToEditItem: (String) -> Unit,
    onNavigateToEditRegistry: () -> Unit,
    onNavigateToInvite: () -> Unit,
    onNavigateToRegistry: (String) -> Unit = {},
    viewModel: RegistryDetailViewModel = hiltViewModelWithNavArgs(
        key = registryId,
        "registryId" to registryId,
    ),
) {
    // --- VM state collection (PRESERVED) ---
    val registry by viewModel.registry.collectAsStateWithLifecycle()
    val items by viewModel.items.collectAsStateWithLifecycle()
    val deleteError by viewModel.deleteError.collectAsStateWithLifecycle()
    val registryDeleted by viewModel.registryDeleted.collectAsStateWithLifecycle()
    val isReserving by viewModel.isReserving.collectAsStateWithLifecycle()
    val hasActiveReservation by viewModel.hasActiveReservation.collectAsStateWithLifecycle()
    val activeReservationId by viewModel.activeReservationId.collectAsStateWithLifecycle()
    val confirmingPurchase by viewModel.confirmingPurchase.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    val snackbarHostState = remember { SnackbarHostState() }
    val unavailableMsg = stringResource(R.string.reservation_error_unavailable)
    val genericErrorMsg = stringResource(R.string.reservation_error_generic)
    val linkCopiedMsg = stringResource(R.string.registry_share_link_copied)

    // --- UI state ---
    var showGuestSheet by remember { mutableStateOf(false) }
    var overflowMenuExpanded by remember { mutableStateOf(false) }
    var showDeleteRegistryDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<Item?>(null) }

    // --- Phase 11 filter + lazy list state ---
    val listState = rememberLazyListState()
    var activeFilterIndex by rememberSaveable { mutableIntStateOf(0) }
    val activeFilter = FilterChipState.entries[activeFilterIndex]
    val filteredItems = remember(items, activeFilter) { items.filter { activeFilter.matches(it.status) } }

    // --- Share tap closure (reused by top-bar share icon AND overflow Share item) ---
    val onShareTap: () -> Unit = {
        val shareUrl = shareUrlOf(registryId)
        clipboardManager.setText(AnnotatedString(shareUrl))
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareUrl)
        }
        context.startActivity(Intent.createChooser(intent, null))
        scope.launch { snackbarHostState.showSnackbar(linkCopiedMsg) }
    }

    // --- LaunchedEffects (PRESERVED) ---
    LaunchedEffect(deleteError) {
        if (deleteError != null) {
            snackbarHostState.showSnackbar(deleteError ?: "")
            viewModel.clearDeleteError()
        }
    }

    LaunchedEffect(registryDeleted) {
        if (registryDeleted) onBack()
    }

    LaunchedEffect(Unit) {
        viewModel.reservationEvents.collect { event ->
            when (event) {
                is RegistryDetailViewModel.ReservationEvent.OpenRetailer -> {
                    runCatching {
                        val intent = Intent(Intent.ACTION_VIEW, event.affiliateUrl.toUri())
                        context.startActivity(intent)
                    }
                }
                RegistryDetailViewModel.ReservationEvent.ShowGuestSheet -> {
                    showGuestSheet = true
                }
                is RegistryDetailViewModel.ReservationEvent.ShowConflictError -> {
                    val msg = if (event.code == "ITEM_UNAVAILABLE") unavailableMsg else genericErrorMsg
                    snackbarHostState.showSnackbar(msg)
                }
            }
        }
    }

    // Phase 6 (UI-SPEC Contract 2 & 3): collect confirm-purchase + FCM push snackbar messages
    LaunchedEffect(Unit) {
        viewModel.snackbarMessages.collect { msg ->
            when (msg) {
                is SnackbarMessage.Resource ->
                    snackbarHostState.showSnackbar(
                        message = context.getString(msg.resId),
                        duration = SnackbarDuration.Short,
                    )
                is SnackbarMessage.Push -> {
                    val text = context.getString(
                        R.string.notifications_purchase_snackbar,
                        msg.registryName,
                    )
                    val actionLabel = context.getString(R.string.notifications_purchase_snackbar_action)
                    val result = snackbarHostState.showSnackbar(
                        message = text,
                        actionLabel = actionLabel,
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed && msg.registryId != registryId) {
                        onNavigateToRegistry(msg.registryId)
                    }
                }
            }
        }
    }

    // --- Phase 11 SCR-08 Layout: Box (not Scaffold — UI-SPEC Pitfall 4) ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GiftMaisonTheme.colors.paper),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "hero") {
                RegistryDetailHero(
                    registry = registry,
                    listState = listState,
                    onBack = onBack,
                    onShare = onShareTap,
                    onOverflow = { overflowMenuExpanded = true },
                )
            }

            item(key = "stats") {
                StatsStrip(items = items)
            }

            item(key = "share") {
                ShareBanner(
                    registryId = registryId,
                    onShared = { scope.launch { snackbarHostState.showSnackbar(linkCopiedMsg) } },
                )
            }

            item(key = "filter") {
                FilterChipsRow(
                    items = items,
                    activeFilter = activeFilter,
                    onFilterSelected = { chip -> activeFilterIndex = chip.ordinal },
                )
            }

            // Phase 6 (UI-SPEC Contract 1): confirm-purchase banner for givers with active reservation
            val reservationId = activeReservationId
            if (hasActiveReservation && reservationId != null) {
                item(key = "confirm-purchase-banner") {
                    ConfirmPurchaseBanner(
                        isConfirming = confirmingPurchase,
                        onConfirm = { viewModel.onConfirmPurchase(reservationId) },
                    )
                }
            }

            if (filteredItems.isEmpty() && items.isNotEmpty()) {
                // Active filter shows no results
                item(key = "empty-filter") {
                    // No items match the active filter — show nothing (filter counts show 0)
                }
            } else if (items.isEmpty()) {
                item(key = "empty-list") {
                    // No items at all
                }
            }

            itemsIndexed(
                items = filteredItems,
                key = { _, item -> item.id },
            ) { idx, item ->
                RegistryItemRow(
                    item = item,
                    isLast = idx == filteredItems.lastIndex,
                    onEdit = { onNavigateToEditItem(item.id) },
                    onDelete = { itemToDelete = item },
                )
            }

            // Bottom padding to account for the Phase 9 bottom nav bar (≈ 90 dp)
            item(key = "bottom-nav-padding") {
                Spacer(Modifier.height(90.dp))
            }
        }

        // --- Overflow DropdownMenu (PRESERVED: Edit / Share / Invite / Delete) ---
        // Anchored at top-end below the hero toolbar icon row (~56 dp)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 56.dp, end = 8.dp),
        ) {
            DropdownMenu(
                expanded = overflowMenuExpanded,
                onDismissRequest = { overflowMenuExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.registry_edit_title)) },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                    onClick = {
                        overflowMenuExpanded = false
                        onNavigateToEditRegistry()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.registry_share_button)) },
                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                    onClick = {
                        overflowMenuExpanded = false
                        onShareTap()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.registry_invite_title)) },
                    leadingIcon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                    onClick = {
                        overflowMenuExpanded = false
                        onNavigateToInvite()
                    },
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.common_delete),
                            color = MaterialTheme.colorScheme.error,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                        )
                    },
                    onClick = {
                        overflowMenuExpanded = false
                        showDeleteRegistryDialog = true
                    },
                )
            }
        }

        // --- SnackbarHost overlay (bottom, above bottom nav) ---
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 90.dp),
        )
    }

    // --- GuestIdentitySheet (PRESERVED) ---
    if (showGuestSheet) {
        GuestIdentitySheet(
            initial = null,
            onDismiss = { showGuestSheet = false },
            onSubmit = { guest ->
                showGuestSheet = false
                viewModel.onGuestIdentitySubmitted(guest)
            },
        )
    }

    // --- Delete registry confirmation AlertDialog (PRESERVED) ---
    if (showDeleteRegistryDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteRegistryDialog = false },
            title = { Text(stringResource(R.string.registry_delete_confirm_title)) },
            text = { Text(stringResource(R.string.registry_delete_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onDeleteRegistry()
                        showDeleteRegistryDialog = false
                    },
                ) {
                    Text(
                        text = stringResource(R.string.common_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteRegistryDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    // --- Delete item confirmation AlertDialog (PRESERVED) ---
    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text(stringResource(R.string.item_delete_confirm_title)) },
            text = { Text(stringResource(R.string.item_delete_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onDeleteItem(item.id)
                        itemToDelete = null
                    },
                ) {
                    Text(
                        text = stringResource(R.string.common_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}
