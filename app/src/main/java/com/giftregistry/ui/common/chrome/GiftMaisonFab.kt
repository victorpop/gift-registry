package com.giftregistry.ui.common.chrome

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.giftregistry.R
import com.giftregistry.ui.theme.GiftMaisonTheme
import com.giftregistry.ui.theme.fabShadow

/**
 * CHROME-02: 54 dp centre FAB — accent fill, 4 dp paper ring, accent shadow.
 *
 * The handoff's 22 dp lift (`top: -22`) is applied by the CALLER
 * (GiftMaisonBottomNav's FAB slot), not here — keeping this composable slot-
 * agnostic so it can be previewed in the style guide without nav context.
 *
 * Modifier chain order is CRITICAL (Pitfall 4): fabShadow → border → background.
 * Applying background before border would paint over the paper ring.
 */
@Composable
fun GiftMaisonFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GiftMaisonTheme.colors
    val contentDesc = stringResource(R.string.nav_fab_add_content_description)
    Box(
        modifier = modifier
            .size(54.dp)
            .fabShadow(tint = colors.accent)
            .border(width = 4.dp, color = colors.paper, shape = CircleShape)
            .background(color = colors.accent, shape = CircleShape)
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = contentDesc
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null, // announced via parent semantics
            tint = colors.accentInk,
            modifier = Modifier.size(24.dp),
        )
    }
}
