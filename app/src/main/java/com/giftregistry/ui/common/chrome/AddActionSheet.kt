package com.giftregistry.ui.common.chrome

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.AddHome
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.giftregistry.R
import com.giftregistry.ui.theme.GiftMaisonTheme

/**
 * CHROME-03: Add-action bottom sheet — 22 dp top corners, drag handle,
 * scrim `ink.copy(alpha=0.55f)`, 2 action rows in fixed order:
 *   1. New registry (primary — accentSoft)
 *   2. Add an item (secondary — paperDeep)
 *
 * Trimmed from 4 rows to 2 rows by quick-260428-iny: the previous
 * "Item from URL", "Browse stores", and "Add manually" rows all routed to
 * the same AddItemScreen — they were consolidated behind a single "Add an
 * item" row that now opens AddItemScreen with a registry picker as the first
 * field. URL/Browse-stores/Manual are reachable from inside AddItemScreen
 * (the segmented tab strip) and Browse-stores is also reachable from the
 * bottom-nav STORES tab.
 *
 * Shape MUST be `RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)` directly —
 * NOT `shapes.radius22` (that rounds all four corners, per Pitfall 2).
 *
 * Drag-handle padding is symmetric (`vertical = 12.dp`) and there is NO
 * `bottomSheetShadow` modifier — both fixes shipped in earlier quick tasks
 * (260427-nkn / 260427-n67) MUST NOT regress.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddActionSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onNewRegistry: () -> Unit,
    onAddItem: () -> Unit,
) {
    if (!visible) return

    val colors = GiftMaisonTheme.colors
    val typography = GiftMaisonTheme.typography
    val spacing = GiftMaisonTheme.spacing
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        // Asymmetric top-only rounding — Pitfall 2: NOT shapes.radius22 (all-4-corners).
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        containerColor = colors.paper,
        scrimColor = colors.ink.copy(alpha = 0.55f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .background(colors.line, GiftMaisonTheme.shapes.pill),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = spacing.edge,
                    end = spacing.edge,
                    top = spacing.gap14,
                    bottom = spacing.edgeWide,
                ),
            verticalArrangement = Arrangement.spacedBy(spacing.gap10),
        ) {
            Text(
                text = stringResource(R.string.add_sheet_title),
                style = typography.displayM.copy(fontStyle = FontStyle.Italic),
                color = colors.ink,
                modifier = Modifier.padding(bottom = spacing.gap10),
            )
            ActionRow(
                icon = Icons.Outlined.AddHome,
                headingRes = R.string.add_sheet_new_registry,
                subtitleRes = R.string.add_sheet_new_registry_sub,
                isPrimary = true,
                onClick = onNewRegistry,
            )
            ActionRow(
                icon = Icons.Outlined.AddCircleOutline,
                headingRes = R.string.add_sheet_add_item,
                subtitleRes = R.string.add_sheet_add_item_sub,
                isPrimary = false,
                onClick = onAddItem,
            )
        }
    }
}

/**
 * Single action row — 36 dp icon square + heading + subtitle + chevron.
 * Primary row (New registry): accentSoft background + accent glyph.
 * Secondary rows: paperDeep background + accent glyph.
 */
@Composable
private fun ActionRow(
    icon: ImageVector,
    headingRes: Int,
    subtitleRes: Int,
    isPrimary: Boolean,
    onClick: () -> Unit,
) {
    val colors = GiftMaisonTheme.colors
    val typography = GiftMaisonTheme.typography
    val shapes = GiftMaisonTheme.shapes
    val rowBg: Color = if (isPrimary) colors.accentSoft else colors.paperDeep

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBg, shapes.radius14)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp)
            .defaultMinSize(minHeight = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(rowBg, shapes.radius10),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(headingRes),
                style = typography.bodyMEmphasis,
                color = colors.ink,
            )
            Text(
                text = stringResource(subtitleRes),
                style = typography.bodyXS,
                color = colors.inkSoft,
            )
        }
        Icon(
            imageVector = Icons.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.inkFaint,
            modifier = Modifier.size(18.dp),
        )
    }
}
