package com.giftregistry.ui.registry.detail

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.giftregistry.R
import com.giftregistry.domain.model.Item
import com.giftregistry.ui.theme.GiftMaisonTheme

/**
 * SCR-08 — horizontally scrolling LazyRow of 4 filter chips (All / Open /
 * Reserved / Completed). Active = colors.ink bg + colors.paper text. Inactive
 * = colors.line border + colors.inkFaint text. Counts inline (e.g. "All 12").
 */
@Composable
internal fun FilterChipsRow(
    items: List<Item>,
    activeFilter: FilterChipState,
    onFilterSelected: (FilterChipState) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GiftMaisonTheme.spacing
    val counts = remember(items) {
        FilterChipState.entries.associateWith { f -> items.count { f.matches(it.status) } }
    }
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = spacing.edge, vertical = spacing.gap8),
        horizontalArrangement = Arrangement.spacedBy(spacing.gap10),
    ) {
        items(FilterChipState.entries.size) { idx ->
            val chip = FilterChipState.entries[idx]
            FilterChip(
                label = stringResource(labelResId(chip)),
                count = counts[chip] ?: 0,
                isActive = chip == activeFilter,
                onClick = { onFilterSelected(chip) },
            )
        }
    }
}

private fun labelResId(chip: FilterChipState): Int = when (chip) {
    FilterChipState.All       -> R.string.registry_filter_all
    FilterChipState.Open      -> R.string.registry_filter_open
    FilterChipState.Reserved  -> R.string.registry_filter_reserved
    FilterChipState.Completed -> R.string.registry_filter_completed
}

@Composable
private fun FilterChip(label: String, count: Int, isActive: Boolean, onClick: () -> Unit) {
    val colors = GiftMaisonTheme.colors
    val typography = GiftMaisonTheme.typography
    val shapes = GiftMaisonTheme.shapes
    val spacing = GiftMaisonTheme.spacing

    val bg by animateColorAsState(
        targetValue = if (isActive) colors.ink else Color.Transparent,
        animationSpec = tween(durationMillis = 150),
        label = "FilterChip_bg",
    )
    val textColor by animateColorAsState(
        targetValue = if (isActive) colors.paper else colors.inkFaint,
        animationSpec = tween(durationMillis = 150),
        label = "FilterChip_text",
    )

    Box(
        modifier = Modifier
            .clip(shapes.pill)
            .background(bg)
            .then(
                if (!isActive) Modifier.border(BorderStroke(1.dp, colors.line), shapes.pill)
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = spacing.gap16, vertical = spacing.gap8),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "$label $count",
            style = typography.monoCaps,
            color = textColor,
        )
    }
}
