package com.giftregistry.ui.item.add

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import com.giftregistry.R
import com.giftregistry.ui.theme.GiftMaisonTheme

/**
 * SCR-10: "✓ auto-filled" pill tag displayed next to the Title field label when
 * OG metadata populated the field.
 */
@Composable
internal fun AutoFillTag(modifier: Modifier = Modifier) {
    val colors = GiftMaisonTheme.colors
    val typography = GiftMaisonTheme.typography
    val shapes = GiftMaisonTheme.shapes
    val spacing = GiftMaisonTheme.spacing
    Box(
        modifier = modifier
            .clip(shapes.pill)
            .background(colors.ok.copy(alpha = 0.12f))
            .padding(horizontal = spacing.gap8, vertical = spacing.gap4),
    ) {
        Text(
            text = "✓ " + stringResource(R.string.add_item_autofilled),
            style = typography.bodyXS,
            color = colors.ok,
        )
    }
}
