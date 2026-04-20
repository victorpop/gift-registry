package com.giftregistry.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * GiftMaison v1.1 spacing tokens.
 *
 * Source-of-truth: `design_handoff/design_handoff_android_owner_flow/README.md`
 * "Spacing / radii / shadows" section. Handoff uses ad-hoc values (14, 16, 20,
 * 6, 10, 12) rather than a single step scale — 08-RESEARCH.md Open Question 3
 * resolves this by enumerating every distinct value as a named field.
 *
 * Call sites should prefer these names (`spacing.gap14`) over raw `14.dp` literals
 * so future mock revisions land in one place.
 */
@Immutable
data class GiftMaisonSpacing(
    /** 16 dp — default screen edge padding. */
    val edge: Dp,
    /** 20 dp — wider screen edge padding (bottom sheet, onboarding). */
    val edgeWide: Dp,
    /** 4 dp gap — tiny (e.g. bullet separators, stroke-to-icon). */
    val gap4: Dp,
    /** 6 dp gap — inter-tile column gap (Create registry tile grid). */
    val gap6: Dp,
    /** 8 dp gap — small stack gap. */
    val gap8: Dp,
    /** 10 dp gap — inter-row gap (registry cards, action sheet rows). */
    val gap10: Dp,
    /** 12 dp gap — field group vertical gap (Add item). */
    val gap12: Dp,
    /** 14 dp gap — form field vertical gap (Create registry). */
    val gap14: Dp,
    /** 16 dp gap — section-to-section gap. */
    val gap16: Dp,
    /** 20 dp gap — generous hero / CTA bar padding. */
    val gap20: Dp,
)

fun giftMaisonSpacing(): GiftMaisonSpacing = GiftMaisonSpacing(
    edge = 16.dp,
    edgeWide = 20.dp,
    gap4 = 4.dp,
    gap6 = 6.dp,
    gap8 = 8.dp,
    gap10 = 10.dp,
    gap12 = 12.dp,
    gap14 = 14.dp,
    gap16 = 16.dp,
    gap20 = 20.dp,
)

val LocalGiftMaisonSpacing = staticCompositionLocalOf { giftMaisonSpacing() }
