package com.giftregistry.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.giftregistry.R
import com.giftregistry.ui.theme.GiftMaisonTheme

/**
 * SCR-07: Avatar button on the Home top bar.
 *
 *  - 30 dp circle on GiftMaisonTheme.colors.second (olive)
 *  - Initials (from `toAvatarInitials`) in bodyMEmphasis, paper-coloured
 *  - Wrapped in a 44 dp tap target for Android accessibility minimum
 *  - On click: navigates to SettingsKey (wired in Plan 04 via onClick param)
 *
 * Content description: stringResource(R.string.auth_settings_title) provisionally;
 * Plan 04 rewires to R.string.home_avatar_content_desc.
 */
@Composable
fun AvatarButton(
    initials: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GiftMaisonTheme.colors
    val typography = GiftMaisonTheme.typography
    val desc = stringResource(R.string.home_avatar_content_desc)

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 44.dp, minHeight = 44.dp)
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = desc
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(colors.second),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initials,
                style = typography.bodyMEmphasis,
                color = colors.paper,
            )
        }
    }
}
