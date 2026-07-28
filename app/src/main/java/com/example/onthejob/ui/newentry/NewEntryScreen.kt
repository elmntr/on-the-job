package com.example.onthejob.ui.newentry

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.onthejob.data.upload.PhotoUploadState
import com.example.onthejob.data.upload.PickedPhoto
import com.example.onthejob.ui.components.PrimaryButton
import com.example.onthejob.ui.components.dashedBorder
import com.example.onthejob.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun NewEntryScreen(onBack: () -> Unit, viewModel: NewEntryViewModel = viewModel()) {
    val photos by viewModel.photos.collectAsState()
    val description by viewModel.description.collectAsState()
    val hours by viewModel.hours.collectAsState()
    val saveState by viewModel.saveState.collectAsState()

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 15),
    ) { uris -> if (uris.isNotEmpty()) viewModel.onPhotosPicked(uris) }

    LaunchedEffect(saveState) {
        if (saveState is SaveState.Saved) {
            onBack()
        }
    }

    val dateLabel = remember {
        SimpleDateFormat("MMM d", Locale.getDefault()).format(Calendar.getInstance().time).uppercase()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Ink)
            }
            Text(
                "New entry",
                fontFamily = CondFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Ink,
            )
            Spacer(Modifier.weight(1f))
            Text(
                dateLabel,
                fontFamily = MonoFontFamily,
                fontSize = 10.sp,
                color = Muted,
            )
        }

        Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
            FieldLabel("Photos · ${photos.size} / 15")
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
            FieldLabel("Description")
            Spacer(Modifier.height(7.dp))
            OutlinedTextField(
                value = description,
                onValueChange = viewModel::onDescriptionChanged,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("What did you do today?", color = Muted) },
                minLines = 3,
                maxLines = 6,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CardSurface,
                    unfocusedContainerColor = CardSurface,
                    focusedBorderColor = Ink,
                    unfocusedBorderColor = Line,
                    focusedTextColor = Ink2,
                    unfocusedTextColor = Ink2,
                ),
            )

            var hoursText by remember { mutableStateOf("") }
            LaunchedEffect(Unit) {
                hoursText = if (hours == hours.toLong().toDouble()) hours.toLong().toString() else hours.toString()
            }

            Spacer(Modifier.height(12.dp))
            FieldLabel("Hours today")
            Spacer(Modifier.height(7.dp))
            OutlinedTextField(
                value = hoursText,
                onValueChange = { input ->
                    hoursText = input
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
                shape = RoundedCornerShape(12.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = MonoFontFamily, fontSize = 16.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CardSurface,
                    unfocusedContainerColor = CardSurface,
                    focusedBorderColor = Ink,
                    unfocusedBorderColor = Line,
                    focusedTextColor = Ink,
                    unfocusedTextColor = Ink,
                ),
            )

            if (saveState is SaveState.Error) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = (saveState as SaveState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    fontFamily = BodyFontFamily,
                    fontSize = 11.sp,
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        Box(modifier = Modifier.padding(16.dp)) {
            PrimaryButton(
                text = "Save entry",
                onClick = { viewModel.saveEntry() },
                enabled = saveState !is SaveState.Saving,
                loading = saveState is SaveState.Saving,
            )
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text.uppercase(),
        fontFamily = CondFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        letterSpacing = 0.6.sp,
        color = Muted,
    )
}

@Composable
private fun AddTile(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .dashedBorder(color = Muted, cornerRadius = 8.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.Add, contentDescription = "Add photo", tint = Muted, modifier = Modifier.size(16.dp))
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
                    color = Amber,
                )
            }
            is PhotoUploadState.Failed -> {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(4.dp))
                        .clickable(onClick = onRetry)
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                ) {
                    Text("Retry", color = Color.White, fontFamily = CondFontFamily, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
            is PhotoUploadState.Success -> {}
            PhotoUploadState.Pending -> {}
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(3.dp)
                .size(18.dp)
                .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(50))
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Text("×", color = Color.White, fontSize = 12.sp)
        }
    }
}