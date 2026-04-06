package com.giftregistry.ui.registry.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.giftregistry.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRegistryScreen(
    registryId: String? = null,
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
    viewModel: CreateRegistryViewModel = hiltViewModel()
) {
    val title by viewModel.title.collectAsStateWithLifecycle()
    val occasion by viewModel.occasion.collectAsStateWithLifecycle()
    val eventLocation by viewModel.eventLocation.collectAsStateWithLifecycle()
    val description by viewModel.description.collectAsStateWithLifecycle()
    val visibility by viewModel.visibility.collectAsStateWithLifecycle()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val savedRegistryId by viewModel.savedRegistryId.collectAsStateWithLifecycle()

    LaunchedEffect(savedRegistryId) {
        savedRegistryId?.let { id -> onSaved(id) }
    }

    val occasions = listOf(
        "wedding" to stringResource(R.string.registry_occasion_wedding),
        "baby_shower" to stringResource(R.string.registry_occasion_baby_shower),
        "anniversary" to stringResource(R.string.registry_occasion_anniversary),
        "christmas" to stringResource(R.string.registry_occasion_christmas),
        "birthday" to stringResource(R.string.registry_occasion_birthday),
        "custom" to stringResource(R.string.registry_occasion_custom)
    )

    var occasionExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (viewModel.isEditMode) stringResource(R.string.registry_edit_title)
                        else stringResource(R.string.registry_create_title)
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { viewModel.title.value = it },
                label = { Text(stringResource(R.string.registry_title_label)) },
                placeholder = { Text(stringResource(R.string.registry_title_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            ExposedDropdownMenuBox(
                expanded = occasionExpanded,
                onExpandedChange = { occasionExpanded = it }
            ) {
                OutlinedTextField(
                    value = occasions.firstOrNull { it.first == occasion }?.second ?: occasion,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.registry_occasion_label)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = occasionExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = occasionExpanded,
                    onDismissRequest = { occasionExpanded = false }
                ) {
                    occasions.forEach { (value, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                viewModel.occasion.value = value
                                occasionExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = eventLocation,
                onValueChange = { viewModel.eventLocation.value = it },
                label = { Text(stringResource(R.string.registry_event_location_label)) },
                placeholder = { Text(stringResource(R.string.registry_event_location_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = description,
                onValueChange = { viewModel.description.value = it },
                label = { Text(stringResource(R.string.registry_description_label)) },
                placeholder = { Text(stringResource(R.string.registry_description_hint)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )

            Text(
                text = stringResource(R.string.registry_visibility_label),
                style = MaterialTheme.typography.labelLarge
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = visibility == "public",
                    onClick = { viewModel.visibility.value = "public" }
                )
                Text(
                    text = stringResource(R.string.registry_visibility_public),
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = visibility == "private",
                    onClick = { viewModel.visibility.value = "private" }
                )
                Text(
                    text = stringResource(R.string.registry_visibility_private),
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.registry_notifications_label),
                        style = MaterialTheme.typography.bodyMedium
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
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = viewModel::onSave,
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                }
                Text(stringResource(R.string.common_save))
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
