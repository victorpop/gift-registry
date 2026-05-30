package com.giftregistry.ui.discover

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.giftregistry.R
import com.giftregistry.domain.discover.DiscoverProduct
import com.giftregistry.ui.theme.GiftMaisonTheme
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

/**
 * Phase 17 — single Discover product card.
 *
 * Visual contract (UI-SPEC "DiscoverProductCard"):
 * - 16:9 AsyncImage at top (Coil placeholder + error → discover_card_placeholder)
 * - Title (bodyL, max 2 lines, ellipsis)
 * - Optional description (bodyM, inkFaint, max 2 lines)
 * - Optional price (bodyMEmphasis) formatted via NumberFormat for Locale("ro","RO")
 * - Retailer-name row with small add-to-registry button right-aligned (quick-260530-nx5)
 *
 * Click behaviour (D-32, D-33):
 * - Card tap → Intent.ACTION_VIEW with the RAW retailerUrl (NO affiliate transform —
 *   that's deliberately deferred to the future "Save to registry" flow per D-32).
 * - ActivityNotFoundException → Snackbar fallback via the parent SnackbarHostState
 *   so the user is not silently dropped on devices without a browser handler.
 * - Small + button → onAddToRegistry callback (gesture consumed; does NOT propagate
 *   to the card's onClick).
 */
@Composable
fun DiscoverProductCard(
    product: DiscoverProduct,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    // quick-260530-nx5: default no-op so StyleGuidePreview call sites keep
    // compiling unchanged (they only pass product + snackbarHostState).
    onAddToRegistry: (DiscoverProduct) -> Unit = {},
) {
    val colors = GiftMaisonTheme.colors
    val typography = GiftMaisonTheme.typography
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val noBrowserMessage = stringResource(R.string.discover_no_browser_toast)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.paperDeep),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = {
            // D-32: raw retailer URL, NO affiliate transform.
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(product.retailerUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                // D-33: Snackbar fallback when the device has no browser/handler.
                scope.launch { snackbarHostState.showSnackbar(noBrowserMessage) }
            }
        },
    ) {
        Column {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(product.imageUrl)
                    .build(),
                contentDescription = null,
                placeholder = painterResource(R.drawable.discover_card_placeholder),
                error = painterResource(R.drawable.discover_card_placeholder),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
            )
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = product.title,
                    style = typography.bodyL,
                    color = colors.ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (product.description.isNotBlank()) {
                    Text(
                        text = product.description,
                        style = typography.bodyM,
                        color = colors.inkFaint,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (product.price > 0.0) {
                    val formatted = NumberFormat.getCurrencyInstance(Locale("ro", "RO"))
                        .format(product.price)
                    Text(
                        text = formatted,
                        style = typography.bodyMEmphasis,
                        color = colors.ink,
                    )
                }
                // quick-260530-nx5: retailer name row + small Add-to-registry button.
                // Row keeps the button right-aligned and vertically centered with the
                // retailer label. Button always renders, even when retailerName is blank
                // (rule: every Discover card has an Add button).
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (product.retailerName.isNotBlank()) {
                        Text(
                            text = product.retailerName,
                            style = typography.bodyM,
                            color = colors.inkFaint,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    DiscoverAddToRegistryButton(
                        onClick = { onAddToRegistry(product) },
                    )
                }
            }
        }
    }
}

/**
 * quick-260530-nx5: tiny terracotta "+" overlay used on Discover product
 * cards. Visual contract mirrors GiftMaisonFab (accent CircleShape +
 * accentInk plus icon) at a much smaller scale, sized so the visual
 * diameter is "just a little bigger than the uppercase cap-height of
 * the retailer-name TextStyle" (typography.bodyM, ~13.5sp font, ~9-10sp
 * cap-height → ~20dp visual circle).
 *
 * - 32dp outer click target (compromise between M3 48dp floor and not
 *   crowding the retailer row at 2-column layout widths).
 * - 20dp visual circle, accent fill.
 * - 14dp plus icon, accentInk tint.
 * - NO shadow, NO paper ring — they look heavy at this size and the
 *   card already provides visual separation.
 *
 * Click does NOT propagate to the parent Card.onClick because Compose
 * routes the gesture to the innermost Modifier.clickable consumer.
 */
@Composable
private fun DiscoverAddToRegistryButton(
    onClick: () -> Unit,
) {
    val colors = GiftMaisonTheme.colors
    val contentDesc = stringResource(R.string.discover_card_add_to_registry)
    Box(
        modifier = Modifier
            .size(32.dp)
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = contentDesc
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(color = colors.accent, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null, // announced via outer Box semantics
                tint = colors.accentInk,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}
