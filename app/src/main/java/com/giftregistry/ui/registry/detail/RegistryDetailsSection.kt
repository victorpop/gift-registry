package com.giftregistry.ui.registry.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import android.text.format.DateFormat as AndroidDateFormat
import com.giftregistry.R
import com.giftregistry.ui.theme.GiftMaisonTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * quick-260605-fy2 — registry "Details" block, inserted between the hero header
 * and the stats strip. Surfaces event context (when / where / what) for both
 * owners and gift-givers.
 *
 * Renders up to three sub-elements, each gated on its underlying [com.giftregistry.domain.model.Registry]
 * field being populated:
 *  - a date-only pill (`MMM d`, e.g. "May 16") with a leading calendar icon,
 *  - a description paragraph, and
 *  - a location card (pin icon + the location string, as-is).
 *
 * If all three fields are empty/blank the whole block — DETAILS header included —
 * renders nothing (early `return`).
 */
@Composable
internal fun RegistryDetailsSection(
    description: String?,
    eventLocation: String?,
    eventDateMs: Long?,
    modifier: Modifier = Modifier,
) {
    val hasDate = eventDateMs != null
    val hasDescription = !description.isNullOrBlank()
    val hasLocation = !eventLocation.isNullOrBlank()

    // Empty-handling gate: nothing populated -> render NOTHING (header included).
    if (!hasDate && !hasDescription && !hasLocation) return

    val colors = GiftMaisonTheme.colors
    val typography = GiftMaisonTheme.typography
    val spacing = GiftMaisonTheme.spacing
    val shapes = GiftMaisonTheme.shapes

    val context = LocalContext.current
    val is24Hour = AndroidDateFormat.is24HourFormat(context)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.edge, vertical = spacing.gap12),
        verticalArrangement = Arrangement.spacedBy(spacing.gap12),
    ) {
        // DETAILS header — mirrors StatsStrip stat-label style (small-caps).
        Text(
            text = stringResource(R.string.registry_details_section_title),
            style = typography.monoCaps,
            color = colors.inkFaint,
        )

        if (hasDate) {
            Row(
                modifier = Modifier
                    .clip(shapes.pill)
                    .background(colors.accentSoft)
                    .padding(horizontal = spacing.gap12, vertical = spacing.gap8),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarToday,
                    contentDescription = stringResource(R.string.registry_event_date_label),
                    tint = colors.accent,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(spacing.gap8))
                Text(
                    text = formatEventDate(eventDateMs!!, is24Hour),
                    style = typography.bodyMEmphasis,
                    color = colors.accent,
                )
            }
        }

        if (hasDescription) {
            Text(
                text = description!!,
                style = typography.bodyM,
                color = colors.inkSoft,
            )
        }

        if (hasLocation) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shapes.radius12)
                    .background(colors.paperDeep)
                    .border(1.dp, colors.line, shapes.radius12)
                    .padding(spacing.gap12),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Place,
                    contentDescription = stringResource(R.string.registry_event_location_label),
                    tint = colors.inkSoft,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(spacing.gap8))
                Text(
                    text = eventLocation!!,
                    style = typography.bodyM,
                    color = colors.ink,
                )
            }
        }
    }
}

private fun formatEventDate(eventDateMs: Long, is24Hour: Boolean): String {
    val date = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(eventDateMs))
    val calendar = Calendar.getInstance().apply { timeInMillis = eventDateMs }
    val hasTime = calendar.get(Calendar.HOUR_OF_DAY) != 0 || calendar.get(Calendar.MINUTE) != 0
    if (!hasTime) return date
    val timePattern = if (is24Hour) "HH:mm" else "h:mm a"
    val time = SimpleDateFormat(timePattern, Locale.getDefault()).format(Date(eventDateMs))
    return "$date · $time"
}
