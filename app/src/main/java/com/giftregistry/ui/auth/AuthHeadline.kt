package com.giftregistry.ui.auth

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import com.giftregistry.R
import com.giftregistry.ui.theme.GiftMaisonTheme

/**
 * SCR-06: Two-line italic-serif headline for the Auth screen.
 *
 *   Line 1 "Start your"                        → inkSoft
 *   \n
 *   Line 2 "first registry"                    → ink
 *   Line 2 "."                                 → accent (trailing accent period)
 *
 * Same AnnotatedString factory pattern as wordmarkAnnotatedString in
 * GiftMaisonWordmark.kt — factory is pure Kotlin so it is unit-testable via
 * AuthHeadlineTest without a Compose test runtime. The @Composable wrapper is
 * untested (Compose-framework dependency) but delegates all logic to the factory.
 */
fun authHeadlineAnnotatedString(
    prefix: String,
    accent: String,
    ink: Color,
    accentColor: Color,
    inkSoft: Color,
): AnnotatedString = buildAnnotatedString {
    withStyle(SpanStyle(color = inkSoft)) {
        append(prefix)
    }
    append("\n")
    withStyle(SpanStyle(color = ink)) {
        append(accent)
    }
    withStyle(SpanStyle(color = accentColor)) {
        append(".")
    }
}

@Composable
fun AuthHeadline(modifier: Modifier = Modifier) {
    val colors = GiftMaisonTheme.colors
    val typography = GiftMaisonTheme.typography
    val prefix = stringResource(R.string.auth_headline_prefix)
    val accentText = stringResource(R.string.auth_headline_accent)
    val annotated = remember(prefix, accentText, colors.ink, colors.accent, colors.inkSoft) {
        authHeadlineAnnotatedString(
            prefix = prefix,
            accent = accentText,
            ink = colors.ink,
            accentColor = colors.accent,
            inkSoft = colors.inkSoft,
        )
    }
    Text(
        text = annotated,
        modifier = modifier,
        style = typography.displayL.copy(fontStyle = FontStyle.Italic),
    )
}
