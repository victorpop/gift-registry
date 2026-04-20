package com.giftregistry.ui.notifications

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.giftregistry.R

/**
 * A TopAppBar actions-slot composable that renders a bell icon with an unread
 * badge. Renders "9+" when the count exceeds 9.
 *
 * Backed by [InboxBellViewModel] which flat-maps auth state → unread count;
 * emits 0 when the user is unauthenticated so the badge never shows on
 * unauthenticated screens (Home is auth-gated anyway but this is belt-and-suspenders).
 */
@Composable
fun NotificationsInboxBell(onClick: () -> Unit) {
    val viewModel: InboxBellViewModel = hiltViewModel()
    val count by viewModel.unreadCount.collectAsStateWithLifecycle()

    IconButton(onClick = onClick) {
        BadgedBox(
            badge = {
                if (count > 0) {
                    Badge {
                        Text(if (count > 9) "9+" else count.toString())
                    }
                }
            }
        ) {
            Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = stringResource(R.string.notifications_bell_cd),
            )
        }
    }
}
