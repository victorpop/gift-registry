package com.giftregistry.ui.registry.create

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.giftregistry.R
import com.giftregistry.ui.theme.GiftMaisonTheme

/**
 * SCR-09 — visibility radio card (paperDeep bg, line border, 12 radius).
 *
 * Custom 18 dp ring + 8 dp inner dot — NOT Material3 RadioButton (ripple + tint
 * don't match the design token system; RESEARCH.md Anti-pattern, Pattern 8).
 */
@Composable
internal fun VisibilityRadioCard(
    selectedVisibility: String,
    onVisibilitySelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GiftMaisonTheme.colors
    val typography = GiftMaisonTheme.typography
    val shapes = GiftMaisonTheme.shapes
    val spacing = GiftMaisonTheme.spacing

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.edge)
            .clip(shapes.radius12)
            .border(BorderStroke(1.dp, colors.line), shapes.radius12)
            .background(colors.paperDeep)
            .padding(spacing.gap12),
        verticalArrangement = Arrangement.spacedBy(spacing.gap12),
    ) {
        Text(
            text = stringResource(R.string.registry_visibility_section_label),
            style = typography.monoCaps,
            color = colors.inkFaint,
        )
        VisibilityRow(
            label = stringResource(R.string.registry_visibility_public),
            isSelected = selectedVisibility == "public",
            onClick = { onVisibilitySelected("public") },
        )
        VisibilityRow(
            label = stringResource(R.string.registry_visibility_private),
            isSelected = selectedVisibility == "private",
            onClick = { onVisibilitySelected("private") },
        )
    }
}

@Composable
private fun VisibilityRow(label: String, isSelected: Boolean, onClick: () -> Unit) {
    val colors = GiftMaisonTheme.colors
    val typography = GiftMaisonTheme.typography
    val spacing = GiftMaisonTheme.spacing

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Custom radio indicator — 18 dp ring, 2 dp border; inner 8 dp accent dot when selected
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .border(
                    BorderStroke(2.dp, if (isSelected) colors.accent else colors.line),
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(colors.accent),
                )
            }
        }
        Spacer(Modifier.width(spacing.gap8))
        Text(text = label, style = typography.bodyMEmphasis, color = colors.ink)
    }
}
