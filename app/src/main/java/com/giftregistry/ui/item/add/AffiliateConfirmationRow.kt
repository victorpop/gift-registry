package com.giftregistry.ui.item.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.giftregistry.R
import com.giftregistry.ui.theme.GiftMaisonTheme

/**
 * SCR-10: Affiliate confirmation row.
 *
 * Shown only when `shouldShowAffiliateRow(url, isAffiliateDomain, ogFetchSucceeded)`
 * returns true — gating is the screen's responsibility; this composable just
 * renders the row.
 *
 * Layout: 1 dp line divider above · Row(SpaceBetween) with left "✓ Affiliate tag
 * applied invisibly" (mono caps, ok green) and right "Clear" ghost TextButton.
 */
@Composable
internal fun AffiliateConfirmationRow(
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GiftMaisonTheme.colors
    val typography = GiftMaisonTheme.typography
    val spacing = GiftMaisonTheme.spacing
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(thickness = 1.dp, color = colors.line)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = spacing.gap8),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "✓ " + stringResource(R.string.add_item_affiliate_confirmed),
                style = typography.monoCaps,
                color = colors.ok,
            )
            TextButton(onClick = onClear) {
                Text(
                    text = stringResource(R.string.add_item_affiliate_clear),
                    style = typography.bodyS,
                    color = colors.inkFaint,
                )
            }
        }
    }
}
