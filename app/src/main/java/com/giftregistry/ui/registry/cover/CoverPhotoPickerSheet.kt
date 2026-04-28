package com.giftregistry.ui.registry.cover

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Phase 12 D-10 + D-11 — Material3 ModalBottomSheet with the cover-photo
 * picker UI: 3×2 preset grid + full-width "Pick from gallery" pill +
 * conditional "Remove cover photo" text button.
 *
 * Body layout (top → bottom):
 *  1. Header text (mono-caps, inkFaint) — caller provides via [headerText].
 *  2. 3×2 [LazyVerticalGrid] of [PresetThumbnail]s for the currently
 *     selected occasion (`PresetCatalog.presetsFor(occasion)`). Grid is
 *     reactive: switching occasion in the parent form causes the grid to
 *     re-emit via `remember(occasion) { … }`. 6 items, no scroll.
 *  3. Full-width "Pick from gallery" [Button] (ink fill, paper text,
 *     pill shape) launching the AndroidX Photo Picker via
 *     `ActivityResultContracts.PickVisualMedia` with `ImageOnly`.
 *  4. Conditional [TextButton] "Remove cover photo" — rendered ONLY when
 *     [currentSelection] is not [CoverPhotoSelection.None].
 *
 * The sheet is intentionally string-resource-agnostic: the caller
 * (Plan 04) passes [headerText], [pickFromGalleryText], [removeText] via
 * `stringResource(...)` so this file stays plain-Compose.
 *
 * RESEARCH.md references:
 * - Pattern 1: Compose Photo Picker (gated, single image)
 * - Pattern 7: ModalBottomSheet (skipPartiallyExpanded = true; wrap dismiss
 *   in `scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }`)
 *
 * @param occasion non-null canonical occasion key — caller (Plan 04) only
 *                 opens the sheet when [isCoverPickerEnabled] returns true.
 * @param currentSelection the active selection so the matching preset
 *                         renders with the accent border (D-11) and the
 *                         "Remove" button shows when something is picked.
 * @param onSelectionChanged invoked with the new [CoverPhotoSelection] when
 *                           the user taps a preset, picks from the gallery,
 *                           or hits remove. Caller (Plan 04) updates the
 *                           ViewModel.
 * @param onDismiss called when the sheet finishes hiding (after a
 *                  selection or scrim tap). Caller hides the sheet from
 *                  composition.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoverPhotoPickerSheet(
    occasion: String,
    currentSelection: CoverPhotoSelection,
    onSelectionChanged: (CoverPhotoSelection) -> Unit,
    onDismiss: () -> Unit,
    headerText: String,
    pickFromGalleryText: String,
    removeText: String,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val colors = com.giftregistry.ui.theme.GiftMaisonTheme.colors
    val typography = com.giftregistry.ui.theme.GiftMaisonTheme.typography
    val shapes = com.giftregistry.ui.theme.GiftMaisonTheme.shapes
    val spacing = com.giftregistry.ui.theme.GiftMaisonTheme.spacing

    // D-11: presets reactive to occasion change (parent ViewModel will already
    // have reset selection -> None when occasion flips, but the grid keying
    // here belt-and-suspenders the contract).
    val presets = remember(occasion) { PresetCatalog.presetsFor(occasion) }

    // Pattern 1 — AndroidX Photo Picker, gallery branch.
    val galleryLauncher = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) {
            onSelectionChanged(CoverPhotoSelection.Gallery(uri))
            scope.launch { sheetState.hide() }.invokeOnCompletion {
                if (!sheetState.isVisible) onDismiss()
            }
        }
        // null = user dismissed the picker; keep current selection, sheet stays open.
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.paper,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.edgeWide)
                .padding(bottom = spacing.gap16),
            verticalArrangement = Arrangement.spacedBy(spacing.gap16),
        ) {
            // 1) Header (mono-caps, inkFaint)
            Text(
                text = headerText,
                style = typography.monoCaps,
                color = colors.inkFaint,
                modifier = Modifier.padding(top = spacing.gap8),
            )

            // 2) 3x2 preset grid — 6 thumbnails, no scroll (D-10).
            // Grid height is computed: 2 rows of 16:9 tiles within the sheet
            // width minus horizontal padding; LazyVerticalGrid sizes itself
            // when given a finite height. We use heightIn via a fixed cap
            // (sheet pads its own width); the grid fills its parent width and
            // each tile uses aspectRatio internally.
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                userScrollEnabled = false,
                horizontalArrangement = Arrangement.spacedBy(spacing.gap8),
                verticalArrangement = Arrangement.spacedBy(spacing.gap8),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
            ) {
                itemsIndexed(presets) { index, drawableId ->
                    val isSelected = currentSelection is CoverPhotoSelection.Preset &&
                        currentSelection.occasion == occasion &&
                        currentSelection.index == index + 1
                    PresetThumbnail(
                        drawableId = drawableId,
                        selected = isSelected,
                        onClick = {
                            onSelectionChanged(
                                CoverPhotoSelection.Preset(occasion = occasion, index = index + 1)
                            )
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                if (!sheetState.isVisible) onDismiss()
                            }
                        },
                    )
                }
            }

            // 3) Full-width gallery pill — ink fill, paper text.
            Button(
                onClick = {
                    galleryLauncher.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
                },
                shape = shapes.pill,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.ink,
                    contentColor = colors.paper,
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = spacing.gap20,
                    vertical = spacing.gap12,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = pickFromGalleryText, style = typography.bodyMEmphasis)
            }

            // 4) Remove cover photo — only when something is selected.
            if (currentSelection !is CoverPhotoSelection.None) {
                TextButton(
                    onClick = {
                        onSelectionChanged(CoverPhotoSelection.None)
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) onDismiss()
                        }
                    },
                    shape = RoundedCornerShape(0.dp),
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(
                        text = removeText,
                        style = typography.bodyM,
                        color = colors.inkSoft,
                    )
                }
            }

            Spacer(Modifier.height(spacing.gap8))
        }
    }
}
