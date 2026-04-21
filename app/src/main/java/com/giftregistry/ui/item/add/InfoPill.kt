package com.giftregistry.ui.item.add

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import com.giftregistry.R
import com.giftregistry.ui.theme.GiftMaisonTheme

/**
 * SCR-10: secondSoft info pill with ℹ glyph + localized affiliate info copy.
 *
 * Copy uses `%1$s` domain placeholder for getString(id, domain) substitution.
 */
@Composable
internal fun InfoPill(
    domain: String,
    modifier: Modifier = Modifier,
) {
    val colors = GiftMaisonTheme.colors
    val typography = GiftMaisonTheme.typography
    val shapes = GiftMaisonTheme.shapes
    val spacing = GiftMaisonTheme.spacing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shapes.radius10)
            .background(colors.secondSoft)
            .padding(spacing.gap12),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = "\u2139",
            style = typography.bodyMEmphasis,
            color = colors.second,
        )
        Spacer(Modifier.width(spacing.gap8))
        Text(
            text = stringResource(R.string.add_item_affiliate_info, domain),
            style = typography.bodyS,
            color = colors.second,
        )
    }
}
