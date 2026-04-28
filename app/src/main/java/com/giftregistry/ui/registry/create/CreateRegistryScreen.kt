package com.giftregistry.ui.registry.create

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.giftregistry.R
import com.giftregistry.ui.navigation.hiltViewModelWithNavArgs
import com.giftregistry.ui.registry.cover.CoverPhotoPickerInline
import com.giftregistry.ui.registry.cover.CoverPhotoPickerSheet
import com.giftregistry.ui.registry.cover.isCoverPickerEnabled
import com.giftregistry.ui.theme.GiftMaisonTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * SCR-09 — Create / Edit Registry screen (Phase 11 GiftMaison re-skin).
 *
 * New in Phase 11:
 *  - Custom Row app bar (NOT Material3 TopAppBar): ← STEP 1 OF 2 Skip
 *  - Italic-accent "What's the / occasion?" headline
 *  - OccasionTileGrid (2×3) replaces ExposedDropdownMenuBox
 *  - VisibilityRadioCard replaces Material3 RadioButton pair
 *  - Scaffold bottomBar ink-pill "Continue · add items →" CTA
 *  - onSkip parameter for the Step 1 skip flow (default no-op → EditRegistryKey compat)
 *
 * Preserved verbatim:
 *  - CreateRegistryViewModel and all 11 StateFlow collections
 *  - Date picker dialog logic
 *  - hiltViewModelWithNavArgs key + registryId arg
 *  - LaunchedEffect(savedRegistryId) → branched on skipMode for onSaved vs onSkip
 *  - Error surface
 */
