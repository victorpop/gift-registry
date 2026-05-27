package com.giftregistry.ui.discover

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.giftregistry.ui.theme.GiftMaisonTheme

/**
 * Phase 17 — shimmer skeleton card for Discover loading state.
 *
 * Per UI-SPEC "Loading State — Shimmer Skeletons":
 * - Card identical in size + radius to DiscoverProductCard (16:9 image area
 *   + body block with title, two description lines, price stub).
 * - Brush: horizontal-gradient sweep of paperDeep -> line -> paperDeep,
 *   translated 0..1000 px in 1200 ms with FastOutSlowInEasing.
 *
 * Used by DiscoverScreen for both the FROM THE COMMUNITY skeleton (popular
 * Loading) and FROM THE WEB skeleton (search Loading) — render 3 of these
 * in the relevant LazyColumn section.
 */
@Composable
fun DiscoverShimmerCard(modifier: Modifier = Modifier) {
    val colors = GiftMaisonTheme.colors
    val transition = rememberInfiniteTransition(label = "discover-shimmer")
    val translate by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer-translate",
    )
    val brush = Brush.horizontalGradient(
        colors = listOf(colors.paperDeep, colors.line, colors.paperDeep),
        startX = translate - 300f,
        endX = translate,
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.paperDeep),
    ) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(brush),
            )
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Box(
                    Modifier
                        .fillMaxWidth(0.75f)
                        .height(16.dp)
                        .background(brush, RoundedCornerShape(8.dp)),
                )
                Box(Modifier.height(8.dp))
                Box(
                    Modifier
                        .fillMaxWidth(0.9f)
                        .height(13.dp)
                        .background(brush, RoundedCornerShape(8.dp)),
                )
                Box(Modifier.height(4.dp))
                Box(
                    Modifier
                        .fillMaxWidth(0.6f)
                        .height(13.dp)
                        .background(brush, RoundedCornerShape(8.dp)),
                )
                Box(Modifier.height(8.dp))
                Box(
                    Modifier
                        .fillMaxWidth(0.35f)
                        .height(14.dp)
                        .background(brush, RoundedCornerShape(8.dp)),
                )
            }
        }
    }
}
