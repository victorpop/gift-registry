package com.giftregistry.ui.item.edit

import android.content.Intent
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.giftregistry.R
import com.giftregistry.domain.model.ItemStatus
import com.giftregistry.ui.navigation.hiltViewModelWithNavArgs
import com.giftregistry.ui.registry.detail.GuestIdentitySheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditItemScreen(
    registryId: String,
    itemId: String,
    onBack: () -> Unit,
    viewModel: EditItemViewModel = hiltViewModelWithNavArgs(
        key = "$registryId/$itemId",
        "registryId" to registryId,
        "itemId" to itemId,
    )
) {
    val url by viewModel.url.collectAsStateWithLifecycle()
    val title by viewModel.title.collectAsStateWithLifecycle()
    val imageUrl by viewModel.imageUrl.collectAsStateWithLifecycle()
    val price by viewModel.price.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isFetchingOg by viewModel.isFetchingOg.collectAsStateWithLifecycle()
    val ogFetchFailed by viewModel.ogFetchFailed.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val savedSuccessfully by viewModel.savedSuccessfully.collectAsStateWithLifecycle()
    // quick-260507-vrp — drives the dual-mode UI: owner = full-edit form;
    // invitee = read-only fields + Reserve / Mark-as-purchased actions.
    val isOwner by viewModel.isOwner.collectAsStateWithLifecycle()
    val activeReservationId by viewModel.activeReservationId.collectAsStateWithLifecycle()
    val isReserving by viewModel.isReserving.collectAsStateWithLifecycle()
    val confirmingPurchase by viewModel.confirmingPurchase.collectAsStateWithLifecycle()
    val item by viewModel.itemFlow.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showGuestSheet by remember { mutableStateOf(false) }

    val unavailableMsg = stringResource(R.string.reservation_error_unavailable)
    val genericErrorMsg = stringResource(R.string.reservation_error_generic)

    LaunchedEffect(savedSuccessfully) {
        if (savedSuccessfully) onBack()
    }

    // quick-260507-vrp — collect reservation events and dispatch to UI side
    // effects. Mirrors RegistryDetailScreen.kt:137-155 verbatim (Intent.ACTION_VIEW
    // for OpenRetailer, sheet open for ShowGuestSheet, snackbar for ShowConflictError).
    LaunchedEffect(Unit) {
        viewModel.reservationEvents.collect { event ->
            when (event) {
                is EditItemViewModel.ReservationEvent.OpenRetailer -> {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, event.affiliateUrl.toUri()))
                    }
                }
                EditItemViewModel.ReservationEvent.ShowGuestSheet -> {
                    showGuestSheet = true
                }
                is EditItemViewModel.ReservationEvent.ShowConflictError -> {
                    val msg = if (event.code == "ITEM_UNAVAILABLE") unavailableMsg else genericErrorMsg
                    snackbarHostState.showSnackbar(msg)
                }
            }
        }
    }

    // quick-260507-vrp — collect confirm-purchase snackbar resIds. On success
    // (R.string.reservation_confirm_purchase_success) the screen pops back so
    // the invitee returns to RegistryDetailScreen and sees the status flip.
    LaunchedEffect(Unit) {
        viewModel.snackbarMessages.collect { resId ->
            snackbarHostState.showSnackbar(context.getString(resId))
            if (resId == R.string.reservation_confirm_purchase_success) {
                onBack()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.item_edit_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // quick-260507-vrp — dual-mode rendering. Owner branch is THE
            // existing form body verbatim, no behavioural change. Invitee
            // branch renders the same fields with enabled=false (no Save,
            // no Delete, no Fetch button). Task 3 appends Reserve +
            // Mark-as-purchased action buttons to the invitee branch.
            if (isOwner) {
                // ---- OWNER MODE: full edit (unchanged from pre-vrp) ----
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = url,
                        onValueChange = { viewModel.url.value = it },
                        label = { Text(stringResource(R.string.item_add_url_label)) },
                        placeholder = { Text(stringResource(R.string.item_add_url_hint)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedButton(
                        onClick = { viewModel.onFetchMetadata() },
                        enabled = !isFetchingOg && url.isNotBlank()
                    ) {
                        Text("Fetch")
                    }
                }

                if (isFetchingOg) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        Text(
                            text = stringResource(R.string.item_add_fetching_metadata),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                if (ogFetchFailed) {
                    Text(
                        text = stringResource(R.string.item_og_fetch_failed),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { viewModel.title.value = it },
                    label = { Text(stringResource(R.string.item_title_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = price,
                    onValueChange = { viewModel.price.value = it },
                    label = { Text(stringResource(R.string.item_price_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (imageUrl.isNotBlank()) {
                    val previewFallback = rememberVectorPainter(Icons.Default.Image)
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = stringResource(R.string.item_image_content_desc),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit,
                        placeholder = previewFallback,
                        error = previewFallback,
                        fallback = previewFallback,
                    )
                }

                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { viewModel.imageUrl.value = it },
                    label = { Text(stringResource(R.string.item_image_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { viewModel.notes.value = it },
                    label = { Text(stringResource(R.string.item_notes_label)) },
                    placeholder = { Text(stringResource(R.string.item_notes_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )

                if (error != null) {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Button(
                    onClick = viewModel::onSave,
                    enabled = !isSaving && !isFetchingOg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(16.dp)
                                .padding(end = 8.dp)
                        )
                    }
                    Text(stringResource(R.string.common_save))
                }

                Spacer(modifier = Modifier.height(16.dp))
            } else {
                // ---- INVITEE MODE: read-only fields (no Save, no Delete, no Fetch) ----
                // Skips the URL+Fetch Row, isFetchingOg / ogFetchFailed indicators,
                // and the `error` text — all of those are owner-side concerns
                // (only onSave + onFetchMetadata produce them, and invitees
                // call neither). Task 3 appends Reserve + Mark-as-purchased.
                OutlinedTextField(
                    value = title,
                    onValueChange = {},
                    label = { Text(stringResource(R.string.item_title_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = false,
                )

                OutlinedTextField(
                    value = price,
                    onValueChange = {},
                    label = { Text(stringResource(R.string.item_price_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = false,
                )

                if (imageUrl.isNotBlank()) {
                    val previewFallback = rememberVectorPainter(Icons.Default.Image)
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = stringResource(R.string.item_image_content_desc),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit,
                        placeholder = previewFallback,
                        error = previewFallback,
                        fallback = previewFallback,
                    )
                }

                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = {},
                    label = { Text(stringResource(R.string.item_image_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = false,
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = {},
                    label = { Text(stringResource(R.string.item_notes_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    enabled = false,
                )

                OutlinedTextField(
                    value = url,
                    onValueChange = {},
                    label = { Text(stringResource(R.string.item_add_url_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = false,
                )

                Spacer(modifier = Modifier.height(16.dp))

                // quick-260507-vrp Task 3 — Reserve + Mark-as-purchased actions.
                // Reuses the SAME ReserveItemUseCase + ConfirmPurchaseUseCase
                // pipeline as RegistryDetailScreen — zero new reserve/purchase
                // code paths. Status gates mirror RegistryDetailScreen's
                // existing reserve gating (item.status == AVAILABLE) and the
                // ConfirmPurchaseBanner gate (RESERVED && activeReservationId != null).
                val currentItem = item
                val canReserve = currentItem?.status == ItemStatus.AVAILABLE && !isReserving
                val canConfirmPurchase = currentItem?.status == ItemStatus.RESERVED &&
                    activeReservationId != null && !confirmingPurchase

                Button(
                    onClick = { viewModel.onReserveClicked(itemId) },
                    enabled = canReserve,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (isReserving) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(16.dp)
                                .padding(end = 8.dp),
                        )
                    }
                    Text(stringResource(R.string.reservation_reserve_button))
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        val rid = activeReservationId
                        if (rid != null) viewModel.onConfirmPurchase(rid)
                    },
                    enabled = canConfirmPurchase,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (confirmingPurchase) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(16.dp)
                                .padding(end = 8.dp),
                        )
                    }
                    Text(stringResource(R.string.reservation_confirm_purchase_cta))
                }
            }
        }
    }

    // quick-260507-vrp — guest identity sheet, mirrors RegistryDetailScreen.kt:384-393.
    // Triggered by ReservationEvent.ShowGuestSheet when the invitee taps Reserve
    // without a saved guest identity in DataStore. After submit, the sheet flips
    // back, the VM persists the GuestUser, and performReservation runs with it.
    if (showGuestSheet) {
        GuestIdentitySheet(
            initial = null,
            onDismiss = { showGuestSheet = false },
            onSubmit = { guest ->
                showGuestSheet = false
                viewModel.onGuestIdentitySubmitted(guest)
            },
        )
    }
}
