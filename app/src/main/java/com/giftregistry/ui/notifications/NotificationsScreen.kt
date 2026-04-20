package com.giftregistry.ui.notifications

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.giftregistry.R
import com.giftregistry.domain.model.Notification
import com.giftregistry.domain.model.NotificationType
import kotlinx.coroutines.delay

/**
 * Standalone notifications inbox screen.
 *
 * - Renders a LazyColumn of notification cards sorted by createdAt DESC (server-ordered).
 * - Each card shows a type-aware leading icon + localized title/body.
 * - Read items are visually muted (onSurfaceVariant text color).
 * - Opening the screen triggers a 500ms-debounced batched mark-as-read for all
 *   currently-visible unread entries (LaunchedEffect keyed on uiState).
 * - Tapping a card navigates to the registry detail for payload.registryId.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onNavigateToRegistry: (registryId: String) -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Batched mark-as-read: 500ms after the visible unread set changes, mark them all read.
    LaunchedEffect(uiState) {
        val state = uiState
        if (state is NotificationsViewModel.UiState.Loaded) {
            val unreadIds = state.notifications
                .filter { it.readAtMs == null }
                .map { it.id }
            if (unreadIds.isNotEmpty()) {
                delay(500)
                viewModel.markVisibleRead(unreadIds)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.notifications_screen_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                        )
                    }
                },
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is NotificationsViewModel.UiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is NotificationsViewModel.UiState.Unauthenticated,
                is NotificationsViewModel.UiState.Empty -> {
                    Text(
                        text = stringResource(R.string.notifications_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                is NotificationsViewModel.UiState.Loaded -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 16.dp,
                            vertical = 8.dp,
                        ),
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.notifications, key = { it.id }) { notification ->
                            NotificationCard(
                                notification = notification,
                                onClick = {
                                    notification.payload["registryId"]?.let { onNavigateToRegistry(it) }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(
    notification: Notification,
    onClick: () -> Unit,
) {
    val isRead = notification.readAtMs != null
    val textColor = if (isRead) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isRead) 0.dp else 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = notification.type.toIcon(),
                contentDescription = null,
                tint = if (isRead) MaterialTheme.colorScheme.onSurfaceVariant
                       else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification.localizedTitle(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                )
                Text(
                    text = notification.localizedBody(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Returns the appropriate Material icon for each notification type.
 * UNKNOWN falls back to the generic notifications bell.
 */
private fun NotificationType.toIcon(): ImageVector = when (this) {
    NotificationType.INVITE -> Icons.Filled.Mail
    NotificationType.RESERVATION_CREATED -> Icons.Filled.Bookmark
    NotificationType.ITEM_PURCHASED -> Icons.Filled.CheckCircle
    NotificationType.RESERVATION_EXPIRED -> Icons.Filled.Schedule
    NotificationType.RE_RESERVE_WINDOW -> Icons.Filled.Refresh
    NotificationType.UNKNOWN -> Icons.Filled.Notifications
}

/**
 * Resolves a localized title using the notification's titleKey and payload args.
 * Falls back to titleFallback if the key is UNKNOWN or payload args are missing.
 *
 * The stringResource call must happen in a @Composable context, so this is an
 * extension function that returns a String after calling stringResource.
 */
@Composable
private fun Notification.localizedTitle(): String {
    val p = payload
    return when (type) {
        NotificationType.INVITE ->
            stringResource(
                R.string.notification_invite_title,
                p["actorName"] ?: "Someone",
                p["registryName"] ?: "a registry",
            )
        NotificationType.RESERVATION_CREATED ->
            stringResource(
                R.string.notification_reservation_created_title,
                p["itemName"] ?: "an item",
            )
        NotificationType.ITEM_PURCHASED ->
            stringResource(
                R.string.notification_item_purchased_title,
                p["itemName"] ?: "an item",
            )
        NotificationType.RESERVATION_EXPIRED ->
            stringResource(
                R.string.notification_reservation_expired_title,
                p["itemName"] ?: "an item",
            )
        NotificationType.RE_RESERVE_WINDOW ->
            stringResource(
                R.string.notification_re_reserve_window_title,
                p["itemName"] ?: "an item",
            )
        NotificationType.UNKNOWN -> titleFallback
    }
}

@Composable
private fun Notification.localizedBody(): String {
    val p = payload
    return when (type) {
        NotificationType.INVITE ->
            stringResource(R.string.notification_invite_body)
        NotificationType.RESERVATION_CREATED ->
            stringResource(
                R.string.notification_reservation_created_body,
                p["actorName"] ?: "Someone",
                p["itemName"] ?: "an item",
                p["registryName"] ?: "a registry",
            )
        NotificationType.ITEM_PURCHASED ->
            stringResource(
                R.string.notification_item_purchased_body,
                p["itemName"] ?: "an item",
                p["registryName"] ?: "a registry",
            )
        NotificationType.RESERVATION_EXPIRED ->
            stringResource(
                R.string.notification_reservation_expired_body,
                p["itemName"] ?: "an item",
                p["registryName"] ?: "a registry",
            )
        NotificationType.RE_RESERVE_WINDOW ->
            stringResource(
                R.string.notification_re_reserve_window_body,
                p["itemName"] ?: "an item",
                p["registryName"] ?: "a registry",
            )
        NotificationType.UNKNOWN -> bodyFallback
    }
}
