package com.giftregistry.ui.common.chrome

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.giftregistry.R
import com.giftregistry.ui.navigation.HomeKey
import com.giftregistry.ui.navigation.RegistryDetailKey
import com.giftregistry.ui.theme.GiftMaisonTheme

private enum class NavSlotId { HOME, STORES, FAB, LISTS, YOU }

/**
 * CHROME-01: 5-slot bottom nav — Home · Stores · [FAB] · Lists · You.
 *
 * Visibility predicate `Any?.showsBottomNav()` lives in NavVisibility.kt
 * (same package). Plan 02 shipped it as a stub; this plan adds the full
 * bottom nav composable alongside it.
 *
 * @param currentKey Current back-stack head — drives selected-slot highlighting
 *   per UI-SPEC mapping: HomeKey → Home, RegistryDetailKey → Lists, else → none.
 * @param onFab Tap on the centre FAB — Plan 04 wires this to open AddActionSheet.
 */
@Composable
fun GiftMaisonBottomNav(
    currentKey: Any?,
    onHome: () -> Unit,
    onStores: () -> Unit,
    onFab: () -> Unit,
    onLists: () -> Unit,
    onYou: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GiftMaisonTheme.colors
    val selected: NavSlotId? = when (currentKey) {
        is HomeKey -> NavSlotId.HOME
        is RegistryDetailKey -> NavSlotId.LISTS
        else -> null
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.paper)
            .border(width = 1.dp, color = colors.line)
            .navigationBarsPadding()
            .padding(top = 4.dp, bottom = 6.dp)
            .height(72.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NavItemSlot(
            icon = Icons.Outlined.Home,
            labelRes = R.string.nav_home_tab,
            isSelected = selected == NavSlotId.HOME,
            onClick = onHome,
            modifier = Modifier.weight(1f),
        )
        NavItemSlot(
            icon = Icons.Outlined.Storefront,
            labelRes = R.string.nav_stores_tab,
            isSelected = selected == NavSlotId.STORES,
            onClick = onStores,
            modifier = Modifier.weight(1f),
        )
        FabSlot(
            onClick = onFab,
            modifier = Modifier.weight(1f),
        )
        NavItemSlot(
            icon = Icons.AutoMirrored.Outlined.List,
            labelRes = R.string.nav_lists_tab,
            isSelected = selected == NavSlotId.LISTS,
            onClick = onLists,
            modifier = Modifier.weight(1f),
        )
        NavItemSlot(
            icon = Icons.Outlined.Person,
            labelRes = R.string.nav_you_tab,
            isSelected = selected == NavSlotId.YOU,
            onClick = onYou,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * Non-FAB slot: icon pill (accentSoft background when selected) + monoCaps label.
 * Uses Material Symbols Outlined variant (stroked, per CONTEXT.md discretion).
 * 44 dp touch-target floor on the pill Box (accessibility requirement).
 */
@Composable
private fun NavItemSlot(
    icon: ImageVector,
    labelRes: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GiftMaisonTheme.colors
    val typography = GiftMaisonTheme.typography
    val shapes = GiftMaisonTheme.shapes
    val iconTint = if (isSelected) colors.accent else colors.inkFaint
    val labelTint = if (isSelected) colors.accent else colors.inkFaint
    val pillBg: Color = if (isSelected) colors.accentSoft else Color.Transparent

    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp) // 44 dp touch target floor (accessibility)
                .clip(shapes.pill)
                .background(pillBg, shapes.pill),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = stringResource(labelRes),
                tint = iconTint,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(labelRes),
            style = typography.monoCaps.copy(
                fontWeight = if (isSelected) FontWeight.SemiBold else typography.monoCaps.fontWeight,
            ),
            color = labelTint,
            maxLines = 1,
            softWrap = false,
        )
    }
}

/**
 * FAB slot: 44 dp invisible scaffold (mirrors NavItemSlot icon-pill footprint
 * so the ADD label aligns with the other nav labels) wrapping a 54 dp FAB
 * that sits flush within the bar (no upward lift) + "ADD" caption.
 *
 * The FAB used to lift 22 dp above the bar per handoff JSX (`top: -22`), but
 * on-device review showed the plus icon getting crossed by the bar's top
 * border line. User feedback 2026-04-27 (quick-260427-nkn): keep the FAB
 * inside the bar, no protrusion above the gray line.
 */
@Composable
private fun FabSlot(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GiftMaisonTheme.colors
    val typography = GiftMaisonTheme.typography
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // 44 dp invisible scaffold — matches NavItemSlot icon-pill footprint so the
        // column wraps to the same height (~62 dp), aligning the ADD label with the
        // other four nav labels. The FAB visually overflows this footprint via
        // requiredSize(54.dp) (5 dp top/bottom overflow), but stays fully inside
        // the bar — see modifier comment below for vertical-position math.
        Box(
            modifier = Modifier.size(44.dp),
            contentAlignment = Alignment.Center,
        ) {
            GiftMaisonFab(
                onClick = onClick,
                // requiredSize forces 54 dp visual; FAB centers in 44 dp Box,
                // overflowing 5 dp top/bottom. With Row padding(top=4.dp) outside
                // the 72.dp height, the FAB top lands 4 dp below the gray border —
                // fully inside the bar, no protrusion. (Removed handoff 22 dp lift
                // per user feedback 2026-04-27 — looked crossed by border line.)
                modifier = Modifier.requiredSize(54.dp),
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.nav_fab_add),
            style = typography.monoCaps,
            color = colors.inkFaint,
            maxLines = 1,
            softWrap = false,
        )
    }
}
