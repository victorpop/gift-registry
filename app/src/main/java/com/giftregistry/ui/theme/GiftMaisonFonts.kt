package com.giftregistry.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.giftregistry.R

/**
 * Shared provider for Google Fonts downloadable fonts.
 *
 * The `certificates = R.array.com_google_android_gms_fonts_certs` reference resolves
 * to `app/src/main/res/values-v23/font_certs.xml`, which carries the published
 * Google Play Services cert hashes (Jetchat sample — public values, not secrets).
 *
 * Pitfall 1 (08-RESEARCH.md): On devices without Google Play Services the fonts
 * silently fail to download and fall back to system serif / sans / mono. v1.1
 * targets GMS-equipped AVDs and production devices — acceptable.
 */
internal const val GMS_FONTS_AUTHORITY = "com.google.android.gms.fonts"
internal const val GMS_FONTS_PACKAGE = "com.google.android.gms"

val giftMaisonFontProvider: GoogleFont.Provider = GoogleFont.Provider(
    providerAuthority = GMS_FONTS_AUTHORITY,
    providerPackage = GMS_FONTS_PACKAGE,
    certificates = R.array.com_google_android_gms_fonts_certs,
)

// Google Fonts catalog names — must match fonts.google.com URLs exactly (Pitfall 7).
// See: https://fonts.google.com/specimen/Instrument+Serif
//      https://fonts.google.com/specimen/Inter
//      https://fonts.google.com/specimen/JetBrains+Mono
private val instrumentSerif = GoogleFont("Instrument Serif")
private val inter = GoogleFont("Inter")
private val jetBrainsMono = GoogleFont("JetBrains Mono")

/**
 * Instrument Serif — displays (h1..card title) and the GiftMaison wordmark.
 * Handoff uses Italic variant for expressive accents (wordmark period, occasion glyphs).
 *
 * GMS entries are listed first so Google Play Services font download takes priority
 * when available. Bundled TTF entries follow as fallback for non-GMS devices or
 * before the async download completes on first cold launch.
 */
val InstrumentSerifFamily: FontFamily = FontFamily(
    Font(googleFont = instrumentSerif, fontProvider = giftMaisonFontProvider, weight = FontWeight.Normal),
    Font(resId = R.font.instrument_serif_regular, weight = FontWeight.Normal),
    Font(googleFont = instrumentSerif, fontProvider = giftMaisonFontProvider, weight = FontWeight.Normal, style = FontStyle.Italic),
    Font(resId = R.font.instrument_serif_italic, weight = FontWeight.Normal, style = FontStyle.Italic),
)

/**
 * Inter — body text. Handoff uses W400 (default) and W500 (emphasis).
 */
val InterFamily: FontFamily = FontFamily(
    Font(googleFont = inter, fontProvider = giftMaisonFontProvider, weight = FontWeight.Normal),
    Font(googleFont = inter, fontProvider = giftMaisonFontProvider, weight = FontWeight.Medium),
    Font(googleFont = inter, fontProvider = giftMaisonFontProvider, weight = FontWeight.SemiBold),
)

/**
 * JetBrains Mono — mono caps labels (nav, stats, timestamps).
 * Handoff uses W500 and W600.
 */
val JetBrainsMonoFamily: FontFamily = FontFamily(
    Font(googleFont = jetBrainsMono, fontProvider = giftMaisonFontProvider, weight = FontWeight.Medium),
    Font(googleFont = jetBrainsMono, fontProvider = giftMaisonFontProvider, weight = FontWeight.SemiBold),
)
