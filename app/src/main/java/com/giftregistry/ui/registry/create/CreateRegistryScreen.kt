package com.giftregistry.ui.registry.create

import android.app.DatePickerDialog
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.giftregistry.R
import com.giftregistry.ui.navigation.hiltViewModelWithNavArgs
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRegistryScreen(
    registryId: String? = null,
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
    viewModel: CreateRegistryViewModel = hiltViewModelWithNavArgs(
        // "new" keeps the create-mode VM distinct from any edit-mode VM cached under a real id.
        key = registryId ?: "new",
        "registryId" to registryId,
    )
) {
    val title by viewModel.title.collectAsStateWithLifecycle()
    val occasion by viewModel.occasion.collectAsStateWithLifecycle()
    val eventDateMs by viewModel.eventDateMs.collectAsStateWithLifecycle()
    val eventLocation by viewModel.eventLocation.collectAsStateWithLifecycle()
    val description by viewModel.description.collectAsStateWithLifecycle()
    val visibility by viewModel.visibility.collectAsStateWithLifecycle()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val savedRegistryId by viewModel.savedRegistryId.collectAsStateWithLifecycle()

    val context = LocalContext.current

    LaunchedEffect(savedRegistryId) {
        savedRegistryId?.let { id -> onSaved(id) }
    }

    val occasionOptions = listOf(
        stringResource(R.string.registry_occasion_wedding),
        stringResource(R.string.registry_occasion_baby_shower),
        stringResource(R.string.registry_occasion_anniversary),
        stringResource(R.string.registry_occasion_christmas),
        stringResource(R.string.registry_occasion_birthday),
        stringResource(R.string.registry_occasion_custom)
    )

    var occasionMenuExpanded by remember { mutableStateOf(false) }

    val dateFormatter = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (viewModel.isEditMode) {
                            stringResource(R.string.registry_edit_title)
                        } else {
                            stringResource(R.string.registry_create_title)
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { viewModel.title.value = it },
                label = { Text(stringResource(R.string.registry_title_label)) },
                placeholder = { Text(stringResource(R.string.registry_title_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = error != null && title.isBlank()
            )

            Spacer(modifier = Modifier.height(12.dp))

            ExposedDropdownMenuBox(
                expanded = occasionMenuExpanded,
                onExpandedChange = { occasionMenuExpanded = it }
            ) {
                OutlinedTextField(
                    value = occasion,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.registry_occasion_label)) },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = occasionMenuExpanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = occasionMenuExpanded,
                    onDismissRequest = { occasionMenuExpanded = false }
                ) {
                    occasionOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                viewModel.occasion.value = option
                                occasionMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = if (eventDateMs != null) dateFormatter.format(Date(eventDateMs!!)) else "",
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.registry_event_date_label)) },
                trailingIcon = {
                    IconButton(
                        onClick = {
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
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = stringResource(R.string.registry_event_date_label)
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = eventLocation,
                onValueChange = { viewModel.eventLocation.value = it },
                label = { Text(stringResource(R.string.registry_event_location_label)) },
                placeholder = { Text(stringResource(R.string.registry_event_location_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { viewModel.description.value = it },
                label = { Text(stringResource(R.string.registry_description_label)) },
                placeholder = { Text(stringResource(R.string.registry_description_hint)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.registry_visibility_label),
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = visibility == "public",
                    onClick = { viewModel.visibility.value = "public" }
                )
                Text(
                    text = stringResource(R.string.registry_visibility_public),
                    modifier = Modifier.padding(end = 16.dp)
                )
                RadioButton(
                    selected = visibility == "private",
                    onClick = { viewModel.visibility.value = "private" }
                )
                Text(stringResource(R.string.registry_visibility_private))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.registry_notifications_label),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(R.string.registry_notifications_enabled),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = { viewModel.notificationsEnabled.value = it }
                )
            }

            if (error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = error ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.onSave() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(stringResource(R.string.common_save))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
