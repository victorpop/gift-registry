package com.giftregistry.ui.item.add

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.giftregistry.R
import com.giftregistry.ui.navigation.hiltViewModelWithNavArgs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemScreen(
    registryId: String,
    initialUrl: String? = null,
    initialRegistryId: String? = null,
    onBack: () -> Unit,
    viewModel: AddItemViewModel = hiltViewModelWithNavArgs(
        key = registryId,
        "registryId" to registryId,
        "initialUrl" to (initialUrl ?: ""),
        "initialRegistryId" to (initialRegistryId ?: ""),
    )
) {
    val url by viewModel.url.collectAsStateWithLifecycle()
    val title by viewModel.title.collectAsStateWithLifecycle()
    val imageUrl by viewModel.imageUrl.collectAsStateWithLifecycle()
    val price by viewModel.price.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val isFetchingOg by viewModel.isFetchingOg.collectAsStateWithLifecycle()
    val ogFetchFailed by viewModel.ogFetchFailed.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val savedItemId by viewModel.savedItemId.collectAsStateWithLifecycle()

    LaunchedEffect(savedItemId) {
        if (savedItemId != null) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.item_add_title)) },
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = url,
                    onValueChange = { viewModel.onUrlChanged(it) },
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
        }
    }
}
