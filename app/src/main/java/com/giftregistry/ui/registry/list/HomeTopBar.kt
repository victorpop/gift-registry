package com.giftregistry.ui.registry.list

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.giftregistry.ui.common.AvatarButton
import com.giftregistry.ui.theme.GiftMaisonTheme
import com.giftregistry.ui.theme.GiftMaisonWordmark

/**
 * SCR-07: Home top bar — wordmark left, avatar right, inline Row (NOT Material3
 * TopAppBar). Replaces the TopAppBar + NotificationsInboxBell combo from the
 * pre-Phase-10 RegistryListScreen. Bell placement in v1.1 is deferred per
 * CONTEXT.md (likely Settings row or AddActionSheet entry in Phase 11).
 */
@Composable
fun HomeTopBar(
    initials: String,
    onAvatarClick: () -> Unit,
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
        AvatarButton(initials = initials, onClick = onAvatarClick)
    }
}
