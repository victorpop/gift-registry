package com.giftregistry.ui.registry.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.giftregistry.R
import com.giftregistry.domain.model.Item
import com.giftregistry.ui.theme.GiftMaisonTheme

/**
 * SCR-08 — 4-stat strip (items / reserved / given / views) with 1 dp vertical
 * dividers between slots. All stats derived client-side from the items list via
 * `registryStatsOf`. `views` renders as "0" per CONTEXT.md § Stats strip
 * (Registry.viewCount deferred to v1.2).
 */
@Composable
internal fun StatsStrip(
    items: List<Item>,
    modifier: Modifier = Modifier,
) {
    val colors = GiftMaisonTheme.colors
    val spacing = GiftMaisonTheme.spacing

    val stats = registryStatsOf(items)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.edge, vertical = spacing.gap12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatSlot(
            value = stats.items.toString(),
            label = stringResource(R.string.registry_stat_items),
            modifier = Modifier.weight(1f),
        )
        VerticalDivider(thickness = 1.dp, color = colors.line, modifier = Modifier.height(40.dp))
        StatSlot(
            value = stats.reserved.toString(),
            label = stringResource(R.string.registry_stat_reserved),
            modifier = Modifier.weight(1f),
        )
        VerticalDivider(thickness = 1.dp, color = colors.line, modifier = Modifier.height(40.dp))
        StatSlot(
            value = stats.given.toString(),
            label = stringResource(R.string.registry_stat_given),
            modifier = Modifier.weight(1f),
        )
        VerticalDivider(thickness = 1.dp, color = colors.line, modifier = Modifier.height(40.dp))
        // TODO v1.2: replace stats.views with Registry.viewCount when the field ships
        StatSlot(
            value = stats.views.toString(),
            label = stringResource(R.string.registry_stat_views),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatSlot(value: String, label: String, modifier: Modifier = Modifier) {
    val colors = GiftMaisonTheme.colors
    val typography = GiftMaisonTheme.typography
    val spacing = GiftMaisonTheme.spacing
    Column(
        modifier = modifier.padding(horizontal = spacing.gap8),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.gap4),
    ) {
        Text(text = value, style = typography.displayM, color = colors.ink)
        Text(text = label, style = typography.monoCaps, color = colors.inkFaint)
    }
}
