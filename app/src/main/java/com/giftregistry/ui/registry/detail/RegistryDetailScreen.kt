package com.giftregistry.ui.registry.detail

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.giftregistry.R
import com.giftregistry.ui.navigation.hiltViewModelWithNavArgs
import com.giftregistry.domain.model.Item
import com.giftregistry.domain.model.ItemStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistryDetailScreen(
    registryId: String,
    onBack: () -> Unit,
    onNavigateToAddItem: () -> Unit,
    onNavigateToEditItem: (String) -> Unit,
    onNavigateToEditRegistry: () -> Unit,
    onNavigateToInvite: () -> Unit,
    onNavigateToRegistry: (String) -> Unit = {},
    viewModel: RegistryDetailViewModel = hiltViewModelWithNavArgs(
        key = registryId,
        "registryId" to registryId,
    )
) {
    val registry by viewModel.registry.collectAsStateWithLifecycle()
    val items by viewModel.items.collectAsStateWithLifecycle()
    val deleteError by viewModel.deleteError.collectAsStateWithLifecycle()
    val registryDeleted by viewModel.registryDeleted.collectAsStateWithLifecycle()
    val isReserving by viewModel.isReserving.collectAsStateWithLifecycle()
    val hasActiveReservation by viewModel.hasActiveReservation.collectAsStateWithLifecycle()
    val confirmingPurchase by viewModel.confirmingPurchase.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val context = LocalContext.current
    var showGuestSheet by remember { mutableStateOf(false) }
    val unavailableMsg = stringResource(R.string.reservation_error_unavailable)
    val genericErrorMsg = stringResource(R.string.reservation_error_generic)

    var overflowMenuExpanded by remember { mutableStateOf(false) }
    var showDeleteRegistryDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<Item?>(null) }

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
                        duration = androidx.compose.material3.SnackbarDuration.Short,
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = registry?.title ?: "",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back)
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { overflowMenuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = null
                            )
                        }
                        DropdownMenu(
                            expanded = overflowMenuExpanded,
                            onDismissRequest = { overflowMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.registry_edit_title)) },
                                leadingIcon = {
                                    Icon(Icons.Default.Edit, contentDescription = null)
                                },
                                onClick = {
                                    overflowMenuExpanded = false
                                    onNavigateToEditRegistry()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.registry_share_button)) },
                                leadingIcon = {
                                    Icon(Icons.Default.Share, contentDescription = null)
                                },
                                onClick = {
                                    overflowMenuExpanded = false
                                    /* Share — Phase 5 */
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.registry_invite_title)) },
                                leadingIcon = {
                                    Icon(Icons.Default.PersonAdd, contentDescription = null)
                                },
                                onClick = {
                                    overflowMenuExpanded = false
                                    onNavigateToInvite()
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(R.string.common_delete),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    overflowMenuExpanded = false
                                    showDeleteRegistryDialog = true
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAddItem) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.item_add_title)
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        if (registry == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 16.dp,
                    vertical = 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Registry info section
                item {
                    RegistryInfoSection(registry = registry!!)
                }

                // Phase 6 (UI-SPEC Contract 1): confirm-purchase banner for givers with active reservation
                if (hasActiveReservation) {
                    item(key = "confirm-purchase-banner") {
                        ConfirmPurchaseBanner(
                            isConfirming = confirmingPurchase,
                            onConfirm = {
                                // Use first reserved item's id as the reservation proxy.
                                // Full reservationId wiring requires Plan 06-03 server response
                                // stored in GuestPreferencesDataStore — deferred to future plan.
                                // For now, use the first RESERVED item id as the reservationId
                                // (matches server-side lookup in confirmPurchase callable).
                                val firstReservedItem = items.firstOrNull {
                                    it.status == com.giftregistry.domain.model.ItemStatus.RESERVED
                                }
                                firstReservedItem?.let { viewModel.onConfirmPurchase(it.id) }
                            },
                        )
                    }
                }

                // Items section header
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.registry_detail_items_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (items.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.registry_detail_no_items),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(items, key = { it.id }) { item ->
                        ItemCard(
                            item = item,
                            isReserving = isReserving,
                            onEditClick = { onNavigateToEditItem(item.id) },
                            onDeleteClick = { itemToDelete = item },
                            onReserveClick = { viewModel.onReserveClicked(item.id) }
                        )
                    }
                }
            }
        }
    }

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
                    }
                ) {
                    Text(
                        text = stringResource(R.string.common_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteRegistryDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

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
                    }
                ) {
                    Text(
                        text = stringResource(R.string.common_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}

@Composable
private fun RegistryInfoSection(registry: com.giftregistry.domain.model.Registry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SuggestionChip(
                    onClick = {},
                    label = { Text(registry.occasion) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = if (registry.visibility == "private") {
                        Icons.Default.Lock
                    } else {
                        Icons.Default.Public
                    },
                    contentDescription = if (registry.visibility == "private") {
                        stringResource(R.string.registry_visibility_private)
                    } else {
                        stringResource(R.string.registry_visibility_public)
                    },
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (registry.visibility == "private") {
                        stringResource(R.string.registry_visibility_private)
                    } else {
                        stringResource(R.string.registry_visibility_public)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            registry.eventDateMs?.let { dateMs ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(dateMs)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            registry.eventLocation?.let { location ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = location,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            registry.description?.let { desc ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun ItemCard(
    item: Item,
    isReserving: Boolean,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onReserveClick: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Placeholder icon for image (Coil deferred per plan spec)
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .padding(end = 8.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                item.price?.let { price ->
                    Text(
                        text = price,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                ItemStatusChip(status = item.status)

                // Reserve button for available items
                if (item.status == ItemStatus.AVAILABLE) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onReserveClick,
                        enabled = !isReserving,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.reservation_reserve_button))
                    }
                }

                // Reserved label + live countdown for reserved items (D-08, D-18, RES-02/RES-06)
                if (item.status == ItemStatus.RESERVED) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.reservation_reserved_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                    item.expiresAt?.let { expiresAtMs ->
                        ReservationCountdown(
                            expiresAtMs = expiresAtMs,
                        )
                    }
                }
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = null
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.item_edit_title)) },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null)
                        },
                        onClick = {
                            menuExpanded = false
                            onEditClick()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(R.string.common_delete),
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onDeleteClick()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ItemStatusChip(status: ItemStatus) {
    val (label, containerColor) = when (status) {
        ItemStatus.AVAILABLE -> Pair(
            stringResource(R.string.item_status_available),
            MaterialTheme.colorScheme.primaryContainer
        )
        ItemStatus.RESERVED -> Pair(
            stringResource(R.string.item_status_reserved),
            MaterialTheme.colorScheme.tertiaryContainer
        )
        ItemStatus.PURCHASED -> Pair(
            stringResource(R.string.item_status_purchased),
            MaterialTheme.colorScheme.surfaceVariant
        )
    }

    SuggestionChip(
        onClick = {},
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = containerColor
        )
    )
}

