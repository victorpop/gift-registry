package com.giftregistry.ui.registry.list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.giftregistry.R
import com.giftregistry.domain.model.Registry
import com.giftregistry.ui.theme.GiftMaisonTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * SCR-07: Primary registry card — dark-ink surface, paper text, image darkened
 * to 70% brightness via Coil 3 ColorFilter.colorMatrix (RESEARCH.md Pattern 4).
 * Exactly one card per list is primary (isPrimary = maxByOrNull updatedAt).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RegistryCardPrimary(
    registry: Registry,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GiftMaisonTheme.colors
    val shapes = GiftMaisonTheme.shapes
    val spacing = GiftMaisonTheme.spacing
    val typography = GiftMaisonTheme.typography

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shapes.radius16)
            .background(colors.ink)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
            AsyncImage(
                model = registry.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                colorFilter = ColorFilter.colorMatrix(
                    ColorMatrix().apply { setToScale(0.7f, 0.7f, 0.7f, 1f) }
                ),
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
            )
            OccasionPill(
                label = registry.occasion,
                background = colors.accent,
                textColor = colors.paper,
                modifier = Modifier.align(Alignment.TopStart).padding(spacing.gap8),
            )
            if (registry.eventDateMs != null) {
                Text(
                    text = formatDate(registry.eventDateMs),
                    style = typography.bodyXS,
                    color = colors.paper,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(spacing.gap8),
                )
            }
        }
        Column(modifier = Modifier.padding(spacing.gap16)) {
            Text(
                text = registry.title,
                style = typography.displayS,
                color = colors.paper,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(spacing.gap4))
            Text(
                text = statsLine(),
                style = typography.monoCaps,
                color = colors.paper.copy(alpha = 0.6f),
            )
        }
    }
}

/**
 * SCR-07: Secondary registry card — paperDeep surface, line border, full-brightness image.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RegistryCardSecondary(
    registry: Registry,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GiftMaisonTheme.colors
    val shapes = GiftMaisonTheme.shapes
    val spacing = GiftMaisonTheme.spacing
    val typography = GiftMaisonTheme.typography

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shapes.radius16)
            .background(colors.paperDeep)
            .border(1.dp, colors.line, shapes.radius16)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
            AsyncImage(
                model = registry.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
            )
            OccasionPill(
                label = registry.occasion,
                background = colors.paperDeep,
                textColor = colors.inkSoft,
                modifier = Modifier.align(Alignment.TopStart).padding(spacing.gap8),
            )
            if (registry.eventDateMs != null) {
                Text(
                    text = formatDate(registry.eventDateMs),
                    style = typography.bodyXS,
                    color = colors.inkSoft,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(spacing.gap8),
                )
            }
        }
        Column(modifier = Modifier.padding(spacing.gap16)) {
            Text(
                text = registry.title,
                style = typography.displayS,
                color = colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(spacing.gap4))
            Text(
                text = statsLine(),
                style = typography.monoCaps,
                color = colors.inkFaint,
            )
        }
    }
}

/** Internal occasion pill — top-left image overlay. */
@Composable
private fun OccasionPill(
    label: String,
    background: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    val shapes = GiftMaisonTheme.shapes
    val spacing = GiftMaisonTheme.spacing
    val typography = GiftMaisonTheme.typography
    Box(
        modifier = modifier
            .clip(shapes.pill)
            .background(background)
            .padding(horizontal = spacing.gap8, vertical = spacing.gap4),
    ) {
        Text(
            text = label.uppercase(Locale.getDefault()),
            style = typography.bodyXS,
            color = textColor,
        )
    }
}

/**
 * Stats line renders as `N items \u2022 M reserved \u2022 K given`. Per CONTEXT.md,
 * per-registry stat aggregation is deferred — Phase 10 renders zeros.
 * Follow-up: Firestore doc-level counts or per-card Flow observation.
 */
@Composable
private fun statsLine(): String {
    val items = stringResource(R.string.home_stats_items, 0)
    val reserved = stringResource(R.string.home_stats_reserved, 0)
    val given = stringResource(R.string.home_stats_given, 0)
    return "$items \u2022 $reserved \u2022 $given"
}

private fun formatDate(timestampMs: Long): String {
    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return sdf.format(Date(timestampMs))
}
