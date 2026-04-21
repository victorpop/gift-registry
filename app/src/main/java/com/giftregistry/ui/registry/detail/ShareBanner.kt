package com.giftregistry.ui.registry.detail

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.giftregistry.R
import com.giftregistry.ui.theme.GiftMaisonTheme
import kotlinx.coroutines.launch

/**
 * SCR-08 — accentSoft pill share banner. Tap: copy `shareUrlOf(registryId)` to
 * clipboard AND launch Intent.ACTION_SEND chooser. Snackbar confirmation fired
 * via onShared callback (RegistryDetailScreen owns the SnackbarHostState).
 */
@Composable
internal fun ShareBanner(
    registryId: String,
    onShared: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GiftMaisonTheme.colors
    val typography = GiftMaisonTheme.typography
    val shapes = GiftMaisonTheme.shapes
    val spacing = GiftMaisonTheme.spacing

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val fullShareUrl = shareUrlOf(registryId)
    // Display without scheme prefix: "gift-registry-ro.web.app/r/abc"
    val displayUrl = fullShareUrl.removePrefix("https://")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.edge, vertical = spacing.gap8)
            .clip(shapes.radius10)
            .background(colors.accentSoft)
            .clickable {
                clipboardManager.setText(AnnotatedString(fullShareUrl))
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, fullShareUrl)
                }
                context.startActivity(Intent.createChooser(intent, null))
                scope.launch { onShared() }
            }
            .padding(horizontal = spacing.gap16, vertical = spacing.gap12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(shapes.radius8)
                .background(colors.accent),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "\u2197",
                style = typography.bodyMEmphasis,
                color = colors.accentInk,
            )
        }
        Spacer(Modifier.width(spacing.gap12))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayUrl,
                style = typography.monoCaps.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = colors.accent,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(spacing.gap4))
            Text(
                text = stringResource(R.string.registry_share_helper),
                style = typography.bodyS,
                color = colors.inkSoft,
            )
        }
    }
}
