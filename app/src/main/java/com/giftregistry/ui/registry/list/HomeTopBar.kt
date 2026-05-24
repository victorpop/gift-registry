package com.giftregistry.ui.registry.list

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.giftregistry.ui.common.AvatarButton
import com.giftregistry.ui.notifications.NotificationsInboxBell
import com.giftregistry.ui.theme.GiftMaisonTheme
import com.giftregistry.ui.theme.GiftMaisonWordmark

/**
 * SCR-07: Home top bar — wordmark left, bell + avatar right, inline Row (NOT
 * Material3 TopAppBar). Bell entry point added in Phase 16 (was removed in the
 * Phase 10 reskin with placement deferred).
 */
@Composable
fun HomeTopBar(
    initials: String,
    onAvatarClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GiftMaisonTheme.spacing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.edge, vertical = spacing.gap16),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GiftMaisonWordmark()
        Spacer(modifier = Modifier.weight(1f))
        NotificationsInboxBell(onClick = onNotificationsClick)
        AvatarButton(initials = initials, onClick = onAvatarClick)
    }
}
