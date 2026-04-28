package com.giftregistry.ui.registry.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.giftregistry.R
import com.giftregistry.domain.model.Registry
import com.giftregistry.ui.registry.cover.HeroImageOrPlaceholder
import com.giftregistry.ui.theme.GiftMaisonTheme

/**
 * SCR-08 — 180 dp hero (Coil 3 AsyncImage or accentSoft→accent placeholder with
 * occasion glyph) + pinned toolbar with alpha-fade tied to LazyListState.
 *
 * Pitfall 1 guard (RESEARCH.md): uses `heroToolbarAlpha` helper which returns 1f
 * once `firstVisibleItemIndex >= 1` — prevents toolbar flash when hero scrolls off.
 *
 * Pitfall 2 guard: `heroThresholdPx` is computed OUTSIDE `derivedStateOf` via
 * `remember(density) { with(density) { 120.dp.toPx() } }` — density access inside the
 * derived lambda would crash (composition locals unavailable outside composition).
 *
 * Phase 12 refactor (D-16): the inline image-or-placeholder block delegates to
 * the shared [HeroImageOrPlaceholder] composable. The 3-stop dark overlay
 * STAYS at this call site, gated on `imageUrl != null` (Pitfall 6 — the
 * overlay must NOT paint over the gradient placeholder, which already has
 * sufficient contrast for the over-hero text). The `glyphSize = 40.sp`
 * argument preserves the Phase 11 hero pixel contract.
 */
@Composable
internal fun RegistryDetailHero(
    registry: Registry?,
    listState: LazyListState,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onOverflow: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * Phase 12 D-13 — owner-only tap on the 180 dp hero opens the
     * cover-photo picker sheet. Pass non-null only for the registry owner;
     * guests / web viewers see no tap affordance because clickable(enabled = false)
     * is a no-op (no ripple, no pressed state).
     */
    onCoverTap: (() -> Unit)? = null,
) {
    val colors = GiftMaisonTheme.colors
    val typography = GiftMaisonTheme.typography
    val shapes = GiftMaisonTheme.shapes
    val spacing = GiftMaisonTheme.spacing

    val density = LocalDensity.current
    // Pitfall 2: density captured outside derivedStateOf via remember
    val heroThresholdPx = remember(density) { with(density) { 120.dp.toPx() } }

    val toolbarAlpha by remember(listState) {
        derivedStateOf {
            heroToolbarAlpha(
                firstVisibleItemIndex = listState.firstVisibleItemIndex,
                firstVisibleItemScrollOffsetPx = listState.firstVisibleItemScrollOffset,
                heroThresholdPx = heroThresholdPx,
            )
        }
    }

    Box(modifier = modifier.fillMaxWidth().height(180.dp)) {
        // --- Hero image OR gradient placeholder via shared composable (D-16). ---
        // D-13 — owner-only tap target on the hero opens the cover-photo picker.
        // clickable(enabled = onCoverTap != null) is a no-op for guests / web
        // viewers (no ripple, no pressed state). The clickable wraps the image
        // area only — the toolbar Row below keeps its own click handlers.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(enabled = onCoverTap != null) { onCoverTap?.invoke() }
        ) {
            HeroImageOrPlaceholder(
                imageUrl = registry?.imageUrl,
                occasion = registry?.occasion,
                glyphSize = 40.sp,                   // hero uses 40 sp (preserve Phase 11 pixel contract)
                modifier = Modifier.fillMaxSize(),
            )
        }
        // 3-stop dark overlay (ink@0x44 top, transparent 40%, ink@0xAA bottom) — ONLY when imageUrl != null (Pitfall 6 guard)
        if (registry?.imageUrl != null) {
            val inkTop = Color(0xFF2A2420).copy(alpha = 0.27f)
            val inkBottom = Color(0xFF2A2420).copy(alpha = 0.67f)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to inkTop,
                            0.4f to Color.Transparent,
                            1f to inkBottom,
                        )
                    )
            )
        }

        // --- Over-hero text: occasion + title (bottom-left) ---
        if (registry != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(spacing.gap8),
                verticalArrangement = Arrangement.spacedBy(spacing.gap4),
            ) {
                // Blur pill (semi-transparent black; Modifier.blur() not safe on API < 31)
                Box(
                    modifier = Modifier
                        .clip(shapes.pill)
                        .background(Color.Black.copy(alpha = 0.25f))
                        .padding(horizontal = spacing.gap8, vertical = spacing.gap4),
                ) {
                    Text(
                        text = registry.occasion.ifBlank { "—" },
                        style = typography.bodyXS,
                        color = colors.paper,
                    )
                }
                Text(
                    text = registry.title,
                    style = typography.displayL,
                    color = colors.paper,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // --- Pinned toolbar overlay, alpha-faded via background alpha ---
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .zIndex(1f)
                .background(colors.paper.copy(alpha = toolbarAlpha))
                .padding(spacing.gap8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.common_back),
                    tint = if (toolbarAlpha > 0.5f) colors.ink else colors.paper,
                )
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onShare) {
                Icon(
                    imageVector = Icons.Default.OpenInNew,
                    contentDescription = stringResource(R.string.registry_detail_share_button_desc),
                    tint = if (toolbarAlpha > 0.5f) colors.ink else colors.paper,
                )
            }
            IconButton(onClick = onOverflow) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.registry_detail_overflow_desc),
                    tint = if (toolbarAlpha > 0.5f) colors.ink else colors.paper,
                )
            }
        }
    }
}
