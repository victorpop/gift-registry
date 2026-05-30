package com.giftregistry.ui.theme.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.giftregistry.domain.model.Item
import com.giftregistry.domain.model.ItemStatus
import com.giftregistry.domain.model.NotificationType
import com.giftregistry.domain.model.Registry
import androidx.compose.material3.SnackbarHostState
import com.giftregistry.domain.discover.DiscoverProduct
import com.giftregistry.ui.auth.AuthHeadline
import com.giftregistry.ui.auth.GoogleBanner
import com.giftregistry.ui.discover.DiscoverProductCard
import com.giftregistry.ui.discover.DiscoverShimmerCard
import com.giftregistry.ui.item.add.ItemPreviewCard
import com.giftregistry.ui.registry.cover.CoverPhotoPickerInline
import com.giftregistry.ui.registry.cover.CoverPhotoSelection
import com.giftregistry.ui.registry.cover.HeroImageOrPlaceholder
import com.giftregistry.ui.registry.cover.PresetCatalog
import com.giftregistry.ui.registry.cover.PresetThumbnail
import com.giftregistry.ui.registry.create.OccasionTileGrid
import com.giftregistry.ui.registry.detail.FilterChipState
import com.giftregistry.ui.registry.detail.FilterChipsRow
import com.giftregistry.ui.registry.detail.RegistryDetailHero
import com.giftregistry.ui.registry.detail.ShareBanner
import com.giftregistry.ui.registry.detail.StatsStrip
import com.giftregistry.ui.registry.list.RegistryCardPrimary
import com.giftregistry.ui.registry.list.RegistryCardSecondary
import com.giftregistry.ui.registry.list.SegmentedTabs
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
            onHome = {}, onDiscover = {}, onFab = {}, onLists = {}, onYou = {},
        )
    }
}

