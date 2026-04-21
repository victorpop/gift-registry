package com.giftregistry.ui.common.status

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/** STAT-01 pulse period (handoff CSS `pulse` keyframe). */
const val PULSING_DOT_DEFAULT_PERIOD_MS: Long = 1_400L

/** STAT-01 alpha keyframe endpoints (1f at keyframe 0%, 0.5f at 50%). */
const val PULSING_DOT_ALPHA_START: Float = 1f
const val PULSING_DOT_ALPHA_END: Float = 0.5f

/** STAT-01 scale keyframe endpoints (1f at keyframe 0%, 0.85f at 50%). */
const val PULSING_DOT_SCALE_START: Float = 1f
const val PULSING_DOT_SCALE_END: Float = 0.85f

/**
 * Reusable pulsing dot — STAT-01 on the Reserved chip (1400 ms) and Phase 11
 * "Fetching from…" URL field (1000 ms). Alpha 1f↔0.5f + scale 1f↔0.85f via
 * `rememberInfiniteTransition` + `tween(period/2, FastOutSlowInEasing)` with
 * RepeatMode.Reverse — one full cycle = [period].
 *
 * The period is split in half because Reverse repeat covers a full cycle in
 * two halves (forward + reverse).
 *
 * @param color Dot fill colour. On Reserved chip = accentInk; on Phase 11
 *   "Fetching" field = accent.
 * @param size Dot diameter. Default 4.dp (handoff spec).
 * @param period Full one-cycle duration. Default 1400 ms.
 */
@Composable
fun PulsingDot(
    color: Color,
    size: Dp = 4.dp,
    period: Duration = PULSING_DOT_DEFAULT_PERIOD_MS.milliseconds,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "pulsingDot")
    val halfPeriodMs = (period.inWholeMilliseconds / 2L).toInt()
    val spec = infiniteRepeatable<Float>(
        animation = tween(durationMillis = halfPeriodMs, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse,
    )
    val alpha by transition.animateFloat(
        initialValue = PULSING_DOT_ALPHA_START,
        targetValue = PULSING_DOT_ALPHA_END,
        animationSpec = spec,
        label = "pulsingDotAlpha",
    )
    val scale by transition.animateFloat(
        initialValue = PULSING_DOT_SCALE_START,
        targetValue = PULSING_DOT_SCALE_END,
        animationSpec = spec,
        label = "pulsingDotScale",
    )
    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .background(color, CircleShape),
    )
}
