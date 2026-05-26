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
// See: https://fonts.google.com/specimen/Inter
// Note: Instrument Serif (260427-lnq) and JetBrains Mono (16-06 Task 8) were removed from
// Google Fonts entries — bundled TTFs ship synchronously in the APK; see family KDocs.
private val inter = GoogleFont("Inter")

/**
 * Instrument Serif — display headings (h1..card title) and the GiftMaison wordmark.
 * Handoff uses Italic variant for expressive accents (wordmark period, occasion glyphs).
 *
 * Bundled as `res/font/instrument_serif_{regular,italic}.ttf` so rendering is synchronous
 * and works on devices without Google Play Services. Replaces the original Google Fonts
 * runtime download (260427-gxu added bundled as fallback; 260427-lnq promoted bundled to
 * primary since GMS async loading caused inconsistent fallback behaviour app-wide).
 */
val InstrumentSerifFamily: FontFamily = FontFamily(
    Font(resId = R.font.instrument_serif_regular, weight = FontWeight.Normal),
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
 *
 * Bundled as `res/font/jetbrains_mono_{medium,semibold}.ttf` (v2.304, OFL-licensed —
 * see `assets/licenses/OFL_jetbrains_mono.txt`) so rendering is synchronous and works
 * on devices without Google Play Services. Replaces the original Google Fonts runtime
 * download — same precedent as InstrumentSerifFamily (260427-lnq): GMS async loading
 * caused inconsistent per-glyph fallback rendering across cards on the same screen
 * (NotificationsScreen timestamps surfaced this during Plan 16-06 UAT Section D1).
 */
val JetBrainsMonoFamily: FontFamily = FontFamily(
    Font(resId = R.font.jetbrains_mono_medium, weight = FontWeight.Medium),
    Font(resId = R.font.jetbrains_mono_semibold, weight = FontWeight.SemiBold),
)
