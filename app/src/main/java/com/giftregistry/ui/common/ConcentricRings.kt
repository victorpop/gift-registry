package com.giftregistry.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * SCR-06: 3 concentric rings for the Google banner top-end decoration.
 *
 * Handoff contract (design_handoff README.md § 06 Google banner spec):
 *   "Two decorative concentric rings absolute-positioned in the top-right
 *    (subtle {accentInk}25 and {accentInk}18 strokes — purely ornamental,
 *    clip with overflow: hidden)."
 *
 * 10-UI-SPEC.md + 10-RESEARCH.md Pattern 3 resolve to 3 rings (outer/middle/inner)
 * with Compose alpha values 0.08 / 0.12 / 0.18 (converted from handoff hex
 * fractional values; see RESEARCH.md for derivation).
 *
 * Canvas is preferred over layered Box(border=) because rings anchor at the
 * corner (center = Offset(width, 0)) — Canvas drawing naturally clips at bounds,
 * achieving the handoff's `overflow: hidden` corner effect.
 */
@Composable
fun ConcentricRings(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(70.dp)) {
        val center = Offset(x = size.width, y = 0f)
        drawCircle(
            color = color.copy(alpha = 0.08f),
            radius = 30.dp.toPx(),
            center = center,
            style = Stroke(width = 1.dp.toPx()),
        )
        drawCircle(
            color = color.copy(alpha = 0.12f),
            radius = 20.dp.toPx(),
            center = center,
            style = Stroke(width = 1.dp.toPx()),
        )
        drawCircle(
            color = color.copy(alpha = 0.18f),
            radius = 12.dp.toPx(),
            center = center,
            style = Stroke(width = 1.dp.toPx()),
        )
    }
}
