package com.giftregistry.ui.registry.detail

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.giftregistry.R
import kotlinx.coroutines.delay

@Composable
fun ReservationCountdown(expiresAtMs: Long, modifier: Modifier = Modifier) {
    var remainingMs by remember(expiresAtMs) {
        mutableLongStateOf((expiresAtMs - System.currentTimeMillis()).coerceAtLeast(0L))
    }
    LaunchedEffect(expiresAtMs) {
        while (remainingMs > 0) {
            delay(1_000L)
            remainingMs = (expiresAtMs - System.currentTimeMillis()).coerceAtLeast(0L)
        }
    }
    val minutes = (remainingMs / 60_000L).toInt()
    val seconds = ((remainingMs % 60_000L) / 1_000L).toInt()
    // D-08: display-only, no Firestore writes.
    Text(
        text = stringResource(R.string.reservation_countdown_format, minutes, seconds),
        modifier = modifier,
    )
}
