package com.giftregistry.ui.item.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.giftregistry.R
import com.giftregistry.ui.common.status.PulsingDot
import com.giftregistry.ui.theme.GiftMaisonTheme
import kotlin.time.Duration.Companion.milliseconds

/**
 * SCR-10: mono-caps "⌕ Fetching from {domain}" + PulsingDot on a 1-second cycle.
 *
 * PulsingDot.period at 1_000.milliseconds is DISTINCT from STAT-01's 1_400 ms
 * default (used on the Reserved chip). Phase 9 call sites pass no explicit period
 * so they default to 1_400 — unaffected by this new call site.
 */
@Composable
internal fun FetchingIndicator(
    domain: String,
    modifier: Modifier = Modifier,
) {
    val colors = GiftMaisonTheme.colors
    val typography = GiftMaisonTheme.typography
    val spacing = GiftMaisonTheme.spacing
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing.gap8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.add_item_fetching_from, domain),
            style = typography.monoCaps,
            color = colors.inkSoft,
        )
        PulsingDot(color = colors.accent, period = 1_000.milliseconds)
    }
}
