package com.giftregistry.ui.theme

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * DES-05 — "GiftMaison" wordmark.
 *
 * Handoff: Instrument Serif italic with a single accent-coloured period. Brand mark
 * (not localizable) — renders identically in every locale.
 *
 * AnnotatedString factory is pure Kotlin so it is unit-testable without a Compose
 * test runtime. See `WordmarkTest`.
 */

private const val WORDMARK_BODY = "giftmaison"
private const val WORDMARK_PERIOD = "."

fun wordmarkAnnotatedString(ink: Color, accent: Color): AnnotatedString =
    buildAnnotatedString {
        withStyle(SpanStyle(color = ink)) {
            append(WORDMARK_BODY)
        }
        withStyle(SpanStyle(color = accent)) {
            append(WORDMARK_PERIOD)
        }
    }

@Composable
fun GiftMaisonWordmark(
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 20.sp,
) {
    val ink = GiftMaisonTheme.colors.ink
    val accent = GiftMaisonTheme.colors.accent
    val annotated = remember(ink, accent) { wordmarkAnnotatedString(ink, accent) }
    Text(
        text = annotated,
        modifier = modifier,
        fontFamily = InstrumentSerifFamily,
        fontStyle = FontStyle.Italic,
        fontWeight = FontWeight.Normal,
        fontSize = fontSize,
        letterSpacing = (-0.4).sp,
    )
}
