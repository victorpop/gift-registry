package com.giftregistry.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * GiftMaison v1.1 shadow tokens.
 *
 * Source-of-truth: `design_handoff/design_handoff_android_owner_flow/README.md`
 * "Spacing / radii / shadows" — three named shadows:
 *
 *   FAB          — 0 8 20 {accent}55       → elevation 20, tint accent
 *   Google banner — 0 10 24 {accent}55      → elevation 24, tint accent
 *   Bottom sheet — 0 -10 40 rgba(0,0,0,0.15) → elevation 40, neutral (up-cast)
 *
 * The handoff's CSS "y-offset · blur · colour" notation translates to Compose's
 * elevation-based shadow via the blur component. Spot-coloured shadows render on
 * API 28+; on API 23-27 (project minSdk 23) the shadow falls back to the Compose
 * default tint, which is acceptable visually (handoff's ornamental shadows are
 * subtle enough that loss of colour on legacy devices is not a regression).
 *
 * Prefer the `Modifier.fabShadow(...)` / `Modifier.googleBannerShadow(...)` /
 * `Modifier.bottomSheetShadow()` extensions at call sites — they wrap the
 * correct shape + elevation pair so screens can't mismatch them.
 */
object GiftMaisonShadows {
    val fabElevation: Dp = 20.dp
    val googleBannerElevation: Dp = 24.dp
    val bottomSheetElevation: Dp = 40.dp

    private val googleBannerShape = RoundedCornerShape(16.dp)
    private val bottomSheetShape = RoundedCornerShape(
        topStart = 22.dp,
        topEnd = 22.dp,
        bottomStart = 0.dp,
        bottomEnd = 0.dp,
    )

    internal fun fabShape() = CircleShape
    internal fun googleBannerShape() = googleBannerShape
    internal fun bottomSheetShape() = bottomSheetShape
}

/**
 * Apply the handoff FAB shadow: elevation 20 dp, tint = accent (passed from caller).
 * Call sites pass `GiftMaisonTheme.colors.accent` to avoid hardcoding the token.
 */
fun Modifier.fabShadow(tint: Color): Modifier = this.shadow(
    elevation = GiftMaisonShadows.fabElevation,
    shape = GiftMaisonShadows.fabShape(),
    spotColor = tint,
    ambientColor = tint,
)

/**
 * Apply the handoff Google-banner shadow: elevation 24 dp, 16-radius, tint = accent.
 */
fun Modifier.googleBannerShadow(tint: Color): Modifier = this.shadow(
    elevation = GiftMaisonShadows.googleBannerElevation,
    shape = GiftMaisonShadows.googleBannerShape(),
    spotColor = tint,
    ambientColor = tint,
)

/**
 * Apply the handoff bottom-sheet shadow: elevation 40 dp, 22-radius top corners,
 * neutral tint (no spot colour — the handoff uses rgba(0,0,0,0.15), which Compose
 * approximates via default shadow rendering).
 */
fun Modifier.bottomSheetShadow(): Modifier = this.shadow(
    elevation = GiftMaisonShadows.bottomSheetElevation,
    shape = GiftMaisonShadows.bottomSheetShape(),
)
