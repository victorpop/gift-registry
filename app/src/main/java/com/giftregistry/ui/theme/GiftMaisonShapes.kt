package com.giftregistry.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * GiftMaison v1.1 shape tokens.
 *
 * Source-of-truth: `design_handoff/design_handoff_android_owner_flow/README.md`
 * "Spacing / radii / shadows" — radii 8/10/12/14/16/22/999.
 *
 * `pill` is the handoff's "999" radius, which in Compose is idiomatic as
 * `CircleShape` (semantically "max radius / fully rounded"). This ensures
 * pill shapes adapt to any container height without recomputing.
 *
 * Device-bezel radius 40 is out of scope here (not a UI element — the bezel
 * is drawn by the OS/emulator chrome).
 */
@Immutable
data class GiftMaisonShapes(
    /** 8 dp — thumbnails. */
    val radius8: Shape,
    /** 10 dp — small cards, chips. */
    val radius10: Shape,
    /** 12 dp — inputs, info pills. */
    val radius12: Shape,
    /** 14 dp — occasion tiles. */
    val radius14: Shape,
    /** 16 dp — registry cards, Google banner. */
    val radius16: Shape,
    /** 22 dp — bottom sheet top corners. */
    val radius22: Shape,
    /** Fully-rounded pill (handoff's "999" radius). */
    val pill: Shape,
)

fun giftMaisonShapes(): GiftMaisonShapes = GiftMaisonShapes(
    radius8 = RoundedCornerShape(8.dp),
    radius10 = RoundedCornerShape(10.dp),
    radius12 = RoundedCornerShape(12.dp),
    radius14 = RoundedCornerShape(14.dp),
    radius16 = RoundedCornerShape(16.dp),
    radius22 = RoundedCornerShape(22.dp),
    pill = CircleShape,
)

val LocalGiftMaisonShapes = staticCompositionLocalOf {
    // Default to fallbacks matching the factory; avoids NPE if a screen forgets to wrap.
    giftMaisonShapes()
}
