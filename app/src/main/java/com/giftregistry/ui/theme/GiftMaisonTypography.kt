package com.giftregistry.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * GiftMaison v1.1 type scale.
 *
 * Source-of-truth: `design_handoff/design_handoff_android_owner_flow/README.md`
 * "Type scale" table. Line-height values from the handoff are CSS unitless multipliers,
 * expressed here as `.em` (per 08-RESEARCH.md Pattern 5). Letter-spacing values in the
 * JSX source (`reference/GiftMaison - gift registry mocks.html`) are naked numerics,
 * which React inline styles treat as **pixels** — NOT em. Mapped here as `.sp` so a
 * "−0.2" handoff value renders as −0.2 scaled-pixels, matching the web prototype.
 * (Corrects the em interpretation in 08-RESEARCH.md Pitfall 3.)
 *
 * Every style sets `PlatformTextStyle(includeFontPadding = false)` + a centred
 * `LineHeightStyle` to avoid the ~20% vertical inflation caused by legacy font
 * padding (Pitfall 2).
 *
 * Body M maps to two variants: `bodyM` (W400 default) and `bodyMEmphasis` (W500)
 * covering the handoff's "400-500" weight range.
 */
@Immutable
data class GiftMaisonTypography(
    val displayXL: TextStyle,
    val displayL: TextStyle,
    val displayM: TextStyle,
    val displayS: TextStyle,
    val bodyL: TextStyle,
    val bodyM: TextStyle,
    val bodyMEmphasis: TextStyle,
    val bodyS: TextStyle,
    val bodyXS: TextStyle,
    val monoCaps: TextStyle,
)

private val noFontPadding = PlatformTextStyle(includeFontPadding = false)
private val centredLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

fun giftMaisonTypography(): GiftMaisonTypography = GiftMaisonTypography(
    displayXL = TextStyle(
        fontFamily = InstrumentSerifFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        letterSpacing = (-0.8).sp,
        lineHeight = 1.0.em,
        platformStyle = noFontPadding,
        lineHeightStyle = centredLineHeight,
    ),
    displayL = TextStyle(
        fontFamily = InstrumentSerifFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        letterSpacing = (-0.4).sp,
        lineHeight = 1.05.em,
        platformStyle = noFontPadding,
        lineHeightStyle = centredLineHeight,
    ),
    displayM = TextStyle(
        fontFamily = InstrumentSerifFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        letterSpacing = (-0.4).sp,
        lineHeight = 1.1.em,
        platformStyle = noFontPadding,
        lineHeightStyle = centredLineHeight,
    ),
    displayS = TextStyle(
        fontFamily = InstrumentSerifFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        letterSpacing = (-0.3).sp,
        lineHeight = 1.1.em,
        platformStyle = noFontPadding,
        lineHeightStyle = centredLineHeight,
    ),
    bodyL = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        letterSpacing = (-0.2).sp,
        lineHeight = 1.35.em,
        platformStyle = noFontPadding,
        lineHeightStyle = centredLineHeight,
    ),
    bodyM = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        letterSpacing = (-0.1).sp,
        lineHeight = 1.45.em,
        platformStyle = noFontPadding,
        lineHeightStyle = centredLineHeight,
    ),
    bodyMEmphasis = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.5.sp,
        letterSpacing = (-0.1).sp,
        lineHeight = 1.45.em,
        platformStyle = noFontPadding,
        lineHeightStyle = centredLineHeight,
    ),
    bodyS = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.5.sp,
        letterSpacing = 0.sp,
        lineHeight = 1.4.em,
        platformStyle = noFontPadding,
        lineHeightStyle = centredLineHeight,
    ),
    bodyXS = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.5.sp,
        letterSpacing = 0.sp,
        lineHeight = 1.35.em,
        platformStyle = noFontPadding,
        lineHeightStyle = centredLineHeight,
    ),
    monoCaps = TextStyle(
        fontFamily = JetBrainsMonoFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 9.5.sp,
        letterSpacing = 1.5.sp,
        lineHeight = 1.3.em,
        platformStyle = noFontPadding,
        lineHeightStyle = centredLineHeight,
    ),
)

val LocalGiftMaisonTypography = staticCompositionLocalOf {
    val empty = TextStyle.Default
    GiftMaisonTypography(
        displayXL = empty, displayL = empty, displayM = empty, displayS = empty,
        bodyL = empty, bodyM = empty, bodyMEmphasis = empty, bodyS = empty, bodyXS = empty,
        monoCaps = empty,
    )
}
