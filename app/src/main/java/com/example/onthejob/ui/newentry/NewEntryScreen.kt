package com.example.onthejob.ui.newentry

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.onthejob.data.upload.PhotoUploadState
import com.example.onthejob.data.upload.PickedPhoto
import com.example.onthejob.ui.theme.CardSurface
import com.example.onthejob.ui.theme.Ink
import com.example.onthejob.ui.theme.Muted
import com.example.onthejob.ui.theme.Paper

@Composable
fun NewEntryScreen(onBack: () -> Unit, viewModel: NewEntryViewModel = viewModel()) {
    val photos by viewModel.photos.collectAsState()
    val description by viewModel.description.collectAsState()
    val hours by viewModel.hours.collectAsState()
    val saveState by viewModel.saveState.collectAsState()

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 15),
    ) { uris -> if (uris.isNotEmpty()) viewModel.onPhotosPicked(uris) }

    // Navigate back once the entry is actually saved — not just on tap.
    LaunchedEffect(saveState) {
        if (saveState is SaveState.Saved) {
            onBack()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
            .padding(16.dp),
    ) {
        Text("Photos · ${photos.size} / 15")
        Spacer(Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item {
                AddTile(onClick = {
                    pickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                })
            }
            items(photos, key = { it.uri.toString() }) { photo ->
                PhotoTile(
                    photo = photo,
                    onRetry = { viewModel.retry(photo.uri) },
                    onRemove = { viewModel.removePhoto(photo.uri) },
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Description", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = description,
            onValueChange = viewModel::onDescriptionChanged,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("What did you do today?") },
            minLines = 3,
            maxLines = 6,
        )

        Spacer(Modifier.height(12.dp))
        Text("Hours today", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = if (hours == hours.toLong().toDouble()) hours.toLong().toString() else hours.toString(),
            onValueChange = { input ->
                val parsed = input.toDoubleOrNull()
                if (input.isEmpty()) {
                    viewModel.onHoursChanged(0.0)
                } else if (parsed != null) {
                    viewModel.onHoursChanged(parsed)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
        )

        if (saveState is SaveState.Error) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = (saveState as SaveState.Error).message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { viewModel.saveEntry() },
            enabled = saveState !is SaveState.Saving,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (saveState is SaveState.Saving) {
                CircularProgressIndicator(
                    modifier = Modifier.height(18.dp),
                    strokeWidth = 2.dp,
                    color = Color.White,
                )
            } else {
                Text("Save entry")
            }
        }
    }
}

@Composable
private fun AddTile(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(CardSurface),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(onClick = onClick) { Text("+") }
    }
}

@Composable
private fun PhotoTile(photo: PickedPhoto, onRetry: () -> Unit, onRemove: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp)),
    ) {
        AsyncImage(
            model = photo.uri,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )
        when (val state = photo.state) {
            is PhotoUploadState.Uploading -> {
                LinearProgressIndicator(
                    progress = { state.progress },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                )
            }
            is PhotoUploadState.Failed -> {
                IconButton(
                    onClick = onRetry,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp)),
                ) {
                    Text("Retry", color = Color.White)
                }
            }
            is PhotoUploadState.Success -> {
                // Uploaded — no overlay needed, thumbnail alone is enough per the mockup's design
            }
            PhotoUploadState.Pending -> {}
        }
        IconButton(
            onClick = onRemove,
            modifier = Modifier.align(Alignment.TopEnd),
        ) {
            Text("×", color = Ink)
        }
    }
}