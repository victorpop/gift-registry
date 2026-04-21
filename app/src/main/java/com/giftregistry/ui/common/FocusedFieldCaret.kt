package com.giftregistry.ui.common

import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * SCR-06: Pulsing caret on the focused email field.
 *
 * Handoff contract (design_handoff README.md § Animations):
 *   "The email field's blinking caret on the onboarding screen (1.1 s infinite
 *    — opacity only, no scale)."
 *
 * DISTINCT from PulsingDot (Phase 9 — 1.4 s, opacity + scale 1f/0.85f).
 * Do NOT reuse PulsingDot here. Constants exposed as top-level `const val` so
 * future tests can assert them without Compose runtime.
 */

/** Full cycle period (ms) for the focused-field caret pulse. Handoff: 1.1 s. */
const val FOCUSED_FIELD_CARET_PERIOD_MS: Long = 1_100L
const val FOCUSED_FIELD_CARET_ALPHA_START: Float = 1f
const val FOCUSED_FIELD_CARET_ALPHA_END: Float = 0.2f

/**
 * Thin 2 dp × 20 dp caret, rendered as a Box with a pulsing alpha modifier.
 * When `isFocused == false`, renders with alpha 0 so the caret is invisible.
 */
@Composable
fun FocusedFieldCaret(
    isFocused: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    width: Dp = 2.dp,
    height: Dp = 20.dp,
) {
    if (!isFocused) {
        Box(modifier = modifier.size(width = width, height = height))
        return
    }

    val transition = rememberInfiniteTransition(label = "FocusedFieldCaret")
    val alpha by transition.animateFloat(
        initialValue = FOCUSED_FIELD_CARET_ALPHA_START,
        targetValue = FOCUSED_FIELD_CARET_ALPHA_END,
        animationSpec = InfiniteRepeatableSpec(
            animation = tween(durationMillis = (FOCUSED_FIELD_CARET_PERIOD_MS / 2).toInt()),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "FocusedFieldCaret_alpha",
    )

    Box(
        modifier = modifier
            .size(width = width, height = height)
            .alpha(alpha)
            .background(color),
    )
}
