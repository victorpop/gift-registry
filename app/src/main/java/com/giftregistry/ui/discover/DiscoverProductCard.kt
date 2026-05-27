package com.giftregistry.ui.discover

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
 *
 * Click behaviour (D-32, D-33):
 * - Tap → Intent.ACTION_VIEW with the RAW retailerUrl (NO affiliate transform —
 *   that's deliberately deferred to the future "Save to registry" flow per D-32).
 * - ActivityNotFoundException → Snackbar fallback via the parent SnackbarHostState
 *   so the user is not silently dropped on devices without a browser handler.
 */
@Composable
fun DiscoverProductCard(
    product: DiscoverProduct,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
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
                if (product.retailerName.isNotBlank()) {
                    Text(
                        text = product.retailerName,
                        style = typography.bodyM,
                        color = colors.inkFaint,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
