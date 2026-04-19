package com.giftregistry.ui.registry.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.giftregistry.R

/**
 * Phase 6 (UI-SPEC Contract 1): sticky banner shown on RegistryDetailScreen when the
 * current user has an active reservation, prompting them to confirm the purchase
 * (which triggers NOTF-01/NOTF-02 via the onDocumentUpdated trigger from Plan 06-02).
 *
 * - Card: containerColor = surfaceVariant, elevation = 2.dp
 * - Min height: 64dp
 * - Left icon: ShoppingCart (24dp), tint = primary (#6750A4 seed — DO NOT hardcode)
 * - Heading: labelLarge style
 * - CTA Button: filled primary, full width, height 48dp; loading state shows 20dp progress indicator
 * - No dismiss button — banner auto-unmounts when reservation transitions away from "active"
 */
@Composable
fun ConfirmPurchaseBanner(
    isConfirming: Boolean,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = stringResource(R.string.reservation_confirm_purchase_heading),
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            Button(
                onClick = onConfirm,
                enabled = !isConfirming,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                if (isConfirming) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.reservation_confirm_purchase_loading))
                } else {
                    Text(stringResource(R.string.reservation_confirm_purchase_cta))
                }
            }
        }
    }
}

@Preview
@Composable
private fun ConfirmPurchaseBannerPreview_Idle() {
    ConfirmPurchaseBanner(isConfirming = false, onConfirm = {})
}

@Preview
@Composable
private fun ConfirmPurchaseBannerPreview_Loading() {
    ConfirmPurchaseBanner(isConfirming = true, onConfirm = {})
}
