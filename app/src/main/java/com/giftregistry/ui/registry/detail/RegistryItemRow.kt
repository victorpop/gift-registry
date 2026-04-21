package com.giftregistry.ui.registry.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.giftregistry.R
import com.giftregistry.domain.model.Item
import com.giftregistry.domain.model.ItemStatus
import com.giftregistry.ui.common.status.StatusChip
import com.giftregistry.ui.theme.GiftMaisonTheme
import java.net.URI

/**
 * SCR-08 — full-width item row: 58×58 10-radius thumbnail + content column
 * (title / price / optional reserver-or-giver sub-line) + right column
 * (StatusChip stacked above always-visible 26×26 ⋯ overflow button). 1 dp
 * colors.line bottom border except for last row.
 *
 * Long-press entry point for the dropdown REMOVED per CONTEXT.md D-06 — the
 * always-visible ⋯ button is the single entry point.
 */
@Composable
internal fun RegistryItemRow(
    item: Item,
    isLast: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GiftMaisonTheme.colors
    val typography = GiftMaisonTheme.typography
    val shapes = GiftMaisonTheme.shapes
    val spacing = GiftMaisonTheme.spacing

    var menuExpanded by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val strokePx = remember(density) { with(density) { 1.dp.toPx() } }
    val lineColor = colors.line

    Row(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                if (!isLast) {
                    drawLine(
                        color = lineColor,
                        start = Offset(0f, size.height - strokePx / 2f),
                        end = Offset(size.width, size.height - strokePx / 2f),
                        strokeWidth = strokePx,
                    )
                }
            }
            .padding(horizontal = spacing.edge, vertical = spacing.gap12),
        verticalAlignment = Alignment.Top,
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(shapes.radius10)
                .background(colors.paperDeep),
        ) {
            val imageUrl = item.imageUrl
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = stringResource(R.string.item_image_content_desc),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }

        Spacer(Modifier.width(spacing.gap8))

        // Content column
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = typography.bodyMEmphasis,
                color = colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (item.status == ItemStatus.PURCHASED) TextDecoration.LineThrough else null,
            )
            Spacer(Modifier.height(spacing.gap4))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val priceText = item.price?.ifBlank { null } ?: "\u2014"
                Text(text = priceText, style = typography.bodyM, color = colors.inkSoft)
                Text(text = " \u00b7 ", style = typography.bodyM, color = colors.inkFaint)
                val retailer = runCatching {
                    URI(item.originalUrl).host?.removePrefix("www.") ?: "\u2014"
                }.getOrElse { "\u2014" }
                Text(text = retailer, style = typography.monoCaps, color = colors.inkFaint)
            }
            // Sub-line: reserver/giver name not in Item domain model (v1.2 TODO)
        }

        Spacer(Modifier.width(spacing.gap8))

        // Right column: StatusChip + always-visible overflow button
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            StatusChip(status = item.status, expiresAt = item.expiresAt)
            Spacer(Modifier.height(spacing.gap8))
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(44.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(shapes.pill)
                            .border(BorderStroke(1.dp, colors.line), shapes.pill),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.registry_detail_item_overflow_desc),
                            tint = colors.inkSoft,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.item_edit_title)) },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null)
                        },
                        onClick = {
                            menuExpanded = false
                            onEdit()
                        },
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(R.string.common_delete),
                                color = MaterialTheme.colorScheme.error,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}
