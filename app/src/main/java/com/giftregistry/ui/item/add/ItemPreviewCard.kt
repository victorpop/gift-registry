package com.giftregistry.ui.item.add

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.giftregistry.ui.theme.GiftMaisonTheme
import java.net.URI

/**
 * SCR-10: 14-radius paperDeep preview card with 80×80 thumbnail + title/price/source.
 *
 * `source` is derived from the URL host: "emag.ro · via Open Graph".
 */
@Composable
internal fun ItemPreviewCard(
    imageUrl: String,
    title: String,
    price: String,
    url: String,
    modifier: Modifier = Modifier,
) {
    val colors = GiftMaisonTheme.colors
    val typography = GiftMaisonTheme.typography
    val shapes = GiftMaisonTheme.shapes
    val spacing = GiftMaisonTheme.spacing
    val host = runCatching { URI(url).host }.getOrNull()?.removePrefix("www.") ?: "—"
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shapes.radius14)
            .border(BorderStroke(1.dp, colors.line), shapes.radius14)
            .background(colors.paperDeep)
            .padding(spacing.gap12),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(shapes.radius8)
                .background(colors.line),
        ) {
            if (imageUrl.isNotBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Spacer(Modifier.width(spacing.gap12))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(spacing.gap4),
        ) {
            Text(
                text = title.ifBlank { "—" },
                style = typography.bodyMEmphasis,
                color = colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (price.isNotBlank()) {
                Row {
                    Text(text = price, style = typography.bodyMEmphasis, color = colors.ink)
                    Spacer(Modifier.width(spacing.gap4))
                    Text(text = "RON", style = typography.monoCaps, color = colors.inkFaint)
                }
            }
            Text(
                text = "$host · via Open Graph",
                style = typography.monoCaps,
                color = colors.inkFaint,
            )
        }
    }
}
