package com.giftregistry.ui.registry.cover

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.giftregistry.ui.theme.GiftMaisonTheme

/**
 * Phase 12 D-09 + D-12 — 16:9 inline cover-photo preview block placed at
 * the top of CreateRegistryScreen above the existing OccasionTileGrid.
 *
 * Three states (driven by [occasion] + [selection]):
 *
 * 1. **Disabled** — [isCoverPickerEnabled] returns false (occasion is
 *    null / blank / whitespace). Renders the gradient placeholder via
 *    [HeroImageOrPlaceholder] + a translucent ink overlay + the
 *    [disabledHint] caption (e.g. "Pick an occasion to see suggested
 *    covers"). Tap is suppressed (`enabled = false`).
 * 2. **Enabled + None** — occasion is set, nothing picked yet. Renders
 *    the gradient placeholder + tap opens the [CoverPhotoPickerSheet].
 * 3. **Enabled + Preset / Gallery** — occasion is set and the user picked
 *    a preset or a gallery image. Renders the selection through
 *    [HeroImageOrPlaceholder] (preset sentinel decodes to drawable Int via
 *    [PresetCatalog.resolve]; gallery URI strings flow through Coil 3
 *    natively) + tap re-opens the sheet for change.
 *
 * The composable is intentionally string-resource-agnostic: the caller
 * (Plan 04) supplies [disabledHint] via `stringResource(R.string.cover_photo_pick_occasion_first)`.
 *
 * @param occasion the canonical [com.giftregistry.ui.registry.create.OccasionCatalog]
 *                 storage key the user has picked in the form. Drives the
 *                 D-12 enabled gate and the placeholder glyph.
 * @param selection current cover-photo selection from the ViewModel
 *                  (`CoverPhotoSelection.None` initially).
 * @param onTap invoked when the user taps the preview to open the sheet.
 *              Caller (Plan 04) is responsible for showing the
 *              [CoverPhotoPickerSheet] in response.
 * @param disabledHint caption rendered over the placeholder when no
 *                     occasion is selected. String resource lookup happens
 *                     at the call site.
 */
@Composable
fun CoverPhotoPickerInline(
    occasion: String?,
    selection: CoverPhotoSelection,
    onTap: () -> Unit,
    disabledHint: String,
    modifier: Modifier = Modifier,
) {
    val colors = GiftMaisonTheme.colors
    val typography = GiftMaisonTheme.typography
    val shapes = GiftMaisonTheme.shapes
    val spacing = GiftMaisonTheme.spacing
    val enabled = isCoverPickerEnabled(occasion)

    // Resolve the imageUrl-equivalent for the current selection. Preset values
    // become the canonical sentinel string (PresetCatalog.encode); gallery
    // selections become the raw content:// URI string Coil 3 handles natively.
    val imageUrl: String? = when (selection) {
        CoverPhotoSelection.None -> null
        is CoverPhotoSelection.Preset -> PresetCatalog.encode(selection.occasion, selection.index)
        is CoverPhotoSelection.Gallery -> selection.uri.toString()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(shapes.radius14)
            .border(1.dp, colors.line, shapes.radius14)
            .clickable(enabled = enabled, onClick = onTap),
    ) {
        HeroImageOrPlaceholder(
            imageUrl = imageUrl,
            occasion = occasion,
            glyphSize = 32.sp,
            modifier = Modifier.fillMaxSize(),
        )
        if (!enabled) {
            // Disabled-state caption — translucent ink scrim over gradient
            // placeholder, with the caller-provided hint centered.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.ink.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = disabledHint,
                    style = typography.bodyM,
                    color = colors.paper,
                    modifier = Modifier.padding(spacing.gap16),
                )
            }
        }
    }
}
