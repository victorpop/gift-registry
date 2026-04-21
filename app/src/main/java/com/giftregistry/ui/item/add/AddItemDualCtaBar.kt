package com.giftregistry.ui.item.add

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.giftregistry.R
import com.giftregistry.ui.theme.GiftMaisonTheme

/**
 * SCR-10: Dual CTA bar pinned via Scaffold.bottomBar.
 *
 *   Ghost "Add another" (flex 1f)  — save + reset form, stay on screen.
 *   Primary "Save to registry ✓" (flex 1.5f) — save + pop to RegistryDetail.
 *
 * Both disabled while isFetching or isSaving; loading indicator on the primary
 * button during save.
 */
@Composable
internal fun AddItemDualCtaBar(
    isSaving: Boolean,
    isFetching: Boolean,
    onAddAnother: () -> Unit,
    onSaveAndExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GiftMaisonTheme.colors
    val typography = GiftMaisonTheme.typography
    val shapes = GiftMaisonTheme.shapes
    val spacing = GiftMaisonTheme.spacing
    val disabled = isSaving || isFetching
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(thickness = 1.dp, color = colors.line)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = spacing.gap12, horizontal = spacing.gap20),
            horizontalArrangement = Arrangement.spacedBy(spacing.gap12),
        ) {
            OutlinedButton(
                onClick = onAddAnother,
                enabled = !disabled,
                modifier = Modifier.weight(1f),
                shape = shapes.pill,
                border = BorderStroke(1.dp, colors.line),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.ink),
            ) {
                Text(
                    text = stringResource(R.string.add_item_cta_add_another),
                    style = typography.bodyMEmphasis,
                )
            }
            Button(
                onClick = onSaveAndExit,
                enabled = !disabled,
                modifier = Modifier.weight(1.5f),
                shape = shapes.pill,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.ink,
                    contentColor = colors.paper,
                ),
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = colors.paper,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.add_item_cta_save),
                        style = typography.bodyMEmphasis,
                    )
                }
            }
        }
    }
}
