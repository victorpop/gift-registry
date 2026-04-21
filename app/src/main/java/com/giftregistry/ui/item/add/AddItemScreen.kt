package com.giftregistry.ui.item.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.giftregistry.R
import com.giftregistry.ui.navigation.hiltViewModelWithNavArgs
import com.giftregistry.ui.registry.list.SegmentedTabs
import com.giftregistry.ui.theme.GiftMaisonTheme
import java.net.URI

@Composable
fun AddItemScreen(
    registryId: String,
    initialUrl: String? = null,
    initialRegistryId: String? = null,
    onBack: () -> Unit,
    onNavigateToBrowseStores: (String) -> Unit = {},   // NEW — Phase 11
    viewModel: AddItemViewModel = hiltViewModelWithNavArgs(
        key = registryId,
        "registryId" to registryId,
        "initialUrl" to (initialUrl ?: ""),
        "initialRegistryId" to (initialRegistryId ?: ""),
    )
) {
    // --- VM state collection (PRESERVED + NEW Phase 11 derived flows) ---
    val url by viewModel.url.collectAsStateWithLifecycle()
    val title by viewModel.title.collectAsStateWithLifecycle()
    val imageUrl by viewModel.imageUrl.collectAsStateWithLifecycle()
    val price by viewModel.price.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val isFetchingOg by viewModel.isFetchingOg.collectAsStateWithLifecycle()
    val ogFetchFailed by viewModel.ogFetchFailed.collectAsStateWithLifecycle()
    val ogFetchSucceeded by viewModel.ogFetchSucceeded.collectAsStateWithLifecycle()
    val isAffiliateDomain by viewModel.isAffiliateDomain.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val savedItemId by viewModel.savedItemId.collectAsStateWithLifecycle()

    // --- Tab state (NEW — rememberSaveable Int-backed per Phase 10 precedent) ---
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(ADD_ITEM_MODE_DEFAULT_ORDINAL) }
    val selectedTab = AddItemMode.entries[selectedTabIndex]

    // --- Skip save-and-return behaviour controlled by flag to distinguish Add-another vs Save ---
    var addAnotherMode by remember { mutableStateOf(false) }

    // --- LaunchedEffect on savedItemId (EXTENDED — branch on addAnotherMode) ---
    LaunchedEffect(savedItemId) {
        if (savedItemId != null) {
            if (addAnotherMode) {
                viewModel.onResetForm()
                addAnotherMode = false
            } else {
                onBack()
            }
        }
    }

    // --- Browse stores tab triggers navigation + resets tab to PasteUrl before leaving ---
    LaunchedEffect(selectedTab) {
        if (selectedTab == AddItemMode.BrowseStores) {
            selectedTabIndex = ADD_ITEM_MODE_DEFAULT_ORDINAL   // reset before navigating so re-entry shows URL tab
            onNavigateToBrowseStores(registryId)
        }
    }

    val colors = GiftMaisonTheme.colors
    val spacing = GiftMaisonTheme.spacing
    val typography = GiftMaisonTheme.typography

    val domain = remember(url) {
        runCatching { URI(url).host }.getOrNull()?.removePrefix("www.") ?: ""
    }
    val showAffiliateRow = shouldShowAffiliateRow(
        url = url,
        isAffiliateDomain = isAffiliateDomain,
        ogFetchSucceeded = ogFetchSucceeded,
    )

    Scaffold(
        containerColor = colors.paper,
        topBar = {
            // × close top bar — Row layout per handoff: close icon left, title centered, balance box right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.gap8),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.add_item_close_cd),
                        tint = colors.ink,
                    )
                }
                Text(
                    text = stringResource(R.string.item_add_title),
                    style = typography.bodyL,
                    color = colors.ink,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                // Balance box — mirrors IconButton width so title is truly centred
                Box(Modifier.size(48.dp))
            }
        },
        bottomBar = {
            AddItemDualCtaBar(
                isSaving = isSaving,
                isFetching = isFetchingOg,
                onAddAnother = {
                    addAnotherMode = true
                    viewModel.onSave()
                },
                onSaveAndExit = {
                    addAnotherMode = false
                    viewModel.onSave()
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            // 3-tab segmented control (reuses Phase 10 SegmentedTabs)
            Box(
                modifier = Modifier.padding(
                    horizontal = spacing.edge,
                    vertical = spacing.gap12,
                )
            ) {
                SegmentedTabs(
                    tabs = listOf(
                        stringResource(R.string.add_item_tab_url),
                        stringResource(R.string.add_item_tab_browse),
                        stringResource(R.string.add_item_tab_manual),
                    ),
                    selectedIndex = selectedTabIndex,
                    onTabSelected = { selectedTabIndex = it },
                )
            }

            when (selectedTab) {
                AddItemMode.PasteUrl -> PasteUrlModeContent(
                    viewModel = viewModel,
                    url = url,
                    title = title,
                    imageUrl = imageUrl,
                    price = price,
                    notes = notes,
                    isFetchingOg = isFetchingOg,
                    ogFetchFailed = ogFetchFailed,
                    ogFetchSucceeded = ogFetchSucceeded,
                    showAffiliateRow = showAffiliateRow,
                    domain = domain,
                )
                AddItemMode.BrowseStores -> {
                    // No content — navigation fires via LaunchedEffect(selectedTab) above
                }
                AddItemMode.Manual -> ManualModeContent(
                    viewModel = viewModel,
                    title = title,
                    imageUrl = imageUrl,
                    price = price,
                    notes = notes,
                )
            }

            error?.let { err ->
                Text(
                    text = err,
                    style = typography.bodyS,
                    color = colors.warn,
                    modifier = Modifier.padding(horizontal = spacing.edge, vertical = spacing.gap8),
                )
            }
        }
    }
}

