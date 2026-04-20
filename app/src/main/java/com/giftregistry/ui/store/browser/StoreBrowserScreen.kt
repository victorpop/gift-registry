package com.giftregistry.ui.store.browser

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.giftregistry.R

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun StoreBrowserScreen(
    onBack: () -> Unit,
    onAddToList: (url: String, registryId: String?) -> Unit,
    viewModel: StoreBrowserViewModel = hiltViewModel(),
) {
    val storeName by viewModel.storeName.collectAsStateWithLifecycle()
    val homepageUrl by viewModel.homepageUrl.collectAsStateWithLifecycle()
    val currentUrl by viewModel.currentUrl.collectAsStateWithLifecycle()
    val pageLoadFailed by viewModel.pageLoadFailed.collectAsStateWithLifecycle()
    val addEnabled by viewModel.addToListEnabled.collectAsStateWithLifecycle()
    val externalBlockedMsg = stringResource(R.string.stores_external_link_blocked)

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var hasLoadedInitialUrl by remember { mutableStateOf(false) }

    // Trigger initial load once homepageUrl arrives from the ViewModel (after GetStoresUseCase
    // resolves). Subsequent recompositions do not reload — D-16: preserve browsing state.
    LaunchedEffect(homepageUrl, webViewRef) {
        val wv = webViewRef
        if (wv != null && homepageUrl.isNotBlank() && !hasLoadedInitialUrl) {
            wv.loadUrl(homepageUrl)
            hasLoadedInitialUrl = true
        }
    }

    // Retry: user taps "Try again" → ViewModel.onRetry() clears pageLoadFailed →
    // this LaunchedEffect calls reload() so the page retries without re-creating WebView.
    LaunchedEffect(pageLoadFailed) {
        if (!pageLoadFailed && hasLoadedInitialUrl) {
            webViewRef?.reload()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(storeName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.stores_webview_close),
                        )
                    }
                },
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .windowInsetsPadding(WindowInsets.navigationBars),
                ) {
                    // TODO(D-10): When LastRegistryPreferencesDataStore + picker ship, use
                    // last-used registryId when entered from Home (currently the Add button is
                    // disabled in that path). For now, users enter Store Browser from Registry
                    // Detail to add — Home-FAB Browse is read-only.
                    Button(
                        onClick = { onAddToList(currentUrl, viewModel.registryId) },
                        enabled = addEnabled && viewModel.registryId != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.stores_add_to_list_cta))
                    }
                }
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            AndroidView(
                factory = { ctx ->
                    // Cookies are process-wide — enable once; idempotent and safe (D-06).
                    CookieManager.getInstance().setAcceptCookie(true)
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true       // D-05
                        settings.domStorageEnabled = true        // required by most Romanian e-commerce sites
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView, url: String) {
                                viewModel.onUrlChanged(url)      // D-07
                            }

                            override fun onReceivedError(
                                view: WebView,
                                request: WebResourceRequest,
                                error: WebResourceError,
                            ) {
                                // D-15 + Pitfall 5 — only treat MAIN frame failures as errors
                                if (request.isForMainFrame) {
                                    viewModel.onPageLoadFailed()
                                }
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView,
                                request: WebResourceRequest,
                            ): Boolean {
                                val scheme = request.url.scheme ?: ""
                                if (scheme != "http" && scheme != "https") {
                                    // D-08 — block non-web schemes; show Toast (see plan note:
                                    // Toast is acceptable MVP; a follow-up can swap to Snackbar
                                    // if UAT flags this).
                                    view.post {
                                        android.widget.Toast.makeText(
                                            view.context,
                                            externalBlockedMsg,
                                            android.widget.Toast.LENGTH_SHORT,
                                        ).show()
                                    }
                                    return true
                                }
                                return false
                            }
                        }
                    }.also { webViewRef = it }
                },
                // update is a no-op — initial load handled in LaunchedEffect to avoid reload
                // on every recomposition (Research Pattern 1 pitfall).
                update = { },
                modifier = Modifier
                    .fillMaxSize()
                    // Keep WebView in composition even during error — preserves instance (UI-SPEC)
                    .alpha(if (pageLoadFailed) 0f else 1f),
            )

            if (pageLoadFailed) {
                WebViewErrorOverlay(
                    onRetry = { viewModel.onRetry() },
                    onBack = onBack,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
}

@Composable
private fun WebViewErrorOverlay(
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.WifiOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.stores_webview_error_heading),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.stores_webview_error_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRetry) {
            Text(stringResource(R.string.stores_retry))
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onBack) {
            Text(stringResource(R.string.common_back))
        }
    }
}
