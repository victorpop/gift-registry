package com.giftregistry.ui.onboarding

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.giftregistry.R
import com.giftregistry.ui.auth.AuthScreen
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel = hiltViewModel()) {
    val pagerState = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()

    // Mark seen when the pager settles on page 4 (AuthScreen). settledPage updates
    // only once the swipe animation fully completes, avoiding a premature write
    // during partial drags.
    LaunchedEffect(pagerState.settledPage) {
        if (pagerState.settledPage == 3) viewModel.markSeen()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            when (page) {
                0 -> OnboardingSlide(
                    titleRes = R.string.onboarding_slide_1_title,
                    bodyRes = R.string.onboarding_slide_1_body,
                    illustration = Icons.Default.FormatListBulleted,
                )
                1 -> OnboardingSlide(
                    titleRes = R.string.onboarding_slide_2_title,
                    bodyRes = R.string.onboarding_slide_2_body,
                    illustration = Icons.Default.Link,
                )
                2 -> OnboardingSlide(
                    titleRes = R.string.onboarding_slide_3_title,
                    bodyRes = R.string.onboarding_slide_3_body,
                    illustration = Icons.Default.CardGiftcard,
                )
                3 -> AuthScreen()
            }
        }

        // Overlay: Skip button + page indicator dots are rendered ONLY on pages 0..2.
        // On page 3 (AuthScreen) the existing composable owns the full viewport with
        // no overlay, preserving its Scaffold / Snackbar / Credential Manager UX.
        if (pagerState.currentPage < 3) {
            TextButton(
                onClick = {
                    viewModel.markSeen()
                    scope.launch { pagerState.animateScrollToPage(3) }
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 16.dp)
                    .defaultMinSize(minHeight = 44.dp),
            ) {
                Text(
                    text = stringResource(R.string.onboarding_skip),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                repeat(3) { index ->
                    val selected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .size(if (selected) 10.dp else 8.dp)
                            .background(
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                },
                                shape = CircleShape,
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingSlide(
    @StringRes titleRes: Int,
    @StringRes bodyRes: Int,
    illustration: ImageVector,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = illustration,
                contentDescription = null,
                modifier = Modifier.size(96.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(48.dp))
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(bodyRes),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        // Leaves room so body text never collides with the bottom dot indicator.
        Spacer(Modifier.height(64.dp))
    }
}