@Composable
private fun PasteUrlModeContent(
    viewModel: AddItemViewModel,
    url: String,
    title: String,
    imageUrl: String,
    price: String,
    notes: String,
    isFetchingOg: Boolean,
    ogFetchFailed: Boolean,
    ogFetchSucceeded: Boolean,
    showAffiliateRow: Boolean,
    domain: String,
) {
    val colors = GiftMaisonTheme.colors
    val spacing = GiftMaisonTheme.spacing
    val typography = GiftMaisonTheme.typography
    val shapes = GiftMaisonTheme.shapes

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.edge),
        verticalArrangement = Arrangement.spacedBy(spacing.gap12),
    ) {
        // URL field
        OutlinedTextField(
            value = url,
            onValueChange = { viewModel.onUrlChanged(it) },
            label = { Text(stringResource(R.string.item_url_label)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            shape = shapes.radius12,
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = { viewModel.onFetchMetadata() }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = colors.accent,
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = giftMaisonFieldColors(),
        )

        if (isFetchingOg && domain.isNotBlank()) {
            FetchingIndicator(domain = domain)
        } else if (ogFetchFailed) {
            Text(
                text = stringResource(R.string.item_og_fetch_failed_inline),
                style = typography.bodyS,
                color = colors.inkSoft,
            )
        }

        if (showAffiliateRow) {
            AffiliateConfirmationRow(onClear = { viewModel.onClearUrl() })
        }

        // Preview card — shown when OG succeeded + title or image populated
        if (ogFetchSucceeded && (title.isNotBlank() || imageUrl.isNotBlank())) {
            ItemPreviewCard(imageUrl = imageUrl, title = title, price = price, url = url)
        }

        // Title field with inline AutoFillTag
        Column(verticalArrangement = Arrangement.spacedBy(spacing.gap4)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.item_title_label),
                    style = typography.bodyS,
                    color = colors.inkSoft,
                )
                if (ogFetchSucceeded && title.isNotBlank()) {
                    Spacer(Modifier.width(spacing.gap8))
                    AutoFillTag()
                }
            }
            OutlinedTextField(
                value = title,
                onValueChange = { viewModel.title.value = it },
                shape = shapes.radius12,
                modifier = Modifier.fillMaxWidth(),
                colors = giftMaisonFieldColors(),
            )
        }

        // Notes field
        OutlinedTextField(
            value = notes,
            onValueChange = { viewModel.notes.value = it },
            label = { Text(stringResource(R.string.item_notes_label)) },
            placeholder = { Text(stringResource(R.string.item_notes_hint_detail)) },
            shape = shapes.radius12,
            minLines = 2,
            maxLines = 4,
            modifier = Modifier.fillMaxWidth(),
            colors = giftMaisonFieldColors(),
        )

        if (showAffiliateRow) {
            InfoPill(domain = domain)
        }

        Spacer(Modifier.height(spacing.gap20))
    }
}

@Composable
private fun ManualModeContent(
    viewModel: AddItemViewModel,
    title: String,
    imageUrl: String,
    price: String,
    notes: String,
) {
    val spacing = GiftMaisonTheme.spacing
    val shapes = GiftMaisonTheme.shapes
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.edge),
        verticalArrangement = Arrangement.spacedBy(spacing.gap12),
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = { viewModel.title.value = it },
            label = { Text(stringResource(R.string.item_title_label)) },
            shape = shapes.radius12,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = giftMaisonFieldColors(),
        )
        OutlinedTextField(
            value = price,
            onValueChange = { viewModel.price.value = it },
            label = { Text(stringResource(R.string.item_price_label)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = shapes.radius12,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = giftMaisonFieldColors(),
        )
        OutlinedTextField(
            value = imageUrl,
            onValueChange = { viewModel.imageUrl.value = it },
            label = { Text(stringResource(R.string.item_image_label)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            shape = shapes.radius12,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = giftMaisonFieldColors(),
        )
        OutlinedTextField(
            value = notes,
            onValueChange = { viewModel.notes.value = it },
            label = { Text(stringResource(R.string.item_notes_label)) },
            shape = shapes.radius12,
            minLines = 2,
            maxLines = 4,
            modifier = Modifier.fillMaxWidth(),
            colors = giftMaisonFieldColors(),
        )
        Spacer(Modifier.height(spacing.gap20))
    }
}

@Composable
private fun giftMaisonFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = GiftMaisonTheme.colors.paperDeep,
    unfocusedContainerColor = GiftMaisonTheme.colors.paperDeep,
    focusedBorderColor = GiftMaisonTheme.colors.accent,
    unfocusedBorderColor = GiftMaisonTheme.colors.line,
    focusedLabelColor = GiftMaisonTheme.colors.accent,
    unfocusedLabelColor = GiftMaisonTheme.colors.inkFaint,
    cursorColor = GiftMaisonTheme.colors.accent,
    focusedTextColor = GiftMaisonTheme.colors.ink,
    unfocusedTextColor = GiftMaisonTheme.colors.ink,
)
