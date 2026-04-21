package com.giftregistry.ui.registry.list

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.giftregistry.ui.theme.GiftMaisonTheme

/**
 * SCR-07: Custom pill-track segmented tabs for the Home screen.
 *
 * Material3's SegmentedButton is single-select button-group and lacks the
 * handoff's pill-on-paperDeep styling — this composable is bespoke.
 *
 * Handoff contract (§ 07 + UI-SPEC.md § Segmented Tabs):
 *   Track: shapes.pill, colors.paperDeep bg, spacing.gap8 vertical padding.
 *   Each tab: equal-width weight(1f), mono caps label.
 *   Selected: colors.paper bg pill, colors.ink label.
 *   Unselected: transparent bg, colors.inkFaint label.
 *   Transition: animateColorAsState 200 ms label colour only (no slide).
 *
 * Tab state is hoisted — caller owns `selectedIndex` + `onTabSelected` so the
 * composable stays stateless and the ViewModel / rememberSaveable owner decide
 * persistence semantics.
 */
@Composable
fun SegmentedTabs(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GiftMaisonTheme.colors
    val shapes = GiftMaisonTheme.shapes
    val spacing = GiftMaisonTheme.spacing
    val typography = GiftMaisonTheme.typography

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shapes.pill)
            .background(colors.paperDeep)
            .padding(vertical = spacing.gap8, horizontal = spacing.gap8),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            tabs.forEachIndexed { index, label ->
                val isSelected = index == selectedIndex
                val labelColor by animateColorAsState(
                    targetValue = if (isSelected) colors.ink else colors.inkFaint,
                    animationSpec = tween(durationMillis = 200),
                    label = "SegmentedTabs_labelColor_$index",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(shapes.pill)
                        .background(if (isSelected) colors.paper else Color.Transparent)
                        .clickable { onTabSelected(index) }
                        .padding(vertical = spacing.gap8),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = typography.monoCaps,
                        color = labelColor,
                    )
                }
            }
        }
    }
}
