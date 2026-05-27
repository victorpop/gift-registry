package com.giftregistry.ui.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.giftregistry.R
import com.giftregistry.ui.theme.GiftMaisonTheme

/**
 * Phase 17 — Discover screen (bottom-nav slot 2).
 *
 * Layout (UI-SPEC "Screen Layout Contract"):
 * - Scaffold with paper background + SnackbarHost
 * - OutlinedTextField (search bar) — IME action Search fires viewModel.search(query)
 * - LazyColumn with two sections:
 *   - FROM THE WEB (only when search is not Idle): Loading/Loaded/Empty/Error
 *   - FROM THE COMMUNITY (always): Loading/Loaded/Empty/Error
 *
 * The search bar does NOT auto-fire on text change — only the IME Search
 * action calls the Callable. This matches D-35 (search section hidden until
 * a query is submitted).
 */
@Composable
fun DiscoverScreen(
    viewModel: DiscoverViewModel = hiltViewModel(),
) {
    val colors = GiftMaisonTheme.colors
    val popular by viewModel.popular.collectAsStateWithLifecycle()
    val search by viewModel.search.collectAsStateWithLifecycle()
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = colors.paper,
    ) { inner ->
        Column(modifier = Modifier.padding(inner).fillMaxWidth()) {
            Box(modifier = Modifier.height(20.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.onQueryChange(it) },
                placeholder = {
                    Text(
                        stringResource(R.string.discover_search_placeholder),
                        style = GiftMaisonTheme.typography.bodyM,
                        color = colors.inkFaint,
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Search,
                        contentDescription = null,
                        tint = colors.inkFaint,
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.accent,
                    unfocusedBorderColor = colors.line,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    viewModel.search(query)
                    focusManager.clearFocus()
                }),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
            Box(modifier = Modifier.height(16.dp))

            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                // FROM THE WEB section — visible only after the user submits a search.
                if (search !is SearchState.Idle) {
                    item { SectionHeader(stringResource(R.string.discover_section_web)) }
                    when (val state = search) {
                        SearchState.Loading -> items(3) { DiscoverShimmerCard() }
                        is SearchState.Loaded -> items(state.products, key = { it.id }) {
                            DiscoverProductCard(product = it, snackbarHostState = snackbarHostState)
                        }
                        SearchState.Empty -> item {
                            EmptyStateText(stringResource(R.string.discover_empty_search))
                        }
                        is SearchState.Error -> item {
                            InlineErrorState(
                                message = stringResource(R.string.discover_error_search),
                                onRetry = { viewModel.retrySearch() },
                            )
                        }
                        SearchState.Idle -> Unit
                    }
                    item {
                        HorizontalDivider(
                            color = colors.line,
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = 12.dp),
                        )
                    }
                }

                // FROM THE COMMUNITY section — always visible.
                item { SectionHeader(stringResource(R.string.discover_section_community)) }
                when (val state = popular) {
                    PopularState.Loading -> items(3) { DiscoverShimmerCard() }
                    is PopularState.Loaded -> items(state.products, key = { it.id }) {
                        DiscoverProductCard(product = it, snackbarHostState = snackbarHostState)
                    }
                    PopularState.Empty -> item {
                        EmptyStateText(stringResource(R.string.discover_empty_popular))
                    }
                    is PopularState.Error -> item {
                        InlineErrorState(
                            message = stringResource(R.string.discover_error_load),
                            onRetry = { viewModel.loadPopular() },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    val colors = GiftMaisonTheme.colors
    val typography = GiftMaisonTheme.typography
    Text(
        text = text,
        style = typography.monoCaps,
        color = colors.inkFaint,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
    )
}

@Composable
private fun EmptyStateText(text: String) {
    val colors = GiftMaisonTheme.colors
    val typography = GiftMaisonTheme.typography
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
    ) {
        Text(
            text,
            style = typography.bodyM,
            color = colors.inkFaint,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun InlineErrorState(message: String, onRetry: () -> Unit) {
    val colors = GiftMaisonTheme.colors
    val typography = GiftMaisonTheme.typography
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Text(
            message,
            style = typography.bodyM,
            color = colors.inkFaint,
            textAlign = TextAlign.Center,
        )
        Box(Modifier.height(8.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.accent,
                contentColor = colors.accentInk,
            ),
            shape = GiftMaisonTheme.shapes.pill,
        ) {
            Text(
                stringResource(R.string.discover_retry),
                style = typography.bodyMEmphasis,
            )
        }
    }
}
