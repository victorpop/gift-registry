package com.giftregistry.ui.registry.create

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.giftregistry.R
import com.giftregistry.ui.theme.GiftMaisonTheme

/**
 * SCR-09 — 2×3 occasion tile grid. Consumes OccasionCatalog.entries (fixed order):
 * Housewarming / Wedding / Baby / Birthday / Christmas / Custom.
 *
 * Each tile:
 *   - Shape: radius14
 *   - Selected:   bg=accent, content=accentInk, border = 1.5 dp accent
 *   - Unselected: bg=paperDeep, content=ink, border = 1.5 dp colors.line
 *   - Glyph: Instrument Serif italic 22 sp (displayM)
 *   - Label: body 13 W500 (bodyMEmphasis)
 *
 * Selection comparison handles legacy Firestore values via OccasionCatalog.storageKeyFor().
 * Animation: animateColorAsState 150 ms on bg + border + content colours.
 */
@Composable
internal fun OccasionTileGrid(
    selectedOccasion: String,
    onOccasionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GiftMaisonTheme.spacing
    val entries = OccasionCatalog.entries
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.edge),
        verticalArrangement = Arrangement.spacedBy(spacing.gap10),
    ) {
        for (rowIdx in 0 until 3) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.gap6),
            ) {
                for (colIdx in 0 until 2) {
                    val entry = entries[rowIdx * 2 + colIdx]
                    // Handle legacy aliases: "Baby shower" -> Baby, "Anniversary" -> Housewarming
                    val canonicalSelected = OccasionCatalog.storageKeyFor(selectedOccasion)
                    val isSelected = selectedOccasion.equals(entry.storageKey, ignoreCase = true) ||
                        canonicalSelected == entry.storageKey
                    OccasionTile(
                        glyph = entry.glyph,
                        labelResId = labelKeyFor(entry.storageKey),
                        isSelected = isSelected,
                        onClick = { onOccasionSelected(entry.storageKey) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

private fun labelKeyFor(storageKey: String): Int = when (storageKey) {
    "Housewarming" -> R.string.registry_occasion_housewarming
    "Wedding"      -> R.string.registry_occasion_wedding
    "Baby"         -> R.string.registry_occasion_baby
    "Birthday"     -> R.string.registry_occasion_birthday
    "Christmas"    -> R.string.registry_occasion_christmas
    else           -> R.string.registry_occasion_custom
}

@Composable
private fun OccasionTile(
    glyph: String,
    labelResId: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GiftMaisonTheme.colors
    val typography = GiftMaisonTheme.typography
    val shapes = GiftMaisonTheme.shapes
    val spacing = GiftMaisonTheme.spacing

    val bg by animateColorAsState(
        targetValue = if (isSelected) colors.accent else colors.paperDeep,
        animationSpec = tween(durationMillis = 150),
        label = "OccasionTile_bg",
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) colors.accent else colors.line,
        animationSpec = tween(durationMillis = 150),
        label = "OccasionTile_border",
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) colors.accentInk else colors.ink,
        animationSpec = tween(durationMillis = 150),
        label = "OccasionTile_content",
    )
    val glyphColor by animateColorAsState(
        targetValue = if (isSelected) colors.accentInk else colors.accent,
        animationSpec = tween(durationMillis = 150),
        label = "OccasionTile_glyph",
    )

    Box(
        modifier = modifier
            .clip(shapes.radius14)
            .background(bg)
            .border(BorderStroke(1.5.dp, borderColor), shapes.radius14)
            .clickable(onClick = onClick)
            .padding(vertical = spacing.gap14, horizontal = spacing.gap16),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.gap6),
        ) {
            Text(
                text = glyph,
                style = typography.displayM.copy(fontStyle = FontStyle.Italic),
                color = glyphColor,
            )
            Text(
                text = stringResource(labelResId),
                style = typography.bodyMEmphasis,
                color = contentColor,
            )
        }
    }
}