@Composable
fun CreateRegistryScreen(
    registryId: String? = null,
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
    onSkip: () -> Unit = {},          // NEW — Phase 11; default no-op so EditRegistryKey entry compiles unchanged
    viewModel: CreateRegistryViewModel = hiltViewModelWithNavArgs(
        // "new" keeps the create-mode VM distinct from any edit-mode VM cached under a real id.
        key = registryId ?: "new",
        "registryId" to registryId,
    ),
) {
    val title by viewModel.title.collectAsStateWithLifecycle()
    val occasion by viewModel.occasion.collectAsStateWithLifecycle()
    val eventDateMs by viewModel.eventDateMs.collectAsStateWithLifecycle()
    val eventLocation by viewModel.eventLocation.collectAsStateWithLifecycle()
    val visibility by viewModel.visibility.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val savedRegistryId by viewModel.savedRegistryId.collectAsStateWithLifecycle()
    val coverPhotoSelection by viewModel.coverPhotoSelection.collectAsStateWithLifecycle()

    val colors = GiftMaisonTheme.colors
    val typography = GiftMaisonTheme.typography
    val shapes = GiftMaisonTheme.shapes
    val spacing = GiftMaisonTheme.spacing

    val context = LocalContext.current
    val dateFormatter = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    // Skip mode: set true when Skip button is tapped; LaunchedEffect routes to onSkip instead of onSaved.
    var skipMode by remember { mutableStateOf(false) }
    val untitledDraftLabel = stringResource(R.string.registry_create_untitled_draft)

    // Phase 12 D-09 / D-12 — cover-photo picker sheet open state. rememberSaveable
    // so it survives configuration changes / process death (Pitfall 4).
    var pickerSheetOpen by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(savedRegistryId) {
        savedRegistryId?.let {
            if (skipMode) onSkip() else onSaved(it)
        }
    }

    Scaffold(
        containerColor = colors.paper,
        topBar = {
            // Custom Row — NOT Material3 TopAppBar (UI-SPEC § Top App Bar)
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(spacing.gap8),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                            tint = colors.ink,
                        )
                    }
                    if (!viewModel.isEditMode) {
                        // Create mode: centered "STEP 1 OF 2" + "Skip" right
                        Text(
                            text = stringResource(R.string.registry_create_step_indicator),
                            style = typography.monoCaps,
                            color = colors.inkFaint,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = {
                            // Skip: set placeholder title so VM validation (3..50) passes.
                            // Phase 10's isDraft(itemCount == 0) heuristic still classifies
                            // as Draft since itemCount is always 0 immediately after create.
                            if (viewModel.title.value.isBlank()) {
                                viewModel.title.value = untitledDraftLabel
                            }
                            skipMode = true
                            viewModel.onSave()
                        }) {
                            Text(
                                text = stringResource(R.string.registry_create_skip),
                                style = typography.bodyM,
                                color = colors.inkSoft,
                            )
                        }
                    } else {
                        // Edit mode: centered registry_edit_title label; no Skip button
                        Text(
                            text = stringResource(R.string.registry_edit_title),
                            style = typography.bodyL,
                            color = colors.ink,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f),
                        )
                        // Placeholder box to balance the back button
                        Box(Modifier.size(48.dp))
                    }
                }
                HorizontalDivider(thickness = 1.dp, color = colors.line)
            }
        },
        bottomBar = {
            Column {
                HorizontalDivider(thickness = 1.dp, color = colors.line)
                Box(
                    modifier = Modifier
                        .background(colors.paper)
                        .padding(vertical = spacing.gap12, horizontal = spacing.gap20),
                ) {
                    Button(
                        onClick = {
                            skipMode = false
                            viewModel.onSave()
                        },
                        enabled = !isSaving,
                        modifier = Modifier.fillMaxWidth(),
                        shape = shapes.pill,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.ink,
                            contentColor = colors.paper,
                        ),
                    ) {
                        if (isSaving && !skipMode) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = colors.paper,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(
                                text = if (viewModel.isEditMode) {
                                    stringResource(R.string.registry_update_cta)
                                } else {
                                    stringResource(R.string.registry_create_cta)
                                },
                                style = typography.bodyMEmphasis,
                            )
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(top = spacing.gap20),
            verticalArrangement = Arrangement.spacedBy(spacing.gap20),
        ) {
            // Italic-accent headline (UI-SPEC § Headline)
            Column(modifier = Modifier.padding(horizontal = spacing.edge)) {
                Text(
                    text = stringResource(R.string.registry_create_headline_prefix),
                    style = typography.displayS,
                    color = colors.ink,
                )
                Text(
                    text = stringResource(R.string.registry_create_headline_accent),
                    style = typography.displayS.copy(fontStyle = FontStyle.Italic),
                    color = colors.accent,
                )
                Spacer(Modifier.height(spacing.gap8))
                Text(
                    text = stringResource(R.string.registry_create_subline),
                    style = typography.bodyM,
                    color = colors.inkSoft,
                )
            }

            // Phase 12 D-09 — Cover photo picker (inline 16:9 preview block).
            // Inserted ABOVE the occasion tile grid per the cover-photo decision
            // doc. Disabled until an occasion is set (D-12). Tap opens the
            // ModalBottomSheet picker hosted at the bottom of this Composable.
            Column(modifier = Modifier.padding(horizontal = spacing.edge)) {
                Text(
                    text = stringResource(R.string.cover_photo_label),
                    style = typography.monoCaps,
                    color = colors.inkFaint,
                    modifier = Modifier.padding(bottom = spacing.gap8),
                )
                CoverPhotoPickerInline(
                    occasion = occasion,
                    selection = coverPhotoSelection,
                    onTap = { pickerSheetOpen = true },
                    disabledHint = stringResource(R.string.cover_photo_pick_occasion_first),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // 2×3 Occasion tile grid (Task 1 composable)
            OccasionTileGrid(
                selectedOccasion = occasion,
                onOccasionSelected = { viewModel.occasion.value = it },
            )

            // Form fields
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.edge),
                verticalArrangement = Arrangement.spacedBy(spacing.gap14),
            ) {
                // Registry name
                OutlinedTextField(
                    value = title,
                    onValueChange = { viewModel.title.value = it },
                    label = { Text(stringResource(R.string.registry_title_label)) },
                    placeholder = { Text(stringResource(R.string.registry_title_hint)) },
                    shape = shapes.radius12,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = giftMaisonFieldColors(),
                )

                // Date / Time row (2-column grid)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.gap10),
                ) {
                    // Date field — existing DatePickerDialog logic preserved
                    OutlinedTextField(
                        value = eventDateMs?.let { dateFormatter.format(Date(it)) } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.registry_event_date_label)) },
                        shape = shapes.radius12,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                val calendar = Calendar.getInstance()
                                eventDateMs?.let { calendar.timeInMillis = it }
                                DatePickerDialog(
                                    context,
                                    { _, year, month, day ->
                                        val cal = Calendar.getInstance()
                                        cal.set(year, month, day, 0, 0, 0)
                                        cal.set(Calendar.MILLISECOND, 0)
                                        viewModel.eventDateMs.value = cal.timeInMillis
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH),
                                ).show()
                            },
                        colors = giftMaisonFieldColors(),
                    )
                    // Time field — placeholder (viewModel.eventTimeMs not in VM v1.1; deferred to v1.2)
                    // TODO v1.2: wire to viewModel.eventTimeMs when the field ships
                    OutlinedTextField(
                        value = "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.registry_event_time_label)) },
                        shape = shapes.radius12,
                        modifier = Modifier.weight(1f),
                        enabled = false,
                        colors = giftMaisonFieldColors(),
                    )
                }

                // Place field
                OutlinedTextField(
                    value = eventLocation,
                    onValueChange = { viewModel.eventLocation.value = it },
                    label = { Text(stringResource(R.string.registry_event_location_label)) },
                    placeholder = { Text(stringResource(R.string.registry_event_location_hint)) },
                    shape = shapes.radius12,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = giftMaisonFieldColors(),
                )
            }

            // Visibility radio card (Task 1 composable)
            VisibilityRadioCard(
                selectedVisibility = visibility,
                onVisibilitySelected = { viewModel.visibility.value = it },
            )

            // Error surface (preserved)
            error?.let { err ->
                Text(
                    text = err,
                    style = typography.bodyS,
                    color = colors.warn,
                    modifier = Modifier.padding(horizontal = spacing.edge),
                )
            }

            Spacer(Modifier.height(spacing.gap20))
        }
    }

    // Phase 12 D-10 — ModalBottomSheet host for the cover-photo picker.
    // Gated on isCoverPickerEnabled(occasion) (D-12) so we never open the sheet
    // without an occasion. Sheet is sibling to the Scaffold so it renders on top
    // with its own scrim.
    if (pickerSheetOpen && isCoverPickerEnabled(occasion)) {
        CoverPhotoPickerSheet(
            occasion = occasion,
            currentSelection = coverPhotoSelection,
            onSelectionChanged = { newSelection ->
                viewModel.coverPhotoSelection.value = newSelection
            },
            onDismiss = { pickerSheetOpen = false },
            headerText = stringResource(R.string.cover_photo_sheet_header),
            pickFromGalleryText = stringResource(R.string.cover_photo_pick_from_gallery),
            removeText = stringResource(R.string.cover_photo_remove),
        )
    }
}

/**
 * GiftMaison-themed OutlinedTextField colours.
 * paperDeep container, accent focused border/label, line unfocused border.
 * Uses OutlinedTextFieldDefaults.colors (M3 3.x API — not deprecated TextFieldDefaults.outlinedTextFieldColors).
 */
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
    disabledTextColor = GiftMaisonTheme.colors.inkFaint,
    disabledContainerColor = GiftMaisonTheme.colors.paperDeep,
    disabledBorderColor = GiftMaisonTheme.colors.line,
    disabledLabelColor = GiftMaisonTheme.colors.inkFaint,
)
