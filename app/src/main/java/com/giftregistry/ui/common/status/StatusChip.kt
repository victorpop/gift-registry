package com.giftregistry.ui.common.status

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.giftregistry.R
import com.giftregistry.domain.model.ItemStatus
import com.giftregistry.ui.theme.GiftMaisonTheme
import kotlinx.coroutines.delay
import kotlin.math.max

/** Display-layer chip types. Domain enum is `ItemStatus` (AVAILABLE/RESERVED/PURCHASED). */
enum class StatusChipType { OPEN, RESERVED, GIVEN }

/**
 * Domain→display mapping (pure Kotlin, testable).
 *
 * CRITICAL: handoff uses "given" but domain uses "purchased" — this function
 * is the single authoritative translator. Do NOT rename the domain enum.
 */
fun statusChipTypeOf(status: ItemStatus): StatusChipType = when (status) {
    ItemStatus.AVAILABLE -> StatusChipType.OPEN
    ItemStatus.RESERVED  -> StatusChipType.RESERVED
    ItemStatus.PURCHASED -> StatusChipType.GIVEN
}

/**
 * STAT-01 countdown helper — pure Kotlin so it's unit-testable without
 * Compose runtime. `expiresAt` is epoch millis per domain `Item.expiresAt`.
 * Returns clamped whole-minute remainder (never negative; truncates down).
 */
fun computeMinutesLeft(
    expiresAt: Long?,
    now: Long = System.currentTimeMillis(),
): Int {
    if (expiresAt == null) return 0
    val diffMs = expiresAt - now
    if (diffMs <= 0L) return 0
    return max(0, (diffMs / 60_000L).toInt())
}

/**
 * STAT-01/02/03 dispatcher — single composable owner screens consume.
 * @param status Domain ItemStatus. Mapped via [statusChipTypeOf].
 * @param expiresAt Epoch millis for RESERVED items; ignored for others.
 */
@Composable
fun StatusChip(
    status: ItemStatus,
    expiresAt: Long?,
    modifier: Modifier = Modifier,
) = when (statusChipTypeOf(status)) {
    StatusChipType.OPEN     -> OpenChip(modifier)
    StatusChipType.RESERVED -> ReservedChip(expiresAt = expiresAt, modifier = modifier)
    StatusChipType.GIVEN    -> GivenChip(modifier)
}

/** STAT-01: filled accent pill with pulsing dot + "Nm" countdown + "RESERVED" label. */
@Composable
fun ReservedChip(expiresAt: Long?, modifier: Modifier = Modifier) {
    val colors = GiftMaisonTheme.colors
    val typography = GiftMaisonTheme.typography
    val shapes = GiftMaisonTheme.shapes

    // Compute initial minutes synchronously on composition (Pitfall 5: don't
    // wait for the first delay tick to set a correct value).
    var minutesLeft by remember(expiresAt) {
        mutableIntStateOf(computeMinutesLeft(expiresAt))
    }
    // Tick once per minute. Cancels automatically when expiresAt changes or
    // composition leaves (no ViewModel ticker — per CONTEXT.md decision).
    LaunchedEffect(expiresAt) {
        if (expiresAt == null) return@LaunchedEffect
        while (true) {
            delay(60_000L)
            minutesLeft = computeMinutesLeft(expiresAt)
        }
    }

    Row(
        modifier = modifier
            .background(colors.accent, shapes.pill)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        PulsingDot(color = colors.accentInk)  // default 4.dp, 1400 ms
        if (expiresAt != null) {
            val countdownText = if (minutesLeft == 0) {
                stringResource(R.string.status_chip_countdown_zero)
            } else {
                stringResource(R.string.status_chip_countdown_template, minutesLeft)
            }
            Text(
                text = countdownText,
                style = typography.bodyS,
                color = colors.accentInk,
            )
        }
        Text(
            text = stringResource(R.string.status_chip_reserved),
            style = typography.monoCaps,
            color = colors.accentInk,
        )
    }
}

/** STAT-02: secondSoft fill, "✓ GIVEN" label. Domain PURCHASED → display GIVEN. */
@Composable
fun GivenChip(modifier: Modifier = Modifier) {
    val colors = GiftMaisonTheme.colors
    val typography = GiftMaisonTheme.typography
    val shapes = GiftMaisonTheme.shapes
    Text(
        text = stringResource(R.string.status_chip_given),
        style = typography.monoCaps,
        color = colors.second,
        modifier = modifier
            .background(colors.secondSoft, shapes.pill)
            .padding(horizontal = 6.dp, vertical = 4.dp),
    )
}

/** STAT-03: transparent outlined pill, inkFaint text, "OPEN" label. */
@Composable
fun OpenChip(modifier: Modifier = Modifier) {
    val colors = GiftMaisonTheme.colors
    val typography = GiftMaisonTheme.typography
    val shapes = GiftMaisonTheme.shapes
    Text(
        text = stringResource(R.string.status_chip_open),
        style = typography.monoCaps,
        color = colors.inkFaint,
        modifier = modifier
            .border(1.dp, colors.line, shapes.pill)
            .padding(horizontal = 6.dp, vertical = 4.dp),
    )
}
