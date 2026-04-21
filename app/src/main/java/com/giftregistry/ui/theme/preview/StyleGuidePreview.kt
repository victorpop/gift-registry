package com.giftregistry.ui.theme.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.giftregistry.ui.theme.GiftMaisonTheme
import com.giftregistry.ui.theme.GiftMaisonWordmark
import com.giftregistry.ui.theme.GiftRegistryTheme
import com.giftregistry.ui.theme.fabShadow
import kotlin.time.Duration.Companion.milliseconds

/**
 * Style-guide @Preview harness for Phase 8 DES-02..05 verification.
 * Private — not a nav destination. Review in Android Studio preview pane.
 */

@Preview(name = "Type scale", showBackground = true, backgroundColor = 0xFFF7F2E9, widthDp = 360, heightDp = 800)
@Composable
private fun TypeScalePreview() {
    GiftRegistryTheme {
        val t = GiftMaisonTheme.typography
        Column(
            modifier = Modifier
                .background(GiftMaisonTheme.colors.paper)
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Display XL 32 / Instrument Serif 400", style = t.displayXL)
            Text("Display L 24", style = t.displayL)
            Text("Display M 22", style = t.displayM)
            Text("Display S 18", style = t.displayS)
            Text("Body L 15 / Inter 500", style = t.bodyL)
            Text("Body M 13.5 / Inter 400 default", style = t.bodyM)
            Text("Body M emphasis 500", style = t.bodyMEmphasis)
            Text("Body S 12.5", style = t.bodyS)
            Text("Body XS 11.5", style = t.bodyXS)
            Text("MONO CAPS 9.5 / JETBRAINS MONO 500", style = t.monoCaps)
        }
    }
}

@Preview(name = "Colour palette", showBackground = true, widthDp = 360, heightDp = 620)
@Composable
private fun PalettePreview() {
    GiftRegistryTheme {
        val c = GiftMaisonTheme.colors
        val swatches = listOf(
            "paper" to c.paper,
            "paperDeep" to c.paperDeep,
            "ink" to c.ink,
            "inkSoft" to c.inkSoft,
            "inkFaint" to c.inkFaint,
            "line" to c.line,
            "accent" to c.accent,
            "accentInk" to c.accentInk,
            "accentSoft" to c.accentSoft,
            "second" to c.second,
            "secondSoft" to c.secondSoft,
            "ok" to c.ok,
            "warn" to c.warn,
        )
        LazyColumn(
            modifier = Modifier
                .background(c.paper)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(swatches) { (name: String, color: Color) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(color)
                            .border(1.dp, c.line, RoundedCornerShape(8.dp)),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(name, style = GiftMaisonTheme.typography.bodyM, color = c.ink)
                }
            }
        }
    }
}

@Preview(name = "Radii + spacing + shadow", showBackground = true, widthDp = 360, heightDp = 480)
@Composable
private fun RadiiAndShadowsPreview() {
    GiftRegistryTheme {
        val c = GiftMaisonTheme.colors
        val s = GiftMaisonTheme.shapes
        val sp = GiftMaisonTheme.spacing
        Column(
            modifier = Modifier
                .background(c.paper)
                .padding(sp.edgeWide),
            verticalArrangement = Arrangement.spacedBy(sp.gap14),
        ) {
            listOf(
                "radius 8 (thumbnail)" to s.radius8,
                "radius 10 (small card)" to s.radius10,
                "radius 12 (input)" to s.radius12,
                "radius 14 (tile)" to s.radius14,
                "radius 16 (card)" to s.radius16,
                "radius 22 (bottom sheet)" to s.radius22,
            ).forEach { (label, shape) ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(shape)
                        .background(c.paperDeep)
                        .border(1.dp, c.line, shape),
                    contentAlignment = Alignment.Center,
                ) { Text(label, style = GiftMaisonTheme.typography.bodyS, color = c.inkSoft) }
            }
            Box(
                Modifier
                    .width(140.dp)
                    .height(40.dp)
                    .clip(CircleShape)
                    .background(c.ink),
                contentAlignment = Alignment.Center,
            ) { Text("pill / 999", style = GiftMaisonTheme.typography.bodyS, color = c.paper) }
            Box(
                Modifier
                    .size(54.dp)
                    .fabShadow(tint = c.accent)
                    .clip(CircleShape)
                    .background(c.accent),
            )
        }
    }
}

