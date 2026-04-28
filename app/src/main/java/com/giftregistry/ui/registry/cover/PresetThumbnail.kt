package com.giftregistry.ui.registry.cover

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.giftregistry.ui.theme.GiftMaisonTheme

/**
 * Phase 12 D-10 — 16:9 selectable preset tile, used inside the
 * [CoverPhotoPickerSheet]'s 3×2 LazyVerticalGrid.
 *
 * Selection styling: 1.5 dp accent border when [selected], 1 dp line border
 * otherwise (mirrors the focused-input + active-tile pattern from Phase 10/11).
 *
 * Coil 3 supports `Int` drawable IDs natively (RESEARCH.md Pattern 4) — no
 * `android.resource://` URI mapping needed.
 *
 * Internal: only the Phase 12 cover package consumes this — [CoverPhotoPickerSheet].
 */
@Composable
internal fun PresetThumbnail(
    @DrawableRes drawableId: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GiftMaisonTheme.colors
    val shapes = GiftMaisonTheme.shapes
    val borderColor = if (selected) colors.accent else colors.line
    val borderWidth = if (selected) 1.5.dp else 1.dp
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(shapes.radius14)
            .background(colors.paperDeep)
            .border(borderWidth, borderColor, shapes.radius14)
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = drawableId,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
