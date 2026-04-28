package com.giftregistry.ui.registry.cover

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.giftregistry.ui.registry.create.OccasionCatalog
import com.giftregistry.ui.theme.GiftMaisonTheme

/**
 * Phase 12 — Shared cover-image surface for the registry detail hero (180 dp)
 * and the home registry cards (16:9). Single source of truth for the
 * gradient + glyph fallback (D-16). Resolves [imageUrl] via
 * [resolveImageModel]: HTTP URLs become [String] models, `preset:*` sentinels
 * become `R.drawable.*` `Int`s, null/unknown values trigger the gradient
 * placeholder branch.
 *
 * Decision references:
 * - D-14: gradient (`accentSoft → accent`) + glyph (`OccasionCatalog.glyphFor`,
 *   Instrument Serif italic, paper) when `imageUrl == null`. Hero uses
 *   `glyphSize = 40.sp` (preserve Phase 11 hero pixel contract); cards use
 *   `glyphSize = 32.sp`.
 * - D-15: callable from `RegistryCardPrimary` AND `RegistryCardSecondary`
 *   so both card variants render the placeholder when `imageUrl == null`
 *   (fixes the visible bug where bare `AsyncImage(model = null)` rendered
 *   an empty box).
 * - D-16: extracted from the Phase 11 `RegistryDetailHero` placeholder block
 *   (lines 108-129). Existing pixel contract on the hero must NOT regress —
 *   the 3-stop dark overlay used in `RegistryDetailHero` is NOT inside this
 *   composable, it stays at the call site (Pitfall 6 — only paint the dark
 *   overlay on the URL branch).
 *
 * RESEARCH.md references:
 * - Pattern 4: Coil 3 with `Int` resource ID OR remote URL via the same
 *   `AsyncImage` call (this composable is the verbatim implementation).
 * - Pitfall 7: Coil cache stale-model issue. The `remember(imageUrl) {
 *   resolveImageModel(imageUrl) }` call ensures Coil tears down + recomposes
 *   when imageUrl flips null↔non-null. Structural if/else (separate composable
 *   subtrees) is used instead of a `painterResource`-style ternary inside a
 *   single `AsyncImage`, which would suppress recomposition on null-flip.
 *
 * @param imageUrl persisted [com.giftregistry.domain.model.Registry.imageUrl]
 *                 — HTTP URL, `preset:{occasion}:{idx}` sentinel, or null.
 * @param occasion the registry's occasion string (canonical
 *                 [OccasionCatalog.storageKeyFor] form recommended). Drives
 *                 the placeholder glyph; ignored when an image renders.
 * @param glyphSize size of the placeholder glyph. Default 32.sp matches the
 *                  card surfaces; pass 40.sp from `RegistryDetailHero` to
 *                  preserve the Phase 11 hero contract.
 * @param colorFilter optional [ColorFilter] applied ONLY to the AsyncImage
 *                    branch — used by `RegistryCardPrimary` to darken its
 *                    real-image branch to 70% brightness. The gradient
 *                    placeholder branch is intentionally unaffected so the
 *                    accent gradient still pops on the dark-ink card.
 */
@Composable
fun HeroImageOrPlaceholder(
    imageUrl: String?,
    occasion: String?,
    glyphSize: TextUnit = 32.sp,
    colorFilter: ColorFilter? = null,
    modifier: Modifier = Modifier,
) {
    val colors = GiftMaisonTheme.colors
    val typography = GiftMaisonTheme.typography
    // Pitfall 7 guard — re-resolve on imageUrl flip so Coil tears down its
    // model cache when the user picks/removes a cover.
    val model: Any? = remember(imageUrl) { resolveImageModel(imageUrl) }

    if (model != null) {
        AsyncImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            colorFilter = colorFilter,
            modifier = modifier,
        )
    } else {
        Box(
            modifier = modifier.background(
                Brush.verticalGradient(
                    0f to colors.accentSoft,
                    1f to colors.accent,
                )
            ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = OccasionCatalog.glyphFor(occasion),
                style = typography.displayXL.copy(
                    fontSize = glyphSize,
                    fontStyle = FontStyle.Italic,
                ),
                color = colors.paper,
            )
        }
    }
}
