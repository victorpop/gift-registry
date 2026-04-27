package com.giftregistry.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.giftregistry.R
import com.giftregistry.ui.common.ConcentricRings
import com.giftregistry.ui.theme.GiftMaisonTheme
import com.giftregistry.ui.theme.InstrumentSerifFamily
import com.giftregistry.ui.theme.googleBannerShadow

/**
 * SCR-06: Google sign-in banner on the Onboarding / Auth screen.
 *
 * Handoff (§ 06 Google banner spec):
 *   - Shape: 16 radius (shapes.radius16). Background: accent.
 *   - Padding: 14 vertical (spacing.gap14), 16 horizontal (spacing.edge). Handoff
 *     says 14/16; UI-SPEC.md says 14/edgeWide (20). Handoff wins → 14/16.
 *   - Shadow: googleBannerShadow(accent) — 0 10 24 {accent}55 handoff mapping.
 *   - Left: 20×20 white circle with Google "G" — minimal implementation is a
 *     white circle with accent "G" letter in bodyMEmphasis (placeholder until
 *     a branded G asset is added; matches handoff fallback "single-colour G").
 *   - Centre: "Continue with Google" label in typography.bodyL, accentInk text.
 *   - Right: italic Instrument Serif "→" arrow at 18 sp in accentInk alpha 0.9.
 *   - Top-end corner: ConcentricRings(color = colors.accentInk).
 *   - Entire banner is clickable, invokes onClick().
 *
 * String: R.string.auth_google_cta — "Continue with Google" (EN) / "Continuă cu Google" (RO).
 * Rewired from the provisional string reference used in Plan 02.
 */
@Composable
fun GoogleBanner(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GiftMaisonTheme.colors
    val typography = GiftMaisonTheme.typography
    val shapes = GiftMaisonTheme.shapes
    val spacing = GiftMaisonTheme.spacing

    Box(
        modifier = modifier
            .fillMaxWidth()
            .googleBannerShadow(colors.accent)
            .clip(shapes.radius16)
            .background(colors.accent)
            .clickable(onClick = onClick)
            .padding(vertical = spacing.gap14, horizontal = spacing.edge),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left: 20×20 white circle with "G" glyph (placeholder for branded asset)
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "G",
                    style = typography.bodyMEmphasis,
                    color = colors.accent,
                    fontSize = 12.sp,
                )
            }

            Spacer(modifier = Modifier.width(spacing.gap8))

            // Centre: CTA label
            Text(
                text = stringResource(R.string.auth_google_cta),
                style = typography.bodyL,
                color = colors.accentInk,
                modifier = Modifier.weight(1f),
            )

            // Right: italic serif arrow
            Text(
                text = "\u2192",
                fontFamily = InstrumentSerifFamily,
                fontStyle = FontStyle.Italic,
                fontSize = 18.sp,
                color = colors.accentInk.copy(alpha = 0.9f),
            )
        }

        // Top-end concentric-rings overlay (ornamental, clipped at radius16 corner).
        // matchParentSize wrapper draws within the banner's natural bounds
        // (Row + padding ≈ 68 dp) without ConcentricRings' Canvas inflating
        // the banner's measured height.
        Box(modifier = Modifier.matchParentSize()) {
            ConcentricRings(
                color = colors.accentInk,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}
