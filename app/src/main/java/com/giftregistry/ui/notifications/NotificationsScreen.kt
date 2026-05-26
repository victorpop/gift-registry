package com.giftregistry.ui.notifications

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.giftregistry.R
import com.giftregistry.domain.model.Notification
import com.giftregistry.domain.model.NotificationType
import com.giftregistry.ui.theme.GiftMaisonTheme
import com.giftregistry.ui.theme.GiftMaisonWordmark
import kotlinx.coroutines.delay

/**
 * D-09 re-skinned notifications inbox.
 *
 * Visual: gm.paper background; wordmark TopAppBar; flat NotificationCards with
 * gm.line divider separators (no M3 Card elevation); MonoCaps timestamp;
 * accent dot for unread.
 *
 * Behaviour preserved from prior plans (16-03 enum extension + 16-04 sheet host):
 * - 500ms batched mark-as-read LaunchedEffect.
 * - Tap-branching via shouldOpenInviteSheet (D-11) — pending invites open
 *   InviteResponseSheet, everything else navigates to RegistryDetail.
 * - InviteResponseSheet host outside Scaffold so the scrim covers bottom nav.
 *
 * D-02 / D-08 negative-coverage: no badge decoration is added to the inbox card,
 * and no separate pending-invites counter path is introduced on the VM. Pending
 * INVITE cards keep the homogeneous payload-driven layout (visual richness lives
 * in the sheet, not the card); pending invites contribute to the existing unread
 * count via the bell's observeUnreadCount flow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onNavigateToRegistry: (registryId: String) -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = GiftMaisonTheme.colors
    val typography = GiftMaisonTheme.typography
    val spacing = GiftMaisonTheme.spacing

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
        containerColor = colors.paper,
        topBar = {
            TopAppBar(
                title = { GiftMaisonWordmark() },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                            tint = colors.ink,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.paper),
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
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = colors.accent,
                    )
                }
                is NotificationsViewModel.UiState.Unauthenticated,
                is NotificationsViewModel.UiState.Empty -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = spacing.edge),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = stringResource(R.string.notifications_empty_heading),
                            style = typography.displayS,
                            color = colors.ink,
                        )
                        Spacer(modifier = Modifier.height(spacing.gap8))
                        Text(
                            text = stringResource(R.string.notifications_empty_body),
                            style = typography.bodyS,
                            color = colors.inkSoft,
                            modifier = Modifier.widthIn(max = 280.dp),
                        )
                    }
                }
                is NotificationsViewModel.UiState.Loaded -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(state.notifications, key = { it.id }) { notification ->
                            NotificationCard(
                                notification = notification,
                                onClick = {
                                    if (shouldOpenInviteSheet(notification)) {
                                        viewModel.openInviteSheet(notification)
                                    } else {
                                        notification.payload["registryId"]?.let { onNavigateToRegistry(it) }
                                    }
                                },
                            )
                            HorizontalDivider(color = colors.line, thickness = 1.dp)
                        }
                    }
                }
            }
        }
    }

    // D-01 — InviteResponseSheet host (over the inbox scaffold). NotificationsScreen
    // reuses onNavigateToRegistry for both legacy invite navigate and D-05 post-accept
    // auto-nav; AppNavigation passes the same RegistryDetailKey backStack.add for both.
    val inviteNotif by viewModel.inviteSheetState.collectAsStateWithLifecycle()
    inviteNotif?.let { n ->
        val registryId = n.payload["registryId"]
        if (registryId != null) {
            InviteResponseSheet(
                registryId = registryId,
                payload = n.payload,
                onAcceptSuccess = { rid ->
                    viewModel.dismissInviteSheet()
                    onNavigateToRegistry(rid)
                },
                onDismiss = { viewModel.dismissInviteSheet() },
            )
        } else {
            // Defensive: malformed payload — close sheet silently
            LaunchedEffect(Unit) { viewModel.dismissInviteSheet() }
        }
    }
}

@Composable
private fun NotificationCard(
    notification: Notification,
    onClick: () -> Unit,
) {
    val isRead = notification.readAtMs != null
    val colors = GiftMaisonTheme.colors
    val typography = GiftMaisonTheme.typography
    val spacing = GiftMaisonTheme.spacing

    val titleColor = if (isRead) colors.inkSoft else colors.ink
    val iconTint = if (isRead) colors.inkSoft else colors.accent

    val timestamp = remember(notification.createdAtMs) {
        if (notification.createdAtMs <= 0L) ""
        else {
            val now = System.currentTimeMillis()
            // Clamp to now: Firestore serverTimestamp() can be microseconds ahead of the
            // device clock, which makes DateUtils render Romanian future-tense ("peste 0
            // minute") instead of past-tense ("acum 0 minute") for just-arrived notifications.
            val createdAt = minOf(notification.createdAtMs, now)
            DateUtils.getRelativeTimeSpanString(
                createdAt,
                now,
                DateUtils.MINUTE_IN_MILLIS,
            ).toString().uppercase()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics { if (!isRead) contentDescription = "Unread notification" }
            .padding(horizontal = spacing.gap16, vertical = spacing.gap14),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = notification.type.toIcon(),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(spacing.gap12))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = notification.localizedTitle(),
                    style = typography.bodyL,
                    color = titleColor,
                    modifier = Modifier.weight(1f),
                )
                if (timestamp.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(spacing.gap8))
                    Text(
                        text = timestamp,
                        style = typography.monoCaps,
                        color = colors.inkSoft,
                    )
                }
                if (!isRead) {
                    Spacer(modifier = Modifier.width(spacing.gap4))
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(colors.accent),
                    )
                }
            }
            Spacer(modifier = Modifier.height(spacing.gap4))
            Text(
                text = notification.localizedBody(),
                style = typography.bodyM,
                color = colors.inkSoft,
            )
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
    NotificationType.INVITE_ACCEPTED_SELF -> Icons.Filled.CheckCircle
    NotificationType.INVITE_ACCEPTED -> Icons.Filled.CheckCircle
    NotificationType.INVITE_DECLINED -> Icons.Filled.Block
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
        NotificationType.INVITE_ACCEPTED_SELF ->
            stringResource(
                R.string.notification_invite_accepted_self_title,
                p["registryName"] ?: "a registry",
            )
        NotificationType.INVITE_ACCEPTED ->
            stringResource(
                R.string.notification_invite_accepted_title,
                p["actorName"] ?: "Someone",
                p["registryName"] ?: "a registry",
            )
        NotificationType.INVITE_DECLINED ->
            stringResource(
                R.string.notification_invite_declined_title,
                p["actorName"] ?: "Someone",
                p["registryName"] ?: "a registry",
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
        NotificationType.INVITE_ACCEPTED_SELF ->
            stringResource(
                R.string.notification_invite_accepted_self_body,
                p["registryName"] ?: "a registry",
            )
        NotificationType.INVITE_ACCEPTED ->
            stringResource(
                R.string.notification_invite_accepted_body,
                p["registryName"] ?: "a registry",
            )
        NotificationType.INVITE_DECLINED ->
            stringResource(
                R.string.notification_invite_declined_body,
                p["registryName"] ?: "a registry",
            )
        NotificationType.UNKNOWN -> bodyFallback
    }
}
