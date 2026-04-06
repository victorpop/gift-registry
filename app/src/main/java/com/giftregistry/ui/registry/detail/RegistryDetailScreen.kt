package com.giftregistry.ui.registry.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.giftregistry.R
import com.giftregistry.domain.model.Item
import com.giftregistry.domain.model.ItemStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistryDetailScreen(
    registryId: String,
    onBack: () -> Unit,
    onNavigateToAddItem: () -> Unit,
    onNavigateToEditItem: (String) -> Unit,
    onNavigateToEditRegistry: () -> Unit,
    onNavigateToInvite: () -> Unit,
    viewModel: RegistryDetailViewModel = hiltViewModel()
) {
    val registry by viewModel.registry.collectAsStateWithLifecycle()
    val items by viewModel.items.collectAsStateWithLifecycle()
    val registryDeleted by viewModel.registryDeleted.collectAsStateWithLifecycle()

    var showDeleteRegistryDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<Item?>(null) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    LaunchedEffect(registryDeleted) {
        if (registryDeleted) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(registry?.title ?: "") },
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
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = null
                            )
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.registry_edit_title)) },
                                onClick = {
                                    showOverflowMenu = false
                                    onNavigateToEditRegistry()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.registry_invite_title)) },
                                onClick = {
                                    showOverflowMenu = false
                                    onNavigateToInvite()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.common_delete)) },
                                onClick = {
                                    showOverflowMenu = false
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
        }
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
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.registry_detail_items_title),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.registry_detail_no_items))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        ItemCard(
                            item = item,
                            onEdit = { onNavigateToEditItem(item.id) },
                            onDelete = { itemToDelete = item }
                        )
                    }
                }
            }
        }
    }

    if (showDeleteRegistryDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteRegistryDialog = false },
            title = { Text(stringResource(R.string.registry_delete_confirm_title)) },
            text = { Text(stringResource(R.string.registry_delete_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteRegistryDialog = false
                        viewModel.onDeleteRegistry()
                    }
                ) {
                    Text(stringResource(R.string.common_delete))
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
                    Text(stringResource(R.string.common_delete))
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
private fun ItemCard(
    item: Item,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall
                )
                item.price?.let { price ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = price,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                ItemStatusChip(status = item.status)
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = null)
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.item_edit_title)) },
                        onClick = {
                            showMenu = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.common_delete)) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ItemStatusChip(status: ItemStatus) {
    val label = when (status) {
        ItemStatus.AVAILABLE -> stringResource(R.string.item_status_available)
        ItemStatus.RESERVED -> stringResource(R.string.item_status_reserved)
        ItemStatus.PURCHASED -> stringResource(R.string.item_status_purchased)
    }
    FilterChip(
        selected = false,
        onClick = {},
        label = { Text(label, style = MaterialTheme.typography.labelSmall) }
    )
}