@Preview(name = "Bottom nav — RegistryDetail selected", showBackground = true, backgroundColor = 0xFFF7F2E9, widthDp = 360, heightDp = 80)
@Composable
private fun BottomNavListsSelectedPreview() {
    GiftRegistryTheme {
        com.giftregistry.ui.common.chrome.GiftMaisonBottomNav(
            currentKey = com.giftregistry.ui.navigation.RegistryDetailKey(registryId = "preview"),
            onHome = {}, onDiscover = {}, onFab = {}, onLists = {}, onYou = {},
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

// ─────────────────────────────────────────────────────────────────────────
// Phase 10 — Onboarding + Home Redesign preview sections (SCR-06 + SCR-07)
// ─────────────────────────────────────────────────────────────────────────

@Preview(name = "Auth headline", showBackground = true, backgroundColor = 0xFFF7F2E9, widthDp = 360, heightDp = 140)
@Composable
private fun AuthHeadlinePreview() {
    GiftRegistryTheme {
        Box(
            modifier = Modifier
                .background(GiftMaisonTheme.colors.paper)
                .padding(16.dp),
        ) {
            AuthHeadline()
        }
    }
}

@Preview(name = "Google banner (SCR-06)", showBackground = true, backgroundColor = 0xFFF7F2E9, widthDp = 360, heightDp = 120)
@Composable
private fun GoogleBannerPreview() {
    GiftRegistryTheme {
        Box(
            modifier = Modifier
                .background(GiftMaisonTheme.colors.paper)
                .padding(16.dp),
        ) {
            GoogleBanner(onClick = {})
        }
    }
}

@Preview(name = "Segmented tabs — both selected states", showBackground = true, backgroundColor = 0xFFF7F2E9, widthDp = 360, heightDp = 220)
@Composable
private fun SegmentedTabsPreview() {
    GiftRegistryTheme {
        val tabs = listOf("ACTIVE", "PAST")
        Column(
            modifier = Modifier
                .background(GiftMaisonTheme.colors.paper)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SegmentedTabs(tabs = tabs, selectedIndex = 0, onTabSelected = {})
            SegmentedTabs(tabs = tabs, selectedIndex = 1, onTabSelected = {})
        }
    }
}

private val previewRegistry: Registry = Registry(
    id = "preview-1",
    ownerId = "owner-1",
    title = "Ana's housewarming",
    occasion = "Housewarming",
    visibility = "public",
    eventDateMs = 1_780_000_000_000L,
    eventLocation = "Bucharest",
    description = null,
    imageUrl = null,  // null exercises the paperDeep placeholder path
    createdAt = 1_740_000_000_000L,
    updatedAt = 1_745_000_000_000L,
)

@Preview(name = "Registry card — primary (ink bg, 70% image)", showBackground = true, backgroundColor = 0xFFF7F2E9, widthDp = 360, heightDp = 320)
@Composable
private fun RegistryCardPrimaryPreview() {
    GiftRegistryTheme {
        Box(
            modifier = Modifier
                .background(GiftMaisonTheme.colors.paper)
                .padding(16.dp),
        ) {
            RegistryCardPrimary(
                registry = previewRegistry,
                onClick = {},
                onLongClick = {},
            )
        }
    }
}

@Preview(name = "Registry card — secondary (paperDeep + line border)", showBackground = true, backgroundColor = 0xFFF7F2E9, widthDp = 360, heightDp = 320)
@Composable
private fun RegistryCardSecondaryPreview() {
    GiftRegistryTheme {
        Box(
            modifier = Modifier
                .background(GiftMaisonTheme.colors.paper)
                .padding(16.dp),
        ) {
            RegistryCardSecondary(
                registry = previewRegistry,
                onClick = {},
                onLongClick = {},
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
// Phase 11 — Registry Detail / Create / Add Item preview sections
// SCR-08 + SCR-09 + SCR-10
// ─────────────────────────────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFFF7F2E9, widthDp = 360, heightDp = 180)
@Composable
private fun HeroPlaceholderHousewarmingPreview() {
    GiftRegistryTheme {
        RegistryDetailHero(
            registry = Registry(id = "r1", title = "Our new home", occasion = "Housewarming", imageUrl = null),
            listState = rememberLazyListState(),
            onBack = {},
            onShare = {},
            onOverflow = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF7F2E9, widthDp = 360, heightDp = 180)
@Composable
private fun HeroPlaceholderWeddingPreview() {
    GiftRegistryTheme {
        RegistryDetailHero(
            registry = Registry(id = "r2", title = "Ana & Radu", occasion = "Wedding", imageUrl = null),
            listState = rememberLazyListState(),
            onBack = {},
            onShare = {},
            onOverflow = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF7F2E9, widthDp = 360, heightDp = 80)
@Composable
private fun StatsStripPreview() {
    GiftRegistryTheme {
        StatsStrip(items = buildList {
            repeat(9) { add(Item(id = "i$it", registryId = "r", title = "item $it", status = ItemStatus.AVAILABLE)) }
            add(Item(id = "r1", registryId = "r", title = "res1", status = ItemStatus.RESERVED))
            add(Item(id = "r2", registryId = "r", title = "res2", status = ItemStatus.RESERVED))
            add(Item(id = "p1", registryId = "r", title = "given", status = ItemStatus.PURCHASED))
        })
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF7F2E9, widthDp = 360)
@Composable
private fun ShareBannerPreview() {
    GiftRegistryTheme {
        ShareBanner(registryId = "abc123", onShared = {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF7F2E9, widthDp = 360)
@Composable
private fun FilterChipRowPreview() {
    GiftRegistryTheme {
        FilterChipsRow(
            items = buildList {
                repeat(9) { add(Item(id = "i$it", registryId = "r", title = "item $it", status = ItemStatus.AVAILABLE)) }
                add(Item(id = "r1", registryId = "r", status = ItemStatus.RESERVED))
                add(Item(id = "r2", registryId = "r", status = ItemStatus.RESERVED))
                add(Item(id = "p1", registryId = "r", status = ItemStatus.PURCHASED))
            },
            activeFilter = FilterChipState.All,
            onFilterSelected = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF7F2E9, widthDp = 360, heightDp = 400)
@Composable
private fun OccasionTileGridPreview() {
    GiftRegistryTheme {
        OccasionTileGrid(selectedOccasion = "Housewarming", onOccasionSelected = {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF7F2E9, widthDp = 360, heightDp = 80)
@Composable
private fun AddItemSegmentedTabsPreview() {
    GiftRegistryTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            SegmentedTabs(
                tabs = listOf("Paste URL", "Browse stores", "Manual"),
                selectedIndex = 0,
                onTabSelected = {},
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF7F2E9, widthDp = 360, heightDp = 140)
@Composable
private fun ItemPreviewCardPreview() {
    GiftRegistryTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            ItemPreviewCard(
                imageUrl = "",  // preview-only: no network; placeholder fills the 80x80 box
                title = "Philips HD9200/90 Airfryer",
                price = "189",
                url = "https://emag.ro/philips-airfryer-product/12345",
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
// Phase 12 — Registry Cover Photo + Themed Placeholder preview sections
// D-09 / D-10 / D-12 / D-14 / D-15 / D-16
// ─────────────────────────────────────────────────────────────────────────

@Preview(
    name = "HeroImageOrPlaceholder — hero (40 sp) + card (32 sp)",
    showBackground = true,
    backgroundColor = 0xFFF7F2E9,
    widthDp = 360,
    heightDp = 420,
)
@Composable
private fun HeroImageOrPlaceholderPreview() {
    GiftRegistryTheme {
        Column(
            modifier = Modifier
                .background(GiftMaisonTheme.colors.paper)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Hero reference (40 sp glyph) — matches RegistryDetailHero pixel contract.
            HeroImageOrPlaceholder(
                imageUrl = null,
                occasion = "Wedding",
                glyphSize = 40.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
            )
            // Card reference (32 sp glyph) — matches RegistryCardPrimary/Secondary 16:9 image area.
            HeroImageOrPlaceholder(
                imageUrl = null,
                occasion = "Birthday",
                glyphSize = 32.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
            )
        }
    }
}

@Preview(
    name = "CoverPhotoPickerInline — 3 states",
    showBackground = true,
    backgroundColor = 0xFFF7F2E9,
    widthDp = 360,
    heightDp = 720,
)
@Composable
private fun CoverPhotoPickerInlinePreview() {
    GiftRegistryTheme {
        val typography = GiftMaisonTheme.typography
        val colors = GiftMaisonTheme.colors
        Column(
            modifier = Modifier
                .background(colors.paper)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Disabled (no occasion)", style = typography.monoCaps, color = colors.inkFaint)
            CoverPhotoPickerInline(
                occasion = null,
                selection = CoverPhotoSelection.None,
                onTap = {},
                disabledHint = "Pick an occasion to see suggested covers",
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Enabled + None (Wedding)", style = typography.monoCaps, color = colors.inkFaint)
            CoverPhotoPickerInline(
                occasion = "Wedding",
                selection = CoverPhotoSelection.None,
                onTap = {},
                disabledHint = "Pick an occasion to see suggested covers",
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Enabled + Preset (Wedding 1)", style = typography.monoCaps, color = colors.inkFaint)
            CoverPhotoPickerInline(
                occasion = "Wedding",
                selection = CoverPhotoSelection.Preset(occasion = "Wedding", index = 1),
                onTap = {},
                disabledHint = "Pick an occasion to see suggested covers",
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(
    name = "CoverPhotoPickerSheet (Wedding, 6 presets, first selected)",
    showBackground = true,
    backgroundColor = 0xFFF7F2E9,
    widthDp = 360,
    heightDp = 600,
)
@Composable
private fun CoverPhotoPickerSheetPreview() {
    GiftRegistryTheme {
        val colors = GiftMaisonTheme.colors
        val typography = GiftMaisonTheme.typography
        val shapes = GiftMaisonTheme.shapes
        // Render the sheet body inline as a Column — ModalBottomSheet itself
        // does not render in @Preview, but the body composition is identical
        // so visual contract is reviewable here.
        Column(
            modifier = Modifier
                .background(colors.paper)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "CHOOSE A COVER",
                style = typography.monoCaps,
                color = colors.inkFaint,
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                userScrollEnabled = false,
            ) {
                itemsIndexed(PresetCatalog.presetsFor("Wedding")) { index, drawableId ->
                    PresetThumbnail(
                        drawableId = drawableId,
                        selected = index == 0,
                        onClick = {},
                    )
                }
            }
            Button(
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                shape = shapes.pill,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.ink,
                    contentColor = colors.paper,
                ),
            ) {
                Text(text = "Pick from gallery", style = typography.bodyMEmphasis)
            }
            TextButton(
                onClick = {},
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(
                    text = "Remove cover photo",
                    style = typography.bodyM,
                    color = colors.inkSoft,
                )
            }
        }
    }
}

@Preview(
    name = "RegistryCard placeholder — Primary (Wedding) + Secondary (Baby)",
    showBackground = true,
    backgroundColor = 0xFFF7F2E9,
    widthDp = 360,
    heightDp = 600,
)
@Composable
private fun RegistryCardPlaceholdersPreview() {
    GiftRegistryTheme {
        Column(
            modifier = Modifier
                .background(GiftMaisonTheme.colors.paper)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // D-15 visible-bug regression check: imageUrl == null must render
            // gradient + glyph placeholder (was empty box pre-Phase-12).
            RegistryCardPrimary(
                registry = Registry(
                    id = "1",
                    title = "Sarah's Wedding",
                    occasion = "Wedding",
                    imageUrl = null,
                ),
                onClick = {},
                onLongClick = {},
            )
            RegistryCardSecondary(
                registry = Registry(
                    id = "2",
                    title = "Welcome Baby Lia",
                    occasion = "Baby",
                    imageUrl = null,
                ),
                onClick = {},
                onLongClick = {},
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
// Phase 16 — Notifications Inbox + Invite Accept/Decline preview sections
// D-09 re-skin + D-25 new types (InviteResponseSheet preview deferred —
// ModalBottomSheet does not render statically in @Preview without a complex
// harness; on-device UAT in Plan 16-06 covers the sheet visuals.)
// ─────────────────────────────────────────────────────────────────────────

@Preview(
    name = "Phase 16 — NotificationsInbox (mixed read/unread + 3 new types)",
    showBackground = true,
    backgroundColor = 0xFFF7F2E9,
    widthDp = 360,
    heightDp = 600,
)
@Composable
private fun NotificationsInboxPreview() {
    GiftRegistryTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(GiftMaisonTheme.colors.paper),
        ) {
            Column {
                PreviewNotificationRow(
                    type = NotificationType.INVITE,
                    title = "Maria invited you to \"Sara's birthday party\"",
                    body = "Tap to view Sara's birthday party",
                    timestampLabel = "3M AGO",
                    isUnread = true,
                )
                PreviewNotificationRow(
                    type = NotificationType.INVITE_ACCEPTED_SELF,
                    title = "You joined \"Sara's birthday party\"",
                    body = "Tap to view Sara's birthday party",
                    timestampLabel = "30M AGO",
                    isUnread = false,
                )
                PreviewNotificationRow(
                    type = NotificationType.INVITE_ACCEPTED,
                    title = "Andrei accepted your invite to \"Housewarming\"",
                    body = "Housewarming",
                    timestampLabel = "1H AGO",
                    isUnread = true,
                )
                PreviewNotificationRow(
                    type = NotificationType.RESERVATION_CREATED,
                    title = "Andrei reserved \"Coffee maker\"",
                    body = "Andrei reserved Coffee maker on Sara's list",
                    timestampLabel = "1H AGO",
                    isUnread = true,
                )
                PreviewNotificationRow(
                    type = NotificationType.ITEM_PURCHASED,
                    title = "\"Coffee maker\" was purchased",
                    body = "Someone bought Coffee maker from Sara's list",
                    timestampLabel = "2H AGO",
                    isUnread = false,
                )
                PreviewNotificationRow(
                    type = NotificationType.INVITE_DECLINED,
                    title = "Alex declined your invite to \"Housewarming\"",
                    body = "Housewarming",
                    timestampLabel = "1D AGO",
                    isUnread = false,
                )
            }
        }
    }
}

@Preview(
    name = "Phase 16 — NotificationsInbox (empty state)",
    showBackground = true,
    backgroundColor = 0xFFF7F2E9,
    widthDp = 360,
    heightDp = 400,
)
@Composable
private fun NotificationsInboxEmptyPreview() {
    GiftRegistryTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(GiftMaisonTheme.colors.paper),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "No notifications yet",
                    style = GiftMaisonTheme.typography.displayS,
                    color = GiftMaisonTheme.colors.ink,
                )
                Spacer(Modifier.height(GiftMaisonTheme.spacing.gap8))
                Text(
                    text = "When someone invites you to a registry or reserves a gift, you'll see it here.",
                    style = GiftMaisonTheme.typography.bodyS,
                    color = GiftMaisonTheme.colors.inkSoft,
                    modifier = Modifier.widthIn(max = 280.dp),
                )
            }
        }
    }
}

/**
 * Visual atom mirroring NotificationCard for offline review.
 * Kept private + preview-only — the real card lives in NotificationsScreen.kt.
 */
@Composable
private fun PreviewNotificationRow(
    type: NotificationType,
    title: String,
    body: String,
    timestampLabel: String,
    isUnread: Boolean,
) {
    val colors = GiftMaisonTheme.colors
    val typography = GiftMaisonTheme.typography
    val spacing = GiftMaisonTheme.spacing
    val titleColor = if (isUnread) colors.ink else colors.inkSoft
    val iconTint = if (isUnread) colors.accent else colors.inkSoft

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.gap16, vertical = spacing.gap14),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = when (type) {
                    NotificationType.INVITE -> Icons.Filled.Mail
                    NotificationType.INVITE_ACCEPTED_SELF -> Icons.Filled.CheckCircle
                    NotificationType.INVITE_ACCEPTED -> Icons.Filled.CheckCircle
                    NotificationType.INVITE_DECLINED -> Icons.Filled.Block
                    NotificationType.RESERVATION_CREATED -> Icons.Filled.Bookmark
                    NotificationType.ITEM_PURCHASED -> Icons.Filled.CheckCircle
                    else -> Icons.Filled.Notifications
                },
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(spacing.gap12))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = typography.bodyL,
                        color = titleColor,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(spacing.gap8))
                    Text(
                        text = timestampLabel,
                        style = typography.monoCaps,
                        color = colors.inkSoft,
                    )
                    if (isUnread) {
                        Spacer(Modifier.width(spacing.gap4))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(colors.accent),
                        )
                    }
                }
                Spacer(Modifier.height(spacing.gap4))
                Text(
                    text = body,
                    style = typography.bodyM,
                    color = colors.inkSoft,
                )
            }
        }
        HorizontalDivider(color = colors.line, thickness = 1.dp)
    }
}

// ─────────────────────────────────────────────────────────────────────────
// Phase 17 — Discover screen preview sections (D-01, D-02, D-32..D-42)
// Renders all 5 interaction states inline so a Compose preview pane can
// validate the visual contract without a device.
// ─────────────────────────────────────────────────────────────────────────

private val discoverPreviewProducts = listOf(
    DiscoverProduct(
        id = "f1",
        title = "Aparat de cafea espresso DeLonghi",
        description = "15 bar, lapte spumat automat",
        imageUrl = "",
        price = 1299.0,
        currency = "RON",
        retailerUrl = "https://emag.ro/x",
    ),
    DiscoverProduct(
        id = "f2",
        title = "Cană termică YETI Rambler 20oz",
        description = "Stainless steel, vacuum insulated",
        imageUrl = "",
        price = 199.99,
        currency = "RON",
        retailerUrl = "https://yeti.com/y",
    ),
)

@Preview(
    name = "Phase 17 — Discover (5 states)",
    showBackground = true,
    backgroundColor = 0xFFF7F2E9,
    widthDp = 390,
    heightDp = 1600,
)
@Composable
private fun DiscoverPreview() {
    GiftRegistryTheme {
        val colors = GiftMaisonTheme.colors
        val typography = GiftMaisonTheme.typography
        val snackbar = remember { SnackbarHostState() }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.paper),
        ) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                item {
                    Text(
                        "1. IDLE — community populated",
                        style = typography.monoCaps,
                        color = colors.inkFaint,
                    )
                }
                item {
                    Text(
                        "FROM THE COMMUNITY",
                        style = typography.monoCaps,
                        color = colors.inkFaint,
                    )
                }
                items(discoverPreviewProducts) { product ->
                    DiscoverProductCard(product = product, snackbarHostState = snackbar)
                }

                item {
                    Text(
                        "2. LOADING — 3 shimmer skeletons",
                        style = typography.monoCaps,
                        color = colors.inkFaint,
                    )
                }
                items(3) { DiscoverShimmerCard() }

                item {
                    Text(
                        "3. LOADED — both sections",
                        style = typography.monoCaps,
                        color = colors.inkFaint,
                    )
                }
                item {
                    Text(
                        "FROM THE WEB",
                        style = typography.monoCaps,
                        color = colors.inkFaint,
                    )
                }
                item {
                    DiscoverProductCard(
                        product = discoverPreviewProducts[0],
                        snackbarHostState = snackbar,
                    )
                }
                item { HorizontalDivider(color = colors.line, thickness = 1.dp) }
                item {
                    Text(
                        "FROM THE COMMUNITY",
                        style = typography.monoCaps,
                        color = colors.inkFaint,
                    )
                }
                item {
                    DiscoverProductCard(
                        product = discoverPreviewProducts[1],
                        snackbarHostState = snackbar,
                    )
                }

                item {
                    Text(
                        "4. EMPTY — no matches",
                        style = typography.monoCaps,
                        color = colors.inkFaint,
                    )
                }
                item {
                    Text(
                        "No matches found. Try a different search.",
                        style = typography.bodyM,
                        color = colors.inkFaint,
                    )
                }

                item {
                    Text(
                        "5. ERROR — load failed + Retry",
                        style = typography.monoCaps,
                        color = colors.inkFaint,
                    )
                }
                item {
                    Text(
                        "Could not load. Try again.",
                        style = typography.bodyM,
                        color = colors.inkFaint,
                    )
                }
            }
        }
    }
}

// 260530-ncw: 2-col layout preview — validates grid tiling, full-width section headers,
// and shimmer skeleton at GridCells.Fixed(2). Leave the 1-col preview above as regression ref.
@Preview(
    name = "Phase 17 — Discover (2-col)",
    showBackground = true,
    backgroundColor = 0xFFF7F2E9,
    widthDp = 390,
    heightDp = 1200,
)
@Composable
private fun DiscoverTwoColumnPreview() {
    GiftRegistryTheme {
        val colors = GiftMaisonTheme.colors
        val typography = GiftMaisonTheme.typography
        val snackbar = remember { SnackbarHostState() }
        val fourProducts = discoverPreviewProducts + discoverPreviewProducts
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.paper),
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        "FROM THE COMMUNITY",
                        style = typography.monoCaps,
                        color = colors.inkFaint,
                    )
                }
                items(items = fourProducts, key = { "${it.id}-${fourProducts.indexOf(it)}" }) { product ->
                    DiscoverProductCard(product = product, snackbarHostState = snackbar)
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        "FROM THE WEB",
                        style = typography.monoCaps,
                        color = colors.inkFaint,
                    )
                }
                items(items = discoverPreviewProducts, key = { it.id }) { product ->
                    DiscoverProductCard(product = product, snackbarHostState = snackbar)
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        "FROM THE WEB — shimmer",
                        style = typography.monoCaps,
                        color = colors.inkFaint,
                    )
                }
                items(2) { DiscoverShimmerCard() }
            }
        }
    }
}