@Preview(name = "Wordmark", showBackground = true, widthDp = 360, heightDp = 120, backgroundColor = 0xFFF7F2E9)
@Composable
private fun WordmarkPreview() {
    GiftRegistryTheme {
        Column(
            modifier = Modifier
                .background(GiftMaisonTheme.colors.paper)
                .padding(GiftMaisonTheme.spacing.edgeWide),
            verticalArrangement = Arrangement.spacedBy(GiftMaisonTheme.spacing.gap10),
        ) {
            GiftMaisonWordmark(fontSize = 20.sp)
            GiftMaisonWordmark(fontSize = 22.sp)
            GiftMaisonWordmark(fontSize = 28.sp)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
// Phase 9 — Shared Chrome + Status UI preview sections (CHROME-01/02/03, STAT-01..04)
// ─────────────────────────────────────────────────────────────────────────

@Preview(name = "Bottom nav — Home selected", showBackground = true, backgroundColor = 0xFFF7F2E9, widthDp = 360, heightDp = 80)
@Composable
private fun BottomNavHomeSelectedPreview() {
    GiftRegistryTheme {
        com.giftregistry.ui.common.chrome.GiftMaisonBottomNav(
            currentKey = com.giftregistry.ui.navigation.HomeKey,
            onHome = {}, onStores = {}, onFab = {}, onLists = {}, onYou = {},
        )
    }
}

@Preview(name = "Bottom nav — RegistryDetail selected", showBackground = true, backgroundColor = 0xFFF7F2E9, widthDp = 360, heightDp = 80)
@Composable
private fun BottomNavListsSelectedPreview() {
    GiftRegistryTheme {
        com.giftregistry.ui.common.chrome.GiftMaisonBottomNav(
            currentKey = com.giftregistry.ui.navigation.RegistryDetailKey(registryId = "preview"),
            onHome = {}, onStores = {}, onFab = {}, onLists = {}, onYou = {},
        )
    }
}

@Preview(name = "Status chips row", showBackground = true, backgroundColor = 0xFFF7F2E9, widthDp = 360, heightDp = 120)
@Composable
private fun StatusChipsPreview() {
    GiftRegistryTheme {
        Row(
            modifier = Modifier
                .background(GiftMaisonTheme.colors.paper)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            com.giftregistry.ui.common.status.StatusChip(
                status = com.giftregistry.domain.model.ItemStatus.AVAILABLE,
                expiresAt = null,
            )
            com.giftregistry.ui.common.status.StatusChip(
                status = com.giftregistry.domain.model.ItemStatus.RESERVED,
                expiresAt = System.currentTimeMillis() + 10L * 60_000L, // 10 min left
            )
            com.giftregistry.ui.common.status.StatusChip(
                status = com.giftregistry.domain.model.ItemStatus.PURCHASED,
                expiresAt = null,
            )
        }
    }
}

@Preview(name = "PulsingDot — 1400ms vs 1000ms", showBackground = true, backgroundColor = 0xFFF7F2E9, widthDp = 200, heightDp = 80)
@Composable
private fun PulsingDotPreview() {
    GiftRegistryTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            com.giftregistry.ui.common.status.PulsingDot(
                color = GiftMaisonTheme.colors.accent,
                size = 8.dp, // upscaled for preview visibility
                period = 1_400.milliseconds,
            )
            com.giftregistry.ui.common.status.PulsingDot(
                color = GiftMaisonTheme.colors.accent,
                size = 8.dp,
                period = 1_000.milliseconds,
            )
        }
    }
}
